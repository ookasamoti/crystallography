package net.ookasamoti.crystallography.common.items.crystals;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.ookasamoti.crystallography.setup.DataComponentsRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Crystal extends Item {
    public Crystal(Properties props){ super(props); }

    public void getOrCreateStats(ItemStack stack, @Nullable Level level){
        DataComponentType<CrystalStats> type = DataComponentsRegistry.CRYSTAL_STATS.get();
        CrystalStats stats = stack.get(type);
        if (stats != null) return;

        var specOpt = CrystalSpecs.find(stack.getItem());
        if (specOpt.isEmpty()) return;

        CrystalStats rolled = CrystalStats.rollFrom(specOpt.get(),
                level != null ? level.getRandom() : net.minecraft.util.RandomSource.create());
        stack.set(type, rolled);
    }

    @Override
    public void onCraftedBy(ItemStack s, Level l, net.minecraft.world.entity.player.Player p){
        if(!l.isClientSide) getOrCreateStats(s,l);
    }

    @Override
    public void inventoryTick(ItemStack s, Level l, net.minecraft.world.entity.Entity e, int slot, boolean sel){
        if(!l.isClientSide) getOrCreateStats(s,l);
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                Item.TooltipContext ctx,
                                List<Component> tip,
                                TooltipFlag flag) {
        var type = DataComponentsRegistry.CRYSTAL_STATS.get();

        CrystalStats st = stack.get(type);
        if (st == null) {
            tip.add(Component.translatable("tooltip.crystallography.unidentified"));
            return;
        }

        tip.add(Component.translatable("tooltip.crystallography.weight",
                String.format("%.2f", st.weight())));
        tip.add(Component.translatable("tooltip.crystallography.purity",
                String.format("%.2f", st.purity())));
        tip.add(Component.translatable("tooltip.crystallography.hardness",
                st.hardness()));
        for (var t : st.traits()) {
            tip.add(Component.literal(" - " + t));
        }
    }

}