package com.powertrip.mod.event;

import com.powertrip.mod.PowerTripMod;
import com.powertrip.mod.power.PowerManager;
import com.powertrip.mod.util.TimeTracker;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Handles server tick events to track time and trigger power cycles
 */
public class ServerTickHandler implements ServerTickEvents.EndTick {
    private final TimeTracker timeTracker;
    private final PowerManager powerManager;
    private boolean isRouletteActive = false;
    private int dailyReminderCounter = 0;
    
    // Tick counter for periodic time updates
    private int tickCounter = 0;
    private static final int TIME_UPDATE_INTERVAL = 100; // Send time update every 5 seconds (100 ticks)
    private static final int MINUTE_UPDATE_INTERVAL = 20; // More frequent updates when minutes are shown (1 second)
    
    // Absolute time tracking
    private long lastCheckedWorldTime = 0;
    private static final int TIME_CHECK_INTERVAL = 1000; // Check absolute time every 1000 ticks (50 seconds)
    
    /**
     * Flag to indicate a manual power cycle has been requested
     */
    private boolean manualPowerCycleRequested = false;
    private MinecraftServer pendingServer = null;
    
    // How often to remind players about days remaining (in ticks)
    private static final int REMINDER_INTERVAL = 24000; // Once per Minecraft day
    
    public ServerTickHandler() {
        this.timeTracker = new TimeTracker();
        this.powerManager = new PowerManager();
    }
    
    /**
     * Manually trigger a power cycle from outside the normal tick routine
     * This is called from PowerManager when the /powertrip start command is used
     * @param server The Minecraft server instance
     */
    public void triggerManualPowerCycle(MinecraftServer server) {
        PowerTripMod.LOGGER.info("Manual power cycle requested - will trigger on next server tick");
        this.manualPowerCycleRequested = true;
        this.pendingServer = server;
    }
    
    @Override
    public void onEndTick(MinecraftServer server) {
        tickCounter++;
        
        // Get current absolute world time
        long currentWorldTime = server.getOverworld().getTimeOfDay();
        
        // Check EVERY tick if the cycle should end based on absolute time
        // This ensures cycle ends exactly when it should rather than waiting for TIME_CHECK_INTERVAL
        if (powerManager.isRunning()) {
            long cycleEndTime = powerManager.getCycleEndTime();
            if (currentWorldTime >= cycleEndTime) {
                handleCycleEnd(server);
            }
        }
        
        // Get current absolute world time to decide update frequency
        long ticksRemaining = powerManager.getCycleEndTime() - currentWorldTime;
        double hoursRaw = (ticksRemaining * 24.0) / 24000.0;
        
        // Periodically send time updates to all clients
        // Use more frequent updates when less than 1 hour remains
        if (hoursRaw < 1.0 && powerManager.isRunning()) {
            if (tickCounter % MINUTE_UPDATE_INTERVAL == 0) {
                sendTimeUpdateToAll(server);
            }
        } else {
            if (tickCounter % TIME_UPDATE_INTERVAL == 0) {
                sendTimeUpdateToAll(server);
            }
        }
        
        // Check for time jumps and day changes every TIME_CHECK_INTERVAL ticks
        // This guards against time manipulation via console, sleeping, or other means
        if (tickCounter % TIME_CHECK_INTERVAL == 0 || 
            Math.abs(currentWorldTime - lastCheckedWorldTime) > 24000) { // Also check if time jumped drastically
            
            // Update last checked time
            lastCheckedWorldTime = currentWorldTime;
            
            // Check if power cycle should end due to time changes
            checkPowerCycleStatus(server, currentWorldTime);
        }
        
        // Skip further checks if no players are online
        if (server.getCurrentPlayerCount() == 0) {
            return;
        }
        
        // Check if a manual power cycle was requested
        if (manualPowerCycleRequested && pendingServer != null) {
            PowerTripMod.LOGGER.info("Executing manually triggered power cycle");
            startPowerCycle(pendingServer);
            // Reset the flag after handling
            manualPowerCycleRequested = false;
            pendingServer = null;
            return; // Skip normal tick processing for this cycle
        }
        
        // Only process if power cycle is running (started by command)
        if (powerManager.isRunning()) {
            // Update the days remaining counter based on current day
            long currentDay = timeTracker.getCurrentDay(server);
            boolean dayChanged = powerManager.updateDaysRemaining(currentDay, server);
            
            // Check the actual time rather than just days
            // Get the current world time and cycle end time
            currentWorldTime = server.getOverworld().getTimeOfDay(); // Using existing variable
            long cycleEndTime = powerManager.getCycleEndTime();
            
            // Only end the cycle when actual time runs out, not when days reach 0
            if (currentWorldTime >= cycleEndTime && !isRouletteActive) {
                PowerTripMod.LOGGER.info("Power cycle completed (actual time), starting new cycle");
                startPowerCycle(server);
            }
            
            // Daily reminders about days remaining
            dailyReminderCounter++;
            if (dailyReminderCounter >= REMINDER_INTERVAL || dayChanged) {
                dailyReminderCounter = 0;
                sendDaysRemainingReminder(server);
            }
        }
    }
    
