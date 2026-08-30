package net.ookasamoti.crystallography.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.client.gui.dial.DialDrawer;
import net.ookasamoti.crystallography.client.gui.dial.DialLayout;
import net.ookasamoti.crystallography.common.menu.DialSlot;
import net.ookasamoti.crystallography.common.menu.JewelryTableMenu;
import net.ookasamoti.crystallography.common.item.tool.ToolBase;
import net.ookasamoti.crystallography.common.item.tool.ToolWand;
import net.ookasamoti.crystallography.common.item.tool.component.ToolLoadout;
import net.ookasamoti.crystallography.network.JewelryActionC2S;
import org.jetbrains.annotations.NotNull;

public class JewelryTableScreen extends AbstractContainerScreen<JewelryTableMenu> {

    private static final Identifier TEXTURE =
            Identifier.parse(CrystallographyMod.MOD_ID + ":textures/gui/jewelry_table_gui.png");
    private static final Identifier GRADIENT =
            Identifier.parse(CrystallographyMod.MOD_ID + ":textures/gui/jewelry_table_gradient.png");

    // Dial placeholder icons. In 1.21.4+ vanilla's empty-slot tool icons are GUI *sprites*
    // (minecraft:container/slot/*), drawn via blitSprite; the mod's own icons are still item
    // *textures* (textures/item/*.png), drawn via blit. Icon.sprite() picks the right call.
    private record Icon(Identifier id, boolean sprite) {}
    private static Icon spr(String spriteId)   { return new Icon(Identifier.parse(spriteId), true); }
    private static Icon tex(String texturePath){ return new Icon(Identifier.parse(texturePath), false); }

    private static final Icon[] ROD_ICONS = {
            spr("minecraft:container/slot/pickaxe"),
            spr("minecraft:container/slot/shovel"),
            spr("minecraft:container/slot/hoe"),
            spr("minecraft:container/slot/sword"),
            spr("minecraft:container/slot/axe"),
            tex("crystallography:textures/item/empty_slot_trident.png"),
    };

    private static final Icon[] WAND_ICONS = {
            tex("crystallography:textures/item/empty_slot_bow.png"),
            tex("crystallography:textures/item/empty_slot_crossbow.png"),
            tex("crystallography:textures/item/empty_slot_knife.png"),
            tex("crystallography:textures/item/empty_slot_wrench.png"),
            tex("crystallography:textures/item/empty_slot_fishing_rod.png"),
            spr("minecraft:container/slot/shield"),
    };

    private static final Icon CRYSTAL_ICON   = spr("minecraft:container/slot/quartz");
    private static final Icon EMPTY_SLOT_STICK = tex("crystallography:textures/item/empty_slot_stick.png");

