package net.ookasamoti.crystallography.client.event;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.ookasamoti.crystallography.common.item.crystal.CrystalSpecRange;
import net.ookasamoti.crystallography.common.item.crystal.CrystalStats;
import net.ookasamoti.crystallography.data.CrystalStatsRegistry;
import net.ookasamoti.crystallography.setup.DataComponentsRegistry;

import java.util.List;
import java.util.function.Function;

public final class CrystalClientHooks {
    private CrystalClientHooks() {}

    /** Modの初期化で呼び出す（Client環境限定） */
    public static void bootstrapClient() {
        // GAMEバスへリスナを登録（Client専用イベント）
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(CrystalClientHooks::onItemTooltip);
    }

    private static void onItemTooltip(ItemTooltipEvent e) {
        ItemStack stack = e.getItemStack();

        var rangeOpt = CrystalStatsRegistry.get(stack);
        if (rangeOpt.isEmpty()) return;
        var range = rangeOpt.get();

        // 見出し
        e.getToolTip().add(Component.literal("◈ ")
                .append(Component.translatable("tooltip.crystallography.crystal_item"))
                .withStyle(ChatFormatting.AQUA));

        var statsType = DataComponentsRegistry.CRYSTAL_STATS.get();
        CrystalStats stats = stack.get(statsType); // 固定値は保存されない設計なので null でもOK

        // 650 Hardness / 1.20 Carat (1.00 - 1.80) / 0.92 Clarity (min-max 常時表示)
        addStatLineSmart(e.getToolTip(), "tooltip.crystallography.hardness_label",
                stats, range, CrystalStats::hardness, CrystalSpecRange::hardness, true);

        addStatLineSmart(e.getToolTip(), "tooltip.crystallography.carat_label",
                stats, range, CrystalStats::weight,   CrystalSpecRange::carat,    false);

        addStatLineSmart(e.getToolTip(), "tooltip.crystallography.clarity_label",
                stats, range, CrystalStats::purity,   CrystalSpecRange::clarity,  false);

        if (!Screen.hasShiftDown()) {
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

    /** 共通：現在値は NBT（あれば）/ 無ければ min。括弧は常に min-max。固定や境界一致は白、その他は灰。値→ラベル順で表示 */
    private static void addStatLineSmart(
            List<Component> out, String labelKey,
            CrystalStats stats, CrystalSpecRange range,
            Function<CrystalStats, Number> nbtGetter,
            Function<CrystalSpecRange, net.ookasamoti.crystallography.data.FloatRange> rangeGetter,
            boolean integerLike
    ) {
        var r = rangeGetter.apply(range);
        float minF = r.min();
        float maxF = r.max();
        boolean fixed = r.isFixed();

        float curF;
        if (stats != null) {
            Number v = nbtGetter.apply(stats);
            curF = integerLike ? v.intValue() : v.floatValue();
        } else {
            // 変動だけど未確定 → min を暫定表示（仕様どおり）
            curF = minF;
        }

        String curText = integerLike ? Integer.toString(Math.round(curF)) : format2(curF);
        String minText = integerLike ? Integer.toString(Math.round(minF)) : format2(minF);
        String maxText = integerLike ? Integer.toString(Math.round(maxF)) : format2(maxF);

        // 範囲色：固定は両端とも白。可変は基本灰、ただし現在値==min/max の端は白で強調
        var minColor = (fixed || Float.compare(curF, minF) == 0) ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY;
        var maxColor = (fixed || Float.compare(curF, maxF) == 0) ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY;

        out.add(Component.empty()
                .append(Component.literal(curText).withStyle(ChatFormatting.WHITE))   // 先に値（白）
                .append(Component.literal(" "))
                .append(Component.translatable(labelKey).withStyle(ChatFormatting.GRAY)) // ラベル（灰）
                .append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(minText).withStyle(minColor))
                .append(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(maxText).withStyle(maxColor))
                .append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY))
        );
    }

    private static String format2(float f) {
        return String.format(java.util.Locale.ROOT, "%.2f", f);
    }
}
