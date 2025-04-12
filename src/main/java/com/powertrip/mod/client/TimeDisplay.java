package com.powertrip.mod.client;

import com.powertrip.mod.PowerTripMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Displays time remaining for PowerTrip in the bottom right corner of the screen
 */
@Environment(EnvType.CLIENT)
public class TimeDisplay {
    // State for time display
    private static int daysRemaining = 0;
    private static int hoursRemaining = 0;
    private static int minutesRemaining = 0;
    private static boolean isPowerTripActive = false;
    
    /**
     * Register the HUD rendering callback for time display
     */
    public static void register() {
        // Register HUD rendering
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            renderTimeRemaining(drawContext);
        });
    }
    
    /**
     * Updates the time values from server data
     */
    public static void updateTimeRemaining(int days, int hours, int minutes, boolean isActive) {
        PowerTripMod.LOGGER.info("[DISPLAY DEBUG] Updating time display - Days: " + days + ", Hours: " + hours + ", Minutes: " + minutes + ", Active: " + isActive);
        daysRemaining = days;
        hoursRemaining = hours;
        minutesRemaining = minutes;
        isPowerTripActive = isActive;
    }
    
    /**
     * Renders the time remaining in the bottom right corner
     */
    private static void renderTimeRemaining(DrawContext drawContext) {
        // Only show time remaining if PowerTrip is active
        if (!isPowerTripActive) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        
        // Don't render if a game menu is open
        if (client.currentScreen != null) {
            return;
        }
        
        // Format the time text based on days/hours/minutes remaining
        PowerTripMod.LOGGER.info("[DISPLAY DEBUG] Rendering time - Days: " + daysRemaining + ", Hours: " + hoursRemaining);
        
        String timeText;
        if (daysRemaining > 0) {
            // Just show days
            timeText = daysRemaining + " day" + (daysRemaining > 1 ? "s" : "");
            PowerTripMod.LOGGER.info("[DISPLAY DEBUG] Showing days: " + timeText);
        } else if (hoursRemaining > 0) {
            // Just show hours
            timeText = hoursRemaining + " hour" + (hoursRemaining > 1 ? "s" : "");
            PowerTripMod.LOGGER.info("[DISPLAY DEBUG] Showing hours: " + timeText);
        } else {
            // Use the minutes value received from the server
            timeText = minutesRemaining + " minute" + (minutesRemaining > 1 ? "s" : "");
            PowerTripMod.LOGGER.info("[DISPLAY DEBUG] Showing minutes: " + timeText);
        }
        
        // Get screen dimensions
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Calculate position (bottom right, with padding)
        int xPos = screenWidth - textRenderer.getWidth(timeText) - 10;
        int yPos = screenHeight - 20; // Above chat, similar to chat text size
        
        // Draw text with shadow
        drawContext.drawText(textRenderer, timeText, xPos, yPos, 0xFFFFFF, true);
    }
}
