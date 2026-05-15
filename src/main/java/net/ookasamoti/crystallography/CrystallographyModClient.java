package net.ookasamoti.crystallography;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.ookasamoti.crystallography.client.event.CrystalClientHooks;
import net.ookasamoti.crystallography.client.screen.JewelryTableScreen;
import net.ookasamoti.crystallography.client.screen.LapidaryAnvilScreen;
import net.ookasamoti.crystallography.common.item.tool.ToolBase;
import net.ookasamoti.crystallography.data.CrystalStatsRegistry;
import net.ookasamoti.crystallography.setup.ItemRegistry;
import net.ookasamoti.crystallography.setup.MenuTypesRegistry;

import java.util.Objects;

@Mod(value = CrystallographyMod.MOD_ID, dist = Dist.CLIENT)
public class CrystallographyModClient {
    public CrystallographyModClient(ModContainer container) {
        CrystalClientHooks.bootstrapClient();

        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        var bus = Objects.requireNonNull(container.getEventBus());
        bus.addListener(CrystallographyModClient::onRegisterScreens);
        bus.addListener(CrystallographyModClient::onRegisterItemColors);
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(MenuTypesRegistry.JEWELRY_TABLE_MENU.get(), JewelryTableScreen::new);
        event.register(MenuTypesRegistry.LAPIDARY_ANVIL_MENU.get(), LapidaryAnvilScreen::new);
    }

    @SubscribeEvent
    static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex < 0 || tintIndex > 2) return -1;
            var lo = ToolBase.getActiveLoadout(stack);
            if (lo == null) return -1;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return -1;
            var lookup = mc.level.registryAccess();
            int tier = (stack.getItem() instanceof ToolBase tb) ? tb.getTier() : 1;
            var inv = net.ookasamoti.crystallography.common.item.tool.ToolInventory.get(
                    stack, ToolBase.crystalSlotCount(tier), lookup);
            int[] indices = lo.crystalIndices();
            if (indices.length == 0) return -1;
            var crystal = inv.getStackInSlot(indices[0]);
            if (crystal.isEmpty()) return -1;
            return CrystalStatsRegistry.get(crystal)
                    .map(r -> r.tint())
                    .orElse(-1);
        }, ItemRegistry.TOOL_ROD_TIER1.get(),
           ItemRegistry.TOOL_ROD_TIER2.get(),
           ItemRegistry.TOOL_ROD_TIER3.get());
    }
}