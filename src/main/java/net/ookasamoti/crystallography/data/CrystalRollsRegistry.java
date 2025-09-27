package net.ookasamoti.crystallography.data;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CrystalRollsRegistry {
    public record Entry(ResourceLocation item, int weight, String type) {}

    private static final Map<ResourceLocation, List<Entry>> BY_INPUT = new HashMap<>();
    public static void clear() { BY_INPUT.clear(); }
    public static void put(ResourceLocation in, List<Entry> list){ BY_INPUT.put(in, List.copyOf(list)); }

    public static Optional<List<Entry>> get(net.minecraft.world.item.ItemStack input) {
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(input.getItem());
        return Optional.ofNullable(BY_INPUT.get(key));
    }
}