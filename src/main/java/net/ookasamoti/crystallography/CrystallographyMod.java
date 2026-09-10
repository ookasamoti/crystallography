package net.ookasamoti.crystallography;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.ookasamoti.crystallography.common.entity.SoulFireIgnitionHooks;
import net.ookasamoti.crystallography.common.entity.TridentVisualData;
import net.ookasamoti.crystallography.common.item.tool.AttackAttributeTooltipHooks;
import net.ookasamoti.crystallography.common.item.tool.SigilCombatHooks;
import net.ookasamoti.crystallography.data.CrystalRollsReloader;
import net.ookasamoti.crystallography.data.CrystalStatsReloader;
import net.ookasamoti.crystallography.data.ServerRegistryHolder;
import net.ookasamoti.crystallography.data.SigilReloader;
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
        NeoForge.EVENT_BUS.addListener(CrystallographyMod::onServerStarting);
        NeoForge.EVENT_BUS.addListener(CrystallographyMod::onServerStopping);

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
        AttackAttributeTooltipHooks.register(modEventBus);
        SigilCombatHooks.register(modEventBus);
        SoulFireIgnitionHooks.register(modEventBus);
    }

    /**
     * シジル付与エンチャントの解決（{@code CrystalToolLogic#applyComputedStats}）用に、
     * サーバーの RegistryAccess を静的に保持しておく（詳細は {@link ServerRegistryHolder}）。
     */
    private static void onServerStarting(ServerStartingEvent event) {
        ServerRegistryHolder.set(event.getServer().registryAccess());
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        ServerRegistryHolder.set(null);
    }

    private static void onAddReloadListeners(AddServerReloadListenersEvent e) {
        e.addListener(Identifier.fromNamespaceAndPath(MOD_ID, "crystal_stats"), new CrystalStatsReloader());
        e.addListener(Identifier.fromNamespaceAndPath(MOD_ID, "crystal_rolls"), new CrystalRollsReloader());
        e.addListener(Identifier.fromNamespaceAndPath(MOD_ID, "sigil"), new SigilReloader());
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
