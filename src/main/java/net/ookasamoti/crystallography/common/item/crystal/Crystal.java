package net.ookasamoti.crystallography.common.item.crystal;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ookasamoti.crystallography.data.CrystalStatsRegistry;
import net.ookasamoti.crystallography.setup.DataComponentsRegistry;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class Crystal extends Item {
    public Crystal(Properties props){ super(props); }

    public void getOrCreateStats(ItemStack stack, @Nullable Level level) {
        var type = DataComponentsRegistry.CRYSTAL_STATS.get();
        if (stack.get(type) != null) return;

        var rangeOpt = CrystalStatsRegistry.get(stack);
        if (rangeOpt.isEmpty()) return;
        var range = rangeOpt.get();

        if (range.allFixed()) return; // 固定値は保存しない

        var rng = (level != null) ? level.getRandom() : net.minecraft.util.RandomSource.create();
        stack.set(type, range.resolveToStats(rng));
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack s, Level l, net.minecraft.world.entity.player.@NotNull Player p){
        if(!l.isClientSide) getOrCreateStats(s,l);
    }

//    @Override
//    public void inventoryTick(@NotNull ItemStack s, Level l, @NotNull Entity e, int slot, boolean sel){
//        if(!l.isClientSide) getOrCreateStats(s,l);
//    }
}

