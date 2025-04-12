package com.powertrip.mod.network;

import com.powertrip.mod.PowerTripMod;
import com.powertrip.mod.client.RouletteDisplay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Handles network communication between server and client
 */
public class NetworkHandler {
    // Constants for the network packet
    private static final Identifier ROULETTE_PACKET_ID = Identifier.of(PowerTripMod.MOD_ID, "roulette");
    
    /**
     * Registers network handlers - called during mod initialization
     */
    public void register() {
        // Register the payload types for server->client packets
        PayloadTypeRegistry.playS2C().register(RoulettePayload.ID, RoulettePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TimeRemainingPayload.ID, TimeRemainingPayload.CODEC);
        
        // Register client->server payload
        PayloadTypeRegistry.playC2S().register(PowerTripStatusRequestPayload.ID, PowerTripStatusRequestPayload.CODEC);
        
        // Register the server-side handler for status requests
        ServerPlayNetworking.registerGlobalReceiver(PowerTripStatusRequestPayload.ID, (payload, context) -> {
            // Execute on the server thread
            context.server().execute(() -> {
                // Get the current PowerTrip status
                PowerTripMod.LOGGER.debug("Received PowerTrip status request from " + context.player().getName().getString());
                
                // Get the PowerManager from the server tick handler
                boolean isActive = false;
                int daysRemaining = 0;
                int hoursRemaining = 0;
                int minutesRemaining = 0;
                
                if (PowerTripMod.SERVER_TICK_HANDLER != null) {
                    var powerManager = PowerTripMod.SERVER_TICK_HANDLER.getPowerManager();
                    isActive = powerManager.isRunning() && powerManager.getCurrentPowerPlayer() != null;
                    
                    if (isActive) {
                        daysRemaining = powerManager.getDaysRemaining();
                        
                        // Calculate hours if we're on the last day
                        if (daysRemaining < 1) {
                            long currentWorldTime = context.server().getOverworld().getTimeOfDay();
                            long ticksRemaining = powerManager.getCycleEndTime() - currentWorldTime;
                            
                            // Calculate hours in the same way as ServerTickHandler
                            double hoursRaw = (ticksRemaining * 24.0) / 24000.0;
                            
                            // If less than 1 hour remains, set hours to 0 and calculate minutes
                            if (hoursRaw < 1.0) {
                                hoursRemaining = 0;
                                // Calculate minutes from ticks - one Minecraft hour is 1000 ticks
                                minutesRemaining = (int)Math.ceil((ticksRemaining * 60.0) / 1000.0);
                            } else {
                                // Otherwise, round up to the next hour
                                hoursRemaining = (int) Math.ceil(hoursRaw);
                            }
                        }
                    }
                }
                
                PowerTripMod.LOGGER.debug("Sending status response: active=" + isActive + 
                                        ", days=" + daysRemaining + 
                                        ", hours=" + hoursRemaining);
                
                // Create response payload (with minutes)
                TimeRemainingPayload response = new TimeRemainingPayload(daysRemaining, hoursRemaining, minutesRemaining, isActive);
                
                // Send response back to the client
                ServerPlayNetworking.send(context.player(), response);
            });
        });
        
        // Client-side receiver registration is done in the client mod class
    }
    
