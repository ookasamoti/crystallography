package net.ookasamoti.crystallography;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.ookasamoti.crystallography.client.color.CrystalTintSource;
import net.ookasamoti.crystallography.client.event.CrystalClientHooks;
import net.ookasamoti.crystallography.client.model.FormProperty;
import net.ookasamoti.crystallography.client.screen.JewelryTableScreen;
import net.ookasamoti.crystallography.client.screen.LapidaryAnvilScreen;
import net.ookasamoti.crystallography.common.item.tool.ICrystalTool;
import net.ookasamoti.crystallography.network.ToolCycleC2S;
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
        bus.addListener(CrystallographyModClient::onRegisterItemTintSources);
        bus.addListener(CrystallographyModClient::onRegisterSelectProperties);
        bus.addListener(KeyBindingRegistry::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(CrystallographyModClient::onMouseScroll);
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(MenuTypesRegistry.JEWELRY_TABLE_MENU.get(), JewelryTableScreen::new);
        event.register(MenuTypesRegistry.LAPIDARY_ANVIL_MENU.get(), LapidaryAnvilScreen::new);
    }

    @SubscribeEvent
    static void onRegisterItemTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
        // Per-crystal tinting is now data-driven: item models reference this source in their
        // "tints" array (see CrystalTintSource). One source instance per crystal slot (0/1/2).
        event.register(Identifier.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "crystal"),
                CrystalTintSource.MAP_CODEC);
    }

    @SubscribeEvent
    static void onRegisterSelectProperties(RegisterSelectItemModelPropertyEvent event) {
        // Replaces the old ItemProperties "form" override; item models dispatch on this via
        // a minecraft:select model (see FormProperty).
        event.register(Identifier.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "form"),
                FormProperty.TYPE);
    }

    @SubscribeEvent
    static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        var main = mc.player.getMainHandItem();
        var off  = mc.player.getOffhandItem();

        boolean rodHeld  = isKind(main, ICrystalTool.Kind.ROD)  || isKind(off, ICrystalTool.Kind.ROD);
        boolean wandHeld = isKind(main, ICrystalTool.Kind.WAND) || isKind(off, ICrystalTool.Kind.WAND);

        boolean rodKey  = KeyBindingRegistry.KEY_ROD_CYCLE.isDown();
        boolean wandKey = KeyBindingRegistry.KEY_WAND_CYCLE.isDown();

        if ((rodHeld && rodKey) || (wandHeld && wandKey)) {
            event.setCanceled(true);
            int delta = event.getScrollDeltaY() > 0 ? 1 : -1;
            ClientPacketDistributor.sendToServer(new ToolCycleC2S(delta));
        }
    }

    /**
     * stack がロッド系／ワンド系かを判定する。フォーム確定前の「素の状態」
     * ({@code ToolRod}/{@code ToolWand}) と、確定後のフォーム別 Item の両方に対応するため
     * {@link ICrystalTool#getKind()} を見る（特定クラスへの instanceof は使わない）。
     */
    private static boolean isKind(ItemStack stack, ICrystalTool.Kind kind) {
        return stack.getItem() instanceof ICrystalTool ct && ct.getKind() == kind;
    }
}