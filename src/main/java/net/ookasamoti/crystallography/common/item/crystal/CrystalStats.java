package net.ookasamoti.crystallography.common.item.crystal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CrystalStats(int hardness, float carat, float clarity) {

    public static final Codec<CrystalStats> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("hardness").forGetter(CrystalStats::hardness),
            Codec.FLOAT.fieldOf("carat").forGetter(CrystalStats::carat),
            Codec.FLOAT.fieldOf("clarity").forGetter(CrystalStats::clarity)
    ).apply(i, CrystalStats::new));

    public static final StreamCodec<FriendlyByteBuf, CrystalStats> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CrystalStats::hardness,
                    ByteBufCodecs.FLOAT,   CrystalStats::carat,
                    ByteBufCodecs.FLOAT,   CrystalStats::clarity,
                    CrystalStats::new
            );
}
