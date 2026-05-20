package net.ookasamoti.crystallography.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record ToolCycleC2S(int delta) implements CustomPacketPayload {
    public static final Type<ToolCycleC2S> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("crystallography", "tool_cycle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolCycleC2S> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ToolCycleC2S::delta,
                    ToolCycleC2S::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
