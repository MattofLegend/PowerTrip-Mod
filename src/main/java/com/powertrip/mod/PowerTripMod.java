package com.powertrip.mod;

import com.powertrip.mod.command.PowerTripCommands;
import com.powertrip.mod.event.ServerTickHandler;
import com.powertrip.mod.network.NetworkHandler;
import com.powertrip.mod.power.PowerManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PowerTrip mod main class
 * Handles server-side initialization
 * 
 * Every 7 Minecraft days:
 * - All players are teleported to spawn
 * - A random player is selected via roulette animation
 * - The selected player is granted operator status
 * - After 7 days, all players are de-opped and the cycle restarts
 */
public class PowerTripMod implements ModInitializer {
    // Define a logger for our mod
    public static final String MOD_ID = "powertrip";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    // Network handler for client-server communication
    public static final NetworkHandler NETWORK = new NetworkHandler();
    
    // Server tick handler for managing power cycles
    public static ServerTickHandler SERVER_TICK_HANDLER;
    
    // Power manager for centralized access
    public static PowerManager POWER_MANAGER;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing PowerTrip Mod");
        
        // Register networking handlers
        NETWORK.register();
        
        // Register event handlers
        registerEventHandlers();
        
        // Register commands
        registerCommands();
        
        LOGGER.info("PowerTrip mod initialized. Use /powertrip start to begin a power cycle.");
    }
    
    /**
     * Register event handlers for the mod
     */
    private void registerEventHandlers() {
        // Create and register the server tick handler
        SERVER_TICK_HANDLER = new ServerTickHandler();
        ServerTickEvents.END_SERVER_TICK.register(SERVER_TICK_HANDLER);
        LOGGER.info("Registered ServerTickHandler");
        
        // Store the PowerManager reference for global access
        POWER_MANAGER = SERVER_TICK_HANDLER.getPowerManager();
        
        LOGGER.info("Registered PowerTrip event handlers");
    }
    
    /**
     * Get the server tick handler instance
     * @return The server tick handler
     */
    public static ServerTickHandler getTickHandler() {
        return SERVER_TICK_HANDLER;
    }
    
    /**
     * Register commands for the mod
     */
    private void registerCommands() {
        // Register command callback
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            PowerTripCommands.register(dispatcher);
        });
        
        LOGGER.info("Registered PowerTrip commands: /powertrip start, /powertrip stop, /powertrip status");
    }
}
