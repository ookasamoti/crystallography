package net.ookasamoti.crystallography.common.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.ookasamoti.crystallography.common.item.tool.CrystalToolLogic;
import net.ookasamoti.crystallography.common.item.tool.ICrystalTool;
import net.ookasamoti.crystallography.common.item.tool.ToolInventory;
import org.jetbrains.annotations.Nullable;

/**
 * ツールベース(ToolRod/ToolWand)のtierアップグレード。3x3グリッドの中央に対象tierの
 * ツールベースを1つ、四隅のうち左上・右下に {@code corner} 素材、残り6枠に {@code filler}
 * 素材を置くと成立する。中央のスタックが持つ全データ（登録済みロードアウト・結晶インベントリ・
 * 耐久値など）を保ったまま、{@code result} で指定した次tierのアイテムへ retarget される
 * （vanilla の netherite アップグレード鍛冶レシピと同じ「コンポーネントを保持したまま
 * アイテム種別だけ差し替える」考え方）。
 * <p>
 * 中央のマッチ判定は固定アイテムID(Ingredient)ではなく {@link ICrystalTool} の
 * kind/tier で行う。フォーム確定済み(例: tool_pickaxe_tier1)のツールも {@code result}
 * と同じ kind・1つ下の tier であれば受け付ける必要があるため（固定Ingredientだと
 * 素の状態の toolrod_tier1/toolwand_tier1 にしか一致せず、フォーム選択中のツールを
 * アップグレードできなくなってしまう）。kind/tier は {@code result} 自身から逆算する。
 */
public class ToolTierUpgradeRecipe extends CustomRecipe {
    public static final MapCodec<ToolTierUpgradeRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                Ingredient.CODEC.fieldOf("corner").forGetter(o -> o.corner),
                Ingredient.CODEC.fieldOf("filler").forGetter(o -> o.filler),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(o -> o.result)
            )
            .apply(i, ToolTierUpgradeRecipe::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolTierUpgradeRecipe> STREAM_CODEC = StreamCodec.composite(
        Ingredient.CONTENTS_STREAM_CODEC, o -> o.corner,
        Ingredient.CONTENTS_STREAM_CODEC, o -> o.filler,
        ByteBufCodecs.registry(Registries.ITEM), o -> o.result,
        ToolTierUpgradeRecipe::new
    );
    public static final RecipeSerializer<ToolTierUpgradeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private static final int[][] CORNER_COORDS = {{0, 0}, {2, 2}};
    private static final int[][] FILLER_COORDS = {{1, 0}, {2, 0}, {0, 1}, {2, 1}, {0, 2}, {1, 2}};

    private final Ingredient corner;
    private final Ingredient filler;
    private final Item result;

    // assemble(CraftingInput) はレジストリアクセス（結晶インベントリの読み書きに必要）を
    // 受け取れないため、直前に必ず呼ばれる matches で渡された Level を一時的にキャッシュする
    // （サーバーのゲームロジックはシングルスレッドなので、同一クラフト操作の中で他の
    // レベルの matches/assemble が割り込むことはない）。
    private @Nullable Level lastLevel;

    public ToolTierUpgradeRecipe(Ingredient corner, Ingredient filler, Item result) {
        this.corner = corner;
        this.filler = filler;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) return false;
        if (!(result instanceof ICrystalTool resultTool)) return false;

        ItemStack center = input.getItem(1, 1);
        if (!(center.getItem() instanceof ICrystalTool centerTool)) return false;
        if (centerTool.getKind() != resultTool.getKind()) return false;
        if (centerTool.getTier() != resultTool.getTier() - 1) return false;
        if (center.getCount() != 1) return false;

        for (int[] c : CORNER_COORDS) {
            if (!corner.test(input.getItem(c[0], c[1]))) return false;
        }
        for (int[] c : FILLER_COORDS) {
            if (!filler.test(input.getItem(c[0], c[1]))) return false;
        }

        this.lastLevel = level;
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack center = input.getItem(1, 1);
        if (center.isEmpty()) return ItemStack.EMPTY;
        Level level = this.lastLevel;
        if (level == null || !(result instanceof ICrystalTool resultTool)) return ItemStack.EMPTY;

        ItemStack upgraded = new ItemStack(result.builtInRegistryHolder(), center.getCount(), center.getComponentsPatch());

        int newTier = resultTool.getTier();
        var crystalInv = ToolInventory.get(upgraded, CrystalToolLogic.crystalSlotCount(newTier), level.registryAccess());
        // 新tierの数値(耐久のtier係数など)で全登録ロードアウトを再計算しつつ、
        // 既に削れていた耐久はそのまま引き継ぐ（reconcileLoadouts の既定動作）。
        CrystalToolLogic.reconcileLoadouts(upgraded, newTier, crystalInv);

        return CrystalToolLogic.retargetToActiveForm(upgraded, newTier);
    }

    @Override
    public RecipeSerializer<ToolTierUpgradeRecipe> getSerializer() {
        return SERIALIZER;
    }
}