    /**
     * Starts a new power cycle with the roulette selection
     * @param server The Minecraft server instance
     */
    private void startPowerCycle(MinecraftServer server) {
        PowerTripMod.LOGGER.info("=== POWER CYCLE STARTING ===");
        isRouletteActive = true;
        
        // Get list of online players
        List<String> playerNames = new ArrayList<>();
        List<ServerPlayerEntity> onlinePlayers = server.getPlayerManager().getPlayerList();
        if (onlinePlayers.isEmpty()) {
            PowerTripMod.LOGGER.info("No players online, skipping power cycle");
            isRouletteActive = false;
            return;
        }
        
        PowerTripMod.LOGGER.info("Found " + onlinePlayers.size() + " players online");
        for (ServerPlayerEntity player : onlinePlayers) {
            String name = player.getName().getString();
            playerNames.add(name);
            PowerTripMod.LOGGER.info("Player in cycle: " + name);
        }
        
        // First remove OP from all players
        PowerTripMod.LOGGER.info("Removing operator status from all players");
        powerManager.removeAllPlayerPowers(server);
        
        // Teleport all players to spawn
        PowerTripMod.LOGGER.info("Teleporting all players to spawn");
        powerManager.teleportAllPlayersToSpawn(server);
        
        // Select a player but don't grant power yet
        Random random = new Random();
        int selectedIndex = random.nextInt(onlinePlayers.size());
        ServerPlayerEntity selectedPlayer = onlinePlayers.get(selectedIndex);
        String selectedPlayerName = selectedPlayer.getName().getString();
        PowerTripMod.LOGGER.info("Selected player: " + selectedPlayerName + " (will be announced after animation)");
        
        // Trigger roulette display on all clients BEFORE actually granting power
        PowerTripMod.LOGGER.info("Triggering roulette animation for all players");
        PowerTripMod.NETWORK.triggerRouletteForAll(server, playerNames, selectedPlayerName);
        
        // No chat message needed - animation will be visible on screen
        
        PowerTripMod.LOGGER.info("Scheduling power grant after animation delay");
        
        // Instead of using server.execute which could create threading issues,
        // create a dedicated task with proper timing
        new Thread(() -> {
            try {
                PowerTripMod.LOGGER.info("Animation delay started - waiting 6 seconds");
                // Wait long enough for animation to complete (in milliseconds)
                Thread.sleep(6000); // 6 seconds, slightly longer than animation duration
                
                // Make sure we're back on the server thread for the final step
                server.execute(() -> {
                    PowerTripMod.LOGGER.info("Animation delay complete - granting power to " + selectedPlayerName);
                    // Now grant power to the selected player
                    powerManager.grantPowerToPlayer(server, selectedPlayer, selectedPlayerName);
                    PowerTripMod.LOGGER.info("=== POWER CYCLE COMPLETE ===");
                    // Server state is now managed independently of client animation
                });
            } catch (InterruptedException e) {
                PowerTripMod.LOGGER.error("Interrupted while waiting for roulette animation", e);
                // Just handle server-side state, don't affect client animation
                PowerTripMod.LOGGER.info("Marking server-side animation state as inactive");
                isRouletteActive = false;
            }
        }, "PowerTrip-AnimationDelay").start();
    }
    
