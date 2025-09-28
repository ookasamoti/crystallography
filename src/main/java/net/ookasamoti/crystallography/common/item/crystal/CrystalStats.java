package net.ookasamoti.crystallography.common.item.crystal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CrystalStats(int hardness, float weight, float purity) {
    public static final CrystalStats DEFAULT = new CrystalStats(0, 1f, 1f);

    public static final Codec<CrystalStats> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("hardness").forGetter(CrystalStats::hardness),
            Codec.FLOAT.fieldOf("weight").forGetter(CrystalStats::weight),
            Codec.FLOAT.fieldOf("purity").forGetter(CrystalStats::purity)
    ).apply(i, CrystalStats::new));

    public static final StreamCodec<FriendlyByteBuf, CrystalStats> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CrystalStats::hardness,
                    ByteBufCodecs.FLOAT,   CrystalStats::weight,
                    ByteBufCodecs.FLOAT,   CrystalStats::purity,
                    CrystalStats::new
            );
}

