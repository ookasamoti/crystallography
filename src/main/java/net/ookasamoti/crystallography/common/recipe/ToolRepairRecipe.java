package net.ookasamoti.crystallography.common.recipe;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.ookasamoti.crystallography.common.item.tool.ToolBase;
import net.ookasamoti.crystallography.common.item.tool.component.ToolLoadout;
import org.jetbrains.annotations.Nullable;

/**
 * ツールベース(ToolRod/ToolWand) + tier別修繕素材(木材/金インゴット/黒曜石) を
 * クラフトグリッドに1個ずつ置くと、アクティブなロードアウトの耐久値を固定量回復する。
 *
 * <p>出力は入力ツールの複製に耐久値だけを書き換えたもの（vanilla の
 * {@code RepairItemRecipe} と同じ構造の "special" レシピ）。踏み倒し対策として、回復量は
 * 必ず固定量加算（現在の stats.durability() でクランプ）にすること。%回復・満タン回復は
 * 一時的な結晶差し替えで水増しできてしまう（{@link ToolBase#repairAmountFor} 参照）。
 */
public class ToolRepairRecipe extends CustomRecipe {
    public static final ToolRepairRecipe INSTANCE = new ToolRepairRecipe();
    public static final MapCodec<ToolRepairRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolRepairRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<ToolRepairRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    /** グリッド内のちょうど2アイテム（ツール1つ+修繕素材1つ）を見つける。条件を満たさなければ null。 */
    private static @Nullable Pair<ItemStack, ItemStack> getToolAndMaterial(CraftingInput input) {
        if (input.ingredientCount() != 2) return null;

        ItemStack tool = null;
        ItemStack material = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof ToolBase) {
                if (tool != null) return null; // ツールが2つ→不成立
                tool = stack;
            } else {
                if (material != null) return null; // 素材枠に2種類目→不成立
                material = stack;
            }
        }
        if (tool == null || material == null) return null;
        if (tool.getCount() != 1 || material.getCount() != 1) return null;

        int tier = ((ToolBase) tool.getItem()).getTier();
        if (!ToolBase.isRepairMaterial(tier, material)) return null;
        if (ToolBase.getActiveLoadout(tool) == null) return null; // 回復対象のロードアウトが無い

        return Pair.of(tool, material);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return getToolAndMaterial(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        var pair = getToolAndMaterial(input);
        if (pair == null) return ItemStack.EMPTY;

        ItemStack tool = pair.getFirst().copy();
        int tier = ((ToolBase) tool.getItem()).getTier();

        ToolLoadout lo = ToolBase.getActiveLoadout(tool);
        if (lo == null) return ItemStack.EMPTY;

        int newCurrent = Math.min(lo.stats().durability(), lo.currentDurability() + ToolBase.repairAmountFor(tier));
        ToolBase.setLoadout(tool, lo.withCurrentDurability(newCurrent));
        ToolBase.applyComputedStats(tool);
        return tool;
    }

    @Override
    public RecipeSerializer<ToolRepairRecipe> getSerializer() {
        return SERIALIZER;
    }
}
