package net.ookasamoti.crystallography.setup;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.item.crystal.Crystal;
import net.ookasamoti.crystallography.common.item.tool.ToolRod;
import net.ookasamoti.crystallography.common.item.tool.ToolWand;

public class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CrystallographyMod.MOD_ID);

    public static final DeferredItem<Item> TOOL_ROD_TIER1 = ITEMS.registerItem("tool_rod_tier1", props -> new ToolRod(props, 1));
    public static final DeferredItem<Item> TOOL_ROD_TIER2 = ITEMS.registerItem("tool_rod_tier2", props -> new ToolRod(props, 2));
    public static final DeferredItem<Item> TOOL_ROD_TIER3 = ITEMS.registerItem("tool_rod_tier3", props -> new ToolRod(props, 3));

    public static final DeferredItem<Item> TOOL_WAND_TIER1 = ITEMS.registerItem("tool_wand_tier1", props -> new ToolWand(props, 1));
    public static final DeferredItem<Item> TOOL_WAND_TIER2 = ITEMS.registerItem("tool_wand_tier2", props -> new ToolWand(props, 2));
    public static final DeferredItem<Item> TOOL_WAND_TIER3 = ITEMS.registerItem("tool_wand_tier3", props -> new ToolWand(props, 3));

    public static final DeferredItem<Item> DIAMOND_CRYSTAL = ITEMS.register("diamond_crystal", () -> new Crystal(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PINK_DIAMOND_CRYSTAL = ITEMS.register("pink_diamond_crystal.json", () -> new Crystal(new Item.Properties().stacksTo(64)));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}