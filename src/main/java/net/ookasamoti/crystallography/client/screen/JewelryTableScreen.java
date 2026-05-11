package net.ookasamoti.crystallography.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.item.tool.ToolBase;
import net.ookasamoti.crystallography.common.item.tool.component.ToolForm;
import net.ookasamoti.crystallography.network.RotateRingC2S;
import org.jetbrains.annotations.NotNull;

public class JewelryTableScreen extends AbstractContainerScreen<JewelryTableMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CrystallographyMod.MOD_ID, "textures/gui/jewelry_table_gui.png");
    private static final ResourceLocation GRADIENT = ResourceLocation.fromNamespaceAndPath(
            CrystallographyMod.MOD_ID, "textures/gui/jewelry_table_gradient.png");

    // imageHeight=166 (Dispenser-style), dial center (88,58), gradient clip y=0..72
    // Vanilla formula: row0=166-82=84, hotbar=166-24=142
    private static final int DIAL_CX   = 88;
    private static final int DIAL_CY   = 58;
    private static final int FORM_R    = 24;   // form icon ring radius (px)
    private static final int LOADOUT_R = 36;   // loadout ring radius
    private static final int CRYSTAL_R = 48;   // crystal ring radius
    private static final int DIAL_H    = 72;   // gradient clip height (= inventoryLabelY)
    private static final int FORMS_VIS = 6;
    private static final int LOADOUT_N = 8;

    // Texture dimensions
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public JewelryTableScreen(JewelryTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth      = 176;
        this.imageHeight     = 166;
        this.inventoryLabelY = DIAL_H;
        this.titleLabelY     = -100;
    }

    // ── rendering ────────────────────────────────────────────────────────────

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        super.render(g, mx, my, pt);       // renderBg → slots → labels

        // Post-slot overlays clipped to the dial area (gradient rectangle)
        g.enableScissor(leftPos, topPos, leftPos + imageWidth, topPos + DIAL_H);
        drawGradientOverlay(g);
        renderFormRing(g, mx, my);
        renderLoadoutRing(g, mx, my);
        renderCrystalOverlay(g);
        if (menu.canRegister()) renderRegisterHint(g, mx, my);
        g.disableScissor();

        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float pt, int mx, int my) {
        // 1. Full GUI background from texture
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        // 2. Crystal ring slot backgrounds — only within the dial clip area
        for (int i = 0; i < 6; i++) {
            if (JewelryTableMenu.CRYSTAL_SLOT_Y[i] + 8 < DIAL_H) {
                drawSlotBg(g,
                        leftPos + JewelryTableMenu.CRYSTAL_SLOT_X[i],
                        topPos  + JewelryTableMenu.CRYSTAL_SLOT_Y[i]);
            }
        }

        // 3. ToolBase center slot background
        drawSlotBg(g,
                leftPos + JewelryTableMenu.TOOLBASE_SLOT_X,
                topPos  + JewelryTableMenu.TOOLBASE_SLOT_Y);
    }

    /** Draws a standard recessed slot background (18×18 border + 16×16 inner). */
    private void drawSlotBg(GuiGraphics g, int x, int y) {
        // Sunken border: dark on top/left, light on bottom/right
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737); // dark outer
        g.fill(x,     y,     x + 16, y + 17, 0xFFAAAAAA); // light bottom edge
        g.fill(x,     y,     x + 17, y + 16, 0xFFAAAAAA); // light right edge
        g.fill(x,     y,     x + 16, y + 16, 0xFF8B8B8B); // inner gray
    }

    private void drawGradientOverlay(GuiGraphics g) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(GRADIENT, leftPos, topPos, 0, 0, imageWidth, DIAL_H, TEX_W, TEX_H);
        RenderSystem.disableBlend();
    }

    // ── form icon ring ────────────────────────────────────────────────────────

    private void renderFormRing(GuiGraphics g, int mx, int my) {
        int offset   = menu.getFormOffset();
        int selected = menu.getSelectedForm();
        ToolForm[] forms = ToolForm.values();

        for (int vi = 0; vi < FORMS_VIS; vi++) {
            int fi = (vi + offset) % forms.length;
            int[] pos = ringPos(FORM_R, vi, FORMS_VIS, -90.0);
            int x = leftPos + pos[0] - 8;
            int y = topPos  + pos[1] - 8;
            if (pos[1] < 4 || pos[1] > DIAL_H - 4) continue;

            boolean active  = (fi == selected);
            boolean hovered = over(mx, my, x, y, 16, 16);
            int bg = active ? 0xFFFFD700 : (hovered ? 0xFFBBBBBB : 0xFF777777);

            g.fill(x,     y,     x + 16, y + 16, bg);
            g.fill(x - 1, y - 1, x + 17, y + 17, 0x44000000); // subtle border
            g.fill(x,     y,     x + 16, y + 16, bg);
            g.drawString(font, abbrev(forms[fi].name(), 2), x + 2, y + 5,
                    active ? 0x333300 : 0xFFFFFF, false);
        }
    }

    // ── loadout ring ──────────────────────────────────────────────────────────

    private void renderLoadoutRing(GuiGraphics g, int mx, int my) {
        var toolStack = menu.slots.get(JewelryTableMenu.SLOT_TOOLBASE).getItem();
        if (toolStack.isEmpty()) return;
        var loadouts  = ToolBase.getLoadout(toolStack).entries();
        boolean canReg = menu.canRegister();

        for (int vi = 0; vi < LOADOUT_N; vi++) {
            int[] pos = ringPos(LOADOUT_R, vi, LOADOUT_N, -90.0);
            if (pos[1] < 6 || pos[1] > DIAL_H - 6) continue;

            int x = leftPos + pos[0] - 8;
            int y = topPos  + pos[1] - 8;
            boolean hovered = over(mx, my, x, y, 16, 16);

            if (vi < loadouts.size()) {
                g.fill(x, y, x + 16, y + 16, hovered ? 0xFFBBBB55 : 0xFF888833);
                g.drawString(font, abbrev(loadouts.get(vi).form().name(), 2),
                        x + 2, y + 5, 0xFFFF88, false);
            } else {
                g.fill(x, y, x + 16, y + 16, (canReg && hovered) ? 0xFF44AA44 : 0xFF445544);
            }
        }
    }

    // ── crystal assignment overlay ────────────────────────────────────────────

    private void renderCrystalOverlay(GuiGraphics g) {
        int selected = menu.getSelectedForm();
        if (selected < 0) return;
        int offset = menu.getCrystalOffset();

        for (int vi = 0; vi < 6; vi++) {
            int actualSlot = Math.floorMod(vi + offset, 6);
            int sx = leftPos + JewelryTableMenu.CRYSTAL_SLOT_X[vi];
            int sy = topPos  + JewelryTableMenu.CRYSTAL_SLOT_Y[vi];

            int assignPos = -1;
            for (int p = 0; p < 3; p++) {
                if (menu.getCrystalAssign(p) == actualSlot) { assignPos = p; break; }
            }

            if (assignPos >= 0) {
                g.fill(sx, sy, sx + 16, sy + 16, 0x9900CC44);
                g.drawCenteredString(font, String.valueOf(assignPos + 1), sx + 8, sy + 4, 0xFFFFFF);
            } else {
                g.fill(sx, sy, sx + 16, sy + 16, 0x44CCCCCC);
            }
        }
    }

    // ── register hint ─────────────────────────────────────────────────────────

    private void renderRegisterHint(GuiGraphics g, int mx, int my) {
        int x = leftPos + DIAL_CX - 7;
        int y = topPos  + DIAL_CY + 14; // just below center slot
        boolean hovered = over(mx, my, x, y, 14, 10);
        g.fill(x, y, x + 14, y + 10, hovered ? 0xFF00CC00 : 0xFF008800);
        g.drawCenteredString(font, "OK", x + 7, y + 1, 0xFFFFFF);
    }

    // ── labels ────────────────────────────────────────────────────────────────

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mx, int my) {
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
    }

    // ── input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int imx = (int) mx, imy = (int) my;
        int selected = menu.getSelectedForm();

        // Crystal ring → button mode when a form is selected (only within clip area)
        if (selected >= 0) {
            for (int vi = 0; vi < 6; vi++) {
                if (JewelryTableMenu.CRYSTAL_SLOT_Y[vi] + 8 >= DIAL_H) continue;
                int sx = leftPos + JewelryTableMenu.CRYSTAL_SLOT_X[vi];
                int sy = topPos  + JewelryTableMenu.CRYSTAL_SLOT_Y[vi];
                if (over(imx, imy, sx, sy, 16, 16)) {
                    Minecraft.getInstance().gameMode.handleInventoryButtonClick(
                            menu.containerId, JewelryTableMenu.BTN_ASSIGN_CRYSTAL_BASE + vi);
                    return true;
                }
            }
        }

        // Form ring buttons
        ToolForm[] forms = ToolForm.values();
        int formOffset = menu.getFormOffset();
        for (int vi = 0; vi < FORMS_VIS; vi++) {
            int[] pos = ringPos(FORM_R, vi, FORMS_VIS, -90.0);
            if (pos[1] < 4 || pos[1] > DIAL_H - 4) continue;
            int x = leftPos + pos[0] - 8;
            int y = topPos  + pos[1] - 8;
            if (over(imx, imy, x, y, 16, 16)) {
                int fi = (vi + formOffset) % forms.length;
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(
                        menu.containerId, JewelryTableMenu.BTN_SELECT_FORM_BASE + fi);
                return true;
            }
        }

        // Loadout ring → register when ready
        if (!menu.slots.get(JewelryTableMenu.SLOT_TOOLBASE).getItem().isEmpty()
                && menu.canRegister()) {
            for (int vi = 0; vi < LOADOUT_N; vi++) {
                int[] pos = ringPos(LOADOUT_R, vi, LOADOUT_N, -90.0);
                if (pos[1] < 6 || pos[1] > DIAL_H - 6) continue;
                int x = leftPos + pos[0] - 8;
                int y = topPos  + pos[1] - 8;
                if (over(imx, imy, x, y, 16, 16)) {
                    Minecraft.getInstance().gameMode.handleInventoryButtonClick(
                            menu.containerId, JewelryTableMenu.BTN_REGISTER);
                    return true;
                }
            }
        }

        // Register hint button
        if (menu.canRegister()) {
            int x = leftPos + DIAL_CX - 7;
            int y = topPos  + DIAL_CY + 14;
            if (over(imx, imy, x, y, 14, 10)) {
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(
                        menu.containerId, JewelryTableMenu.BTN_REGISTER);
                return true;
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        double lx   = mx - leftPos - DIAL_CX;
        double ly   = my - topPos  - DIAL_CY;
        double dist = Math.sqrt(lx * lx + ly * ly);

        if (dist > CRYSTAL_R - 18 && dist < CRYSTAL_R + 18) {
            int steps = dy > 0 ? -1 : 1;
            PacketDistributor.sendToServer(
                    new RotateRingC2S(menu.containerId, RotateRingC2S.RING_CRYSTALS, steps));
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    // ── static helpers ────────────────────────────────────────────────────────

    /** Center pixel of ring position i in GUI-image coordinates. */
    private static int[] ringPos(int radius, int i, int count, double startDeg) {
        double angle = Math.toRadians(startDeg + 360.0 / count * i);
        int cx = DIAL_CX + (int) Math.round(radius * Math.cos(angle));
        int cy = DIAL_CY + (int) Math.round(radius * Math.sin(angle));
        return new int[]{cx, cy};
    }

    private static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static String abbrev(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
