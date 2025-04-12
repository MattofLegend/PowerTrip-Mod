package com.powertrip.mod.power;

import com.powertrip.mod.PowerTripMod;
import com.powertrip.mod.event.ServerTickHandler;
import com.powertrip.mod.util.TimeTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Manages the power cycle, player selection, and operator status
 */
public class PowerManager {
    private final Random random = new Random();
    private String currentPowerPlayer = null;
    private boolean isRunning = false;
    private int daysRemaining = 7;
    private long cycleEndTime = -1; // Absolute world time when cycle ends
    private long cycleDayStart = -1; // The Minecraft day when the cycle started
    private static int CYCLE_DURATION = 7; // Default duration of 7 days
    private boolean autostartEnabled = false; // Whether to automatically start a new cycle when the current one ends
    private static final long TICKS_PER_DAY = 24000; // Minecraft day length in ticks
    private boolean justInitialized = false; // Flag to prevent immediate update after init
    private boolean isPowerGrantPending = false; // Flag to prevent multiple overlapping power grants
    
    /**
     * Starts a new power cycle
     * @param server The Minecraft server instance
     * @deprecated This method is deprecated; power cycle now handled directly in ServerTickHandler
     * with separate steps for animation and power granting
     */
    @Deprecated
    public void startNewCycle(MinecraftServer server) {
        // This method is maintained for compatibility but is no longer used directly
        // The ServerTickHandler now coordinates the power cycle steps with animation
        PowerTripMod.LOGGER.info("startNewCycle called - this method is deprecated");
        
        // Remove operator status
        removeAllPlayerPowers(server);
        
        // Teleportation to spawn feature removed as requested
        
        // The rest of the cycle (roulette and granting power) is now handled by ServerTickHandler
    }
    
