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

/**
 * ツールベース(ToolRod/ToolWand)のtierアップグレード。3x3グリッドの中央に対象tierの
 * ツールベースを1つ、四隅のうち左上・右下に {@code corner} 素材、残り6枠に {@code filler}
 * 素材を置くと成立する。中央のスタックが持つ全データ（登録済みロードアウト・結晶インベントリ・
 * 耐久値など）を保ったまま、{@code result} で指定した次tierのアイテムへ retarget される
 * （vanilla の netherite アップグレード鍛冶レシピと同じ「コンポーネントを保持したまま
 * アイテム種別だけ差し替える」考え方）。
 */
public class ToolTierUpgradeRecipe extends CustomRecipe {
    public static final MapCodec<ToolTierUpgradeRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                Ingredient.CODEC.fieldOf("base").forGetter(o -> o.base),
                Ingredient.CODEC.fieldOf("corner").forGetter(o -> o.corner),
                Ingredient.CODEC.fieldOf("filler").forGetter(o -> o.filler),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(o -> o.result)
            )
            .apply(i, ToolTierUpgradeRecipe::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolTierUpgradeRecipe> STREAM_CODEC = StreamCodec.composite(
        Ingredient.CONTENTS_STREAM_CODEC, o -> o.base,
        Ingredient.CONTENTS_STREAM_CODEC, o -> o.corner,
        Ingredient.CONTENTS_STREAM_CODEC, o -> o.filler,
        ByteBufCodecs.registry(Registries.ITEM), o -> o.result,
        ToolTierUpgradeRecipe::new
    );
    public static final RecipeSerializer<ToolTierUpgradeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private static final int[][] CORNER_COORDS = {{0, 0}, {2, 2}};
    private static final int[][] FILLER_COORDS = {{1, 0}, {2, 0}, {0, 1}, {2, 1}, {0, 2}, {1, 2}};

    private final Ingredient base;
    private final Ingredient corner;
    private final Ingredient filler;
    private final Item result;

    public ToolTierUpgradeRecipe(Ingredient base, Ingredient corner, Ingredient filler, Item result) {
        this.base = base;
        this.corner = corner;
        this.filler = filler;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) return false;
        if (!base.test(input.getItem(1, 1))) return false;
        for (int[] c : CORNER_COORDS) {
            if (!corner.test(input.getItem(c[0], c[1]))) return false;
        }
        for (int[] c : FILLER_COORDS) {
            if (!filler.test(input.getItem(c[0], c[1]))) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack center = input.getItem(1, 1);
        if (center.isEmpty()) return ItemStack.EMPTY;
        return new ItemStack(result.builtInRegistryHolder(), center.getCount(), center.getComponentsPatch());
    }

    @Override
    public RecipeSerializer<ToolTierUpgradeRecipe> getSerializer() {
        return SERIALIZER;
    }
}
