package net.ookasamoti.crystallography.setup;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.item.crystal.Crystal;
import net.ookasamoti.crystallography.common.item.tool.ToolRod;
import net.ookasamoti.crystallography.common.item.tool.ToolWand;
import net.ookasamoti.crystallography.common.item.tool.component.ToolForm;
import net.ookasamoti.crystallography.common.item.tool.form.CrystalAxe;
import net.ookasamoti.crystallography.common.item.tool.form.CrystalBow;
import net.ookasamoti.crystallography.common.item.tool.form.CrystalCrossbow;
import net.ookasamoti.crystallography.common.item.tool.form.CrystalFishingRod;
import net.ookasamoti.crystallography.common.item.tool.form.CrystalHoe;
import net.ookasamoti.crystallography.common.item.tool.form.CrystalMace;
import net.ookasamoti.crystallography.common.item.tool.form.CrystalPickaxe;
import net.ookasamoti.crystallography.common.item.tool.form.CrystalShears;
import net.ookasamoti.crystallography.common.item.tool.form.CrystalShield;
import net.ookasamoti.crystallography.common.item.tool.form.CrystalShovel;
import net.ookasamoti.crystallography.common.item.tool.form.CrystalSpear;
import net.ookasamoti.crystallography.common.item.tool.form.CrystalSpyglass;
import net.ookasamoti.crystallography.common.item.tool.form.CrystalSword;
import net.ookasamoti.crystallography.common.item.tool.form.CrystalTrident;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;

public class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CrystallographyMod.MOD_ID);

    public static final DeferredItem<Item> TOOLROD_TIER1 = ITEMS.registerItem("toolrod_tier1", props -> new ToolRod(props, 1));
    public static final DeferredItem<Item> TOOLROD_TIER2 = ITEMS.registerItem("toolrod_tier2", props -> new ToolRod(props, 2));
    public static final DeferredItem<Item> TOOLROD_TIER3 = ITEMS.registerItem("toolrod_tier3", props -> new ToolRod(props, 3));

    public static final DeferredItem<Item> TOOLWAND_TIER1 = ITEMS.registerItem("toolwand_tier1", props -> new ToolWand(props, 1));
    public static final DeferredItem<Item> TOOLWAND_TIER2 = ITEMS.registerItem("toolwand_tier2", props -> new ToolWand(props, 2));
    public static final DeferredItem<Item> TOOLWAND_TIER3 = ITEMS.registerItem("toolwand_tier3", props -> new ToolWand(props, 3));

    // ---- フォーム別 Item（アクティブなロードアウトのフォームに応じて retarget される先） ----
    // 素の状態(TOOLROD_TIER*/TOOLWAND_TIER*)とは別に、フォーム×tier ごとに専用の Item を1つずつ
    // 登録する。クリエイティブタブには意図的に載せない（retarget 経由でのみ出現する）。
    private static final Map<ToolForm, BiFunction<Item.Properties, Integer, Item>> FORM_FACTORIES = new EnumMap<>(ToolForm.class);
    static {
        FORM_FACTORIES.put(ToolForm.PICKAXE,     CrystalPickaxe::new);
        FORM_FACTORIES.put(ToolForm.SHOVEL,      CrystalShovel::new);
        FORM_FACTORIES.put(ToolForm.HOE,         CrystalHoe::new);
        FORM_FACTORIES.put(ToolForm.AXE,         CrystalAxe::new);
        FORM_FACTORIES.put(ToolForm.SWORD,       CrystalSword::new);
        FORM_FACTORIES.put(ToolForm.SPEAR,       CrystalSpear::new);
        FORM_FACTORIES.put(ToolForm.TRIDENT,     CrystalTrident::new);
        FORM_FACTORIES.put(ToolForm.MACE,        CrystalMace::new);
        FORM_FACTORIES.put(ToolForm.BOW,         CrystalBow::new);
        FORM_FACTORIES.put(ToolForm.CROSSBOW,    CrystalCrossbow::new);
        FORM_FACTORIES.put(ToolForm.SHEARS,      CrystalShears::new);
        FORM_FACTORIES.put(ToolForm.SPYGLASS,    CrystalSpyglass::new);
        FORM_FACTORIES.put(ToolForm.FISHING_ROD, CrystalFishingRod::new);
        FORM_FACTORIES.put(ToolForm.SHIELD,      CrystalShield::new);
    }

    public static final Map<ToolForm, DeferredItem<Item>[]> FORM_TIER_ITEMS = registerFormItems();

    @SuppressWarnings("unchecked")
    private static Map<ToolForm, DeferredItem<Item>[]> registerFormItems() {
        Map<ToolForm, DeferredItem<Item>[]> out = new EnumMap<>(ToolForm.class);
        FORM_FACTORIES.forEach((form, factory) -> {
            DeferredItem<Item>[] tiers = new DeferredItem[3];
            String formName = form.name().toLowerCase();
            for (int tier = 1; tier <= 3; tier++) {
                int t = tier;
                tiers[tier - 1] = ITEMS.registerItem("tool_" + formName + "_tier" + tier, props -> factory.apply(props, t));
            }
            out.put(form, tiers);
        });
        return out;
    }

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

    public static final DeferredItem<Item> STONE = ITEMS.registerSimpleItem("stone");

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}