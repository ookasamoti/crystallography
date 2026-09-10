package net.ookasamoti.crystallography.setup;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ookasamoti.crystallography.CrystallographyMod;


public class CreativeTabRegistry {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CrystallographyMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB = CREATIVE_MODE_TABS.register("creative_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(BlockRegistry.WEDGE.get()))
                    .title(Component.translatable("creativetab.crystallography.tab_name"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(BlockRegistry.WEDGE.get());
                        output.accept(BlockRegistry.JEWELRY_TABLE.get().asItem());
                        output.accept(BlockRegistry.LAPIDARY_ANVIL.get().asItem());
                        output.accept(ItemRegistry.TOOLROD_TIER1.get());
                        output.accept(ItemRegistry.TOOLROD_TIER2.get());
                        output.accept(ItemRegistry.TOOLROD_TIER3.get());
                        output.accept(ItemRegistry.TOOLWAND_TIER1.get());
                        output.accept(ItemRegistry.TOOLWAND_TIER2.get());
                        output.accept(ItemRegistry.TOOLWAND_TIER3.get());
                        // ItemRegistry.FORM_TIER_ITEMS (tool_<form>_tierN) はここに意図的に含めない。
                        // これらはロードアウトのフォーム確定に伴う retarget でのみ出現するべきアイテムで、
                        // /give 以外では直接入手できない状態を保つ。
                        output.accept(ItemRegistry.CHALCOPYRITE.get());
                        output.accept(ItemRegistry.MAGNETITE.get());
                        output.accept(ItemRegistry.PYRITE.get());
                        output.accept(ItemRegistry.ANTHRACITE.get());
                        output.accept(ItemRegistry.GOLD.get());
                        output.accept(ItemRegistry.BRUTE_GOLD.get());
                        output.accept(ItemRegistry.BLUE_ICE_CRYSTAL.get());
                        output.accept(ItemRegistry.PRISMARINE_CRYSTAL.get());
                        output.accept(ItemRegistry.REDSTONE.get());
                        output.accept(ItemRegistry.LAPIS_LAZULI.get());
                        output.accept(ItemRegistry.AMETHYST.get());
                        output.accept(ItemRegistry.DIAMOND.get());
                        output.accept(ItemRegistry.PINK_DIAMOND.get());
                        output.accept(ItemRegistry.BLACK_DIAMOND.get());
                        output.accept(ItemRegistry.EMERALD.get());
                        output.accept(ItemRegistry.TRAPICHE_EMERALD.get());
                        output.accept(ItemRegistry.NETHER_QUARTZ.get());
                        output.accept(ItemRegistry.GLOWSTONE.get());
                        output.accept(ItemRegistry.WITHER_ROSE_QUARTZ.get());
                        output.accept(ItemRegistry.BLAZE_CRYSTAL.get());
                        output.accept(ItemRegistry.BREEZE_CRYSTAL.get());
                        output.accept(ItemRegistry.END_CRYSTAL.get());
                        output.accept(ItemRegistry.ENDER_PEARL.get());
                        output.accept(ItemRegistry.SHULKER_PEARL.get());
                        output.accept(ItemRegistry.OBSIDIAN.get());
                        output.accept(ItemRegistry.OBSIDIAN_TEAR.get());
                        output.accept(ItemRegistry.SONAR_SHARD.get());
                        output.accept(ItemRegistry.TRIDENT_CORE.get());
                        output.accept(ItemRegistry.HEAVY_CORE.get());
                        output.accept(ItemRegistry.STONE.get());
                        output.accept(ItemRegistry.SIGIL_FORCE.get());
                        output.accept(ItemRegistry.SIGIL_SMITE.get());
                        output.accept(ItemRegistry.SIGIL_FUMIGATE.get());
                        output.accept(ItemRegistry.SIGIL_CHANNELING.get());
                        output.accept(ItemRegistry.SIGIL_LOYALTY.get());
                        output.accept(ItemRegistry.SIGIL_INFINITY.get());
                        output.accept(ItemRegistry.SIGIL_DENSITY.get());
                        output.accept(ItemRegistry.SIGIL_SURGE.get());
                        output.accept(ItemRegistry.SIGIL_GRACE.get());
                        output.accept(ItemRegistry.SIGIL_STEADY.get());
                        output.accept(ItemRegistry.SIGIL_FROST.get());
                        output.accept(ItemRegistry.SIGIL_GALE.get());
                        output.accept(ItemRegistry.SIGIL_HASTE.get());
                        output.accept(ItemRegistry.SIGIL_SWEEP.get());
                        output.accept(ItemRegistry.SIGIL_FLAME.get());
                        output.accept(ItemRegistry.SIGIL_PIERCE.get());
                        output.accept(ItemRegistry.SIGIL_UNBREAKING.get());
                    }).build()
    );

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
