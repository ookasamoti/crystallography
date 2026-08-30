package net.ookasamoti.crystallography.setup;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.block.entity.JewelryTableBlockEntity;
import net.ookasamoti.crystallography.common.block.entity.LapidaryAnvilBlockEntity;

public class BlockEntitiesRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CrystallographyMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<JewelryTableBlockEntity>> JEWELRY_TABLE_BE =
            BLOCK_ENTITIES.register("jewelry_table_be",
                    () -> new BlockEntityType<>(
                            JewelryTableBlockEntity::new,
                            BlockRegistry.JEWELRY_TABLE.get()
                    )
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LapidaryAnvilBlockEntity>> LAPIDARY_ANVIL_BE =
            BLOCK_ENTITIES.register("lapidary_anvil_be",
                    () -> new BlockEntityType<>(
                            LapidaryAnvilBlockEntity::new,
                            BlockRegistry.LAPIDARY_ANVIL.get()
                    )
            );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}