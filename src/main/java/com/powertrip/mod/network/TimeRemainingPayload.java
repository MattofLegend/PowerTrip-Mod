package com.powertrip.mod.network;

import com.powertrip.mod.PowerTripMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Custom payload for time remaining information
 * Using the modern networking API for Minecraft 1.21.4
 */
public record TimeRemainingPayload(int daysRemaining, int hoursRemaining, boolean isPowerTripActive) implements CustomPayload {
    // Create an ID for this payload type
    public static final CustomPayload.Id<TimeRemainingPayload> ID = new CustomPayload.Id<>(
            Identifier.of(PowerTripMod.MOD_ID, "time_remaining"));
    
    // Create a codec to serialize/deserialize the payload
    public static final PacketCodec<PacketByteBuf, TimeRemainingPayload> CODEC = new PacketCodec<>() {
        @Override
        public void encode(PacketByteBuf buf, TimeRemainingPayload payload) {
            buf.writeInt(payload.daysRemaining);
            buf.writeInt(payload.hoursRemaining);
            buf.writeBoolean(payload.isPowerTripActive);
        }
        
        @Override
        public TimeRemainingPayload decode(PacketByteBuf buf) {
            int days = buf.readInt();
            int hours = buf.readInt();
            boolean isActive = buf.readBoolean();
            return new TimeRemainingPayload(days, hours, isActive);
        }
    };
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
