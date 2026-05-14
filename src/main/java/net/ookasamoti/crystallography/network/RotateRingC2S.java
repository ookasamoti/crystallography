package net.ookasamoti.crystallography.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * C2S: リング回転通知（現在は CRYSTALS のみ想定）
 */
public record RotateRingC2S(int containerId, int ring, int steps) implements CustomPacketPayload {

    public static final int RING_TOOLS      = 0;
    public static final int RING_CRYSTALS   = 1;
    public static final int RING_REGISTRIES = 2;

    public static final Type<RotateRingC2S> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("crystallography", "rotate_ring"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RotateRingC2S> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, RotateRingC2S::containerId,
                    ByteBufCodecs.VAR_INT, RotateRingC2S::ring,
                    ByteBufCodecs.VAR_INT, RotateRingC2S::steps,
                    RotateRingC2S::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}