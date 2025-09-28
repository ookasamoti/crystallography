package net.ookasamoti.crystallography.setup;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.item.crystal.CrystalStats;

public final class DataComponentsRegistry {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CrystallographyMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CrystalStats>> CRYSTAL_STATS =
            DATA_COMPONENT_TYPES.register("crystal_stats", () ->
                    DataComponentType.<CrystalStats>builder()
                            .persistent(CrystalStats.CODEC)
                            .networkSynchronized(CrystalStats.STREAM_CODEC)
                            .build()
            );

    public static void register(IEventBus bus) {
        DATA_COMPONENT_TYPES.register(bus);
    }

    private DataComponentsRegistry() {}
}
