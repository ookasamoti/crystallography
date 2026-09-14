package net.ookasamoti.crystallography.client.color;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.ookasamoti.crystallography.common.item.armor.AmuletColorHelper;
import org.jetbrains.annotations.Nullable;

/**
 * アミュレット（頭/胴/脚/足）のインベントリアイコン用、結晶スロット1個ぶんの色を解決するタイント。
 * {@code CrystalTintSource}（ツール用、ロードアウトの結晶インデックス経由）と違い、アミュレットは
 * 3枠の結晶インベントリに直接対応するので {@link AmuletColorHelper#colorForSlot} をそのまま使う。
 * <p>
 * 結晶が入っていないスロットは3D装備描画と同じ既定色（{@link AmuletColorHelper#EMPTY_SLOT_COLOR}、
 * #FFFFFF）にする。アイテムモデル定義の {@code tints} 配列から
 * {@code {"type": "crystallography:amulet_crystal", "crystal_slot": N}}（N=0〜2）として参照する。
 */
public record AmuletCrystalTintSource(int crystalSlot) implements ItemTintSource {

    public static final MapCodec<AmuletCrystalTintSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Codec.intRange(0, 2).fieldOf("crystal_slot").forGetter(AmuletCrystalTintSource::crystalSlot)
            ).apply(i, AmuletCrystalTintSource::new));

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        int color = AmuletColorHelper.colorForSlot(stack, level != null ? level.registryAccess() : null, crystalSlot);
        return color == AmuletColorHelper.NO_COLOR ? AmuletColorHelper.EMPTY_SLOT_COLOR : color;
    }

    @Override
    public MapCodec<AmuletCrystalTintSource> type() {
        return MAP_CODEC;
    }
}
