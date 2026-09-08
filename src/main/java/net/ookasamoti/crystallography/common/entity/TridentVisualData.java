package net.ookasamoti.crystallography.common.entity;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.ookasamoti.crystallography.common.item.tool.CrystalColorHelper;
import net.ookasamoti.crystallography.common.item.tool.ICrystalTool;

/**
 * Tier + per-crystal-slot tint colours needed to render a thrown crystal trident.
 *
 * <p>The client can never see the real weapon {@link ItemStack} a {@code ThrownTrident} carries
 * — vanilla only syncs a couple of individual bits (loyalty, foil) for that entity, not the full
 * stack — so this is computed server-side once (when the entity joins the level) and synced to
 * clients as an entity data attachment instead.
 */
public record TridentVisualData(int tier, int centerColor, int leftColor, int rightColor) {
    public static final TridentVisualData DEFAULT = new TridentVisualData(1, -1, -1, -1);

    public static final StreamCodec<RegistryFriendlyByteBuf, TridentVisualData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TridentVisualData::tier,
            ByteBufCodecs.INT, TridentVisualData::centerColor,
            ByteBufCodecs.INT, TridentVisualData::leftColor,
            ByteBufCodecs.INT, TridentVisualData::rightColor,
            TridentVisualData::new
    );

    public static TridentVisualData compute(ItemStack weapon, RegistryAccess registryAccess) {
        int tier = weapon.getItem() instanceof ICrystalTool ct ? Math.max(1, Math.min(3, ct.getTier())) : 1;
        int center = CrystalColorHelper.colorForSlot(weapon, registryAccess, 0);
        int left = CrystalColorHelper.colorForSlot(weapon, registryAccess, 1);
        int right = CrystalColorHelper.colorForSlot(weapon, registryAccess, 2);
        return new TridentVisualData(tier, center, left, right);
    }
}
