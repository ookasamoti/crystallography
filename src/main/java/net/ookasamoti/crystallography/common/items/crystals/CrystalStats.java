package net.ookasamoti.crystallography.common.items.crystals;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.List;

import static net.ookasamoti.crystallography.common.util.RangeUtil.roll;

public record CrystalStats(float weight, float purity, int hardness, List<ResourceLocation> traits) {
    public static final CrystalStats DEFAULT = new CrystalStats(1f,1f,0,List.of());

    public static final Codec<CrystalStats> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("weight").forGetter(CrystalStats::weight),
            Codec.FLOAT.fieldOf("purity").forGetter(CrystalStats::purity),
            Codec.INT.fieldOf("hardness").forGetter(CrystalStats::hardness),
            ResourceLocation.CODEC.listOf().fieldOf("traits").forGetter(CrystalStats::traits)
    ).apply(i, CrystalStats::new));

    public static final StreamCodec<FriendlyByteBuf, CrystalStats> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, CrystalStats::weight,
                    ByteBufCodecs.FLOAT, CrystalStats::purity,
                    ByteBufCodecs.VAR_INT, CrystalStats::hardness,
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), CrystalStats::traits,
                    CrystalStats::new
            );

    public static CrystalStats rollFrom(CrystalSpec spec, RandomSource r) {
        float carat   = roll(spec.caratMin(),   spec.caratMax(),   r);
        float clarity = roll(spec.clarityMin(), spec.clarityMax(), r);
        int hardness  = spec.hardness();
        var traits = List.copyOf(spec.categories()); // ひとまず categories をそのまま表示用に
        return new CrystalStats(carat, clarity, hardness, traits);
    }
}
