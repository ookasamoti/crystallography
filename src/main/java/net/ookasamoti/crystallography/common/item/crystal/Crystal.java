package net.ookasamoti.crystallography.common.item.crystal;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.data.CrystalStatsRegistry;
import net.ookasamoti.crystallography.setup.DataComponentsRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

public class Crystal extends Item {
    public Crystal(Properties props){ super(props); }

    public void getOrCreateStats(ItemStack stack, @Nullable Level level) {
        var type = DataComponentsRegistry.CRYSTAL_STATS.get();

        if (stack.get(type) != null) return;

        var rangeOpt = net.ookasamoti.crystallography.data.CrystalStatsRegistry.get(stack);
        if (rangeOpt.isEmpty()) return;

        var range = rangeOpt.get();
        var rng = (level != null) ? level.getRandom() : net.minecraft.util.RandomSource.create();

        CrystalStats stats;
        if (range.allFixed()) {
            stats = new CrystalStats(
                    range.carat().min(),
                    range.clarity().min(),
                    Math.round(range.hardness().min()),
                    toTraits(range.categories())
            );
        } else {
            var v = range.resolve((RandomGenerator) rng);
            stats = new CrystalStats(
                    v.carat(),
                    v.clarity(),
                    Math.round(v.hardness()),
                    toTraits(v.categories())
            );
        }

        stack.set(type, stats);
    }

    private static List<ResourceLocation> toTraits(Set<String> cats) {
        if (cats == null || cats.isEmpty()) return List.of();
        var out = new ArrayList<ResourceLocation>(cats.size());
        for (String s : cats) {
            var rl = ResourceLocation.tryParse(s);
            if (rl == null) {
                rl = ResourceLocation.fromNamespaceAndPath(
                        CrystallographyMod.MOD_ID, s);
            } else if (rl.getNamespace().isEmpty()) {
                rl = ResourceLocation.fromNamespaceAndPath(
                        CrystallographyMod.MOD_ID, rl.getPath());
            }
            out.add(rl);
        }
        return java.util.List.copyOf(out);
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack s, Level l, net.minecraft.world.entity.player.@NotNull Player p){
        if(!l.isClientSide) getOrCreateStats(s,l);
    }

    @Override
    public void inventoryTick(ItemStack s, Level l, net.minecraft.world.entity.Entity e, int slot, boolean sel){
        if(!l.isClientSide) getOrCreateStats(s,l);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack,
                                @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip,
                                @NotNull TooltipFlag flag) {
        CrystalSpecResolved resolved = CrystalNBT.resolveForUse(stack).orElseGet(() ->
                CrystalStatsRegistry.get(stack)
                        .filter(CrystalSpecRange::allFixed)
                        .map(r -> new CrystalSpecResolved(
                                r.tier(),
                                r.hardness().min(),
                                r.carat().min(),
                                r.clarity().min(),
                                r.categories()
                        ))
                        .orElse(null)
        );





        // Shift
        if (resolved != null) {
            tooltip.add(Component.translatable(
                    "tooltip.crystallography.hardness",
                    format1(resolved.hardness())
            ).withStyle(ChatFormatting.GRAY));

            tooltip.add(Component.translatable(
                    "tooltip.crystallography.carat",
                    format2(resolved.carat())
            ).withStyle(ChatFormatting.GRAY));

            tooltip.add(Component.translatable(
                    "tooltip.crystallography.clarity",
                    format2(resolved.clarity())
            ).withStyle(ChatFormatting.GRAY));

            if (!Screen.hasShiftDown()) {
                // ◈ Crystal item
                tooltip.add(Component.literal("◈ ")
                        .append(Component.translatable("tooltip.crystallography.crystal_item"))
                        .withStyle(ChatFormatting.AQUA));
                // Press Shift for more info
                tooltip.add(Component.translatable("tooltip.crystallography.press_shift")
                        .withStyle(ChatFormatting.DARK_GRAY));
            } else {
                if (resolved.categories() != null && !resolved.categories().isEmpty()) {
                    for (String cat : resolved.categories()) {
                        // [category]
                        tooltip.add(Component.literal("[" + cat + "]").withStyle(ChatFormatting.GOLD));
                        tooltip.add(Component.translatable("tooltip.crystallography.category." + cat + ".desc")
                                .withStyle(ChatFormatting.DARK_GRAY));
                    }
                }
            }

        } else {
            tooltip.add(Component.translatable("tooltip.crystallography.unresolved")
                    .withStyle(ChatFormatting.RED));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }

    private static String format1(float f) {
        return String.format(java.util.Locale.ROOT, "%.1f", f);
    }
    private static String format2(float f) {
        return String.format(java.util.Locale.ROOT, "%.2f", f);
    }


}