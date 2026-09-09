package net.ookasamoti.crystallography.common.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import net.ookasamoti.crystallography.common.item.tool.CrystalToolLogic;
import net.ookasamoti.crystallography.common.item.tool.ICrystalTool;
import net.ookasamoti.crystallography.common.item.tool.ToolFormRegistry;
import net.ookasamoti.crystallography.common.item.tool.ToolInventory;
import net.ookasamoti.crystallography.common.item.tool.component.ToolForm;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ツールベース(ToolRod/ToolWand)のtier2→tier3鍛冶アップグレード。vanilla の
 * {@code minecraft:smithing_transform} は base を固定 {@link Ingredient} で照合するため、
 * フォーム確定済み(例: tool_pickaxe_tier2)のツールが一致せずアップグレードできなくなる
 * （{@link ToolTierUpgradeRecipe} で tier1→tier2 に対して直した問題と同根）。
 * ここでは base の照合を {@link ICrystalTool} の kind/tier（{@code result} から逆算）で行う
 * カスタム鍛冶レシピとして実装し、同じ問題を鍛冶台側でも解消する。
 */
public class ToolTierSmithingUpgradeRecipe implements SmithingRecipe {
    public static final MapCodec<ToolTierSmithingUpgradeRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                Ingredient.CODEC.optionalFieldOf("template").forGetter(o -> o.template),
                Ingredient.CODEC.optionalFieldOf("addition").forGetter(o -> o.addition),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(o -> o.result)
            )
            .apply(i, ToolTierSmithingUpgradeRecipe::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolTierSmithingUpgradeRecipe> STREAM_CODEC = StreamCodec.composite(
        Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC, o -> o.template,
        Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC, o -> o.addition,
        ByteBufCodecs.registry(Registries.ITEM), o -> o.result,
        ToolTierSmithingUpgradeRecipe::new
    );
    public static final RecipeSerializer<ToolTierSmithingUpgradeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final Optional<Ingredient> template;
    private final Optional<Ingredient> addition;
    private final Item result;

    // assemble(SmithingRecipeInput) はレジストリアクセスを受け取れないため、直前に必ず呼ばれる
    // matches で渡された Level を一時的にキャッシュする（ToolTierUpgradeRecipe と同じ理由）。
    private @Nullable Level lastLevel;

    public ToolTierSmithingUpgradeRecipe(Optional<Ingredient> template, Optional<Ingredient> addition, Item result) {
        this.template = template;
        this.addition = addition;
        this.result = result;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        if (!(result instanceof ICrystalTool resultTool)) return false;
        if (!Ingredient.testOptionalIngredient(template, input.template())) return false;
        if (!Ingredient.testOptionalIngredient(addition, input.addition())) return false;

        ItemStack base = input.base();
        if (!(base.getItem() instanceof ICrystalTool baseTool)) return false;
        if (baseTool.getKind() != resultTool.getKind()) return false;
        if (baseTool.getTier() != resultTool.getTier() - 1) return false;
        if (base.getCount() != 1) return false;

        this.lastLevel = level;
        return true;
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        ItemStack base = input.base();
        if (base.isEmpty()) return ItemStack.EMPTY;
        Level level = this.lastLevel;
        if (level == null || !(result instanceof ICrystalTool resultTool)) return ItemStack.EMPTY;

        ItemStack upgraded = new ItemStack(result.builtInRegistryHolder(), base.getCount(), base.getComponentsPatch());

        int newTier = resultTool.getTier();
        var crystalInv = ToolInventory.get(upgraded, CrystalToolLogic.crystalSlotCount(newTier), level.registryAccess());
        CrystalToolLogic.reconcileLoadouts(upgraded, newTier, crystalInv);

        return CrystalToolLogic.retargetToActiveForm(upgraded, newTier);
    }

    @Override
    public Optional<Ingredient> templateIngredient() {
        return template;
    }

    @Override
    public Ingredient baseIngredient() {
        return buildBaseIngredient();
    }

    /**
     * base スロットに置ける全アイテム(素の状態のToolRod/ToolWand ＋ 同tier・同kindの
     * フォーム確定済みアイテム全て)を1つの Ingredient にまとめる。
     * <p>
     * {@link net.minecraft.world.inventory.SmithingMenu} はスロットへの挿入可否自体を
     * {@code RecipePropertySet}（全 {@link SmithingRecipe} の {@link #placementInfo()} を
     * 集約したもの）で判定するため、ここを {@code Ingredient.of(result)} のような単一アイテム
     * だけにすると、フォーム確定済みのツールがスロットに一切入らなくなってしまう
     * （{@link #matches} 側だけ直しても不十分）。
     */
    private Ingredient buildBaseIngredient() {
        if (!(result instanceof ICrystalTool resultTool)) return Ingredient.of();
        ICrystalTool.Kind kind = resultTool.getKind();
        int sourceTier = resultTool.getTier() - 1;

        List<Holder<Item>> items = new ArrayList<>();
        items.add(ToolFormRegistry.blankFor(kind, sourceTier).builtInRegistryHolder());
        for (ToolForm form : ToolForm.values()) {
            Item formItem = ToolFormRegistry.itemFor(form, sourceTier);
            if (formItem instanceof ICrystalTool ct && ct.getKind() == kind) {
                items.add(formItem.builtInRegistryHolder());
            }
        }
        return Ingredient.of(HolderSet.direct(items));
    }

    @Override
    public Optional<Ingredient> additionIngredient() {
        return addition;
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public PlacementInfo placementInfo() {
        if (placementInfoCache == null) {
            placementInfoCache = PlacementInfo.createFromOptionals(List.of(template, Optional.of(buildBaseIngredient()), addition));
        }
        return placementInfoCache;
    }

    private @Nullable PlacementInfo placementInfoCache;

    @Override
    public RecipeSerializer<ToolTierSmithingUpgradeRecipe> getSerializer() {
        return SERIALIZER;
    }
}
