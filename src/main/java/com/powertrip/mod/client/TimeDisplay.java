package com.powertrip.mod.client;

import com.powertrip.mod.PowerTripMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Displays time remaining for PowerTrip above the hotbar
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
        // Register HUD rendering using HudRenderCallback
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            // The second parameter may be RenderTickCounter in your Minecraft version
            renderTimeRemaining(drawContext, tickCounter);
        });
    }
    
    /**
     * Updates the time values from server data
     */
    public static void updateTimeRemaining(int days, int hours, int minutes, boolean isActive) {
        daysRemaining = days;
        hoursRemaining = hours;
        minutesRemaining = minutes;
        isPowerTripActive = isActive;
    }
    
    /**
     * Renders the time remaining above the hotbar
     */
    private static void renderTimeRemaining(DrawContext drawContext, Object tickCounter) {
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
        String timeText;
        if (daysRemaining > 0) {
            // Just show days
            timeText = daysRemaining + " day" + (daysRemaining > 1 ? "s" : "");
        } else if (hoursRemaining > 0) {
            // Just show hours
            timeText = hoursRemaining + " hour" + (hoursRemaining > 1 ? "s" : "");
        } else {
            // Use the minutes value received from the server
            timeText = minutesRemaining + " minute" + (minutesRemaining > 1 ? "s" : "");
        }
        
        // Get screen dimensions
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Calculate position (center below item name position but above hotbar)
        int xPos = screenWidth / 2 - textRenderer.getWidth(timeText) / 2;
        int yPos = screenHeight - 50; // Position below item names (59) but above hotbar
        
        // Draw text with shadow
        drawContext.drawText(textRenderer, timeText, xPos, yPos, 0xFFFFFF, true);
    }
}
