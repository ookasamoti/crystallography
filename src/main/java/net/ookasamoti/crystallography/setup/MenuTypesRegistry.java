package net.ookasamoti.crystallography.setup;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.client.screen.JewelryTableMenu;
import net.ookasamoti.crystallography.client.screen.LapidaryAnvilMenu;
import net.ookasamoti.crystallography.common.block.entity.JewelryTableBlockEntity;

import java.util.function.Supplier;

public final class MenuTypesRegistry {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CrystallographyMod.MOD_ID);

    /** JewelryTable */
    public static final Supplier<MenuType<JewelryTableMenu>> JEWELRY_TABLE_MENU =
            MENUS.register("jewelry_table_menu",
                    () -> IMenuTypeExtension.create((windowId, inventory, buf) -> {
                        BlockPos pos = buf.readBlockPos();
                        BlockEntity be = inventory.player.level().getBlockEntity(pos);
                        if (be instanceof JewelryTableBlockEntity tableBE) {
                            return new JewelryTableMenu(windowId, inventory, tableBE, tableBE.getContainerData());
                        } else {
                            CrystallographyMod.LOGGER.warn("Failed to open JewelryTableMenu: BE not found at {}", pos);
                            return new JewelryTableMenu(windowId, inventory, (JewelryTableBlockEntity) be, new SimpleContainerData(1));
                        }
                    })
            );

    public static final Supplier<MenuType<LapidaryAnvilMenu>> LAPIDARY_ANVIL_MENU =
            MENUS.register("lapidary_anvil_menu",
                    () -> IMenuTypeExtension.create((windowId, inventory, buf) -> {
                        BlockPos pos = buf.readBlockPos();
                        return new LapidaryAnvilMenu(windowId, inventory, inventory.player, pos);
                    }));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }

    private MenuTypesRegistry() {}
}
