package com.powertrip.mod.client;

import com.powertrip.mod.PowerTripMod;
import com.powertrip.mod.network.PowerTripStatusRequestPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

/**
 * Handles client tick events to periodically request PowerTrip status from server
 */
@Environment(EnvType.CLIENT)
public class ClientTickHandler implements ClientTickEvents.EndTick {
    // Check status every 5 seconds (100 ticks)
    private static final int STATUS_CHECK_INTERVAL = 100;
    private int tickCounter = 0;
    
    /**
     * Register the client tick handler
     */
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickHandler());
    }
    
    @Override
    public void onEndTick(MinecraftClient client) {
        // Only send requests if we're in a world and connected to a server
        if (client.world == null || client.player == null) {
            return;
        }
        
        tickCounter++;
        if (tickCounter % STATUS_CHECK_INTERVAL == 0) {
            PowerTripMod.LOGGER.debug("Sending PowerTrip status request to server");
            // Send request to server
            ClientPlayNetworking.send(new PowerTripStatusRequestPayload());
        }
    }
}
