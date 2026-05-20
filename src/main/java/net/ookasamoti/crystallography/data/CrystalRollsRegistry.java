package net.ookasamoti.crystallography.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CrystalRollsRegistry {
    public record Entry(Identifier item, int weight, String type) {}

    static final Map<Identifier, List<Entry>> BY_INPUT = new HashMap<>();
    public static void clear() { BY_INPUT.clear(); }
    public static void put(Identifier in, List<Entry> list){ BY_INPUT.put(in, List.copyOf(list)); }

    public static Optional<List<Entry>> get(ItemStack input) {
        var itemKey = BuiltInRegistries.ITEM.getKey(input.getItem());
        var list = BY_INPUT.get(itemKey);
        if (list != null) return Optional.of(list);

        // BlockItem ならブロックIDでも引く
        if (input.getItem() instanceof net.minecraft.world.item.BlockItem bi) {
            var blockKey = BuiltInRegistries.BLOCK.getKey(bi.getBlock());
            list = BY_INPUT.get(blockKey);
            if (list != null) return Optional.of(list);
        }

        return Optional.empty();
    }
}