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

    /** slotIndex が一致するエントリを返す。 */
    public java.util.Optional<ToolLoadout> getAtSlot(int slotIndex) {
        return entries.stream().filter(e -> e.slotIndex() == slotIndex).findFirst();
    }

    public boolean full(int maxLoadouts){ return entries.size() >= maxLoadouts; }
}
