package net.ookasamoti.crystallography.setup;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.recipe.ToolRepairRecipe;

public final class RecipeSerializersRegistry {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, CrystallographyMod.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ToolRepairRecipe>> TOOL_REPAIR =
            RECIPE_SERIALIZERS.register("tool_repair", () -> ToolRepairRecipe.SERIALIZER);

    public static void register(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
    }

    private RecipeSerializersRegistry() {}
}