    private static void drawIcon(GuiGraphicsExtractor g, Icon ic, int dx, int dy) {
        if (ic.sprite()) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, ic.id(), dx, dy, 16, 16);
        } else {
            g.blit(RenderPipelines.GUI_TEXTURED, ic.id(), dx, dy, 0F, 0F, 16, 16, 16, 16);
        }
    }

    private static final int TOOL_SLOT_X = JewelryTableMenu.TOOL_SLOT_X;
    private static final int TOOL_SLOT_Y = JewelryTableMenu.TOOL_SLOT_Y;

    private DialLayout layTools;

    private ItemStack lastKnownCenter = ItemStack.EMPTY;

    /* ===== ダイヤル回転アニメーション（クライアント専用） ===== */
    private final float[] ringTargetRot = new float[JewelryTableMenu.Ring.values().length];
    private final float[] ringAnimFrom  = new float[JewelryTableMenu.Ring.values().length];
    private final long[]  ringAnimStart = new long [JewelryTableMenu.Ring.values().length];
    private static final long  RING_ANIM_MS     = 60L;
    /** ホイール1ノッチで回す量（スロット単位 = 1 コマ）。 */
    private static final float SLOTS_PER_SCROLL = 1f;

    public JewelryTableScreen(JewelryTableMenu menu, Inventory inv, Component title) {
        // imageWidth/imageHeight are now final and supplied to the super constructor.
        super(menu, inv, title, 176, 166);
    }

    @Override
    protected void init() {
        super.init();

        int left = (width - imageWidth) / 2;
        int top  = (height - imageHeight) / 2;
        int cx = left + TOOL_SLOT_X;
        int cy = top  + TOOL_SLOT_Y;

        layTools = new DialLayout().center(cx, cy)
                .radius(JewelryTableMenu.radiusOf(JewelryTableMenu.Ring.TOOLS))
                .count (JewelryTableMenu.countOf (JewelryTableMenu.Ring.TOOLS))
                .iconDiameter(16).angularOffsetDeg(JewelryTableMenu.baseDegOf(JewelryTableMenu.Ring.TOOLS));

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

    /** ホイール1ステップ分の回転をトリガー（サーバー通知なし）。step はスロット数単位。無限回転。 */
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
            // 間隔は物理数(位置数)で固定 = tier 非依存。無限回転。
            float step    = 360f / JewelryTableMenu.countOf(ring);
            float rot     = getVisualRot(ring);
            int   radius  = JewelryTableMenu.radiusOf(ring);
            float baseDeg = JewelryTableMenu.baseDegOf(ring);

            DialSlot[] slots = menu.getRingSlots(ring);
            for (int vi = 0; vi < slots.length; vi++) {
                DialSlot s = slots[vi];
                double rotRad = Math.toRadians(-90.0 + baseDeg + step * (vi + rot));
                double exactX = JewelryTableMenu.TOOL_SLOT_X + radius * Math.cos(rotRad) - 8;
                double exactY = JewelryTableMenu.TOOL_SLOT_Y + radius * Math.sin(rotRad) - 8;
                int intX = (int) Math.round(exactX);
                int intY = (int) Math.round(exactY);
                s.moveTo(intX, intY);
                s.renderFracX = (float)(exactX - intX);
                s.renderFracY = (float)(exactY - intY);
            }
        }
    }

    // ---- クリック ----
    // slot.x/y が回転済み位置に更新されているため、BUTTON スロットのヒット判定もそのまま動作する。
    // INTERACTIVE スロット（CRYSTALS/CENTER）はバニラの isHovering が slot.x/y を参照するため自動的に正しく動作する。

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();

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
                    ClientPacketDistributor.sendToServer(new JewelryActionC2S(
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
                            ClientPacketDistributor.sendToServer(new JewelryActionC2S(
                                    menu.containerId, JewelryActionC2S.ACTION_TOGGLE_CRYSTAL, backing));
                        }
                        return true;
                    }
                }
            }

        }
        // REGISTRIESボタン: リングがscissor外に回転する場合もあるためscissor判定の外で処理。
        // ただし判定は isActive()(クリップ考慮)で行う。クリップ外へ回転したボタンが、その下の
        // プレイヤーインベントリへのクリックを奪わないようにするため(getVisibleFlag だと奪ってしまう)。
        DialSlot[] regSlots = menu.getRingSlots(JewelryTableMenu.Ring.REGISTRIES);
        for (int vi = 0; vi < regSlots.length; vi++) {
            DialSlot s = regSlots[vi];
            if (!s.isActive() || s.mode() != DialSlot.Mode.BUTTON) continue;
            int sx = leftPos + s.x;
            int sy = topPos  + s.y;
            if (mx >= sx && mx < sx + 16 && my >= sy && my < sy + 16) {
                handleRegistryClick(vi);
                return true;
            }
        }
        // フォーム選択中はプレイヤーインベントリへのクリックを無効化
        if (menu.pendingFormIndex >= 0 && my > topPos + 82) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void handleRegistryClick(int vi) {
        ItemStack tool = menu.getRingSlots(JewelryTableMenu.Ring.CENTER)[0].peekRealItem();
        int tier = (tool.getItem() instanceof ToolBase tb) ? tb.getTier() : 1;
        int slotIndex = vi % Math.max(1, ToolBase.maxLoadouts(tier)); // 24 位置 → 登録枠を繰り返しマップ

        if (menu.pendingFormIndex < 0) {
            // 初期状態: 登録済み枠を選ぶと編集モード（TOOLS選択+CRYSTALS3枠選択 相当）に入る
            if (ToolBase.getLoadout(tool).getAtSlot(slotIndex).isEmpty()) return;
            menu.applyStartEditRegistry(slotIndex);
            ClientPacketDistributor.sendToServer(new JewelryActionC2S(
                    menu.containerId, JewelryActionC2S.ACTION_EDIT_REGISTRY, slotIndex));
            return;
        }

        if (menu.editingRegistrySlot >= 0 && slotIndex != menu.editingRegistrySlot) {
            // REGISTRIES 編集中に別枠を選択 → 登録内容をスワップ（並べ替え）して初期状態化
            menu.applySelectForm(-1);
            ClientPacketDistributor.sendToServer(new JewelryActionC2S(
                    menu.containerId, JewelryActionC2S.ACTION_SWAP_REGISTRY, slotIndex));
            return;
        }

        // 通常登録（新規登録、または編集中の同一枠への上書き保存）
        if (menu.pendingCrystals.size() != ToolLoadout.CRYSTAL_SLOTS) return;
        menu.applySelectForm(-1);
        ClientPacketDistributor.sendToServer(new JewelryActionC2S(
                menu.containerId, JewelryActionC2S.ACTION_REGISTER, slotIndex));
    }

    // ---- キー操作（Esc=編集キャンセル、Space=編集中のREGISTRIES枠を削除） ----
    @Override
    public boolean keyPressed(@NotNull net.minecraft.client.input.KeyEvent event) {
        if (event.isEscape() && menu.pendingFormIndex >= 0) {
            menu.applySelectForm(-1);
            ClientPacketDistributor.sendToServer(new JewelryActionC2S(
                    menu.containerId, JewelryActionC2S.ACTION_CANCEL, 0));
            return true;
        }
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE && menu.editingRegistrySlot >= 0) {
            int slot = menu.editingRegistrySlot;
            menu.applySelectForm(-1);
            ClientPacketDistributor.sendToServer(new JewelryActionC2S(
                    menu.containerId, JewelryActionC2S.ACTION_DELETE_REGISTRY, slot));
            return true;
        }
        return super.keyPressed(event);
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
            rotateRing(JewelryTableMenu.Ring.TOOLS, dir * SLOTS_PER_SCROLL);
        } else if (dist < midCR) {
            rotateRing(JewelryTableMenu.Ring.CRYSTALS, dir * SLOTS_PER_SCROLL);
        } else {
            rotateRing(JewelryTableMenu.Ring.REGISTRIES, dir * SLOTS_PER_SCROLL);
        }
        return true;
    }

    // ---- ラベル ----
    @Override
    protected void extractLabels(@NotNull GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, 8, 6, 0x404040, false);
        g.text(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0x404040, false);
    }

    // ---- 背景（旧 renderBg + renderBackground）----
    // 1.21.5 では描画メソッドが render*→extract* にリネームされ、背景パネルは extractBackground で描く。
    // extractBackground は extractRenderState より前に毎フレーム呼ばれるため、回転位置の更新もここで行う。
    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        updateSlotPositions(); // 毎フレーム slot.x/y を回転位置に更新（ヒットボックスも移動）

        ItemStack current = menu.getRingSlots(JewelryTableMenu.Ring.CENTER)[0].peekRealItem();
        if (!ItemStack.matches(current, lastKnownCenter)) {
            lastKnownCenter = current.copy();
            menu.clientSyncState();
        }

        super.extractBackground(g, mouseX, mouseY, partialTick); // 背後の暗転

        int left = (width - imageWidth) / 2;
        int top  = (height - imageHeight) / 2;
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,  left, top, 0F, 0F, imageWidth, imageHeight, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, GRADIENT, left, top, 0F, 0F, imageWidth, imageHeight, 256, 256);

        if (menu.pendingFormIndex >= 0) {
            for (Slot slot : this.menu.slots) {
                if (slot instanceof DialSlot) continue;
                int sx = this.leftPos + slot.x;
                int sy = this.topPos  + slot.y;
                g.fill(sx, sy, sx + 16, sy + 16, 0x88000000);
            }
        }

        renderDialIcons(g);
    }

    // ---- ダイヤルアイコン描画 ----
    private void renderDialIcons(GuiGraphicsExtractor g) {
        int[] sc = scissorRectPixels();
        g.enableScissor(sc[0], sc[1], sc[2], sc[3]);
        try {
            ItemStack center = menu.getRingSlots(JewelryTableMenu.Ring.CENTER)[0].peekRealItem();
            Icon[] toolIcons = (center.getItem() instanceof ToolWand) ? WAND_ICONS : ROD_ICONS;

            // TOOLSボタン: slot[vi] のアイコンは vi % length で固定、選択中は pendingFormIndex でハイライト
            DialSlot[] toolSlots = menu.getRingSlots(JewelryTableMenu.Ring.TOOLS);
            for (int vi = 0; vi < toolSlots.length; vi++) {
                DialSlot s = toolSlots[vi];
                if (!s.getVisibleFlag() || s.mode() != DialSlot.Mode.BUTTON) continue;
                g.pose().pushMatrix();
                g.pose().translate(s.renderFracX, s.renderFracY);
                int dx = leftPos + s.x;
                int dy = topPos  + s.y;
                drawIcon(g, toolIcons[vi % toolIcons.length], dx, dy);
                if (vi == menu.pendingFormIndex) {
                    DialDrawer.circleOutline(g, dx + 8f, dy + 8f,  9.55f, 1.5f, 0xFFFFFFFF);
                    DialDrawer.circleOutline(g, dx + 8f, dy + 8f, 10.45f, 1.5f, 0xFFFFFFFF);
                }
                g.pose().popMatrix();
            }

            // CRYSTALS: 空プレースホルダー + 選択インジケーター
            DialSlot[] crystalSlots = menu.getRingSlots(JewelryTableMenu.Ring.CRYSTALS);
            for (int vi = 0; vi < crystalSlots.length; vi++) {
                DialSlot s = crystalSlots[vi];
                if (!s.getVisibleFlag()) continue;
                g.pose().pushMatrix();
                g.pose().translate(s.renderFracX, s.renderFracY);
                int dx = leftPos + s.x;
                int dy = topPos  + s.y;
                if (s.peekRealItem().isEmpty()) {
                    drawIcon(g, CRYSTAL_ICON, dx, dy);
                }
                if (menu.pendingFormIndex >= 0
                        && menu.pendingCrystals.contains(menu.crystalBackingOf(vi))) {
                    DialDrawer.circleOutline(g, dx + 8f, dy + 8f,  9.55f, 1.5f, 0xFFFFFFFF);
                    DialDrawer.circleOutline(g, dx + 8f, dy + 8f, 10.45f, 1.5f, 0xFFFFFFFF);
                }
                g.pose().popMatrix();
            }

            // REGISTRIES: ロードアウト枠表示（24 位置に登録枠を vi % maxLoadouts で繰り返しマップ）
            DialSlot[] registrySlots = menu.getRingSlots(JewelryTableMenu.Ring.REGISTRIES);
            var loadoutList = ToolBase.getLoadout(center);
            int regTier = (center.getItem() instanceof ToolBase tb) ? tb.getTier() : 1;
            int regMax  = Math.max(1, ToolBase.maxLoadouts(regTier));
            ItemStack displayBase = center.isEmpty() ? null : center.copy();
            if (displayBase != null) ToolBase.clearDraftLoadout(displayBase);
            for (int vi = 0; vi < registrySlots.length; vi++) {
                DialSlot s = registrySlots[vi];
                if (!s.getVisibleFlag() || s.mode() != DialSlot.Mode.BUTTON) continue;
                int li = vi % regMax;
                g.pose().pushMatrix();
                g.pose().translate(s.renderFracX, s.renderFracY);
                int dx = leftPos + s.x;
                int dy = topPos  + s.y;
                if (displayBase != null && loadoutList.getAtSlot(li).isPresent()) {
                    ToolBase.setActiveIndex(displayBase, li);
                    g.item(displayBase, dx, dy);
                } else {
                    drawIcon(g, EMPTY_SLOT_STICK, dx, dy);
                }
                if (li == menu.editingRegistrySlot) {
                    DialDrawer.circleOutline(g, dx + 8f, dy + 8f,  9.55f, 1.5f, 0xFFFFFFFF);
                    DialDrawer.circleOutline(g, dx + 8f, dy + 8f, 10.45f, 1.5f, 0xFFFFFFFF);
                }
                g.pose().popMatrix();
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
                            g.pose().pushMatrix();
                            g.pose().translate(s.renderFracX, s.renderFracY);
                            int dx = leftPos + s.x;
                            int dy = topPos  + s.y;
                            g.item(item, dx, dy);
                            g.itemDecorations(this.font, item, dx, dy);
                            g.pose().popMatrix();
                        }
                    }
                }
            }
        } finally {
            g.disableScissor();
        }
    }

    // ---- ホバー円ハイライト（全スロット描画後＝最前面に描く） ----
    // 旧 renderSlotHighlight 相当。バニラの白四角は DialSlot.isHighlightable()=false で抑制済み。
    // INTERACTIVE のダイヤルスロットをホバー中のときだけ、コンテナ座標で円を描画する。
    @Override
    public void extractContents(@NotNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractContents(g, mouseX, mouseY, partialTick);
        if (this.hoveredSlot instanceof DialSlot ds
                && ds.getVisibleFlag()
                && ds.mode() == DialSlot.Mode.INTERACTIVE) {
            g.pose().pushMatrix();
            g.pose().translate(this.leftPos, this.topPos);
            DialDrawer.filledCircleGui(g, ds.x + 8f, ds.y + 8f, 10f, 0x80FFFFFF);
            g.pose().popMatrix();
        }
    }

    // ---- スロット描画（サブピクセル補正を pose.translate で加算） ----
    @Override
    protected void extractSlot(@NotNull GuiGraphicsExtractor g, @NotNull Slot slot, int mouseX, int mouseY) {
        if (slot instanceof DialSlot ds) {
            // extractSlot runs with the pose already translated to (leftPos, topPos), and
            // enableScissor applies the current pose — so pass clip coords RELATIVE to the
            // container origin (scissorRectContainer), not absolute screen coords.
            int[] sc = scissorRectContainer();
            g.enableScissor(sc[0], sc[1], sc[2], sc[3]);
            try {
                // ホバー円ハイライトは全スロット描画後（最前面）に extractContents で描く。
                if (ds.renderFracX != 0f || ds.renderFracY != 0f) {
                    g.pose().pushMatrix();
                    g.pose().translate(ds.renderFracX, ds.renderFracY);
                    super.extractSlot(g, ds, mouseX, mouseY);
                    g.pose().popMatrix();
                } else {
                    super.extractSlot(g, ds, mouseX, mouseY);
                }
            } finally {
                g.disableScissor();
            }
            return;
        }
        super.extractSlot(g, slot, mouseX, mouseY);
    }

    // ---- scissor 補助（GUI 座標。スケール変換は GuiGraphics 側が行う） ----
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
