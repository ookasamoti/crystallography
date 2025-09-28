package net.ookasamoti.crystallography;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.ookasamoti.crystallography.client.event.CrystalClientHooks;
import net.ookasamoti.crystallography.client.screen.JewelryTableScreen;
import net.ookasamoti.crystallography.client.screen.LapidaryAnvilScreen;
import net.ookasamoti.crystallography.setup.MenuTypesRegistry;

import java.util.Objects;

@Mod(value = CrystallographyMod.MOD_ID, dist = Dist.CLIENT)
public class CrystallographyModClient {
    public CrystallographyModClient(ModContainer container) {
        CrystalClientHooks.bootstrapClient();

        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        Objects.requireNonNull(container.getEventBus()).addListener(CrystallographyModClient::onRegisterScreens);
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(MenuTypesRegistry.JEWELRY_TABLE_MENU.get(), JewelryTableScreen::new);
        event.register(MenuTypesRegistry.LAPIDARY_ANVIL_MENU.get(), LapidaryAnvilScreen::new);
    }
}