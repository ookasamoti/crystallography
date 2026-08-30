package net.ookasamoti.crystallography.setup;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.block.JewelryTableBlock;
import net.ookasamoti.crystallography.common.block.LapidaryAnvilBlock;
import net.ookasamoti.crystallography.common.block.Wedge;

public final class BlockRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CrystallographyMod.MOD_ID);

    // registerBlock sets the block's registry id on the Properties before the factory runs.
    // (Block construction now requires an id, so the plain register(name, Supplier) form crashes
    // with "Block id not set".)
    public static final DeferredBlock<Wedge> WEDGE = BLOCKS.registerBlock("wedge",
            Wedge::new,
            p -> p.mapColor(MapColor.METAL)
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion());

    public static final DeferredBlock<JewelryTableBlock> JEWELRY_TABLE = BLOCKS.registerBlock("jewelry_table",
            JewelryTableBlock::new,
            p -> p.mapColor(MapColor.METAL)
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    public static final DeferredBlock<LapidaryAnvilBlock> LAPIDARY_ANVIL = BLOCKS.registerBlock("lapidary_anvil",
            LapidaryAnvilBlock::new,
            p -> p.mapColor(MapColor.METAL)
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    public static void registerBlockItems() {
        // useBlockDescriptionPrefix(): BlockItem の翻訳キーを item.* ではなく block.* にする。
        // これが無いと "item.crystallography.wedge" を引いて未翻訳キーがそのまま表示される
        // （lang は block.crystallography.* で定義されているため）。
        ItemRegistry.ITEMS.registerItem("wedge",
                props -> new BlockItem(WEDGE.get(), props.useBlockDescriptionPrefix()));
        ItemRegistry.ITEMS.registerItem("jewelry_table",
                props -> new BlockItem(JEWELRY_TABLE.get(), props.useBlockDescriptionPrefix()));
        ItemRegistry.ITEMS.registerItem("lapidary_anvil",
                props -> new BlockItem(LAPIDARY_ANVIL.get(), props.useBlockDescriptionPrefix()));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
