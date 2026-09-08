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
        var draft = ToolBase.getDraftLoadout(stack);
        var lo = draft != null ? draft : ToolBase.getActiveLoadout(stack);
        if (lo == null) return -1;

        int[] indices = lo.crystalIndices();
        if (crystalSlot >= indices.length) return -1;
        int crystalIndex = indices[crystalSlot];
        if (crystalIndex < 0) return -1;

        if (registryAccess == null) return -1;
        int tier = (stack.getItem() instanceof ICrystalTool ct) ? ct.getTier() : 1;
        var inv = ToolInventory.get(stack, ToolBase.crystalSlotCount(tier), registryAccess);
        var crystal = inv.getResource(crystalIndex).toStack(inv.getAmountAsInt(crystalIndex));
        if (crystal.isEmpty()) return -1;

        return CrystalStatsRegistry.get(crystal).map(r -> r.color()).orElse(-1);
    }
}
