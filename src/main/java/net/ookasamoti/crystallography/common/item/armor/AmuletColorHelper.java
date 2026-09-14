package net.ookasamoti.crystallography.common.item.armor;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.ookasamoti.crystallography.common.item.tool.ToolInventory;
import net.ookasamoti.crystallography.data.CrystalStatsRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * アミュレットの3個の結晶スロットそれぞれの色を解決する。{@code CrystalColorHelper}
 * （ツール用）と違い、アミュレットにはロードアウト/結晶インデックスの間接参照が無く、
 * 3枠のインベントリがそのまま3個の結晶キューブに対応する。
 */
public final class AmuletColorHelper {
    /** 結晶が入っていない/色未設定のスロットを表す値。 */
    public static final int NO_COLOR = -1;

    /** 結晶未装着スロットの既定色 (#FFFFFF)。3D装備描画・インベントリアイコン双方で共通。 */
    public static final int EMPTY_SLOT_COLOR = 0xFFFFFFFF;

    private AmuletColorHelper() {
    }

    /** 1スロットぶんの色（結晶が無ければ {@link #NO_COLOR}）。 */
    public static int colorForSlot(ItemStack stack, @Nullable RegistryAccess registryAccess, int slot) {
        return colorsForAllSlots(stack, registryAccess)[slot];
    }

    public static int[] colorsForAllSlots(ItemStack stack, @Nullable RegistryAccess registryAccess) {
        int[] colors = new int[IAmuletItem.CRYSTAL_SLOTS];
        java.util.Arrays.fill(colors, NO_COLOR);
        if (registryAccess == null) return colors;

        var inv = ToolInventory.get(stack, IAmuletItem.CRYSTAL_SLOTS, registryAccess);
        for (int i = 0; i < colors.length && i < inv.size(); i++) {
            var crystal = inv.getResource(i).toStack(inv.getAmountAsInt(i));
            if (crystal.isEmpty()) continue;
            colors[i] = CrystalStatsRegistry.get(crystal).map(r -> r.color()).orElse(NO_COLOR);
        }
        return colors;
    }
}
