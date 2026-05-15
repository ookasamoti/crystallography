package net.ookasamoti.crystallography.client.gui.dial;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

/**
 * 円配置向けカスタムスロット。
 *
 * Slot.x/y はリフレクションで書き換えることでヒットボックスごと移動させる。
 * origX/origY がコンストラクタで設定した元の位置。
 * INTERACTIVE のみ通常スロットとして動作。BUTTON/DECORATION/HIDDEN は isActive()=false。
 */
public class DialSlot extends Slot {

    public enum Mode { INTERACTIVE, DECORATION, HIDDEN, BUTTON }

    // ---- Slot.x/y を書き換えるリフレクション ----
    private static final Field FIELD_X;
    private static final Field FIELD_Y;
    static {
        Field fx = null, fy = null;
        try {
            fx = Slot.class.getDeclaredField("x");
            fy = Slot.class.getDeclaredField("y");
            fx.setAccessible(true);
            fy.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            // フォールバック：ヒットボックスは動かないが描画は維持される
        }
        FIELD_X = fx;
        FIELD_Y = fy;
    }

    private final Supplier<? extends IItemHandler> handlerSupplier;
    private final IntUnaryOperator mapper;
    private final int ringOrdinal;
    private final int visualIndex;

    /** コンストラクタで設定した元のコンテナ座標（reset 用） */
    public final int origX, origY;

    /** 整数 slot.x/y に対するサブピクセル補正（描画専用。毎フレーム Screen 側が更新）。 */
    public float renderFracX = 0f, renderFracY = 0f;

    private Mode mode;
    private boolean visibleFlag = true;
    private int diameter = 16;

    // クリップ（コンテナ座標）
    private boolean hasClip = false;
    private int clipX0 = Integer.MIN_VALUE, clipY0 = Integer.MIN_VALUE;
    private int clipX1 = Integer.MAX_VALUE, clipY1 = Integer.MAX_VALUE;

    public DialSlot(Supplier<? extends IItemHandler> handlerSupplier,
                    IntUnaryOperator mapper,
                    int ringOrdinal,
                    int visualIndex,
                    int x, int y,
                    Mode initialMode,
                    int diameterPx) {
        super(new SimpleContainer(1), 0, x, y);
        this.handlerSupplier = Objects.requireNonNull(handlerSupplier);
        this.mapper          = Objects.requireNonNull(mapper);
        this.ringOrdinal     = ringOrdinal;
        this.visualIndex     = visualIndex;
        this.mode            = Objects.requireNonNull(initialMode);
        this.diameter        = Math.max(1, diameterPx);
        this.origX           = x;
        this.origY           = y;
    }

    // ---- 位置移動 ----

    /**
     * Slot.x/y をリフレクションで書き換えてヒットボックスごと移動する。
     * クライアント専用（描画スレッドからのみ呼ぶこと）。
     */
    public void moveTo(int newX, int newY) {
        if (FIELD_X == null || FIELD_Y == null) return;
        try {
            FIELD_X.setInt(this, newX);
            FIELD_Y.setInt(this, newY);
        } catch (IllegalAccessException ignored) {}
    }

    /** 元の位置に戻す。 */
    public void resetPosition() {
        moveTo(origX, origY);
    }

    // ---- public API ----

    public int ring()        { return ringOrdinal; }
    public int visualIndex() { return visualIndex; }

    public Mode mode()                { return mode; }
    public void setMode(Mode m)       { this.mode = Objects.requireNonNull(m); }

    public boolean getVisibleFlag()       { return visibleFlag; }
    public void setVisibleFlag(boolean v) { this.visibleFlag = v; }

    public int  diameter()           { return diameter; }
    public void setDiameter(int px)  { this.diameter = Math.max(1, px); }

    /** クリップ（コンテナ座標の半開区間 [x0,x1) [y0,y1)）を設定。 */
    public void setClipBox(int x0, int y0, int x1, int y1) {
        if (x1 < x0) { int t = x0; x0 = x1; x1 = t; }
        if (y1 < y0) { int t = y0; y0 = y1; y1 = t; }
        hasClip = true;
        clipX0 = x0; clipY0 = y0; clipX1 = x1; clipY1 = y1;
    }

    public void clearClipBox() {
        hasClip = false;
        clipX0 = Integer.MIN_VALUE; clipY0 = Integer.MIN_VALUE;
        clipX1 = Integer.MAX_VALUE; clipY1 = Integer.MAX_VALUE;
    }

    /** モードに関係なく実際の中身を覗く（DECORATION 手描き用）。 */
    public @NotNull ItemStack peekRealItem() {
        IItemHandler h = handlerSupplier.get();
        return h.getStackInSlot(mapIndex(h));
    }

    // ---- Slot overrides (input) ----

    @Override
    public boolean isActive() {
        return visibleFlag && mode == Mode.INTERACTIVE && fullyInsideClip();
    }

    @Override
    public boolean isHighlightable() {
        return visibleFlag && mode == Mode.INTERACTIVE && intersectsClip();
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return isActive() && super.mayPlace(stack);
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return isActive() && super.mayPickup(player);
    }

    // ---- Slot overrides (data) ----

    @Override
    public @NotNull ItemStack getItem() {
        if (!visibleFlag || mode != Mode.INTERACTIVE) return ItemStack.EMPTY;
        return peekRealItem();
    }

    @Override
    public void set(@NotNull ItemStack stack) {
        if (mode != Mode.INTERACTIVE) return;
        IItemHandler h = handlerSupplier.get();
        if (h instanceof IItemHandlerModifiable hm) {
            hm.setStackInSlot(mapIndex(hm), stack);
            setChanged();
        }
    }

    @Override
    public @NotNull ItemStack remove(int amount) {
        if (mode != Mode.INTERACTIVE) return ItemStack.EMPTY;
        IItemHandler h = handlerSupplier.get();
        if (!(h instanceof IItemHandlerModifiable hm)) return ItemStack.EMPTY;
        int idx = mapIndex(hm);
        ItemStack ex = hm.getStackInSlot(idx);
        if (ex.isEmpty()) return ItemStack.EMPTY;
        ItemStack split = ex.split(amount);
        hm.setStackInSlot(idx, ex);
        setChanged();
        return split;
    }

    @Override
    public int getMaxStackSize() { return 64; }

    // ---- helpers ----

    private int mapIndex(IItemHandler h) {
        int slots  = Math.max(1, h.getSlots());
        int mapped = mapper.applyAsInt(visualIndex);
        int m = mapped % slots;
        if (m < 0) m += slots;
        return m;
    }

    // クリップ判定は this.x/y（moveTo 後は回転済み位置）を参照する
    private boolean intersectsClip() {
        if (!hasClip) return true;
        int x1 = this.x + 16, y1 = this.y + 16;
        return (x1 > clipX0) && (this.x < clipX1) && (y1 > clipY0) && (this.y < clipY1);
    }

    private boolean fullyInsideClip() {
        if (!hasClip) return true;
        int x1 = this.x + 16, y1 = this.y + 16;
        return (this.x >= clipX0) && (this.y >= clipY0) && (x1 <= clipX1) && (y1 <= clipY1);
    }

    /** 円ヒット判定補助（Screen 座標で渡すこと）。 */
    public boolean isMouseOverCircle(double mouseX, double mouseY, int guiLeft, int guiTop) {
        double cx = guiLeft + this.x + 8.0;
        double cy = guiTop  + this.y + 8.0;
        double r  = this.diameter / 2.0;
        double dx = mouseX - cx, dy = mouseY - cy;
        return (dx * dx + dy * dy) <= (r * r);
    }
}
