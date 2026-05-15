package net.ookasamoti.crystallography.common.item.tool.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 登録時に1回だけ計算され、以後変更されない性能値。
 * 結晶が破損してもこの値は変わらず、修繕で耐久が戻れば同じ性能で復帰する。
 */
public record ToolStats(
        int   tier,
        int   durability,
        float attackDamage,
        float attackSpeed,
        float miningSpeed
) {
    public static final ToolStats DEFAULT = new ToolStats(1, 1, 1f, 1f, 1f);

    public static final Codec<ToolStats> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("tier").forGetter(ToolStats::tier),
            Codec.INT.fieldOf("durability").forGetter(ToolStats::durability),
            Codec.FLOAT.fieldOf("attack_damage").forGetter(ToolStats::attackDamage),
            Codec.FLOAT.fieldOf("attack_speed").forGetter(ToolStats::attackSpeed),
            Codec.FLOAT.fieldOf("mining_speed").forGetter(ToolStats::miningSpeed)
    ).apply(i, ToolStats::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolStats> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ToolStats::tier,
                    ByteBufCodecs.VAR_INT, ToolStats::durability,
                    ByteBufCodecs.FLOAT,   ToolStats::attackDamage,
                    ByteBufCodecs.FLOAT,   ToolStats::attackSpeed,
                    ByteBufCodecs.FLOAT,   ToolStats::miningSpeed,
                    ToolStats::new
            );
}
