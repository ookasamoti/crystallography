package net.ookasamoti.crystallography.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record JewelryActionC2S(int containerId, int action, int param) implements CustomPacketPayload {

    /** param: formIndex (0-5 = select, -1 = deselect) */
    public static final int ACTION_SELECT_FORM    = 0;
    /** param: crystalBacking (backing slot index) */
    public static final int ACTION_TOGGLE_CRYSTAL = 1;
    /** param: formIndex to register to */
    public static final int ACTION_REGISTER       = 2;

    public static final Type<JewelryActionC2S> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("crystallography", "jewelry_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JewelryActionC2S> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, JewelryActionC2S::containerId,
                    ByteBufCodecs.VAR_INT, JewelryActionC2S::action,
                    ByteBufCodecs.VAR_INT, JewelryActionC2S::param,
                    JewelryActionC2S::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
