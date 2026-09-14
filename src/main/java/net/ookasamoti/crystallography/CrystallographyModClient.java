package net.ookasamoti.crystallography;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.ookasamoti.crystallography.client.model.AmuletBodyModel;
import net.ookasamoti.crystallography.client.model.AmuletFootModel;
import net.ookasamoti.crystallography.client.model.AmuletHeadModel;
import net.ookasamoti.crystallography.client.model.AmuletLegModel;
import net.ookasamoti.crystallography.client.renderer.AmuletArmorClientExtensions;
import net.ookasamoti.crystallography.client.renderer.AmuletCrystalLayer;
import net.ookasamoti.crystallography.setup.ItemRegistry;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.ookasamoti.crystallography.client.color.AmuletCrystalTintSource;
import net.ookasamoti.crystallography.client.color.CrystalTintSource;
import net.ookasamoti.crystallography.client.event.CrystalClientHooks;
import net.ookasamoti.crystallography.client.model.FormProperty;
import net.ookasamoti.crystallography.client.renderer.CrystalThrownTridentRenderer;
import net.ookasamoti.crystallography.client.renderer.CrystalTridentSpecialRenderer;
import net.ookasamoti.crystallography.client.renderer.SoulFireFlameRenderer;
import net.ookasamoti.crystallography.client.renderer.SoulFireScreenOverlay;
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
        bus.addListener(CrystallographyModClient::onRegisterEntityRenderers);
        bus.addListener(CrystallographyModClient::onRegisterLayerDefinitions);
        bus.addListener(CrystallographyModClient::onRegisterClientExtensions);
        bus.addListener(CrystallographyModClient::onAddLayers);
        bus.addListener(CrystallographyModClient::onRegisterSpecialModelRenderers);
        bus.addListener(KeyBindingRegistry::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(CrystallographyModClient::onMouseScroll);
        SoulFireFlameRenderer.register(bus);
        SoulFireScreenOverlay.register(bus);
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
        // Same idea for the amulet armor pieces' inventory icon (see AmuletCrystalTintSource).
        event.register(Identifier.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "amulet_crystal"),
                AmuletCrystalTintSource.MAP_CODEC);
    }

    @SubscribeEvent
    static void onRegisterSelectProperties(RegisterSelectItemModelPropertyEvent event) {
        // Replaces the old ItemProperties "form" override; item models dispatch on this via
        // a minecraft:select model (see FormProperty).
        event.register(Identifier.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "form"),
                FormProperty.TYPE);
    }

    @SubscribeEvent
    static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Overrides vanilla's ThrownTridentRenderer, which always draws the hardcoded
        // entity/trident/trident.png special model regardless of the actual thrown stack.
        event.registerEntityRenderer(EntityType.TRIDENT, CrystalThrownTridentRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(AmuletHeadModel.LAYER_LOCATION, AmuletHeadModel::createLayer);
        event.registerLayerDefinition(AmuletBodyModel.LAYER_LOCATION, AmuletBodyModel::createLayer);
        event.registerLayerDefinition(AmuletLegModel.LAYER_LOCATION, AmuletLegModel::createLayer);
        event.registerLayerDefinition(AmuletFootModel.LAYER_LOCATION, AmuletFootModel::createLayer);
    }

    @SubscribeEvent
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        // アミュレットの結晶キューブ個別着色（AmuletCrystalRenderer）を、通常の防具レイヤーと
        // 同じタイミングで描画するための専用レイヤー（詳細は AmuletCrystalLayer のコメント参照）。
        // 現状はプレイヤーのみが対象（一般の人型モブは未対応）。
        for (PlayerModelType skin : event.getSkins()) {
            var renderer = event.getPlayerRenderer(skin);
            if (renderer != null) {
                renderer.addLayer(new AmuletCrystalLayer<>(renderer));
            }
        }
    }

    @SubscribeEvent
    static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        // アミュレット4部位の独自防具モデル（鎖付き）を HumanoidArmorLayer に差し込む。
        event.registerItem(new AmuletArmorClientExtensions(AmuletHeadModel.LAYER_LOCATION, AmuletHeadModel::new), ItemRegistry.AMULET_HEAD.get());
        event.registerItem(new AmuletArmorClientExtensions(AmuletBodyModel.LAYER_LOCATION, AmuletBodyModel::new), ItemRegistry.AMULET_BODY.get());
        event.registerItem(new AmuletArmorClientExtensions(AmuletLegModel.LAYER_LOCATION, AmuletLegModel::new), ItemRegistry.AMULET_LEG.get());
        event.registerItem(new AmuletArmorClientExtensions(AmuletFootModel.LAYER_LOCATION, AmuletFootModel::new), ItemRegistry.AMULET_FOOT.get());
    }

    @SubscribeEvent
    static void onRegisterSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        // Same TridentModel geometry/pipeline as vanilla's "trident" special model, just with a
        // data-driven texture field instead of a hardcoded one, so each tier's item model JSON
        // can point at its own composited texture (see items/tool_trident_tierN.json).
        event.register(Identifier.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "crystal_trident"),
                CrystalTridentSpecialRenderer.Unbaked.MAP_CODEC);
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