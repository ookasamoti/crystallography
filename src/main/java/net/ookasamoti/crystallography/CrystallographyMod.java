package net.ookasamoti.crystallography;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.ookasamoti.crystallography.common.entity.TridentVisualData;
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
        NeoForge.EVENT_BUS.addListener(CrystallographyMod::onEntityJoinLevel);

        DataComponentsRegistry.register(modEventBus);
        ItemRegistry.register(modEventBus);
        BlockRegistry.register(modEventBus);
        BlockRegistry.registerBlockItems();
        BlockEntitiesRegistry.register(modEventBus);
        CreativeTabRegistry.register(modEventBus);
        MenuTypesRegistry.register(modEventBus);
        ToolComponentsRegistry.register(modEventBus);
        RecipeSerializersRegistry.register(modEventBus);
        AttachmentTypeRegistry.register(modEventBus);
        ModNet.register(modEventBus);
    }

    private static void onAddReloadListeners(AddServerReloadListenersEvent e) {
        e.addListener(Identifier.fromNamespaceAndPath(MOD_ID, "crystal_stats"), new CrystalStatsReloader());
        e.addListener(Identifier.fromNamespaceAndPath(MOD_ID, "crystal_rolls"), new CrystalRollsReloader());
    }

    /**
     * A thrown trident's real weapon stack is never sent to the client (vanilla only syncs a
     * couple of individual bits for that entity), so the tier/crystal-colour data the client
     * needs to render it correctly is computed here, server-side, and stashed in a synced
     * attachment (see {@link TridentVisualData}).
     */
    private static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof ThrownTrident trident) {
            var data = TridentVisualData.compute(trident.getWeaponItem(), event.getLevel().registryAccess());
            trident.setData(AttachmentTypeRegistry.TRIDENT_VISUAL, data);
        }
    }
}
