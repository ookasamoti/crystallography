package net.ookasamoti.crystallography.common.item.crystal;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ookasamoti.crystallography.data.CrystalStatsRegistry;
import net.ookasamoti.crystallography.setup.DataComponentsRegistry;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class Crystal extends Item {
    public Crystal(Properties props){ super(props); }

    /**
     * 対象アイテムに CrystalStats を解決して付与する。{@code stack.getItem()} が {@link Crystal}
     * である必要はない（例: minecraft:raw_iron 等、CrystalStatsRegistry にだけ登録されたバニラ
     * アイテムでも呼べる）。既に付与済みなら何もしない。
     *
     * <p>以前は全ステータス固定（{@code range.allFixed()}）の場合に付与自体をスキップしていたが、
     * これだと {@link net.ookasamoti.crystallography.common.item.tool.ToolBase#buildStats} 等
     * コンポーネントを直接読む側が「結晶が存在するのに stats==null」となり、固定値結晶が常に
     * 0 扱いされてしまう不具合があった（原石系 raw_iron/raw_gold/raw_copper は全て allFixed で
     * このバグを踏む）。allFixed でも常に解決・付与するよう修正。
     */
    public static void resolveStats(ItemStack stack, @Nullable Level level) {
        var type = DataComponentsRegistry.CRYSTAL_STATS.get();
        if (stack.get(type) != null) return;

        var rangeOpt = CrystalStatsRegistry.get(stack);
        if (rangeOpt.isEmpty()) return;
        var range = rangeOpt.get();

        var rng = (level != null) ? level.getRandom() : net.minecraft.util.RandomSource.create();
        stack.set(type, range.resolveToStats(rng));
    }

    @Override
    public void onCraftedPostProcess(@NotNull ItemStack s, Level l){
        if(!l.isClientSide()) resolveStats(s, l);
    }
}
