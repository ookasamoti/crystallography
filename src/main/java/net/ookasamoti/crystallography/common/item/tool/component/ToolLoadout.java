package net.ookasamoti.crystallography.common.item.tool.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

public record ToolLoadout(ToolForm form, ItemStack[] crystals) {

    // --- ItemStack[] <-> json のための補助 Codec ---
    private static final Codec<ItemStack[]> ITEMSTACK_ARRAY_CODEC =
            ItemStack.CODEC.listOf().xmap(
                    list -> list.toArray(ItemStack[]::new),
                    Arrays::asList
            );

    // --- ItemStack[] <-> ByteBuf のための補助 StreamCodec ---
    private static final StreamCodec<RegistryFriendlyByteBuf, ItemStack[]> ITEMSTACK_ARRAY_STREAM_CODEC =
            ItemStack.STREAM_CODEC
                    .apply(ByteBufCodecs.list()) // List<ItemStack>
                    .map(list -> list.toArray(ItemStack[]::new), Arrays::asList);

    // ---------- JSON Codec ----------
    public static final Codec<ToolLoadout> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.xmap(ToolForm::valueOf, ToolForm::name)
                    .fieldOf("form").forGetter(ToolLoadout::form),
            ITEMSTACK_ARRAY_CODEC
                    .fieldOf("crystals").forGetter(ToolLoadout::crystals)
    ).apply(i, ToolLoadout::new));

    // ---------- Network StreamCodec ----------
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolLoadout> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.map(ToolForm::valueOf, ToolForm::name), ToolLoadout::form,
                    ITEMSTACK_ARRAY_STREAM_CODEC, ToolLoadout::crystals,
                    ToolLoadout::new
            );
}
