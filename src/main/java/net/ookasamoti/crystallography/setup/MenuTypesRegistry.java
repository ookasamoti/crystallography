package net.ookasamoti.crystallography.setup;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.client.screen.JewelryTableMenu;
import net.ookasamoti.crystallography.common.blocks.JewelryTableBlockEntity;

public class MenuTypesRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CrystallographyMod.MOD_ID);

    public static final Supplier<MenuType<JewelryTableMenu>> JEWELRY_TABLE_MENU =
            registerMenuType((windowId, inventory, extraData) -> {
                BlockPos pos = extraData.readBlockPos();
                BlockEntity be = inventory.player.level().getBlockEntity(pos);
                if (be instanceof JewelryTableBlockEntity jewelryBE) {
                    return new JewelryTableMenu(windowId, inventory, jewelryBE, jewelryBE.getContainerData());
                } else {
                    CrystallographyMod.LOGGER.warn("Failed to open JewelryTableMenu: BlockEntity not found or invalid at {}", pos);
                    return new JewelryTableMenu(windowId, inventory, null, new SimpleContainerData(1));
                }
            });

    private static <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenuType(IContainerFactory<T> factory) {
        return MENUS.register("jewelry_table_menu", () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
