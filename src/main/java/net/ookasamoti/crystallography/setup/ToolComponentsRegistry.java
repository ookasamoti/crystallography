// ToolComponentsRegistry.java
package net.ookasamoti.crystallography.setup;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.item.tool.component.ToolLoadout;
import net.ookasamoti.crystallography.common.item.tool.component.ToolLoadoutList;

public final class ToolComponentsRegistry {
    private ToolComponentsRegistry(){}

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CrystallographyMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ToolLoadoutList>> TOOL_LOADOUTS =
            COMPONENTS.register("tool_loadouts", () ->
                    DataComponentType.<ToolLoadoutList>builder()
                            .persistent(ToolLoadoutList.CODEC)
                            .networkSynchronized(ToolLoadoutList.STREAM_CODEC)
                            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TOOL_ACTIVE_INDEX =
            COMPONENTS.register("tool_active_index", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(net.minecraft.util.ExtraCodecs.NON_NEGATIVE_INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build());

    /** 仮登録中の ToolLoadout（REGISTRIES 選択前のドラフト）。本登録時または取り消し時に削除する。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ToolLoadout>> TOOL_DRAFT_LOADOUT =
            COMPONENTS.register("tool_draft_loadout", () ->
                    DataComponentType.<ToolLoadout>builder()
                            .persistent(ToolLoadout.CODEC)
                            .networkSynchronized(ToolLoadout.STREAM_CODEC)
                            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> TOOL_ACTIVE_MODEL =
            COMPONENTS.register("tool_active_model", () ->
                    DataComponentType.<ResourceLocation>builder()
                            .persistent(ResourceLocation.CODEC)
                            .networkSynchronized(ResourceLocation.STREAM_CODEC)
                            .build());

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}
