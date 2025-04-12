package com.powertrip.mod.util;

import com.powertrip.mod.PowerTripMod;
import com.powertrip.mod.config.ModConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelProperties;

/**
 * Utility class to track Minecraft time/days and determine when cycles should occur
 */
public class TimeTracker {
    private static final int TICKS_PER_DAY = 24000;
    private long lastCycleDay = -1;
    
    /**
     * Checks if a new power cycle should be triggered
     * @param server The Minecraft server instance
     * @return True if a new cycle should begin
     */
    public boolean shouldTriggerCycle(MinecraftServer server) {
        long currentDay = getCurrentMinecraftDay(server);
        
        // First server start - initialize but don't trigger immediately
        if (lastCycleDay == -1) {
            lastCycleDay = currentDay;
            PowerTripMod.LOGGER.info("Initialized cycle tracker at day " + currentDay);
            return false;
        }
        
        // Check if enough days have passed
        if (currentDay >= lastCycleDay + ModConfig.DAYS_BETWEEN_CYCLES) {
            PowerTripMod.LOGGER.info("Starting new cycle. Last cycle: " + lastCycleDay + ", Current day: " + currentDay);
            lastCycleDay = currentDay;
            return true;
        }
        
        return false;
    }
    
    /**
     * Legacy method kept for compatibility
     * @param server The Minecraft server instance
     * @param worldTime The current absolute world time in ticks
     * @return True if a new cycle should begin
     */
    public boolean shouldTriggerCycle(MinecraftServer server, long worldTime) {
        return shouldTriggerCycle(server);
    }
    
    /**
     * Gets the current Minecraft day
     * @param server The Minecraft server instance
     * @return The current day number
     */
    public long getCurrentMinecraftDay(MinecraftServer server) {
        LevelProperties worldData = (LevelProperties)server.getOverworld().getLevelProperties();
        long dayTime = worldData.getTimeOfDay();
        return dayTime / TICKS_PER_DAY;
    }
    
    /**
     * Gets the current day (alias for getCurrentMinecraftDay)
     * @param server The Minecraft server instance
     * @return The current day number
     */
    public long getCurrentDay(MinecraftServer server) {
        return getCurrentMinecraftDay(server);
    }
    
    /**
     * Gets days remaining until next cycle
     * @param server The Minecraft server instance
     * @return Days remaining until next cycle
     */
    public int getDaysUntilNextCycle(MinecraftServer server) {
        if (lastCycleDay == -1) {
            return ModConfig.DAYS_BETWEEN_CYCLES;
        }
        
        long currentDay = getCurrentMinecraftDay(server);
        long daysElapsed = currentDay - lastCycleDay;
        
        return (int) Math.max(0, ModConfig.DAYS_BETWEEN_CYCLES - daysElapsed);
    }
}
