package net.ookasamoti.crystallography.common.item.crystal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;

/** 結晶に露天堀り採掘台で付与済みのシジル(アイテムID)一覧。 */
public record AttachedSigils(List<Identifier> sigils) {
    public static final AttachedSigils EMPTY = new AttachedSigils(List.of());

    public static final Codec<AttachedSigils> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.listOf().fieldOf("sigils").forGetter(AttachedSigils::sigils)
    ).apply(i, AttachedSigils::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AttachedSigils> STREAM_CODEC =
            StreamCodec.composite(
                    Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), AttachedSigils::sigils,
                    AttachedSigils::new
            );

    public AttachedSigils withAdded(Identifier sigilItem) {
        var mod = new java.util.ArrayList<>(sigils);
        mod.add(sigilItem);
        return new AttachedSigils(List.copyOf(mod));
    }
}
