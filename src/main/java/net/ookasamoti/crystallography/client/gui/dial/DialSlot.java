package net.ookasamoti.crystallography.client.gui.dial;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

/**
 * 円配置向けカスタムスロット（"スロット/表示専用/非表示"の3状態）。
 *
 * 重要：
 * - DECORATION は「スロットとして存在しない（ヒット無し）」にするため isActive()=false。
 *   → そのままだと AbstractContainerScreen#renderSlot が描画もしないため、
 *      Screen 側で rs.peekRealItem() を使って DECORATION を手描きする前提。
 * - INTERACTIVE のみ通常スロットとして動作（set/remove も有効）。
 *
 * handlerSupplier は IItemHandler で受ける（IItemHandlerModifiable も渡せる）。
 * 変更操作は IItemHandlerModifiable のときのみ行う。
 *
 * コンストラクタ引数順:
 *   (handlerSupplier, mapper, ringOrdinal, visualIndex, x, y, initialMode, diameterPx)
 */
public class DialSlot extends Slot {

    public enum Mode { INTERACTIVE, DECORATION, HIDDEN, BUTTON }

    private final Supplier<? extends IItemHandler> handlerSupplier;
    private final IntUnaryOperator mapper;
    private final int ringOrdinal;
    private final int visualIndex;

    private Mode mode;
    private boolean visibleFlag = true;
    private int diameter = 16;

    // 入力用クリップ（コンテナ座標：Slot.x/y はコンテナ基準）
    private boolean hasClip = false;
    private int clipX0 = Integer.MIN_VALUE, clipY0 = Integer.MIN_VALUE;
    private int clipX1 = Integer.MAX_VALUE, clipY1 = Integer.MAX_VALUE;

    /**
     * @param ringOrdinal  所属リングの enum ordinal（{@code Ring.values()[rs.ring()]} で逆引きできる）
     * @param visualIndex  リング内の視覚インデックス（mapper に渡される）
     * @param x            コンテナ座標 X（スロット左上）
     * @param y            コンテナ座標 Y（スロット左上）
     */
    public DialSlot(Supplier<? extends IItemHandler> handlerSupplier,
                    IntUnaryOperator mapper,
                    int ringOrdinal,
                    int visualIndex,
                    int x, int y,
                    Mode initialMode,
                    int diameterPx) {
        // Slot は Container を要求するためダミーを渡す（実データは handlerSupplier に委譲）
        super(new SimpleContainer(1), 0, x, y);
        this.handlerSupplier = Objects.requireNonNull(handlerSupplier);
        this.mapper          = Objects.requireNonNull(mapper);
        this.ringOrdinal     = ringOrdinal;
        this.visualIndex     = visualIndex;
        this.mode            = Objects.requireNonNull(initialMode);
        this.diameter        = Math.max(1, diameterPx);
    }

    // ---- public API ----

    /** 所属リングの ordinal。{@code Ring.values()[rs.ring()]} で enum に戻せる。 */
    public int ring()        { return ringOrdinal; }
    public int visualIndex() { return visualIndex; }

    public Mode mode()                { return mode; }
    public void setMode(Mode m)       { this.mode = Objects.requireNonNull(m); }

    public boolean getVisibleFlag()   { return visibleFlag; }
    public void setVisibleFlag(boolean v) { this.visibleFlag = v; }

    public int  diameter()            { return diameter; }
    public void setDiameter(int px)   { this.diameter = Math.max(1, px); }

    /** 入力（クリック）向けクリップ：半開区間 [x0,x1) [y0,y1) */
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

    /**
     * モードに関係なく「実際の中身」を覗く。
     * Screen 側が DECORATION を手描きするときに使う。
     */
    public @NotNull ItemStack peekRealItem() {
        IItemHandler h = handlerSupplier.get();
        return h.getStackInSlot(mapIndex(h));
    }

    // ---- Slot overrides (input) ----

    /** ヒットボックスを消すため、INTERACTIVE のときだけ true */
    @Override
    public boolean isActive() {
        return visibleFlag && mode == Mode.INTERACTIVE && fullyInsideClip();
    }

    /** 枠ハイライトも INTERACTIVE のみ */
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

    /**
     * バニラ描画や shift-click 判定に使われるので、
     * INTERACTIVE 以外は EMPTY を返し「スロットとして存在しない」扱いにする。
     */
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

    private boolean intersectsClip() {
        if (!hasClip) return true;
        int x0 = this.x, y0 = this.y, x1 = x0 + 16, y1 = y0 + 16;
        return (x1 > clipX0) && (x0 < clipX1) && (y1 > clipY0) && (y0 < clipY1);
    }

    private boolean fullyInsideClip() {
        if (!hasClip) return true;
        int x0 = this.x, y0 = this.y, x1 = x0 + 16, y1 = y0 + 16;
        return (x0 >= clipX0) && (y0 >= clipY0) && (x1 <= clipX1) && (y1 <= clipY1);
    }

    // ---- optional: circle hover helper ----

    /**
     * 四角の代わりに「円」としてのヒット判定が欲しい場合の補助。
     * ※ Screen 側で mouseX/mouseY は"画面座標"、guiLeft/guiTop を渡すこと。
     */
    public boolean isMouseOverCircle(double mouseX, double mouseY, int guiLeft, int guiTop) {
        double cx = guiLeft + this.x + 8.0;
        double cy = guiTop  + this.y + 8.0;
        double r  = this.diameter / 2.0;
        double dx = mouseX - cx, dy = mouseY - cy;
        return (dx * dx + dy * dy) <= (r * r);
    }
}
