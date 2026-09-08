package net.ookasamoti.crystallography.common.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.ookasamoti.crystallography.common.item.tool.ToolRod;
import net.ookasamoti.crystallography.setup.BlockRegistry;

/**
 * ツールロッド（tier問わず、素の状態のみ。フォーム確定済みの派生アイテムは対象外）を
 * 1個と作業台を1個、グリッド内の任意の位置に置くと成立する。ツールロッドは消費されず
 * （{@link #getRemainingItems}でそのまま返す）、作業台だけが消費されてジュエリーテーブルになる。
 */
public class JewelryTableRecipe extends CustomRecipe {
    public static final JewelryTableRecipe INSTANCE = new JewelryTableRecipe();
    public static final MapCodec<JewelryTableRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, JewelryTableRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<JewelryTableRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private static int findRodSlot(CraftingInput input) {
        int rodSlot = -1;
        int tableCount = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof ToolRod) {
                if (rodSlot != -1 || stack.getCount() != 1) return -1;
                rodSlot = slot;
            } else if (stack.is(Items.CRAFTING_TABLE)) {
                if (stack.getCount() != 1) return -1;
                tableCount++;
            } else {
                return -1;
            }
        }
        return (rodSlot != -1 && tableCount == 1) ? rodSlot : -1;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findRodSlot(input) != -1;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        if (findRodSlot(input) == -1) return ItemStack.EMPTY;
        return new ItemStack(BlockRegistry.JEWELRY_TABLE.get().asItem());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        int rodSlot = findRodSlot(input);
        if (rodSlot != -1) {
            remaining.set(rodSlot, input.getItem(rodSlot).copy());
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<JewelryTableRecipe> getSerializer() {
        return SERIALIZER;
    }
}
