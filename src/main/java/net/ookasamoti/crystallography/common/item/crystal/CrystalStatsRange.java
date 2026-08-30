package net.ookasamoti.crystallography.common.item.crystal;

import net.ookasamoti.crystallography.common.util.LapidaryAnvilOperations;
import net.ookasamoti.crystallography.data.FloatRange;

public record CrystalStatsRange(
        int tier,
        FloatRange hardness,
        FloatRange cut,
        FloatRange carat,
        FloatRange clarity,
        java.util.Set<String> categories,
        java.util.Set<String> traits,
        @org.jetbrains.annotations.Nullable LapidaryAnvilOperations.CrackResult crackResult,
        int color
) {
    /** color未設定のデフォルト値（無着色） */
    public static final int NO_COLOR = -1;

    public boolean allFixed() {
        return hardness.isFixed() && cut.isFixed() && carat.isFixed() && clarity.isFixed();
    }

    /** DataComponent 用の確定値を生成 */
    public CrystalStats resolveToStats(net.minecraft.util.RandomSource rng) {
        float h  = hardness.isFixed() ? hardness.min() : lerp(rng.nextFloat(), hardness);
        float ct = cut.isFixed()      ? cut.min()      : lerp(rng.nextFloat(), cut);
        float c  = carat.isFixed()    ? carat.min()    : lerp(rng.nextFloat(), carat);
        float p  = clarity.isFixed()  ? clarity.min()  : lerp(rng.nextFloat(), clarity);
        // carat（エンチャント上限）は整数値で扱う
        return new CrystalStats(Math.round(h), ct, Math.round(c), p);
    }

    private static float lerp(float t, FloatRange r) {
        return r.min() + (r.max() - r.min()) * t;
    }
}
