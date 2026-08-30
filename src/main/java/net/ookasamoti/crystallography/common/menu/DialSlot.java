package net.ookasamoti.crystallography.common.menu;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
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

    private final Supplier<? extends ItemStacksResourceHandler> handlerSupplier;
    private final IntUnaryOperator mapper;
    private final int ringOrdinal;
    private final int visualIndex;
    private final Predicate<ItemStack> placePredicate;

    /**
     * 完全に取り外す（backing が空になる）除去操作に追加ペナルティを課すか判定する
     * (backingIndex, 除去前のスタック) -> ペナルティを課すなら true。既定は常に false（無効）。
     * 原石バッテリー（[[ToolBase#ORE_CATEGORY]]）専用のフック。
     */
    private BiPredicate<Integer, ItemStack> removalPenalty = (idx, stack) -> false;

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

    public DialSlot(Supplier<? extends ItemStacksResourceHandler> handlerSupplier,
                    IntUnaryOperator mapper,
                    int ringOrdinal,
                    int visualIndex,
                    int x, int y,
                    Mode initialMode,
                    int diameterPx,
                    Predicate<ItemStack> placePredicate) {
        super(new SimpleContainer(1), 0, x, y);
        this.handlerSupplier  = Objects.requireNonNull(handlerSupplier);
        this.mapper           = Objects.requireNonNull(mapper);
        this.ringOrdinal      = ringOrdinal;
        this.visualIndex      = visualIndex;
        this.mode             = Objects.requireNonNull(initialMode);
        this.diameter         = Math.max(1, diameterPx);
        this.origX            = x;
        this.origY            = y;
        this.placePredicate   = Objects.requireNonNull(placePredicate);
    }

    public DialSlot(Supplier<? extends ItemStacksResourceHandler> handlerSupplier,
                    IntUnaryOperator mapper,
                    int ringOrdinal,
                    int visualIndex,
                    int x, int y,
                    Mode initialMode,
                    int diameterPx) {
        this(handlerSupplier, mapper, ringOrdinal, visualIndex, x, y, initialMode, diameterPx, stack -> true);
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

    public void setRemovalPenalty(BiPredicate<Integer, ItemStack> p) {
        this.removalPenalty = Objects.requireNonNull(p);
    }

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
        ItemStacksResourceHandler h = handlerSupplier.get();
        int idx = mapIndex(h);
        return h.getResource(idx).toStack(h.getAmountAsInt(idx));
    }

    // ---- Slot overrides (input) ----

    @Override
    public boolean isActive() {
        // INTERACTIVE: アイテム操作を許可する（完全にクリップ内に入っている場合のみ）
        // BUTTON: ホバー検知のみ有効にする（クリップと交差していれば可）
        return visibleFlag && (
                (mode == Mode.INTERACTIVE && fullyInsideClip()) ||
                (mode == Mode.BUTTON     && intersectsClip()));
    }

    @Override
    public boolean isHighlightable() {
        // Suppress vanilla's slot-highlight sprite for all dial slots; JewelryTableScreen draws
        // its own circular highlight for INTERACTIVE slots in extractSlot().
        return false;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return isActive() && placePredicate.test(stack) && super.mayPlace(stack);
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return isActive() && super.mayPickup(player);
    }

    // ---- Slot overrides (data) ----

    @Override
    public @NotNull ItemStack getItem() {
        if (!visibleFlag || mode == Mode.HIDDEN || mode == Mode.DECORATION) return ItemStack.EMPTY;
        // INTERACTIVE / BUTTON ともに backing の実アイテムを返す
        // BUTTON は mayPickup/mayPlace が false かつ menu.clicked() でブロック済みのため操作は発生しない
        return peekRealItem();
    }

    @Override
    public void set(@NotNull ItemStack stack) {
        // HIDDEN スロットは backing index を可視スロットと共有する（mapper の剰余で折り返すため）。
        // getItem() が常に EMPTY を返すのでサーバーは内容を同期しないが、表示↔非表示の切替時に
        // EMPTY 同期パケットが飛ぶと、共有先の可視スロットの結晶を消してしまう。よって書き込まない。
        if (mode == Mode.HIDDEN) return;
        // 残りのモードはモードに関わらず backing handler を更新する。
        // バニラのスロット同期（ClientboundContainerSetSlotPacket → slot.set()）もここを通るため
        // モードチェックすると BUTTON/DECORATION モード中のネットワーク同期が失敗する。
        // プレイヤー操作のガードは mayPlace() / mayPickup() が担当する。
        ItemStacksResourceHandler h = handlerSupplier.get();
        h.set(mapIndex(h), ItemResource.of(stack), stack.getCount());
        setChanged();
    }

    @Override
    public @NotNull ItemStack remove(int amount) {
        if (mode != Mode.INTERACTIVE) return ItemStack.EMPTY;
        ItemStacksResourceHandler h = handlerSupplier.get();
        int idx = mapIndex(h);
        ItemStack ex = h.getResource(idx).toStack(h.getAmountAsInt(idx));
        if (ex.isEmpty()) return ItemStack.EMPTY;
        boolean willEmptySlot = amount >= ex.getCount();
        ItemStack split = ex.split(amount);
        // backing が完全に空になる除去（＝取り外し）で、かつ removalPenalty が課金対象と判定した場合、
        // 取り出した側から追加で1個消し込む（原石バッテリー中の原石を取り外すペナルティ）。
        if (willEmptySlot && !split.isEmpty() && removalPenalty.test(idx, split)) {
            split.shrink(1);
        }
        h.set(idx, ItemResource.of(ex), ex.getCount());
        setChanged();
        return split;
    }

    @Override
    public int getMaxStackSize() { return 64; }

    // ---- helpers ----

    private int mapIndex(ItemStacksResourceHandler h) {
        int slots  = Math.max(1, h.size());
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
