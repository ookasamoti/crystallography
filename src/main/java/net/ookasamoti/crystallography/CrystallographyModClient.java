package net.ookasamoti.crystallography;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ookasamoti.crystallography.client.event.CrystalClientHooks;
import net.ookasamoti.crystallography.common.menu.JewelryTableMenu;
import net.ookasamoti.crystallography.client.screen.JewelryTableScreen;
import net.ookasamoti.crystallography.client.screen.LapidaryAnvilScreen;
import net.ookasamoti.crystallography.common.item.tool.ToolBase;
import net.ookasamoti.crystallography.common.item.tool.ToolInventory;
import net.ookasamoti.crystallography.common.item.tool.ToolRod;
import net.ookasamoti.crystallography.common.item.tool.ToolWand;
import net.ookasamoti.crystallography.common.item.tool.component.ToolForm;
import net.ookasamoti.crystallography.data.CrystalStatsRegistry;
import net.ookasamoti.crystallography.network.ToolCycleC2S;
import net.ookasamoti.crystallography.setup.ItemRegistry;
import net.ookasamoti.crystallography.setup.KeyBindingRegistry;
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
        bus.addListener(CrystallographyModClient::onClientSetup);
        bus.addListener(KeyBindingRegistry::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(CrystallographyModClient::onMouseScroll);
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(MenuTypesRegistry.JEWELRY_TABLE_MENU.get(), JewelryTableScreen::new);
        event.register(MenuTypesRegistry.LAPIDARY_ANVIL_MENU.get(), LapidaryAnvilScreen::new);
    }

    @SubscribeEvent
    static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        // tintIndex 0=base tier texture (no tint), 1=center crystal, 2=left crystal, 3=right crystal
        event.register((stack, tintIndex) -> {
            if (tintIndex < 1 || tintIndex > 3) return -1;
            var draft = ToolBase.getDraftLoadout(stack);
            var lo = draft != null ? draft : ToolBase.getActiveLoadout(stack);
            if (lo == null) return -1;
            int[] indices = lo.crystalIndices();
            if (tintIndex - 1 >= indices.length) return -1;
            int crystalSlot = indices[tintIndex - 1];
            if (crystalSlot < 0) return -1;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return -1;
            var lookup = mc.level.registryAccess();
            int tier = (stack.getItem() instanceof ToolBase tb) ? tb.getTier() : 1;
            var inv = ToolInventory.get(stack, ToolBase.crystalSlotCount(tier), lookup);
            var crystal = inv.getStackInSlot(crystalSlot);
            if (crystal.isEmpty()) return -1;
            return CrystalStatsRegistry.get(crystal).map(r -> r.tint()).orElse(-1);
        }, ItemRegistry.TOOLROD_TIER1.get(),
           ItemRegistry.TOOLROD_TIER2.get(),
           ItemRegistry.TOOLROD_TIER3.get(),
           ItemRegistry.TOOLWAND_TIER1.get(),
           ItemRegistry.TOOLWAND_TIER2.get(),
           ItemRegistry.TOOLWAND_TIER3.get());
    }

    @SubscribeEvent
    static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        var main = mc.player.getMainHandItem();
        var off  = mc.player.getOffhandItem();

        boolean rodHeld  = main.getItem() instanceof ToolRod  || off.getItem() instanceof ToolRod;
        boolean wandHeld = main.getItem() instanceof ToolWand || off.getItem() instanceof ToolWand;

        boolean rodKey  = KeyBindingRegistry.KEY_ROD_CYCLE.isDown();
        boolean wandKey = KeyBindingRegistry.KEY_WAND_CYCLE.isDown();

        if ((rodHeld && rodKey) || (wandHeld && wandKey)) {
            event.setCanceled(true);
            int delta = event.getScrollDeltaY() > 0 ? 1 : -1;
            PacketDistributor.sendToServer(new ToolCycleC2S(delta));
        }
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            var propId = Identifier.parse(CrystallographyMod.MOD_ID + ":form");
            // Returns (formIndex + 1) / 10 for active loadout form, 0 if none
            var propFn = (net.minecraft.client.renderer.item.ClampedItemPropertyFunction)
                (stack, level, entity, seed) -> {
                    var draft = ToolBase.getDraftLoadout(stack);
                    var lo = draft != null ? draft : ToolBase.getActiveLoadout(stack);
                    if (lo == null) return 0f;
                    boolean isWand = stack.getItem() instanceof ToolWand;
                    ToolForm[] forms = isWand ? JewelryTableMenu.WAND_FORMS : JewelryTableMenu.ROD_FORMS;
                    for (int i = 0; i < forms.length; i++) {
                        if (forms[i] == lo.form()) return (i + 1) / 10.0f;
                    }
                    return 0f;
                };
            ItemProperties.register(ItemRegistry.TOOLROD_TIER1.get(),  propId, propFn);
            ItemProperties.register(ItemRegistry.TOOLROD_TIER2.get(),  propId, propFn);
            ItemProperties.register(ItemRegistry.TOOLROD_TIER3.get(),  propId, propFn);
            ItemProperties.register(ItemRegistry.TOOLWAND_TIER1.get(), propId, propFn);
            ItemProperties.register(ItemRegistry.TOOLWAND_TIER2.get(), propId, propFn);
            ItemProperties.register(ItemRegistry.TOOLWAND_TIER3.get(), propId, propFn);
        });
    }
}