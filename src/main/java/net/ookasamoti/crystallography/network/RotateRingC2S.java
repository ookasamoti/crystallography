package net.ookasamoti.crystallography.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record RotateRingC2S(int containerId, int ring, int steps) implements CustomPacketPayload {

    public static final int RING_CRYSTALS = 1;

    public static final Type<RotateRingC2S> TYPE =
            new Type<>(ResourceLocation.parse("crystallography:rotate_ring"));

    public static final StreamCodec<FriendlyByteBuf, RotateRingC2S> STREAM_CODEC =
            CustomPacketPayload.codec(RotateRingC2S::write, RotateRingC2S::read);

    private static RotateRingC2S read(FriendlyByteBuf buf) {
        int c = buf.readVarInt();
        int r = buf.readVarInt();
        int s = buf.readVarInt();
        return new RotateRingC2S(c, r, s);
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeVarInt(ring);
        buf.writeVarInt(steps);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
