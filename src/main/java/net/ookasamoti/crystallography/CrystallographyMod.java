package net.ookasamoti.crystallography;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.ookasamoti.crystallography.data.CrystalRollsReloader;
import net.ookasamoti.crystallography.data.CrystalStatsReloader;
import net.ookasamoti.crystallography.network.ModNet;
import net.ookasamoti.crystallography.setup.*;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(CrystallographyMod.MOD_ID)
public class CrystallographyMod {
    public static final String MOD_ID = "crystallography";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CrystallographyMod(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.addListener(CrystallographyMod::onAddReloadListeners);

        DataComponentsRegistry.register(modEventBus);
        ItemRegistry.register(modEventBus);
        BlockRegistry.register(modEventBus);
        BlockRegistry.registerBlockItems();
        BlockEntitiesRegistry.register(modEventBus);
        CreativeTabRegistry.register(modEventBus);
        MenuTypesRegistry.register(modEventBus);
        ToolComponentsRegistry.register(modEventBus);
        ModNet.register(modEventBus);
    }

    private static void onAddReloadListeners(AddReloadListenerEvent e) {
        e.addListener(new CrystalStatsReloader());
        e.addListener(new CrystalRollsReloader());
    }
}