    /**
     * Triggers the roulette animation for all online players
     * @param server The Minecraft server
     * @param playerNames List of player names to include in the roulette
     * @param selectedPlayer The pre-selected player who will win
     * @param isBeginning Whether this is a beginning (true) or ending (false) animation
     */
    public void triggerRouletteForAll(MinecraftServer server, List<String> playerNames, String selectedPlayer, boolean isBeginning) {
        // Create the custom payload
        RoulettePayload payload = new RoulettePayload(playerNames, selectedPlayer, isBeginning);
        
        // Send to all players
        PowerTripMod.LOGGER.info("[NETWORK DEBUG] Sending roulette payload to all players at " + System.currentTimeMillis());
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PowerTripMod.LOGGER.info("[NETWORK DEBUG] Sending to player: " + player.getName().getString());
            ServerPlayNetworking.send(player, payload);
        }
        PowerTripMod.LOGGER.info("[NETWORK DEBUG] All roulette packets sent");
    }
    
    /**
     * Sends time remaining information to all online players
     * @param server The Minecraft server
     * @param daysRemaining Days remaining in the power cycle
     * @param hoursRemaining Hours remaining in the current day (if days < 1)
     * @param isActive Whether PowerTrip is active (operator exists)
     */
    public void sendTimeRemainingToAll(MinecraftServer server, int daysRemaining, int hoursRemaining, int minutesRemaining, boolean isActive) {
        // Create the custom payload
        TimeRemainingPayload payload = new TimeRemainingPayload(daysRemaining, hoursRemaining, minutesRemaining, isActive);
        
        // Send to all players
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
    
    /**
     * Register the client-side receivers for network packets
     * This is called from the client mod class
     */
    @Environment(EnvType.CLIENT)
    public static void registerClientReceiver() {
        // Register roulette animation packet handler
        ClientPlayNetworking.registerGlobalReceiver(RoulettePayload.ID, (payload, context) -> {
            PowerTripMod.LOGGER.info("[NETWORK DEBUG] Client received roulette packet at " + System.currentTimeMillis());
            context.client().execute(() -> {
                PowerTripMod.LOGGER.info("[NETWORK DEBUG] Client executing roulette animation with " + 
                    payload.playerNames().size() + " players, selected: " + payload.selectedPlayer() + 
                    ", isBeginning: " + payload.isBeginning());
                RouletteDisplay.startRoulette(payload.playerNames(), payload.selectedPlayer(), payload.isBeginning());
            });
        });
        
        // Register time remaining packet handler
        ClientPlayNetworking.registerGlobalReceiver(TimeRemainingPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                PowerTripMod.LOGGER.debug("Received time remaining update: " + 
                    payload.daysRemaining() + " days, " + payload.hoursRemaining() + " hours, " +
                    payload.minutesRemaining() + " minutes, active: " + payload.isPowerTripActive());
                
                // Call the TimeDisplay class safely through reflection to avoid direct class reference
                // This prevents class loading issues between client/server environments
                try {
                    // Get the TimeDisplay class
                    Class<?> timeDisplayClass = Class.forName("com.powertrip.mod.client.TimeDisplay");
                    
                    // Get the updateTimeRemaining method
                    java.lang.reflect.Method updateMethod = timeDisplayClass.getMethod(
                        "updateTimeRemaining", int.class, int.class, int.class, boolean.class);
                    
                    // Call the method with our payload data
                    updateMethod.invoke(null, payload.daysRemaining(), 
                        payload.hoursRemaining(), payload.minutesRemaining(), payload.isPowerTripActive());
                } catch (Exception e) {
                    PowerTripMod.LOGGER.error("Failed to update time display", e);
                }
            });
        });
    }
    
    /**
     * Custom payload record for the roulette animation packet
     * Using the modern networking API introduced in 1.20.5
     */
    public static record RoulettePayload(List<String> playerNames, String selectedPlayer, boolean isBeginning) implements CustomPayload {
        // Create an ID for this payload type
        public static final CustomPayload.Id<RoulettePayload> ID = new CustomPayload.Id<>(ROULETTE_PACKET_ID);
        
        // Create a codec to serialize/deserialize the payload
        // This is a simplified implementation that encodes/decodes strings manually
        public static final PacketCodec<PacketByteBuf, RoulettePayload> CODEC = new PacketCodec<>() {
            @Override
            public void encode(PacketByteBuf buf, RoulettePayload payload) {
                // Write the number of player names
                buf.writeInt(payload.playerNames.size());
                
                // Write each player name
                for (String name : payload.playerNames) {
                    buf.writeString(name);
                }
                
                // Write the selected player
                buf.writeString(payload.selectedPlayer);
                
                // Write whether this is a beginning or ending animation
                buf.writeBoolean(payload.isBeginning);
            }
            
            @Override
            public RoulettePayload decode(PacketByteBuf buf) {
                // Read the number of player names
                int count = buf.readInt();
                List<String> playerNames = new java.util.ArrayList<>(count);
                
                // Read each player name
                for (int i = 0; i < count; i++) {
                    playerNames.add(buf.readString());
                }
                
                // Read the selected player
                String selectedPlayer = buf.readString();
                
                // Read whether this is a beginning or ending animation
                boolean isBeginning = buf.readBoolean();
                
                return new RoulettePayload(playerNames, selectedPlayer, isBeginning);
            }
        };
        
        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
