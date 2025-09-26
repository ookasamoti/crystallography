package net.ookasamoti.crystallography.setup;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.blocks.JewelryTableBlock;
import net.ookasamoti.crystallography.common.blocks.Wedge;

public final class BlockRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CrystallographyMod.MOD_ID);

    public static final DeferredBlock<Wedge> WEDGE = BLOCKS.register("wedge", () ->
            new Wedge(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion())
    );

    public static final DeferredBlock<JewelryTableBlock> JEWELRY_TABLE = BLOCKS.register("jewelry_table", () ->
            new JewelryTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion())
    );

    public static void registerBlockItems() {
        ItemRegistry.ITEMS.registerItem("wedge", props -> new BlockItem(WEDGE.get(), props));
        ItemRegistry.ITEMS.registerItem("jewelry_table", props -> new BlockItem(JEWELRY_TABLE.get(), props));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
