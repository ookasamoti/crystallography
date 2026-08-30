package net.ookasamoti.crystallography.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record JewelryActionC2S(int containerId, int action, int param) implements CustomPacketPayload {

    /** param: formIndex (0-5 = select, -1 = deselect) */
    public static final int ACTION_SELECT_FORM    = 0;
    /** param: crystalBacking (backing slot index) */
    public static final int ACTION_TOGGLE_CRYSTAL = 1;
    /** param: formIndex to register to */
    public static final int ACTION_REGISTER       = 2;
    /** param: registry slotIndex to start editing (must already hold a registered loadout) */
    public static final int ACTION_EDIT_REGISTRY    = 3;
    /** param: unused (0) — Esc cancel of the current pending edit */
    public static final int ACTION_CANCEL           = 4;
    /** param: unused (0) — Space deletes the loadout at editingRegistrySlot */
    public static final int ACTION_DELETE_REGISTRY  = 5;
    /** param: target registry slotIndex to swap the edited slot's contents with */
    public static final int ACTION_SWAP_REGISTRY    = 6;

    public static final Type<JewelryActionC2S> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("crystallography", "jewelry_action"));

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
