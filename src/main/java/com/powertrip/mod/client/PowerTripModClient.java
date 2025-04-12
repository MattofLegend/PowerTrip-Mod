package com.powertrip.mod.client;

import com.powertrip.mod.PowerTripMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * PowerTrip mod client class
 * Handles client-side initialization
 * 
 * Responsible for registering client-side components:
 * - Roulette animation display
 * - Time remaining display
 * - Network packet handlers
 */
@Environment(EnvType.CLIENT)
public class PowerTripModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PowerTripMod.LOGGER.info("Initializing PowerTrip Mod Client");
        
        // Register client-specific content
        registerClientContent();
        
        PowerTripMod.LOGGER.info("PowerTrip Mod Client initialized");
    }
    
    /**
     * Register client-specific content
     */
    private void registerClientContent() {
        // Register the roulette display renderer
        RouletteDisplay.register();
        
        // Register the time remaining display
        TimeDisplay.register();
        
        // Register client-side network receivers
        com.powertrip.mod.network.NetworkHandler.registerClientReceiver();
        
        // Register the client tick handler for status requests
        ClientTickHandler.register();
        
        PowerTripMod.LOGGER.info("Registered PowerTrip client components");
    }
}
