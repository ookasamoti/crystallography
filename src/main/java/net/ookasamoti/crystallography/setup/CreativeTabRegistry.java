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
                        output.accept(ItemRegistry.DIAMOND_CRYSTAL.get());
                        output.accept(ItemRegistry.PINK_DIAMOND_CRYSTAL.get());
                    }).build()
    );

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
