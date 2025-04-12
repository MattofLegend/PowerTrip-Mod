package com.powertrip.mod.config;

/**
 * Configuration settings for the PowerTrip mod
 */
public class ModConfig {
    // Number of Minecraft days between power cycles
    public static final int DAYS_BETWEEN_CYCLES = 7;
    
    // Duration of the roulette animation in ticks (20 ticks = 1 second)
    // 5 seconds = 100 ticks
    public static final int ROULETTE_DURATION_TICKS = 100;
    
    // Delay between each name change in the roulette (in ticks)
    // Higher value = slower cycling of names for better readability
    // 10 ticks = 0.5 seconds between name changes
    public static final int ROULETTE_TICK_DELAY = 10;
    
    // Duration to display the final selected player message (in ticks)
    public static final int RESULT_DISPLAY_DURATION = 100;
    
    // Scaling factor for the roulette text - larger = bigger text
    public static final float ROULETTE_TEXT_SCALE = 2.0F;
    
    // Different colors for roulette names (in hexadecimal)
    public static final int[] ROULETTE_COLORS = {
        0xFF5555, // Red
        0x55FF55, // Green
        0x5555FF, // Blue
        0xFFFF55, // Yellow
        0xFF55FF, // Magenta
        0x55FFFF, // Cyan
        0xFFAA00, // Orange
        0xAA00FF, // Purple
        0x00AAFF  // Light Blue
    };
}
