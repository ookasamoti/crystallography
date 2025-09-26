package net.ookasamoti.crystallography.setup;

import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public final class CreativeTabContents {
    private CreativeTabContents() {}

    public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == CreativeTabRegistry.CREATIVE_TAB.get()) {
            event.accept(BlockRegistry.WEDGE.get());
            event.accept(BlockRegistry.JEWELRY_TABLE.get().asItem());
            event.accept(ItemRegistry.TOOL_ROD_TIER1.get());
            event.accept(ItemRegistry.TOOL_ROD_TIER2.get());
            event.accept(ItemRegistry.TOOL_ROD_TIER3.get());
            event.accept(ItemRegistry.TOOL_WAND_TIER1.get());
            event.accept(ItemRegistry.TOOL_WAND_TIER2.get());
            event.accept(ItemRegistry.TOOL_WAND_TIER3.get());
        }
    }
}

