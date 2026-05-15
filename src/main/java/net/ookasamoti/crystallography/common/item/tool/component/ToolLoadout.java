package net.ookasamoti.crystallography.common.item.tool.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Arrays;

public record ToolLoadout(int slotIndex, ToolForm form, int[] crystalIndices, ToolStats stats) {

    /** 1つのツール登録に使う結晶スロット数（固定）。 */
    public static final int CRYSTAL_SLOTS = 3;

    public ToolLoadout {
        if (crystalIndices.length != CRYSTAL_SLOTS)
            throw new IllegalArgumentException("crystalIndices must have exactly " + CRYSTAL_SLOTS + " elements");
    }

    // ---- JSON Codec ----
    private static final Codec<int[]> INT_ARRAY_CODEC =
            Codec.INT.listOf().xmap(
                    list -> list.stream().mapToInt(Integer::intValue).toArray(),
                    arr  -> Arrays.stream(arr).boxed().toList()
            );

    public static final Codec<ToolLoadout> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("slot_index", 0)
                    .forGetter(ToolLoadout::slotIndex),
            Codec.STRING.xmap(ToolForm::valueOf, ToolForm::name)
                    .fieldOf("form").forGetter(ToolLoadout::form),
            INT_ARRAY_CODEC
                    .fieldOf("crystal_indices").forGetter(ToolLoadout::crystalIndices),
            ToolStats.CODEC
                    .fieldOf("stats").forGetter(ToolLoadout::stats)
    ).apply(i, ToolLoadout::new));

    // ---- Network StreamCodec ----
    private static final StreamCodec<RegistryFriendlyByteBuf, int[]> INT3_STREAM_CODEC =
            StreamCodec.of(
                    (buf, arr) -> { for (int v : arr) buf.writeVarInt(v); },
                    buf -> new int[]{ buf.readVarInt(), buf.readVarInt(), buf.readVarInt() }
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolLoadout> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,                                                   ToolLoadout::slotIndex,
                    ByteBufCodecs.STRING_UTF8.map(ToolForm::valueOf, ToolForm::name),        ToolLoadout::form,
                    INT3_STREAM_CODEC,                                                       ToolLoadout::crystalIndices,
                    ToolStats.STREAM_CODEC,                                                  ToolLoadout::stats,
                    ToolLoadout::new
            );
}
