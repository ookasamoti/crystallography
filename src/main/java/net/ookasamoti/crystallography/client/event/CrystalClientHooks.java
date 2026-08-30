package net.ookasamoti.crystallography.client.event;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.ookasamoti.crystallography.common.item.crystal.CrystalStatsRange;
import net.ookasamoti.crystallography.common.item.crystal.CrystalStats;
import net.ookasamoti.crystallography.data.CrystalStatsRegistry;
import net.ookasamoti.crystallography.data.FloatRange;
import net.ookasamoti.crystallography.setup.DataComponentsRegistry;

import java.util.List;
import java.util.function.Function;

public final class CrystalClientHooks {
    private CrystalClientHooks() {}

    public static void bootstrapClient() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(CrystalClientHooks::onItemTooltip);
    }

    private static void onItemTooltip(ItemTooltipEvent e) {
        ItemStack stack = e.getItemStack();

        var rangeOpt = CrystalStatsRegistry.get(stack);
        if (rangeOpt.isEmpty()) return;
        var range = rangeOpt.get();

        e.getToolTip().add(Component.literal("◈ ")
                .append(Component.translatable("tooltip.crystallography.crystal_item"))
                .withStyle(ChatFormatting.AQUA));

        var statsType = DataComponentsRegistry.CRYSTAL_STATS.get();
        CrystalStats stats = stack.get(statsType);

        if(stats == null && !range.allFixed()) {
            e.getToolTip().add(Component.translatable("tooltip.crystallography.unresolved")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        addStatLine(e.getToolTip(), "tooltip.crystallography.hardness_label",
                stats, range, CrystalStats::hardness, CrystalStatsRange::hardness, true);

        addStatLine(e.getToolTip(), "tooltip.crystallography.cut_label",
                stats, range, CrystalStats::cut,      CrystalStatsRange::cut,      false);

        addStatLine(e.getToolTip(), "tooltip.crystallography.carat_label",
                stats, range, CrystalStats::carat,    CrystalStatsRange::carat,    true);

        addStatLine(e.getToolTip(), "tooltip.crystallography.clarity_label",
                stats, range, CrystalStats::clarity,  CrystalStatsRange::clarity,  false);

        if (!Minecraft.getInstance().hasShiftDown()) {
            e.getToolTip().add(Component.translatable("tooltip.crystallography.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else if (range.categories() != null && !range.categories().isEmpty()) {
            for (String cat : range.categories()) {
                e.getToolTip().add(Component.translatable("tooltip.crystallography.category." + cat + ".name")
                        .withStyle(ChatFormatting.GOLD));
                e.getToolTip().add(Component.translatable("tooltip.crystallography.category." + cat + ".desc")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    private static void addStatLine(
            List<Component> out, String labelKey,
            CrystalStats stats, CrystalStatsRange range,
            Function<CrystalStats, Number> nbtGetter,
            Function<CrystalStatsRange, FloatRange> rangeGetter,
            boolean integerLike
    ) {
        var r = rangeGetter.apply(range);
        float minF = r.min();
        float maxF = r.max();
        boolean fixed = r.isFixed();

        float curF;
        if (stats != null) {
            Number v = nbtGetter.apply(stats);
            curF = integerLike ? (float) v.intValue() : v.floatValue();
        } else if (fixed) {
            curF = minF;
        } else {
            return;
        }

        String curText = integerLike ? Integer.toString(Math.round(curF)) : format2(curF);
        String minText = integerLike ? Integer.toString(Math.round(minF)) : format2(minF);
        String maxText = integerLike ? Integer.toString(Math.round(maxF)) : format2(maxF);

        ChatFormatting valueColor = (fixed || Float.compare(curF, maxF) == 0)
                ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY;

        if (minF == maxF) {
            out.add(Component.empty()
                    .append(Component.literal(curText).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" "))
                    .append(Component.translatable(labelKey).withStyle(ChatFormatting.GRAY)));
        } else {
            out.add(Component.empty()
                    .append(Component.literal(curText).withStyle(valueColor))
                    .append(Component.literal(" "))
                    .append(Component.translatable(labelKey).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(minText).withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(maxText).withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY)));
        }
    }

    private static String format2(float f) {
        return String.format(java.util.Locale.ROOT, "%.2f", f);
    }
}
