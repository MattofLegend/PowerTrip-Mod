package com.powertrip.mod.client;

import com.powertrip.mod.PowerTripMod;
import com.powertrip.mod.config.ModConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.util.List;
import java.util.Random;

/**
 * Handles the visual roulette effect on the client side
 */
@Environment(EnvType.CLIENT)
public class RouletteDisplay {
    private static final Random random = new Random();
    private static boolean isClientAnimationActive = false;
    private static int displayResultTicks = 0;
    private static List<String> playerNames;
    private static String selectedPlayer;
    private static String currentDisplayName;
    private static int currentColorIndex = 0;
    private static boolean hasPlayedWinSound = false;
    private static long lastNameUpdateTime = 0; // Used for real-time based name cycling
    private static long animationStartTime = 0; // When the animation started
    private static int currentNameIndex = 0; // Index to track current position in player name list
    private static boolean animationPhaseComplete = false; // Tracks if roulette animation is done
    private static boolean resultPhaseComplete = false; // Tracks if result display is done
    private static boolean isPowerTripBeginning = true; // Whether this is a beginning (true) or ending (false) animation
    
    /**
     * Starts the roulette animation with the given player names
     * @param players List of player names to include in the roulette
     * @param selected The pre-selected winner (determined server-side)
     * @param isBeginning Whether this is a beginning (true) or ending (false) animation
     */
    public static void startRoulette(List<String> players, String selected, boolean isBeginning) {
        // If we already have an active animation, force it to complete first
        if (isClientAnimationActive) {
            PowerTripMod.LOGGER.info("[ANIMATION DEBUG] Forcing completion of previous animation before starting new one");
            // Force cleanup of the previous animation state
            isClientAnimationActive = false;
            resultPhaseComplete = true;
            displayResultTicks = ModConfig.RESULT_DISPLAY_DURATION;
        }
        
        // Now start the new animation with clean state
        PowerTripMod.LOGGER.info("[ANIMATION DEBUG] Starting roulette animation with " + players.size() + " players, selected: " + selected + ", isBeginning: " + isBeginning);
        playerNames = players;
        selectedPlayer = selected;
        isClientAnimationActive = true;
        displayResultTicks = 0;
        hasPlayedWinSound = false;
        resultPhaseComplete = false;
        isPowerTripBeginning = isBeginning;
        
        // For ending animations, skip the animation phase and go directly to the result
        if (!isBeginning) {
            animationPhaseComplete = true;
        } else {
            animationPhaseComplete = false;
        }
        PowerTripMod.LOGGER.info("[ANIMATION DEBUG] Animation state initialized at " + System.currentTimeMillis());
        
        // Start at a random position in the list for better unpredictability
        if (!players.isEmpty()) {
            currentNameIndex = new Random().nextInt(players.size());
            currentDisplayName = players.get(currentNameIndex);
        } else {
            currentDisplayName = "No players";
            currentNameIndex = 0;
        }
        
        currentColorIndex = 0;
        
        // Initialize animation timing
        long currentTime = System.currentTimeMillis();
        animationStartTime = currentTime; // Record when animation started
        lastNameUpdateTime = currentTime; // Initialize the name update timer
        
        // Play a sound to indicate the roulette has started
        // Play the sound using our helper method
        playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0F);
    }
    
    /**
     * Registers the HUD rendering callback and client tick callback
     */
    public static void register() {
        // Register HUD rendering for visual display
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            // Extract tick delta from the RenderTickCounter
            float tickDelta = tickCounter.getTickDelta(false);
            renderHud(drawContext, tickDelta);
        });
        
        // Register client tick event for proper tick-based timing
        // This ensures our counter increments at the fixed game tick rate (20 ticks/second)
        // instead of at the variable render frame rate
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tickClientAnimation();
        });
    }
    
    /**
     * Update animation state based on client game ticks
     * This runs at a fixed 20 ticks per second regardless of render FPS
     */
    private static void tickClientAnimation() {
        if (isClientAnimationActive && animationPhaseComplete && !resultPhaseComplete) {
            // Only increment during the result display phase
            displayResultTicks++;
            
            if (displayResultTicks % 20 == 0) { // Log every second (20 ticks)
                PowerTripMod.LOGGER.info("[ANIMATION DEBUG] Result display tick: " + displayResultTicks + "/" + ModConfig.RESULT_DISPLAY_DURATION);
            }
            
            // Check if we've reached the end of the display duration
            if (displayResultTicks >= ModConfig.RESULT_DISPLAY_DURATION) {
                PowerTripMod.LOGGER.info("[ANIMATION DEBUG] Animation ending at displayResultTicks=" + displayResultTicks);
                resultPhaseComplete = true;
                isClientAnimationActive = false;
            }
        }
    }
    
    /**
     * Renders the roulette animation and result on the HUD
     * @param drawContext The draw context
     * @param tickDelta The tick delta
     */
    private static void renderHud(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        
        // Only render if the client animation is active
        if (!isClientAnimationActive) {
            return;
        }
        
        // Log if we're in a game menu - this could affect rendering
        if (client.currentScreen != null) {
            PowerTripMod.LOGGER.info("[ANIMATION DEBUG] Animation running while screen open: " + client.currentScreen.getClass().getSimpleName());
        }
        
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        MatrixStack matrices = drawContext.getMatrices();
        
        // Get current time in milliseconds for real-time animation 
        long currentTime = System.currentTimeMillis();
        
        // Calculate how long the animation has been running
        long animationElapsedTime = currentTime - animationStartTime;
        long animationDurationMs = ModConfig.ROULETTE_DURATION_TICKS * 50; // Convert ticks to ms (1 tick = 50ms)
        
        // Render the roulette animation if not yet at the end of duration and animation phase not complete
        if (animationElapsedTime < animationDurationMs && !animationPhaseComplete) {
            // Calculate animation progress percentage (0-100)
            int animationProgress = (int)((animationElapsedTime * 100) / animationDurationMs);
            
            // Use real-time based cycling regardless of frame rate
            // Each name should display for about 500ms (1/2 second) in early stages
            long nameTimeElapsed = currentTime - lastNameUpdateTime;
            
            // Determine name change delay based on animation progress
            long nameChangeDelay;
            if (animationProgress > 80) {
                nameChangeDelay = 1000; // Slow near the end (1 second per name)
            } else if (animationProgress > 60) {
                nameChangeDelay = 500; // Medium speed in the middle (2 names per second)
            } else {
                nameChangeDelay = 250; // Fast cycling at the start (4 names per second)
            }
            
            // Change the displayed name based on the appropriate delay
            if (nameTimeElapsed >= nameChangeDelay) {
                // Reset the name update timer
                lastNameUpdateTime = currentTime;
                
                // During roulette animation, cycle through names in sequential order
                if (!playerNames.isEmpty()) {
                    // Gradually slow down the cycling as we progress
                    if (animationProgress > 80) {
                        // In the last 20%, slow down cycling and occasionally show winner
                        if (random.nextInt(4) < 1) { // 25% chance to advance
                            // Advance to next name in cycle
                            currentNameIndex = (currentNameIndex + 1) % playerNames.size();
                            currentDisplayName = playerNames.get(currentNameIndex);
                        }
                    } else if (animationProgress > 60) {
                        // In 60-80% range, cycle somewhat slower
                        if (random.nextInt(2) == 0) { // 50% chance to advance
                            // Advance to next name in cycle
                            currentNameIndex = (currentNameIndex + 1) % playerNames.size();
                            currentDisplayName = playerNames.get(currentNameIndex);
                        }
                    } else {
                        // In first 60%, cycle through all names at full speed
                        // Advance to next name in cycle
                        currentNameIndex = (currentNameIndex + 1) % playerNames.size();
                        currentDisplayName = playerNames.get(currentNameIndex);
                    }
                }
                
                // Cycle through colors
                currentColorIndex = (currentColorIndex + 1) % ModConfig.ROULETTE_COLORS.length;
                
                // Play tick sound for each name change
                float randomPitch = 0.75F + random.nextFloat() * 0.5F;
                // Play the sound using our helper method
                playSound(SoundEvents.UI_BUTTON_CLICK, randomPitch); // Random pitch variation
            }
            
            // Prep for centered text drawing with scale
            matrices.push();
            matrices.translate(screenWidth / 2.0, screenHeight / 2.0, 0);
            matrices.scale(ModConfig.ROULETTE_TEXT_SCALE, ModConfig.ROULETTE_TEXT_SCALE, 1.0F);
            
            // First draw the "Selecting next operator" heading with pulsing animation
            Text headerText = Text.literal("Who will reign next?");
            
            // Calculate pulsing animation for the header
            // This varies the color between yellow and gold based on elapsed time
            int pulseRateMs = 500; // Pulse rate in milliseconds (500ms = 2 pulses per second)
            float pulsePhase = ((animationElapsedTime % pulseRateMs) / (float)pulseRateMs);
            int headerColor = pulsePhase < 0.5f ? 0xFFFF55 : 0xFFAA00; // Yellow to gold
            
            int headerWidth = textRenderer.getWidth(headerText);
            drawContext.drawText(textRenderer, 
                    headerText, 
                    -headerWidth / 2, 
                    -40, 
                    headerColor,
                    true);
            
            // Then draw the player name in the center with the current color
            // Make it larger and more prominent with a shadow effect
            Text nameText = Text.literal(currentDisplayName);
            int nameWidth = textRenderer.getWidth(nameText);
            
            // Draw name shadow first (black offset slightly)
            drawContext.drawText(textRenderer, 
                    nameText, 
                    -nameWidth / 2 + 2, 
                    -15 + 2, 
                    0x992200, // Dark shadow color
                    true);
            
            // Draw the actual name text
            drawContext.drawText(textRenderer, 
                    nameText, 
                    -nameWidth / 2, 
                    -15, 
                    ModConfig.ROULETTE_COLORS[currentColorIndex], 
                    true);
            
            // Restore matrices
            matrices.pop();
        }
        else {
            // Automatically set animation phase as complete when animation time expires
            if (!animationPhaseComplete) {
                PowerTripMod.LOGGER.info("[ANIMATION DEBUG] Animation duration complete, transitioning to result phase");
                animationPhaseComplete = true;
            }
            
            // Render the result after animation phase completes and until result display is done
            if (displayResultTicks < ModConfig.RESULT_DISPLAY_DURATION && !resultPhaseComplete) {
            // Mark animation phase as complete when we enter result phase
            if (!animationPhaseComplete) {
                PowerTripMod.LOGGER.info("[ANIMATION DEBUG] Entering result display phase at tick " + displayResultTicks + ", time: " + System.currentTimeMillis());
                animationPhaseComplete = true;
            }
            // Play the firework sound when the winner is first displayed
            if (!hasPlayedWinSound) {
                PowerTripMod.LOGGER.info("[ANIMATION DEBUG] Playing sounds at tick " + displayResultTicks);
                
                // Different sounds for beginning vs ending
                if (isPowerTripBeginning) {
                    // Play celebration sounds for beginning
                    playSound(SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0F);
                    playSound(SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.8F);
                } else {
                    // Play more somber sounds for ending
                    playSound(SoundEvents.BLOCK_BELL_USE, 1.0F);
                }
                hasPlayedWinSound = true;
                
                // Set the result text to show the selected player (which might not match
                // where the animation actually stopped, but that's part of the illusion)
                currentDisplayName = selectedPlayer;
            }
            
            // Prep for centered text drawing with scale
            matrices.push();
            matrices.translate(screenWidth / 2.0, screenHeight / 2.0, 0);
            matrices.scale(ModConfig.ROULETTE_TEXT_SCALE, ModConfig.ROULETTE_TEXT_SCALE, 1.0F);
            
            // Draw a slightly transparent background
            int bgWidth = textRenderer.getWidth(selectedPlayer) + 40;
            drawContext.fill(
                    -bgWidth / 2,
                    -30,
                    bgWidth / 2,
                    30,
                    0xA0000000); // Semi-transparent black
            
            // Draw the appropriate header text
            String headerString = isPowerTripBeginning ? "The reign of" : "The reign of";
            Text headerText = Text.literal(headerString);
            int headerWidth = textRenderer.getWidth(headerText);
            drawContext.drawText(textRenderer, 
                    headerText, 
                    -headerWidth / 2, 
                    -25, 
                    0xFFFF55, // Yellow color
                    true);
            
            // Draw the selected player name
            Text nameText = Text.literal(selectedPlayer);
            int nameWidth = textRenderer.getWidth(nameText);
            drawContext.drawText(textRenderer, 
                    nameText, 
                    -nameWidth / 2, 
                    0, 
                    0xFFD700, // Gold color
                    true);
            
            // Draw ending text based on whether this is beginning or ending
            String endingText = isPowerTripBeginning ? "has begun!" : "has ended!";
            Text endText = Text.literal(endingText);
            int endWidth = textRenderer.getWidth(endText);
            drawContext.drawText(textRenderer, 
                    endText, 
                    -endWidth / 2, 
                    15, 
                    0xFFFF55, // Yellow color
                    true);
            
            // Restore matrices
            matrices.pop();
            
            // We no longer increment ticks here - it's done in the tickClientAnimation method
            // Just render the current state
            }
        } 
        
        // Animation has ended naturally
        if (resultPhaseComplete) {
            // Animation has ended naturally through the tick callback
            isClientAnimationActive = false;
        }
    }
    
    /**
     * Helper method to play a sound consistently in Minecraft 1.21.4
     * @param soundEvent The sound event to play
     * @param pitch The pitch of the sound
     */
    private static void playSound(Object soundEvent, float pitch) {
        MinecraftClient client = MinecraftClient.getInstance();
        SoundInstance soundInstance;
        
        try {
            // Try to use it as a SoundEvent
            if (soundEvent instanceof SoundEvent) {
                soundInstance = PositionedSoundInstance.master((SoundEvent)soundEvent, pitch);
                client.getSoundManager().play(soundInstance);
            } else {
                // Fall back to a standard game sound if all else fails
                client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING, pitch));
            }
        } catch (Exception e) {
            // If any issues, fall back to a standard game sound
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING, pitch));
        }
    }
}
