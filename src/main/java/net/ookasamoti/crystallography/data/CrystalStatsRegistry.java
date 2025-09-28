package net.ookasamoti.crystallography.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.ookasamoti.crystallography.common.item.crystal.CrystalStatsRange;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class CrystalStatsRegistry {
    private static final Map<ResourceLocation, CrystalStatsRange> BY_ITEM = new HashMap<>();
    private static final java.util.Set<net.minecraft.resources.ResourceLocation> FIXED = new java.util.HashSet<>();
    private static final java.util.Set<net.minecraft.resources.ResourceLocation> VARIABLE = new java.util.HashSet<>();

    public static void clear() {
        BY_ITEM.clear();
        FIXED.clear();
        VARIABLE.clear();
    }

    public static void put(net.minecraft.resources.ResourceLocation itemId, CrystalStatsRange spec) {
        BY_ITEM.put(itemId, spec);
        if (spec.allFixed()) {
            FIXED.add(itemId);
            VARIABLE.remove(itemId);
        } else {
            FIXED.remove(itemId);
            VARIABLE.add(itemId);
        }
    }

    public static boolean isFixed(net.minecraft.world.item.ItemStack stack) {
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return FIXED.contains(key);
    }
    public static boolean isVariable(net.minecraft.world.item.ItemStack stack) {
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return VARIABLE.contains(key);
    }

    public static Optional<CrystalStatsRange> get(ResourceLocation itemId) {
        return Optional.ofNullable(BY_ITEM.get(itemId));
    }

    public static Optional<CrystalStatsRange> get(ItemStack stack) {
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return Optional.ofNullable(BY_ITEM.get(key));
    }
    public static int size() {
        return BY_ITEM.size();
    }
    public static java.util.Set<ResourceLocation> keys() {
        return java.util.Set.copyOf(BY_ITEM.keySet());
    }
}