    /**
     * Removes operator status from all players
     * @param server The Minecraft server instance
     */
    public void removeAllPlayerPowers(MinecraftServer server) {
        PowerTripMod.LOGGER.info("Removing operator status from all players");
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            server.getPlayerManager().removeFromOperators(player.getGameProfile());
        }
        currentPowerPlayer = null;
        cycleEndTime = -1;
        cycleDayStart = -1; // Reset the cycle start day
        isRunning = false;  // Set isRunning to false when the cycle ends
        PowerTripMod.LOGGER.info("Power cycle marked as inactive");
    }
    
    /**
     * Grants operator status to the selected player
     * @param server The Minecraft server instance
     * @param player The player to grant operator status to
     * @param playerName The name of the player (used to update current power player)
     * @return boolean Whether the power was successfully granted
     */
    public boolean grantPowerToPlayer(MinecraftServer server, ServerPlayerEntity player, String playerName) {
        // Check if a power grant is already pending
        if (isPowerGrantPending) {
            PowerTripMod.LOGGER.warn("Power grant already in progress, ignoring request for: " + playerName);
            return false;
        }
        
        // Set power grant in progress flag
        isPowerGrantPending = true;
        PowerTripMod.LOGGER.info("Granting operator status to " + playerName);
        PowerTripMod.LOGGER.info("[SERVER DEBUG] About to grant operator status at time: " + System.currentTimeMillis());
        
        // Grant operator status
        server.getPlayerManager().addToOperators(player.getGameProfile());
        PowerTripMod.LOGGER.info("[SERVER DEBUG] Operator status granted - recording any server packets sent...");
        
        // Update the current power player
        currentPowerPlayer = playerName;
        
        // Record the absolute world time when this cycle will end
        long currentWorldTime = server.getOverworld().getTimeOfDay();
        cycleEndTime = currentWorldTime + (CYCLE_DURATION * TICKS_PER_DAY);
        
        // Calculate and store the current day when cycle starts
        TimeTracker timeTracker = new TimeTracker();
        cycleDayStart = timeTracker.getCurrentMinecraftDay(server);
        
        PowerTripMod.LOGGER.info("Cycle started at world time: " + currentWorldTime);
        PowerTripMod.LOGGER.info("Cycle started on day: " + cycleDayStart);
        PowerTripMod.LOGGER.info("Cycle will end at world time: " + cycleEndTime);
        
        // Directly set to full duration and mark as initialized
        this.daysRemaining = CYCLE_DURATION;
        this.justInitialized = true;
        PowerTripMod.LOGGER.info("Initial days remaining set to: " + CYCLE_DURATION);
        isRunning = true;
        
        // Notify the selected player
        player.sendMessage(Text.literal("You have been selected as the operator for the next " + CYCLE_DURATION + " days!")
                .formatted(Formatting.GOLD), false);
        
        // Notify all other players
        for (ServerPlayerEntity otherPlayer : server.getPlayerManager().getPlayerList()) {
            if (otherPlayer != player) {
                otherPlayer.sendMessage(Text.literal(playerName + 
                        " has been selected as the operator for the next " + CYCLE_DURATION + " days!")
                        .formatted(Formatting.GOLD), false);
            }
        }
        
        // Reset the power grant pending flag
        isPowerGrantPending = false;
        return true;
    }
    
    /**
     * Teleports all players to the world spawn point
     * @param server The Minecraft server instance
     */
    /**
     * Teleports all players to the world spawn point
     * @param server The Minecraft server instance
     * @deprecated This method is no longer used as the teleportation feature has been removed
     */
    public void teleportAllPlayersToSpawn(MinecraftServer server) {
        // Teleportation feature has been removed as requested
        PowerTripMod.LOGGER.info("Player teleportation feature is disabled");
        // Method kept for backwards compatibility
    }
    
    /**
     * Gets the current power player name
     * @return The current player with operator status, or null if none
     */
    public String getCurrentPowerPlayer() {
        return currentPowerPlayer;
    }
    
    /**
     * Gets the absolute world time when the current cycle will end
     * @return The world time when cycle ends, or -1 if no cycle is active
     */
    public long getCycleEndTime() {
        return cycleEndTime;
    }
    
    /**
     * Gets the cycle duration in days
     * @return The number of days in a power cycle
     */
    public int getCycleDuration() {
        return CYCLE_DURATION;
    }
    
    /**
     * Updates days remaining based on current world time
     * @param currentWorldTime The current absolute world time
     */
    public void updateDaysRemaining(long currentWorldTime) {
        if (cycleEndTime > 0) { // Only if cycle is active
            // Skip the first update after initialization
            if (justInitialized) {
                PowerTripMod.LOGGER.info("Skipping first update due to initialization");
                justInitialized = false;
                return;
            }
            
            long ticksRemaining = cycleEndTime - currentWorldTime;
            int exactDays = (int)(ticksRemaining / TICKS_PER_DAY);
            int remainingTicks = (int)(ticksRemaining % TICKS_PER_DAY);
            
            PowerTripMod.LOGGER.info("[PM DEBUG] Calculating days - ticksRemaining: " + ticksRemaining + 
                                  ", exactDays: " + exactDays + ", remainingTicks: " + remainingTicks);
            
            // Keep rounding logic EXCEPT when it would round up to exactly 1 day
            if (remainingTicks > 1000 && exactDays > 0) { // More than 1000 ticks remaining AND not on last day
                PowerTripMod.LOGGER.info("[PM DEBUG] Rounding up days from " + exactDays + " to " + (exactDays + 1));
                exactDays += 1;
            }
            
            PowerTripMod.LOGGER.info("[PM DEBUG] Final days value: " + exactDays);
            this.daysRemaining = exactDays;
            PowerTripMod.LOGGER.debug("Updated days remaining to " + exactDays + 
                                  " (ticks remaining: " + ticksRemaining + ")");
        }
    }
    
    /**
     * Checks if the power cycle is currently running
     * @return true if the cycle is running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Gets the number of days remaining in the current cycle
     * @return The number of days remaining
     */
    public int getDaysRemaining() {
        return daysRemaining;
    }
    
    /**
     * Updates the days remaining counter
     * @param currentDay The current Minecraft day
     * @param server The Minecraft server
     * @return true if a new day has passed, false otherwise
     */
    public boolean updateDaysRemaining(long currentDay, MinecraftServer server) {
        if (!isRunning || cycleDayStart == -1) {
            return false;
        }
        
        // Calculate days elapsed since cycle started
        long daysElapsed = currentDay - cycleDayStart;
        
        // Calculate remaining days based on the cycle start day
        int newDaysRemaining = Math.max(0, CYCLE_DURATION - (int)daysElapsed);
        PowerTripMod.LOGGER.debug("Current day: " + currentDay + ", Cycle start day: " + cycleDayStart + 
                            ", Days elapsed: " + daysElapsed + ", Days remaining: " + newDaysRemaining);
        
        // Check if a day has passed (days remaining decreased)
        boolean dayChanged = newDaysRemaining < daysRemaining;
        if (dayChanged) {
            daysRemaining = newDaysRemaining;
            PowerTripMod.LOGGER.info("Days remaining updated to: " + daysRemaining);
        }
        
        // Handle cycle completion
        if (daysRemaining <= 0) {
            // Don't immediately end the cycle when days reach 0
            // Instead, let the ServerTickHandler handle it based on actual time
            // startNewCycle(server); <- This line was removed to allow hours/minutes display
        }
        
        return dayChanged;
    }
    
    /**
     * Manually starts the power cycle
     * @param server The Minecraft server
     */
    public void startCycle(MinecraftServer server) {
        isRunning = true;
        
        // Instead of using the deprecated startNewCycle method, we'll trigger the cycle from ServerTickHandler
        PowerTripMod.LOGGER.info("Power cycle started manually by command");
        
        // Get the server tick handler instance
        ServerTickHandler tickHandler = PowerTripMod.SERVER_TICK_HANDLER;
        if (tickHandler != null) {
            // Directly trigger the power cycle using the ServerTickHandler
            tickHandler.triggerManualPowerCycle(server);
            PowerTripMod.LOGGER.info("Manual power cycle triggered via ServerTickHandler");
        } else {
            PowerTripMod.LOGGER.error("Could not trigger power cycle - ServerTickHandler not available");
            // Fall back to the old method as a last resort
            startNewCycle(server);
        }
    }
    
    /**
     * Manually stops the power cycle
     * @param server The Minecraft server
     */
    public void stopCycle(MinecraftServer server) {
        isRunning = false;
        removeAllPlayerPowers(server);
        
        // Send explicit 'inactive' state update to all clients
        PowerTripMod.LOGGER.info("Sending inactive state to all clients from stopCycle");
        PowerTripMod.NETWORK.sendTimeRemainingToAll(server, 0, 0, 0, false);
        
        // Power cycle stopped
    }
    
    /**
     * Sets the cycle duration in days
     * @param days The number of days for a power cycle
     * @return true if the duration was changed, false if a cycle is currently running
     */
    public boolean setCycleDuration(int days) {
        // Cannot change duration while a cycle is running
        if (isRunning()) {
            return false;
        }
        
        // Set the new cycle duration
        CYCLE_DURATION = days;
        PowerTripMod.LOGGER.info("PowerTrip cycle duration set to " + days + " days");
        return true;
    }
    
    /**
     * Gets whether autostart is enabled
     * @return true if autostart is enabled, false otherwise
     */
    public boolean isAutostartEnabled() {
        return autostartEnabled;
    }
    
    /**
     * Sets whether autostart is enabled
     * @param enabled true to enable autostart, false to disable
     */
    public void setAutostartEnabled(boolean enabled) {
        this.autostartEnabled = enabled;
        PowerTripMod.LOGGER.info("PowerTrip autostart " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Checks if a power grant operation is currently in progress
     * @return true if a power grant is pending, false otherwise
     */
    public boolean isPowerGrantPending() {
        return isPowerGrantPending;
    }
}
