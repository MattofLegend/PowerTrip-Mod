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
    private static final int CYCLE_DURATION = 7; // Default duration of 7 days
    private static final long TICKS_PER_DAY = 24000; // Minecraft day length in ticks
    private boolean justInitialized = false; // Flag to prevent immediate update after init
    
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
        
        // Teleport everyone to spawn
        teleportAllPlayersToSpawn(server);
        
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
     */
    public void grantPowerToPlayer(MinecraftServer server, ServerPlayerEntity player, String playerName) {
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
        player.sendMessage(Text.literal("You have been selected as the operator for the next 7 days!")
                .formatted(Formatting.GOLD), false);
        
        // Notify all other players
        for (ServerPlayerEntity otherPlayer : server.getPlayerManager().getPlayerList()) {
            if (otherPlayer != player) {
                otherPlayer.sendMessage(Text.literal(playerName + 
                        " has been selected as the operator for the next 7 days!")
                        .formatted(Formatting.GOLD), false);
            }
        }
    }
    
    /**
     * Teleports all players to the world spawn point
     * @param server The Minecraft server instance
     */
    public void teleportAllPlayersToSpawn(MinecraftServer server) {
        PowerTripMod.LOGGER.info("Teleporting all players to spawn");
        BlockPos spawnPos = server.getOverworld().getSpawnPos();
        
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // Use the proper teleport method signature for 1.21.4
            player.teleport(server.getOverworld(), 
                    spawnPos.getX() + 0.5, 
                    spawnPos.getY(), 
                    spawnPos.getZ() + 0.5, 
                    Set.of(), // No position flags
                    player.getYaw(), 
                    player.getPitch(),
                    true); // Cancel vehicle dismount
            
            player.sendMessage(Text.literal("A new power cycle begins! All players have been teleported to spawn.")
                    .formatted(Formatting.AQUA), false);
        }
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
            int days = (int)Math.max(0, ticksRemaining / TICKS_PER_DAY);
            int remainingTicks = (int)(ticksRemaining % TICKS_PER_DAY);
            
            // If we have a partial day, count it as one more day unless it's very small
            if (remainingTicks > 1000) { // More than 1000 ticks (about 1 minute) remaining
                days += 1;
            }
            
            this.daysRemaining = days;
            PowerTripMod.LOGGER.debug("Updated days remaining to " + days + 
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
            // Start a new cycle
            startNewCycle(server);
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
        PowerTripMod.NETWORK.sendTimeRemainingToAll(server, 0, 0, false);
        
        // Power cycle stopped
    }
    
    // setDaysRemaining method is already defined above with better implementation
}
