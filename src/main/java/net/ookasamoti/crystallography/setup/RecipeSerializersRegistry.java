package net.ookasamoti.crystallography.setup;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.recipe.JewelryTableRecipe;
import net.ookasamoti.crystallography.common.recipe.ToolRepairRecipe;
import net.ookasamoti.crystallography.common.recipe.ToolRodPickaxeRecipe;
import net.ookasamoti.crystallography.common.recipe.ToolTierSmithingUpgradeRecipe;
import net.ookasamoti.crystallography.common.recipe.ToolTierUpgradeRecipe;

public final class RecipeSerializersRegistry {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, CrystallographyMod.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ToolRepairRecipe>> TOOL_REPAIR =
            RECIPE_SERIALIZERS.register("tool_repair", () -> ToolRepairRecipe.SERIALIZER);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ToolTierUpgradeRecipe>> TOOL_TIER_UPGRADE =
            RECIPE_SERIALIZERS.register("tool_tier_upgrade", () -> ToolTierUpgradeRecipe.SERIALIZER);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<JewelryTableRecipe>> JEWELRY_TABLE =
            RECIPE_SERIALIZERS.register("jewelry_table", () -> JewelryTableRecipe.SERIALIZER);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ToolRodPickaxeRecipe>> TOOL_ROD_PICKAXE =
            RECIPE_SERIALIZERS.register("tool_rod_pickaxe", () -> ToolRodPickaxeRecipe.SERIALIZER);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ToolTierSmithingUpgradeRecipe>> TOOL_TIER_SMITHING_UPGRADE =
            RECIPE_SERIALIZERS.register("tool_tier_smithing_upgrade", () -> ToolTierSmithingUpgradeRecipe.SERIALIZER);

    public static void register(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
    }

    private RecipeSerializersRegistry() {}
}
