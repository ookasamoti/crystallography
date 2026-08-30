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

    public static final DeferredItem<Item> TOOLROD_TIER1 = ITEMS.registerItem("toolrod_tier1", props -> new ToolRod(props, 1));
    public static final DeferredItem<Item> TOOLROD_TIER2 = ITEMS.registerItem("toolrod_tier2", props -> new ToolRod(props, 2));
    public static final DeferredItem<Item> TOOLROD_TIER3 = ITEMS.registerItem("toolrod_tier3", props -> new ToolRod(props, 3));

    public static final DeferredItem<Item> TOOLWAND_TIER1 = ITEMS.registerItem("toolwand_tier1", props -> new ToolWand(props, 1));
    public static final DeferredItem<Item> TOOLWAND_TIER2 = ITEMS.registerItem("toolwand_tier2", props -> new ToolWand(props, 2));
    public static final DeferredItem<Item> TOOLWAND_TIER3 = ITEMS.registerItem("toolwand_tier3", props -> new ToolWand(props, 3));

    // registerItem supplies a Properties with the item's registry id already set; the plain
    // register(name, Supplier) form with a hand-made Item.Properties crashes with "Item id not set".
    private static DeferredItem<Item> crystal(String name) {
        return ITEMS.registerItem(name, props -> new Crystal(props.stacksTo(1)));
    }

    public static final DeferredItem<Item> CHALCOPYRITE       = crystal("chalcopyrite");
    public static final DeferredItem<Item> MAGNETITE          = crystal("magnetite");
    public static final DeferredItem<Item> PYRITE             = crystal("pyrite");
    public static final DeferredItem<Item> ANTHRACITE         = crystal("anthracite");
    public static final DeferredItem<Item> GOLD               = crystal("gold");
    public static final DeferredItem<Item> BRUTE_GOLD         = crystal("brute_gold");
    public static final DeferredItem<Item> BLUE_ICE_CRYSTAL   = crystal("blue_ice_crystal");
    public static final DeferredItem<Item> PRISMARINE_CRYSTAL = crystal("prismarine_crystal");
    public static final DeferredItem<Item> REDSTONE           = crystal("redstone");
    public static final DeferredItem<Item> LAPIS_LAZULI       = crystal("lapis_lazuli");
    public static final DeferredItem<Item> AMETHYST           = crystal("amethyst");
    public static final DeferredItem<Item> DIAMOND            = crystal("diamond");
    public static final DeferredItem<Item> PINK_DIAMOND       = crystal("pink_diamond");
    public static final DeferredItem<Item> BLACK_DIAMOND      = crystal("black_diamond");
    public static final DeferredItem<Item> EMERALD            = crystal("emerald");
    public static final DeferredItem<Item> TRAPICHE_EMERALD   = crystal("trapiche_emerald");
    public static final DeferredItem<Item> NETHER_QUARTZ      = crystal("nether_quartz");
    public static final DeferredItem<Item> GLOWSTONE          = crystal("glowstone");
    public static final DeferredItem<Item> WITHER_ROSE_QUARTZ = crystal("wither_rose_quartz");
    public static final DeferredItem<Item> BLAZE_CRYSTAL      = crystal("blaze_crystal");
    public static final DeferredItem<Item> BREEZE_CRYSTAL     = crystal("breeze_crystal");
    public static final DeferredItem<Item> END_CRYSTAL        = crystal("end_crystal");
    public static final DeferredItem<Item> ENDER_PEARL        = crystal("ender_pearl");
    public static final DeferredItem<Item> SHULKER_PEARL      = crystal("shulker_pearl");
    public static final DeferredItem<Item> OBSIDIAN           = crystal("obsidian");
    public static final DeferredItem<Item> OBSIDIAN_TEAR      = crystal("obsidian_tear");
    public static final DeferredItem<Item> SONAR_SHARD        = crystal("sonar_shard");
    public static final DeferredItem<Item> TRIDENT_CORE       = crystal("trident_core");
    public static final DeferredItem<Item> HEAVY_CORE         = crystal("heavy_core");

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}