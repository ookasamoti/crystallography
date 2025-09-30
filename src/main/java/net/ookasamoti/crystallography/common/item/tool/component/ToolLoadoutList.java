package net.ookasamoti.crystallography.common.item.tool.component;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ToolLoadoutList(java.util.List<ToolLoadout> entries) {
    public static final Codec<ToolLoadoutList> CODEC =
            ToolLoadout.CODEC.listOf().xmap(ToolLoadoutList::new, ToolLoadoutList::entries);

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolLoadoutList> STREAM_CODEC =
            ToolLoadout.STREAM_CODEC.apply(ByteBufCodecs.list())
                    .map(ToolLoadoutList::new, ToolLoadoutList::entries);

    public int clampActive(int idx){ return entries.isEmpty() ? 0 : Math.floorMod(idx, entries.size()); }
    public boolean full(){ return entries.size() >= 30; } // 上限30
}
