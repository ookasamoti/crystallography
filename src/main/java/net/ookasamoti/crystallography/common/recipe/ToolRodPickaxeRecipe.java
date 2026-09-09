package net.ookasamoti.crystallography.common.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.ookasamoti.crystallography.common.item.crystal.Crystal;
import net.ookasamoti.crystallography.common.item.tool.CrystalToolLogic;
import net.ookasamoti.crystallography.common.item.tool.ToolBase;
import net.ookasamoti.crystallography.common.item.tool.ToolInventory;
import net.ookasamoti.crystallography.common.item.tool.component.ToolForm;
import net.ookasamoti.crystallography.common.item.tool.component.ToolLoadout;
import net.ookasamoti.crystallography.common.item.tool.component.ToolStats;
import net.ookasamoti.crystallography.setup.ItemRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * 丸石5枠(上段3枠＋中段中央/右)＋下段中央に木のツルハシ(耐久値無視)で tier1 ToolRod を作る。
 * 丸石の各枠は {@code crystallography:stone} でも代用可。使用した丸石/stoneの数から
 * 「丸石1個=stone9、stone1個=stone1」で結晶インベントリ用の stone 合計量を計算し、
 * 3枠(x, 1, 1。xに端数を寄せる)へ分配して装着した状態で、PICKAXE(アクティブ)・AXE・
 * SWORD・SHOVEL・HOE の5ロードアウトを登録済みで払い出す。
 * <p>
 * {@link #assemble} は {@code HolderLookup.Provider}（結晶インベントリの NBT シリアライズに必要）
 * を受け取れないため、直前に必ず呼ばれる {@link #matches} で渡された {@link Level} を一時的に
 * キャッシュして使う（サーバーのゲームロジックはシングルスレッドで、同一クラフト操作の中で
 * 他のレベルの matches/assemble が割り込むことはない）。
 */
public class ToolRodPickaxeRecipe extends CustomRecipe {
    public static final ToolRodPickaxeRecipe INSTANCE = new ToolRodPickaxeRecipe();
    public static final MapCodec<ToolRodPickaxeRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolRodPickaxeRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<ToolRodPickaxeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private static final int[][] MATERIAL_CELLS = {{0, 0}, {1, 0}, {2, 0}, {1, 1}, {2, 1}};
    private static final int[][] EMPTY_CELLS = {{0, 1}, {0, 2}, {2, 2}};
    private static final ToolForm[] REGISTERED_FORMS =
            {ToolForm.PICKAXE, ToolForm.AXE, ToolForm.SWORD, ToolForm.SHOVEL, ToolForm.HOE};

    private @Nullable Level lastLevel;

    private static boolean isCobbleOrStone(ItemStack stack) {
        return stack.is(Items.COBBLESTONE) || stack.is(ItemRegistry.STONE.get());
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) return false;

        for (int[] c : EMPTY_CELLS) {
            if (!input.getItem(c[0], c[1]).isEmpty()) return false;
        }

        ItemStack pickaxe = input.getItem(1, 2);
        if (!pickaxe.is(Items.WOODEN_PICKAXE) || pickaxe.getCount() != 1) return false;

        for (int[] c : MATERIAL_CELLS) {
            ItemStack s = input.getItem(c[0], c[1]);
            if (!isCobbleOrStone(s) || s.getCount() != 1) return false;
        }

        this.lastLevel = level;
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Level level = this.lastLevel;
        if (level == null) return ItemStack.EMPTY;

        int cobbleCount = 0, stoneCount = 0;
        for (int[] c : MATERIAL_CELLS) {
            ItemStack s = input.getItem(c[0], c[1]);
            if (s.is(Items.COBBLESTONE)) cobbleCount++;
            else if (s.is(ItemRegistry.STONE.get())) stoneCount++;
            else return ItemStack.EMPTY;
        }

        int totalStone = cobbleCount * 9 + stoneCount;
        int tier = 1;
        int[] crystalIndices = {0, 1, 2};

        ItemStack stack = new ItemStack(ItemRegistry.TOOLROD_TIER1.get());
        ItemStacksResourceHandler crystalInv =
                ToolInventory.get(stack, CrystalToolLogic.crystalSlotCount(tier), level.registryAccess());
        setStoneSlot(crystalInv, level, 0, totalStone - 2);
        setStoneSlot(crystalInv, level, 1, 1);
        setStoneSlot(crystalInv, level, 2, 1);

        for (int i = 0; i < REGISTERED_FORMS.length; i++) {
            ToolForm form = REGISTERED_FORMS[i];
            ToolStats stats = ToolBase.buildStats(form, tier, crystalIndices, crystalInv);
            ToolBase.setLoadout(stack, ToolLoadout.fresh(i, form, crystalIndices, stats));
        }
        ToolBase.setActiveIndex(stack, 0); // PICKAXE をアクティブに
        ToolBase.applyComputedStats(stack);
        return CrystalToolLogic.retargetToActiveForm(stack, tier);
    }

    private static void setStoneSlot(ItemStacksResourceHandler inv, Level level, int index, int amount) {
        ItemStack stone = new ItemStack(ItemRegistry.STONE.get(), amount);
        Crystal.resolveStats(stone, level);
        inv.set(index, ItemResource.of(stone), amount);
    }

    @Override
    public RecipeSerializer<ToolRodPickaxeRecipe> getSerializer() {
        return SERIALIZER;
    }
}
