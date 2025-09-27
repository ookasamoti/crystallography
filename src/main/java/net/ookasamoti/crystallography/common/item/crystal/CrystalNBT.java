package net.ookasamoti.crystallography.common.item.crystal;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.ookasamoti.crystallography.data.CrystalStatsRegistry;

import java.util.Optional;
import java.util.Random;
import java.util.Set;

public final class CrystalNBT {
    public static final String TAG = "crystallography:stats";
    public static final String HARDNESS = "hardness";
    public static final String CARAT = "carat";
    public static final String CLARITY = "clarity";
    public static final String TIER = "tier";

    /** CustomData(CUSTOM_DATA) から stats サブタグを読む */
    private static CompoundTag getStatsTag(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return null;
        // CustomDataは任意のCompoundTagを内包。ここではその中の TAG サブタグを使う。
        CompoundTag root = cd.copyTag(); // defensive copy
        if (root == null || !root.contains(TAG)) return null;
        return root.getCompound(TAG);
    }

    /** CustomData(CUSTOM_DATA) に stats サブタグを書き戻す（他の既存カスタムデータは保持） */
    private static void setStatsTag(ItemStack stack, CompoundTag stats) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = (cd != null) ? cd.copyTag() : new CompoundTag();
        root.put(TAG, stats);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public static Optional<CrystalSpecResolved> fromNBT(ItemStack stack) {
        CompoundTag s = getStatsTag(stack);
        if (s == null) return Optional.empty();
        return Optional.of(new CrystalSpecResolved(
                s.getInt(TIER),
                s.getFloat(HARDNESS),
                s.getFloat(CARAT),
                s.getFloat(CLARITY),
                Set.of() // categories を保持したければここに保存・復元を追加
        ));
    }

    public static void write(ItemStack stack, CrystalSpecResolved v) {
        CompoundTag s = new CompoundTag();
        s.putInt(TIER, v.tier());
        s.putFloat(HARDNESS, v.hardness());
        s.putFloat(CARAT, v.carat());
        s.putFloat(CLARITY, v.clarity());
        setStatsTag(stack, s);
    }

    /** ランダム必要なら確定してNBTに書く。固定値なら不要（書かない） */
    public static void assignRandomIfNeeded(ItemStack stack, Random rng) {
        if (fromNBT(stack).isPresent()) return;

        var rangeOpt = CrystalStatsRegistry.get(stack);
        if (rangeOpt.isEmpty()) return;

        var range = rangeOpt.get();
        if (!range.allFixed()) {
            var resolved = range.resolve(rng);
            write(stack, resolved);
        }
    }

    /** 表示や使用時に「Resolved」を取得。NBT優先、無ければ固定値から合成 */
    public static Optional<CrystalSpecResolved> resolveForUse(ItemStack stack) {
        var from = fromNBT(stack);
        if (from.isPresent()) return from;

        var range = CrystalStatsRegistry.get(stack).orElse(null);
        if (range == null) return Optional.empty();

        if (range.allFixed()) {
            return Optional.of(new CrystalSpecResolved(
                    range.tier(),
                    range.hardness().min(),
                    range.carat().min(),
                    range.clarity().min(),
                    range.categories()
            ));
        }
        return Optional.empty();
    }
}