    /**
     * Sends a reminder to all players about days remaining in current cycle
     * @param server The Minecraft server instance
     */
    private void sendDaysRemainingReminder(MinecraftServer server) {
        int daysRemaining = powerManager.getDaysRemaining();
        String currentRuler = powerManager.getCurrentPowerPlayer();
        
        if (daysRemaining > 0 && currentRuler != null) {
            // Format the message
            String message = String.format("PowerTrip: %d day%s remaining with %s as operator.",
                    daysRemaining, daysRemaining > 1 ? "s" : "", currentRuler);
            
            // Send to all players
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                player.sendMessage(Text.literal(message).formatted(Formatting.YELLOW), false);
            }
        }
    }
    
    /**
     * Sends time information to all clients for HUD display
     * @param server The Minecraft server
     */
    /**
     * Checks if the power cycle status needs to be updated due to time changes
     * @param server The Minecraft server
     * @param currentWorldTime Current absolute world time
     */
    private void checkPowerCycleStatus(MinecraftServer server, long currentWorldTime) {
        // Only check if we have an active power cycle and we're actively running
        if (powerManager.getCurrentPowerPlayer() != null && powerManager.isRunning()) {
            // Update days remaining based on current world time
            powerManager.updateDaysRemaining(currentWorldTime);
            
            // Note: The cycle end check is now done every tick in onEndTick
            // so it's removed from here to avoid duplication
        }
        
        // Check if we should start a new cycle
        if (!isRouletteActive && !powerManager.isRunning() && server.getCurrentPlayerCount() > 0) {
            // Check with time tracker if it's time for a new cycle
            if (timeTracker.shouldTriggerCycle(server)) {
                PowerTripMod.LOGGER.info("PowerTrip cycle started! A new operator will be chosen.");
                startPowerCycle(server);
            }
        }
    }
    
    /**
     * Handles the end of a power cycle
     * @param server The Minecraft server
     */
    private void handleCycleEnd(MinecraftServer server) {
        // Get the absolute world time when the cycle will end
        long cycleEndTime = powerManager.getCycleEndTime();
        long currentWorldTime = server.getOverworld().getTimeOfDay();
        
        PowerTripMod.LOGGER.info("Cycle complete! Current time: " + currentWorldTime + 
                               ", End time: " + cycleEndTime);
        powerManager.removeAllPlayerPowers(server);
        
        // Send explicit 'inactive' state update to all clients when cycle ends
        PowerTripMod.LOGGER.info("Sending inactive state to all clients");
        PowerTripMod.NETWORK.sendTimeRemainingToAll(server, 0, 0, 0, false);
    }
    
    /**
     * Sends time information to all clients for HUD display
     * @param server The Minecraft server
     */
    private void sendTimeUpdateToAll(MinecraftServer server) {
        // Only send updates if PowerTrip is active (has an operator)
        boolean isActive = powerManager.getCurrentPowerPlayer() != null;
        
        if (isActive) {
            // Update days remaining based on current time
            long currentWorldTime = server.getOverworld().getTimeOfDay();
            long cycleEndTime = powerManager.getCycleEndTime();
            long ticksRemaining = cycleEndTime - currentWorldTime;
            
            PowerTripMod.LOGGER.info("[TIME DEBUG] Current world time: " + currentWorldTime);
            PowerTripMod.LOGGER.info("[TIME DEBUG] Cycle end time: " + cycleEndTime);
            PowerTripMod.LOGGER.info("[TIME DEBUG] Ticks remaining: " + ticksRemaining);
            PowerTripMod.LOGGER.info("[TIME DEBUG] Days remaining (raw): " + (ticksRemaining / 24000.0));
            
            // If time has run out, set all time values to 0 and mark as inactive
            if (ticksRemaining <= 0) {
                PowerTripMod.LOGGER.info("[TIME DEBUG] Time has run out, setting all values to 0");
                PowerTripMod.NETWORK.sendTimeRemainingToAll(server, 0, 0, 0, false);
                return;
            }
            
            powerManager.updateDaysRemaining(currentWorldTime);
            
            int daysRemaining = powerManager.getDaysRemaining();
            PowerTripMod.LOGGER.info("[TIME DEBUG] Days remaining (after PowerManager): " + daysRemaining);
            
            // Calculate hours remaining if less than 1 day
            int hoursRemaining = 0;
            if (daysRemaining < 1) {
                // Calculate hours remaining based on actual ticks remaining
                double hoursRaw = (ticksRemaining * 24.0) / 24000.0;
                PowerTripMod.LOGGER.info("[TIME DEBUG] Raw hours calculated: " + hoursRaw);
                
                // If less than 1 hour remains, set hours to 0 and calculate minutes
                int minutesRemaining = 0;
                if (hoursRaw < 1.0) {
                    hoursRemaining = 0;
                    // Calculate minutes from ticks - one hour is 1000 ticks (24000/24)
                    minutesRemaining = (int)Math.ceil((ticksRemaining * 60.0) / 1000.0);
                    // Ensure minutes don't go negative
                    minutesRemaining = Math.max(0, minutesRemaining);
                    PowerTripMod.LOGGER.info("[TIME DEBUG] Less than 1 hour remains, showing " + minutesRemaining + " minutes");
                } else {
                    // Otherwise, round up to the next hour
                    hoursRemaining = (int) Math.ceil(hoursRaw);
                    PowerTripMod.LOGGER.info("[TIME DEBUG] Hours calculated for < 1 day: " + hoursRemaining + " (from ticks: " + ticksRemaining + ")");
                }
            } else if (daysRemaining == 1) {
                // If exactly 1 day left, show 24 hours instead of 0
                hoursRemaining = 24;
                PowerTripMod.LOGGER.info("[TIME DEBUG] Setting hours to 24 for exactly 1 day remaining");
            }
            
            int minutesValue = 0;
            if (daysRemaining == 0 && hoursRemaining == 0) {
                // Calculate minutes from ticks if hours is 0
                minutesValue = (int)Math.ceil((ticksRemaining * 60.0) / 1000.0);
                // Ensure minutes don't go negative
                minutesValue = Math.max(0, minutesValue);
                PowerTripMod.LOGGER.info("[TIME DEBUG] Final values - Days: " + daysRemaining + ", Hours: " + hoursRemaining + ", Minutes: " + minutesValue);
            } else {
                PowerTripMod.LOGGER.info("[TIME DEBUG] Final values - Days: " + daysRemaining + ", Hours: " + hoursRemaining);
            }
            
            // Send the time update to all clients
            PowerTripMod.NETWORK.sendTimeRemainingToAll(server, daysRemaining, hoursRemaining, minutesValue, isActive);
        }
    }
    
    /**
     * Gets the power manager instance
     * @return The power manager
     */
    public PowerManager getPowerManager() {
        return powerManager;
    }
}
