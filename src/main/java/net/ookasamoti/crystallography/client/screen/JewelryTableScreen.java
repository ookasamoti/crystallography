package net.ookasamoti.crystallography.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.client.gui.dial.DialLayout;
import net.ookasamoti.crystallography.client.gui.dial.DialSlot;
import net.ookasamoti.crystallography.common.item.tool.ToolWand;
import net.ookasamoti.crystallography.network.RotateRingC2S;
import org.jetbrains.annotations.NotNull;

public class JewelryTableScreen extends AbstractContainerScreen<JewelryTableMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.parse(CrystallographyMod.MOD_ID + ":textures/gui/jewelry_table_gui.png");
    private static final ResourceLocation GRADIENT =
            ResourceLocation.parse(CrystallographyMod.MOD_ID + ":textures/gui/jewelry_table_gradient.png");

    /* ===== ツール種別アイコン ===== */
    private static final ResourceLocation[] ROD_ICONS = {
            ResourceLocation.parse("minecraft:textures/item/empty_slot_pickaxe.png"),
            ResourceLocation.parse("minecraft:textures/item/empty_slot_shovel.png"),
            ResourceLocation.parse("minecraft:textures/item/empty_slot_hoe.png"),
            ResourceLocation.parse("minecraft:textures/item/empty_slot_sword.png"),
            ResourceLocation.parse("minecraft:textures/item/empty_slot_axe.png"),
            ResourceLocation.parse("crystallography:textures/item/empty_slot_trident.png"),
    };

    private static final ResourceLocation[] WAND_ICONS = {
            ResourceLocation.parse("crystallography:textures/item/empty_slot_bow.png"),
            ResourceLocation.parse("crystallography:textures/item/empty_slot_crossbow.png"),
            ResourceLocation.parse("crystallography:textures/item/empty_slot_knife.png"),
            ResourceLocation.parse("crystallography:textures/item/empty_slot_wrench.png"),
            ResourceLocation.parse("crystallography:textures/item/empty_slot_fishing_rod.png"),
            ResourceLocation.parse("minecraft:textures/item/empty_armor_slot_shield.png"),
    };

    /* ===== スロットプレースホルダーアイコン ===== */
    private static final ResourceLocation CRYSTAL_ICON =
            ResourceLocation.parse("minecraft:textures/item/empty_slot_quartz.png");

    private static final ResourceLocation REGISTRY_ICON =
            ResourceLocation.parse("crystallography:textures/item/empty_slot_stick.png");

    private static final int TOOL_SLOT_X = JewelryTableMenu.TOOL_SLOT_X;
    private static final int TOOL_SLOT_Y = JewelryTableMenu.TOOL_SLOT_Y;

    private DialLayout layTools, layCrystals, layRegistries, layCenter;

    private ItemStack lastKnownCenter = ItemStack.EMPTY;

    public JewelryTableScreen(JewelryTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }


    @Override
    protected void init() {
        super.init();
        this.imageWidth  = 176;
        this.imageHeight = 166;

        int left = (width - imageWidth) / 2;
        int top  = (height - imageHeight) / 2;
        int cx = left + TOOL_SLOT_X;
        int cy = top  + TOOL_SLOT_Y;

        layCenter     = new DialLayout().center(cx, cy)
                .radius(JewelryTableMenu.radiusOf(JewelryTableMenu.Ring.CENTER))
                .count (JewelryTableMenu.countOf (JewelryTableMenu.Ring.CENTER))
                .iconDiameter(16).angularOffsetDeg(JewelryTableMenu.baseDegOf(JewelryTableMenu.Ring.CENTER));

        layTools      = new DialLayout().center(cx, cy)
                .radius(JewelryTableMenu.radiusOf(JewelryTableMenu.Ring.TOOLS))
                .count (JewelryTableMenu.countOf (JewelryTableMenu.Ring.TOOLS))
                .iconDiameter(16).angularOffsetDeg(JewelryTableMenu.baseDegOf(JewelryTableMenu.Ring.TOOLS));

        layCrystals   = new DialLayout().center(cx, cy)
                .radius(JewelryTableMenu.radiusOf(JewelryTableMenu.Ring.CRYSTALS))
                .count (JewelryTableMenu.countOf (JewelryTableMenu.Ring.CRYSTALS))
                .iconDiameter(16).angularOffsetDeg(JewelryTableMenu.baseDegOf(JewelryTableMenu.Ring.CRYSTALS));

        layRegistries = new DialLayout().center(cx, cy)
                .radius(JewelryTableMenu.radiusOf(JewelryTableMenu.Ring.REGISTRIES))
                .count (JewelryTableMenu.countOf (JewelryTableMenu.Ring.REGISTRIES))
                .iconDiameter(16).angularOffsetDeg(JewelryTableMenu.baseDegOf(JewelryTableMenu.Ring.REGISTRIES));

        int[] cc = scissorRectContainer();
        menu.applyClipBoxToAllRings(cc[0], cc[1], cc[2], cc[3]);
    }

    // ---- ホイール ----
    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int dir = (int) Math.signum(dy);
        if (dir == 0) return false;

        int[] sc = scissorRectPixels();
        if (!pointInRect((int)mx, (int)my, sc[0], sc[1], sc[2], sc[3])) return false;

        if (hitRing(mx, my, layTools)) {
            menu.rotateToolsView(dir);
            PacketDistributor.sendToServer(new RotateRingC2S(this.menu.containerId, RotateRingC2S.RING_TOOLS, dir));
            return true;
        }
        if (hitRing(mx, my, layCrystals)) {
            menu.rotateCrystalsView(dir);
            PacketDistributor.sendToServer(new RotateRingC2S(this.menu.containerId, RotateRingC2S.RING_CRYSTALS, dir));
            return true;
        }
        if (hitRing(mx, my, layRegistries)) {
            menu.rotateRegistriesView(dir);
            PacketDistributor.sendToServer(new RotateRingC2S(this.menu.containerId, RotateRingC2S.RING_REGISTRIES, dir));
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    private boolean hitRing(double mx, double my, DialLayout L) {
        double d   = Math.hypot(mx - L.centerX(), my - L.centerY());
        double tol = L.iconDiameter() / 2.0 + 2.0;
        return Math.abs(d - L.radius()) <= tol;
    }

    // ---- ラベル ----
    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, 8, 6, 0x404040, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0x404040, false);
    }

    // ---- 背景 ----
    @Override
    protected void renderBg(GuiGraphics g, float pt, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int left = (width - imageWidth) / 2;
        int top  = (height - imageHeight) / 2;
        g.blit(TEXTURE, left, top, 0, 0, imageWidth, imageHeight);

        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, GRADIENT);
        g.blit(GRADIENT, left, top, 0, 0, imageWidth, imageHeight);
        RenderSystem.disableBlend();

        renderDialIcons(g);
    }

    // ---- ダイヤルアイコン描画 ----
    private void renderDialIcons(GuiGraphics g) {
        int[] sc = scissorRectPixels();
        enableGuiScissor(sc[0], sc[1], sc[2], sc[3]);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            ItemStack center = menu.getRingSlots(JewelryTableMenu.Ring.CENTER)[0].peekRealItem();
            ResourceLocation[] toolIcons = (center.getItem() instanceof ToolWand) ? WAND_ICONS : ROD_ICONS;

            // TOOLSボタンアイコン（headTools でサイクル）
            int headTools = menu.getHead(JewelryTableMenu.Ring.TOOLS);
            DialSlot[] toolSlots = menu.getRingSlots(JewelryTableMenu.Ring.TOOLS);
            for (int i = 0; i < toolSlots.length; i++) {
                DialSlot s = toolSlots[i];
                if (s.getVisibleFlag() && s.mode() == DialSlot.Mode.BUTTON) {
                    g.blit(toolIcons[(i + headTools) % toolIcons.length],
                            leftPos + s.x, topPos + s.y, 0, 0, 16, 16, 16, 16);
                }
            }

            // CRYSTALS空スロットプレースホルダー
            DialSlot[] crystalSlots = menu.getRingSlots(JewelryTableMenu.Ring.CRYSTALS);
            for (DialSlot s : crystalSlots) {
                if (s.getVisibleFlag() && s.peekRealItem().isEmpty()) {
                    g.blit(CRYSTAL_ICON, leftPos + s.x, topPos + s.y, 0, 0, 16, 16, 16, 16);
                }
            }

            // REGISTRIES空スロットプレースホルダー
            DialSlot[] registrySlots = menu.getRingSlots(JewelryTableMenu.Ring.REGISTRIES);
            for (DialSlot s : registrySlots) {
                if (s.getVisibleFlag() && s.peekRealItem().isEmpty()) {
                    g.blit(REGISTRY_ICON, leftPos + s.x, topPos + s.y, 0, 0, 16, 16, 16, 16);
                }
            }

            // 装飾スロット（isActive=false）の実アイテム描画
            // バニラの renderSlots ループは isActive=true のスロットしか描画しないため、
            // clip 境界にかかる装飾スロットは明示的にここで描画する
            for (JewelryTableMenu.Ring ring : JewelryTableMenu.Ring.values()) {
                for (DialSlot s : menu.getRingSlots(ring)) {
                    if (s.getVisibleFlag() && !s.isActive()) {
                        ItemStack item = s.peekRealItem();
                        if (!item.isEmpty()) {
                            g.renderItem(item, leftPos + s.x, topPos + s.y);
                            g.renderItemDecorations(this.font, item, leftPos + s.x, topPos + s.y);
                        }
                    }
                }
            }
        } finally {
            RenderSystem.disableBlend();
            RenderSystem.disableScissor();
        }
    }

    // ---- スロット描画（RadialSlotはscissor付き） ----
    @Override
    protected void renderSlot(@NotNull GuiGraphics g, @NotNull Slot slot) {
        if (slot instanceof DialSlot) {
            int[] sc = scissorRectPixels();
            enableGuiScissor(sc[0], sc[1], sc[2], sc[3]);
            try {
                super.renderSlot(g, slot);
            } finally {
                RenderSystem.disableScissor();
            }
            return;
        }
        super.renderSlot(g, slot);
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float pt) {
        ItemStack current = menu.getRingSlots(JewelryTableMenu.Ring.CENTER)[0].peekRealItem();
        if (!ItemStack.matches(current, lastKnownCenter)) {
            lastKnownCenter = current.copy();
            menu.clientSyncState();
        }
        renderBackground(g, mouseX, mouseY, pt);
        super.render(g, mouseX, mouseY, pt);
        renderTooltip(g, mouseX, mouseY);
    }

    // ---- scissor補助 ----
    private int[] scissorRectPixels() {
        int left = (width - imageWidth) / 2;
        int top  = (height - imageHeight) / 2;
        int inset = 4, cutoffY = top + 86;

        int x0 = left + inset, y0 = top + inset;
        int x1 = left + imageWidth - inset, y1 = cutoffY - inset;

        if (x1 <= x0) x1 = x0 + 1;
        if (y1 <= y0) y1 = y0 + 1;
        return new int[]{x0, y0, x1, y1};
    }

    private void enableGuiScissor(int guiX0, int guiY0, int guiX1, int guiY1) {
        if (guiX1 < guiX0) { int t = guiX0; guiX0 = guiX1; guiX1 = t; }
        if (guiY1 < guiY0) { int t = guiY0; guiY0 = guiY1; guiY1 = t; }

        var window = this.minecraft.getWindow();
        double scale = window.getGuiScale();

        int px0 = (int) Math.floor(guiX0 * scale), py0 = (int) Math.floor(guiY0 * scale);
        int px1 = (int) Math.ceil (guiX1 * scale), py1 = (int) Math.ceil (guiY1 * scale);

        if (px1 <= px0) px1 = px0 + 1;
        if (py1 <= py0) py1 = py0 + 1;

        RenderSystem.enableScissor(px0, window.getHeight() - py1, px1 - px0, py1 - py0);
    }

    private int[] scissorRectContainer() {
        int left = (width - imageWidth) / 2;
        int top  = (height - imageHeight) / 2;
        int[] p = scissorRectPixels();
        return new int[]{ p[0] - left, p[1] - top, p[2] - left, p[3] - top };
    }

    private static boolean pointInRect(int x, int y, int x0, int y0, int x1, int y1) {
        return x >= x0 && x <= x1 && y >= y0 && y <= y1;
    }
}
