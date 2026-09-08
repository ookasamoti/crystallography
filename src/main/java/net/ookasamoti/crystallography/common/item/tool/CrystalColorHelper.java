package net.ookasamoti.crystallography.common.item.tool;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.ookasamoti.crystallography.data.CrystalStatsRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the tint colour for one crystal slot (0=center, 1=left, 2=right) of a tool stack.
 * Common-side (not client-only) so it can be called both from client rendering code
 * ({@code CrystalTintSource}, {@code CrystalTridentSpecialRenderer}) and from server-side code
 * that needs to compute the same colours for network sync (see {@code TridentVisualData}).
 */
public final class CrystalColorHelper {
    private CrystalColorHelper() {
    }

    public static int colorForSlot(ItemStack stack, @Nullable RegistryAccess registryAccess, int crystalSlot) {
        return colorsForAllSlots(stack, registryAccess)[crystalSlot];
    }

    /**
     * Same as {@link #colorForSlot} but resolves all three crystal slots (center/left/right) at
     * once, building the stack's {@link ToolInventory} only once instead of once per slot. Prefer
     * this whenever more than one slot's colour is needed in the same place (e.g. a special-model
     * renderer tinting several overlay passes per frame) — {@code ToolInventory.get} deep-copies
     * and re-deserialises the crystal inventory's NBT on every call, so calling it three times for
     * the same stack is pure waste.
     */
    public static int[] colorsForAllSlots(ItemStack stack, @Nullable RegistryAccess registryAccess) {
        int[] colors = {-1, -1, -1};

        var draft = ToolBase.getDraftLoadout(stack);
        var lo = draft != null ? draft : ToolBase.getActiveLoadout(stack);
        if (lo == null || registryAccess == null) return colors;

        int[] indices = lo.crystalIndices();
        int tier = (stack.getItem() instanceof ICrystalTool ct) ? ct.getTier() : 1;
        var inv = ToolInventory.get(stack, ToolBase.crystalSlotCount(tier), registryAccess);

        for (int slot = 0; slot < colors.length && slot < indices.length; slot++) {
            int crystalIndex = indices[slot];
            if (crystalIndex < 0) continue;
            var crystal = inv.getResource(crystalIndex).toStack(inv.getAmountAsInt(crystalIndex));
            if (crystal.isEmpty()) continue;
            colors[slot] = CrystalStatsRegistry.get(crystal).map(r -> r.color()).orElse(-1);
        }
        return colors;
    }
}
