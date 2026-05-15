package net.ookasamoti.crystallography.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.client.gui.dial.DialDrawer;
import net.ookasamoti.crystallography.client.gui.dial.DialLayout;
import net.ookasamoti.crystallography.client.gui.dial.DialSlot;
import net.ookasamoti.crystallography.common.item.crystal.CrystalStatsRange;
import net.ookasamoti.crystallography.common.item.tool.ToolBase;
import net.ookasamoti.crystallography.common.item.tool.ToolWand;
import net.ookasamoti.crystallography.common.item.tool.component.ToolForm;
import net.ookasamoti.crystallography.common.item.tool.component.ToolLoadout;
import net.ookasamoti.crystallography.data.CrystalStatsRegistry;
import net.ookasamoti.crystallography.network.JewelryActionC2S;
import org.jetbrains.annotations.NotNull;

public class JewelryTableScreen extends AbstractContainerScreen<JewelryTableMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.parse(CrystallographyMod.MOD_ID + ":textures/gui/jewelry_table_gui.png");
    private static final ResourceLocation GRADIENT =
            ResourceLocation.parse(CrystallographyMod.MOD_ID + ":textures/gui/jewelry_table_gradient.png");

    private static final String[] ROD_FORM_NAMES  = {"pickaxe", "shovel", "hoe", "sword", "axe", "spear"};
    private static final String[] WAND_FORM_NAMES = {"bow", "crossbow", "knife", "wrench", "fishing_rod", "shield"};

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

    private static final ResourceLocation CRYSTAL_ICON =
            ResourceLocation.parse("minecraft:textures/item/empty_slot_quartz.png");
    private static final ResourceLocation EMPTY_SLOT_STICK =
            ResourceLocation.parse("crystallography:textures/item/empty_slot_stick.png");

    private static final int TOOL_SLOT_X = JewelryTableMenu.TOOL_SLOT_X;
    private static final int TOOL_SLOT_Y = JewelryTableMenu.TOOL_SLOT_Y;

    private DialLayout layTools, layCrystals, layRegistries, layCenter;

    private ItemStack lastKnownCenter = ItemStack.EMPTY;

    /* ===== ダイヤル回転アニメーション（クライアント専用） ===== */
    private final float[] ringTargetRot = new float[JewelryTableMenu.Ring.values().length];
    private final float[] ringAnimFrom  = new float[JewelryTableMenu.Ring.values().length];
    private final long[]  ringAnimStart = new long [JewelryTableMenu.Ring.values().length];
    private static final long  RING_ANIM_MS    = 60L;
    private static final float DEG_PER_SCROLL  = 5f;

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

    // ---- ダイヤル回転 ----

    /** リングの現在の視覚回転量（スロット数単位）。ease-out で target に向かって補間。 */
    private float getVisualRot(JewelryTableMenu.Ring ring) {
        int ri = ring.ordinal();
        long elapsed = Util.getMillis() - ringAnimStart[ri];
        if (elapsed >= RING_ANIM_MS) return ringTargetRot[ri];
        float t     = elapsed / (float) RING_ANIM_MS;
        float eased = 1f - (1f - t) * (1f - t);
        return ringAnimFrom[ri] + (ringTargetRot[ri] - ringAnimFrom[ri]) * eased;
    }

    /** dir (+1/-1) をスロット数単位のステップに変換。DEG_PER_SCROLL 度 / スクロール。 */
    private float scrollStep(JewelryTableMenu.Ring ring, int dir) {
        return dir * DEG_PER_SCROLL * JewelryTableMenu.countOf(ring) / 360f;
    }

    /** ホイール1ステップ分の回転をトリガー（サーバー通知なし）。step はスロット数単位。 */
    private void rotateRing(JewelryTableMenu.Ring ring, float step) {
        int ri = ring.ordinal();
        ringAnimFrom[ri]  = getVisualRot(ring);
        ringTargetRot[ri] -= step;
        ringAnimStart[ri] = Util.getMillis();
    }

    /**
     * 毎フレーム、視覚回転量に合わせて DialSlot.moveTo() でヒットボックスごと位置を更新する。
     * slot.x/y 自体が現在の回転位置になるため、バニラのホバー・クリック判定もそのまま動作する。
     */
    private void updateSlotPositions() {
        for (JewelryTableMenu.Ring ring : JewelryTableMenu.Ring.values()) {
            if (ring == JewelryTableMenu.Ring.CENTER) continue;
            float rot = getVisualRot(ring);
            int count  = JewelryTableMenu.countOf(ring);
            if (count <= 0) continue;
            int   radius  = JewelryTableMenu.radiusOf(ring);
            float baseDeg = JewelryTableMenu.baseDegOf(ring);
            float step    = 360f / count;

            DialSlot[] slots = menu.getRingSlots(ring);
            for (int vi = 0; vi < slots.length; vi++) {
                DialSlot s = slots[vi];
                if (rot == 0f) {
                    s.resetPosition();
                    s.renderFracX = 0f;
                    s.renderFracY = 0f;
                } else {
                    // JewelryTableMenu.polar と同じ角度計算＋rot*step 度の回転を加算
                    double rotRad = Math.toRadians(-90.0 + baseDeg + step * vi + rot * step);
                    double exactX = JewelryTableMenu.TOOL_SLOT_X + radius * Math.cos(rotRad) - 8;
                    double exactY = JewelryTableMenu.TOOL_SLOT_Y + radius * Math.sin(rotRad) - 8;
                    int intX = (int) Math.round(exactX);
                    int intY = (int) Math.round(exactY);
                    s.moveTo(intX, intY);
                    // サブピクセル補正：整数化した誤差を描画時に float で補う
                    s.renderFracX = (float)(exactX - intX);
                    s.renderFracY = (float)(exactY - intY);
                }
            }
        }
    }

    // ---- クリック ----
    // slot.x/y が回転済み位置に更新されているため、BUTTON スロットのヒット判定もそのまま動作する。
    // INTERACTIVE スロット（CRYSTALS/CENTER）はバニラの isHovering が slot.x/y を参照するため自動的に正しく動作する。

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int[] sc = scissorRectPixels();
        if (pointInRect((int)mx, (int)my, sc[0], sc[1], sc[2], sc[3])) {

            // TOOLSボタン: フォーム選択トグル（同じスロットなら解除）
            DialSlot[] toolSlots = menu.getRingSlots(JewelryTableMenu.Ring.TOOLS);
            for (int vi = 0; vi < toolSlots.length; vi++) {
                DialSlot s = toolSlots[vi];
                if (!s.getVisibleFlag() || s.mode() != DialSlot.Mode.BUTTON) continue;
                int sx = leftPos + s.x;
                int sy = topPos  + s.y;
                if (mx >= sx && mx < sx + 16 && my >= sy && my < sy + 16) {
                    int newForm = (menu.pendingFormIndex == vi) ? -1 : vi;
                    menu.applySelectForm(newForm);
                    PacketDistributor.sendToServer(new JewelryActionC2S(
                            menu.containerId, JewelryActionC2S.ACTION_SELECT_FORM, newForm));
                    return true;
                }
            }

            // FORM_SELECTED: CRYSTALSクリック → 結晶選択トグル
            if (menu.pendingFormIndex >= 0) {
                DialSlot[] crystalSlots = menu.getRingSlots(JewelryTableMenu.Ring.CRYSTALS);
                for (int vi = 0; vi < crystalSlots.length; vi++) {
                    DialSlot s = crystalSlots[vi];
                    if (!s.getVisibleFlag()) continue;
                    int sx = leftPos + s.x;
                    int sy = topPos  + s.y;
                    if (mx >= sx && mx < sx + 16 && my >= sy && my < sy + 16) {
                        if (!s.peekRealItem().isEmpty()) {
                            int backing = menu.crystalBackingOf(vi);
                            menu.applyToggleCrystal(backing);
                            PacketDistributor.sendToServer(new JewelryActionC2S(
                                    menu.containerId, JewelryActionC2S.ACTION_TOGGLE_CRYSTAL, backing));
                        }
                        return true;
                    }
                }
            }

            // REGISTRIESボタン: 次の空き枠に登録
            DialSlot[] regSlots = menu.getRingSlots(JewelryTableMenu.Ring.REGISTRIES);
            for (int vi = 0; vi < regSlots.length; vi++) {
                DialSlot s = regSlots[vi];
                if (!s.getVisibleFlag() || s.mode() != DialSlot.Mode.BUTTON) continue;
                int sx = leftPos + s.x;
                int sy = topPos  + s.y;
                if (mx >= sx && mx < sx + 16 && my >= sy && my < sy + 16) {
                    handleRegistryClick(vi);
                    return true;
                }
            }
        }
        // フォーム選択中はプレイヤーインベントリへのクリックを無効化
        if (menu.pendingFormIndex >= 0 && my > topPos + 82) {
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private void handleRegistryClick(int slotIndex) {
        if (menu.pendingFormIndex < 0 || menu.pendingCrystals.size() != ToolLoadout.CRYSTAL_SLOTS) return;
        ItemStack tool = menu.getRingSlots(JewelryTableMenu.Ring.CENTER)[0].peekRealItem();
        // そのスロットが既に埋まっているか、tier上限を超えていれば何もしない
        int tier = (tool.getItem() instanceof ToolBase tb) ? tb.getTier() : 1;
        if (slotIndex >= ToolBase.maxLoadouts(tier)) return;
        if (ToolBase.getLoadout(tool).getAtSlot(slotIndex).isPresent()) return;
        menu.applySelectForm(-1);
        PacketDistributor.sendToServer(new JewelryActionC2S(
                menu.containerId, JewelryActionC2S.ACTION_REGISTER, slotIndex));
    }

    // ---- ホイール（TOOLS・CRYSTALS のみ回転、サーバー通知なし） ----
    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int dir = (int) Math.signum(dy);
        if (dir == 0) return false;
        if (layTools == null) return false;

        int[] sc = scissorRectPixels();
        if (!pointInRect((int)mx, (int)my, sc[0], sc[1], sc[2], sc[3])) return false;

        // 中心からの距離で最近傍リングを判定（各隣接リング間の中間点を境界とする）
        double cx   = layTools.centerX();
        double cy   = layTools.centerY();
        double dist = Math.hypot(mx - cx, my - cy);
        double midTC = (JewelryTableMenu.radiusOf(JewelryTableMenu.Ring.TOOLS)
                      + JewelryTableMenu.radiusOf(JewelryTableMenu.Ring.CRYSTALS)) / 2.0;
        double midCR = (JewelryTableMenu.radiusOf(JewelryTableMenu.Ring.CRYSTALS)
                      + JewelryTableMenu.radiusOf(JewelryTableMenu.Ring.REGISTRIES)) / 2.0;

        if (dist < midTC) {
            rotateRing(JewelryTableMenu.Ring.TOOLS, scrollStep(JewelryTableMenu.Ring.TOOLS, dir));
        } else if (dist < midCR) {
            rotateRing(JewelryTableMenu.Ring.CRYSTALS, scrollStep(JewelryTableMenu.Ring.CRYSTALS, dir));
        } else {
            rotateRing(JewelryTableMenu.Ring.REGISTRIES, scrollStep(JewelryTableMenu.Ring.REGISTRIES, dir));
        }
        return true;
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

        if (menu.pendingFormIndex >= 0) {
            g.fill(left, top + 82, left + imageWidth, top + imageHeight, 0x88000000);
        }

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

            // TOOLSボタン: slot[vi] のアイコンは vi % length で固定、選択中は pendingFormIndex でハイライト
            DialSlot[] toolSlots = menu.getRingSlots(JewelryTableMenu.Ring.TOOLS);
            for (int vi = 0; vi < toolSlots.length; vi++) {
                DialSlot s = toolSlots[vi];
                if (!s.getVisibleFlag() || s.mode() != DialSlot.Mode.BUTTON) continue;
                g.pose().pushPose();
                g.pose().translate(s.renderFracX, s.renderFracY, 0f);
                int dx = leftPos + s.x;
                int dy = topPos  + s.y;
                g.blit(toolIcons[vi % toolIcons.length], dx, dy, 0, 0, 16, 16, 16, 16);
                if (vi == menu.pendingFormIndex) {
                    DialDrawer.circleOutline(g, dx + 8f, dy + 8f,  9.55f, 1.5f, 0xFFFFFFFF);
                    DialDrawer.circleOutline(g, dx + 8f, dy + 8f, 10.45f, 1.5f, 0xFFFFFFFF);
                }
                g.pose().popPose();
            }

            // CRYSTALS: 空プレースホルダー + 選択インジケーター
            DialSlot[] crystalSlots = menu.getRingSlots(JewelryTableMenu.Ring.CRYSTALS);
            for (int vi = 0; vi < crystalSlots.length; vi++) {
                DialSlot s = crystalSlots[vi];
                if (!s.getVisibleFlag()) continue;
                g.pose().pushPose();
                g.pose().translate(s.renderFracX, s.renderFracY, 0f);
                int dx = leftPos + s.x;
                int dy = topPos  + s.y;
                if (s.peekRealItem().isEmpty()) {
                    g.blit(CRYSTAL_ICON, dx, dy, 0, 0, 16, 16, 16, 16);
                }
                if (menu.pendingFormIndex >= 0
                        && menu.pendingCrystals.contains(menu.crystalBackingOf(vi))) {
                    DialDrawer.circleOutline(g, dx + 8f, dy + 8f,  9.55f, 1.5f, 0xFFFFFFFF);
                    DialDrawer.circleOutline(g, dx + 8f, dy + 8f, 10.45f, 1.5f, 0xFFFFFFFF);
                }
                g.pose().popPose();
            }

            // REGISTRIES: ロードアウト枠表示（登録済み=着彩ツールテクスチャ、空=empty_slot_stick）
            DialSlot[] registrySlots = menu.getRingSlots(JewelryTableMenu.Ring.REGISTRIES);
            var loadoutList = ToolBase.getLoadout(center);
            boolean centerIsWand = center.getItem() instanceof ToolWand;
            int centerTier = (center.getItem() instanceof ToolBase ctb) ? ctb.getTier() : 1;
            for (int vi = 0; vi < registrySlots.length; vi++) {
                DialSlot s = registrySlots[vi];
                if (!s.getVisibleFlag() || s.mode() != DialSlot.Mode.BUTTON) continue;
                g.pose().pushPose();
                g.pose().translate(s.renderFracX, s.renderFracY, 0f);
                int dx = leftPos + s.x;
                int dy = topPos  + s.y;
                var entry = loadoutList.getAtSlot(vi);
                if (entry.isPresent()) {
                    var lo = entry.get();
                    renderToolLayers(g, dx, dy, centerIsWand, centerTier, lo.form(), lo.crystalIndices());
                } else {
                    g.blit(EMPTY_SLOT_STICK, dx, dy, 0, 0, 16, 16, 16, 16);
                }
                g.pose().popPose();
            }

            // 装飾スロット（isActive=false）の実アイテム描画
            for (JewelryTableMenu.Ring ring : JewelryTableMenu.Ring.values()) {
                DialSlot[] slots = menu.getRingSlots(ring);
                for (int vi = 0; vi < slots.length; vi++) {
                    DialSlot s = slots[vi];
                    if (s.getVisibleFlag() && !s.isActive()) {
                        if (ring == JewelryTableMenu.Ring.CENTER
                                && menu.getUiState() == JewelryTableMenu.UiState.FORM_SELECTED) continue;
                        ItemStack item = s.peekRealItem();
                        if (!item.isEmpty()) {
                            g.pose().pushPose();
                            g.pose().translate(s.renderFracX, s.renderFracY, 0f);
                            int dx = leftPos + s.x;
                            int dy = topPos  + s.y;
                            g.renderItem(item, dx, dy);
                            g.renderItemDecorations(this.font, item, dx, dy);
                            g.pose().popPose();
                        }
                    }
                }
            }
        } finally {
            RenderSystem.disableBlend();
            RenderSystem.disableScissor();
        }
    }

    /**
     * center/left/right を結晶色で着彩し、tierレイヤーを白で重ねる。
     * crystalIndices は backingCrystals のスロットインデックス。未選択分は白。
     */
    private void renderToolLayers(GuiGraphics g, int px, int py,
                                  boolean isWand, int tier, ToolForm form, int[] crystalIndices) {
        ToolForm[] forms   = isWand ? JewelryTableMenu.WAND_FORMS : JewelryTableMenu.ROD_FORMS;
        String[]   fNames  = isWand ? WAND_FORM_NAMES : ROD_FORM_NAMES;
        String prefix = isWand ? "toolwand_" : "toolrod_";

        String formName = null;
        for (int i = 0; i < forms.length; i++) {
            if (forms[i] == form) { formName = fNames[i]; break; }
        }
        if (formName == null) return;

        var inv = menu.getBackingCrystals();
        String[] suffixes = {"center", "left", "right"};
        for (int i = 0; i < suffixes.length; i++) {
            int tint = CrystalStatsRange.NO_TINT;
            if (i < crystalIndices.length) {
                int bi = crystalIndices[i];
                if (bi >= 0 && bi < inv.getSlots()) {
                    ItemStack cr = inv.getStackInSlot(bi);
                    if (!cr.isEmpty())
                        tint = CrystalStatsRegistry.get(cr).map(CrystalStatsRange::tint)
                                .orElse(CrystalStatsRange.NO_TINT);
                }
            }
            float r, gr, b;
            if (tint == CrystalStatsRange.NO_TINT) { r = gr = b = 1f; }
            else {
                r  = ((tint >> 16) & 0xFF) / 255f;
                gr = ((tint >>  8) & 0xFF) / 255f;
                b  = ( tint        & 0xFF) / 255f;
            }
            RenderSystem.setShaderColor(r, gr, b, 1f);
            g.blit(ResourceLocation.parse(CrystallographyMod.MOD_ID + ":textures/item/"
                    + prefix + formName + "_" + suffixes[i] + ".png"),
                    px, py, 0, 0, 16, 16, 16, 16);
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        g.blit(ResourceLocation.parse(CrystallographyMod.MOD_ID + ":textures/item/"
                + prefix + formName + "_tier" + tier + ".png"),
                px, py, 0, 0, 16, 16, 16, 16);
    }

    // ---- スロット描画（サブピクセル補正を pose.translate で加算） ----
    @Override
    protected void renderSlot(@NotNull GuiGraphics g, @NotNull Slot slot) {
        if (slot instanceof DialSlot ds) {
            int[] sc = scissorRectPixels();
            enableGuiScissor(sc[0], sc[1], sc[2], sc[3]);
            try {
                if (ds.renderFracX != 0f || ds.renderFracY != 0f) {
                    g.pose().pushPose();
                    g.pose().translate(ds.renderFracX, ds.renderFracY, 0f);
                    super.renderSlot(g, ds);
                    g.pose().popPose();
                } else {
                    super.renderSlot(g, ds);
                }
            } finally {
                RenderSystem.disableScissor();
            }
            return;
        }
        super.renderSlot(g, slot);
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float pt) {
        updateSlotPositions(); // 毎フレーム slot.x/y を回転位置に更新（ヒットボックスも移動）
        ItemStack current = menu.getRingSlots(JewelryTableMenu.Ring.CENTER)[0].peekRealItem();
        if (!ItemStack.matches(current, lastKnownCenter)) {
            lastKnownCenter = current.copy();
            menu.clientSyncState();
        }
        renderBackground(g, mouseX, mouseY, pt);
        super.render(g, mouseX, mouseY, pt);
        if (menu.getUiState() == JewelryTableMenu.UiState.FORM_SELECTED) {
            renderCenterPreview(g);
        }
        renderTooltip(g, mouseX, mouseY);
    }

    // ---- CENTERプレビュー（FORM_SELECTED: 組み立て中のプレビュー） ----
    private void renderCenterPreview(GuiGraphics g) {
        if (menu.pendingFormIndex < 0) return;
        DialSlot centerSlot = menu.getRingSlots(JewelryTableMenu.Ring.CENTER)[0];
        ItemStack center = centerSlot.peekRealItem();
        boolean isWand = center.getItem() instanceof ToolWand;
        ToolForm[] forms = isWand ? JewelryTableMenu.WAND_FORMS : JewelryTableMenu.ROD_FORMS;
        if (menu.pendingFormIndex >= forms.length) return;

        int tier = (center.getItem() instanceof ToolBase tb) ? tb.getTier() : 1;
        int[] crystalIndices = menu.pendingCrystals.stream().mapToInt(i -> i).toArray();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        renderToolLayers(g, leftPos + centerSlot.x, topPos + centerSlot.y,
                isWand, tier, forms[menu.pendingFormIndex], crystalIndices);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    // ---- scissor 補助 ----
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
