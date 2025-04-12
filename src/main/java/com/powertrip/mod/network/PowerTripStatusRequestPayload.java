package com.powertrip.mod.network;

import com.powertrip.mod.PowerTripMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client-to-server payload for requesting current PowerTrip status
 * Acts as a heartbeat to ensure client display state is synchronized with server
 */
public record PowerTripStatusRequestPayload() implements CustomPayload {
    // Create an ID for this payload type
    public static final CustomPayload.Id<PowerTripStatusRequestPayload> ID = new CustomPayload.Id<>(
            Identifier.of(PowerTripMod.MOD_ID, "power_status_request"));
    
    // Create a codec to serialize/deserialize the payload
    // This payload has no data fields, but we still need the codec
    public static final PacketCodec<PacketByteBuf, PowerTripStatusRequestPayload> CODEC = new PacketCodec<>() {
        @Override
        public void encode(PacketByteBuf buf, PowerTripStatusRequestPayload payload) {
            // No data to encode
        }
        
        @Override
        public PowerTripStatusRequestPayload decode(PacketByteBuf buf) {
            return new PowerTripStatusRequestPayload();
        }
    };
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
