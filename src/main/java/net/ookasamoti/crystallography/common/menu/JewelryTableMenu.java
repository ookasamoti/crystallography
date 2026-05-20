package net.ookasamoti.crystallography.common.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ookasamoti.crystallography.common.block.entity.JewelryTableBlockEntity;
import net.ookasamoti.crystallography.common.item.crystal.Crystal;
import net.ookasamoti.crystallography.common.item.tool.ToolBase;
import net.ookasamoti.crystallography.common.item.tool.ToolInventory;
import net.ookasamoti.crystallography.common.item.tool.ToolRod;
import net.ookasamoti.crystallography.common.item.tool.ToolWand;
import net.ookasamoti.crystallography.common.item.tool.component.ToolForm;
import net.ookasamoti.crystallography.common.item.tool.component.ToolLoadout;
import net.ookasamoti.crystallography.common.item.tool.component.ToolStats;
import net.ookasamoti.crystallography.setup.BlockRegistry;
import net.ookasamoti.crystallography.setup.MenuTypesRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class JewelryTableMenu extends AbstractContainerMenu {

    /* ===== 位置（中央） ===== */
    public static final int TOOL_SLOT_X = 32;
    public static final int TOOL_SLOT_Y = 35;

    /* ===== リング ===== */
    public enum Ring { CENTER, TOOLS, CRYSTALS, REGISTRIES }

    public record RingDef(int count, int radius, float baseDeg) {}

    private static final EnumMap<Ring, RingDef> DEF = new EnumMap<>(Map.of(
            Ring.CENTER,     new RingDef( 1,  0,  0f),
            Ring.TOOLS,      new RingDef( 6, 32, 60f),
            Ring.CRYSTALS,   new RingDef(24, 64, 78f),
            Ring.REGISTRIES, new RingDef(32, 96, 78f)
    ));

    public static int   countOf (Ring r){ return Objects.requireNonNull(DEF.get(r)).count(); }
    public static int   radiusOf(Ring r){ return Objects.requireNonNull(DEF.get(r)).radius(); }
    public static float baseDegOf(Ring r){ return Objects.requireNonNull(DEF.get(r)).baseDeg(); }

    /**
     * max を超えない tierCount の最大倍数を返す（二つが等距離の場合は小さい方）。
     * 例: tierCount=12, max=32 → floor(32/12)*12=24
     */
    public static int nearestMultiple(int tierCount, int max) {
        if (tierCount <= 0) return 0;
        return (max / tierCount) * tierCount;
    }

    public static int crystalCountForTier(int tier) {
        return nearestMultiple(ToolBase.crystalSlotCount(tier), countOf(Ring.CRYSTALS));
    }

    public static int registryCountForTier(int tier) {
        return nearestMultiple(ToolBase.maxLoadouts(tier), countOf(Ring.REGISTRIES));
    }

    /** tier に応じた実効スロット数（CRYSTALS/REGISTRIES は動的、その他は物理数）。 */
    public int effectiveCountOf(Ring r) {
        return switch (r) {
            case CRYSTALS   -> crystalCountForTier(getToolTier());
            case REGISTRIES -> registryCountForTier(getToolTier());
            default         -> countOf(r);
        };
    }

    /* ===== フォームマッピング ===== */
    public static final ToolForm[] ROD_FORMS  = {
            ToolForm.PICKAXE, ToolForm.SHOVEL, ToolForm.HOE,
            ToolForm.SWORD,   ToolForm.AXE,    ToolForm.SPEAR
    };
    public static final ToolForm[] WAND_FORMS = {
            ToolForm.BOW, ToolForm.CROSSBOW, ToolForm.KNIFE,
            ToolForm.SPYGLASS, ToolForm.FISHING_ROD, ToolForm.SHIELD
    };

    /* ===== 状態 ===== */
    public enum UiState { EMPTY, EDIT_CRYSTALS, FORM_SELECTED }

    public final JewelryTableBlockEntity blockEntity;
    private final Level level;

    private final EnumMap<Ring, DialSlot[]> rings = new EnumMap<>(Ring.class);
    private final EnumMap<Ring, Integer> firstIndex = new EnumMap<>(Ring.class);

    private UiState uiState = UiState.EMPTY;

    private ItemStack lastCenterTool = ItemStack.EMPTY;

    private int playerStartIndex = -1;
    private int playerEndIndex   = -1;

    /* TOOLS/REGISTRIES スロットは常に BUTTON モードで実アイテムを持たないため、共有の空ハンドラを使う */
    private static final ItemStackHandler EMPTY_HANDLER = new ItemStackHandler(1);

    private final ItemStackHandler fallbackEmpty = new ItemStackHandler(9);
    private IItemHandler backingCrystals = fallbackEmpty;

    /** 保留中フォームインデックス（-1 = 未選択） */
    public int pendingFormIndex = -1;
    /** 選択済み結晶の backing index 一覧 */
    public final ArrayList<Integer> pendingCrystals = new ArrayList<>();

    public JewelryTableMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv,
                (JewelryTableBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(0));
    }

    public JewelryTableMenu(int containerId, Inventory inv, JewelryTableBlockEntity be, ContainerData data) {
        super(MenuTypesRegistry.JEWELRY_TABLE_MENU.get(), containerId);
        this.blockEntity = be;
        this.level = inv.player.level();

        initRings();

        playerStartIndex = this.slots.size();
        addPlayerSlots(inv);
        playerEndIndex = this.slots.size();

        bindCrystalsBacking();
        refreshUiState();
        layoutSlots();
    }

    /* ---------- リング生成 ---------- */
    private void initRings() {
        addRing(Ring.CENTER,
                () -> blockEntity.getItemHandler(),
                vi -> 0,
                this::isTool);

        addRing(Ring.TOOLS,
                () -> EMPTY_HANDLER,
                vi -> 0,
                stack -> false);

        addRing(Ring.CRYSTALS,
                () -> backingCrystals,
                vi -> vi % Math.max(1, backingCrystals.getSlots()),
                stack -> stack.getItem() instanceof Crystal);

        addRing(Ring.REGISTRIES,
                () -> EMPTY_HANDLER,
                vi -> 0,
                stack -> false);
    }

    private void addRing(Ring ring,
                         Supplier<IItemHandler> supplier,
                         IntUnaryOperator mapper,
                         Predicate<ItemStack> placePredicate) {
        RingDef d = DEF.get(ring);
        int first = this.slots.size();
        DialSlot[] arr = new DialSlot[d.count()];

        for (int v = 0; v < d.count(); v++) {
            int[] xy = polar(TOOL_SLOT_X, TOOL_SLOT_Y, d.radius(), v, d.count(), d.baseDeg());
            DialSlot s = new DialSlot(
                    supplier, mapper,
                    ring.ordinal(), v,
                    xy[0] - 8, xy[1] - 8,
                    DialSlot.Mode.HIDDEN, 16,
                    placePredicate
            );
            arr[v] = s;
            this.addSlot(s);
        }

        rings.put(ring, arr);
        firstIndex.put(ring, first);
    }

    private void addPlayerSlots(Inventory inv) {
        for (int r = 0; r < 3; ++r)
            for (int c = 0; c < 9; ++c)
                this.addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int k = 0; k < 9; ++k)
            this.addSlot(new Slot(inv, k, 8 + k * 18, 142));
    }

    /* ---------- アクセサ ---------- */
    public DialSlot[] getRingSlots(Ring ring) { return rings.get(ring); }
    public UiState getUiState() { return uiState; }
    public IItemHandler getBackingCrystals() { return backingCrystals; }

    /** クライアント専用: CENTER スロットの変化を検知したときに呼ぶ。draft DataComponent から状態を復元する。 */
    public void clientSyncState() {
        bindCrystalsBacking();
        refreshUiState();
        layoutSlots();
    }

    /* ---------- クリップ ---------- */
    public void applyClipBoxToAllRings(int x0, int y0, int x1, int y1) {
        for (var r : Ring.values())
            for (var s : rings.get(r)) s.setClipBox(x0, y0, x1, y1);
    }

    /* ---------- UI state ---------- */
    private void refreshUiState() {
        ItemStack tool = rings.get(Ring.CENTER)[0].peekRealItem();
        pendingFormIndex = -1;
        pendingCrystals.clear();

        if (!isTool(tool)) {
            uiState = UiState.EMPTY;
            return;
        }

        // draft DataComponent があれば状態を復元（サーバー書き込み後のクライアント sync 時に使用）
        ToolLoadout draft = ToolBase.getDraftLoadout(tool);
        if (draft != null) {
            boolean isWand = tool.getItem() instanceof ToolWand;
            ToolForm[] forms = isWand ? WAND_FORMS : ROD_FORMS;
            for (int i = 0; i < forms.length; i++) {
                if (forms[i] == draft.form()) { pendingFormIndex = i; break; }
            }
            for (int ci : draft.crystalIndices()) {
                if (ci >= 0) pendingCrystals.add(ci);
            }
            uiState = pendingFormIndex >= 0 ? UiState.FORM_SELECTED : UiState.EDIT_CRYSTALS;
        } else {
            uiState = UiState.EDIT_CRYSTALS;
        }
    }

    private void layoutSlots() {
        for (Ring r : Ring.values())
            for (DialSlot s : rings.get(r)) { s.setMode(DialSlot.Mode.HIDDEN); s.setVisibleFlag(false); }

        switch (uiState) {
            case EMPTY -> {
                showFirstN(Ring.CENTER, 1, DialSlot.Mode.INTERACTIVE);
            }
            case EDIT_CRYSTALS -> {
                showFirstN(Ring.CENTER, 1, DialSlot.Mode.INTERACTIVE);
                showFirstN(Ring.TOOLS, countOf(Ring.TOOLS), DialSlot.Mode.BUTTON);
                showFirstN(Ring.REGISTRIES, effectiveCountOf(Ring.REGISTRIES), DialSlot.Mode.BUTTON);
                showFirstN(Ring.CRYSTALS, effectiveCountOf(Ring.CRYSTALS), DialSlot.Mode.INTERACTIVE);
            }
            case FORM_SELECTED -> {
                showFirstN(Ring.CENTER, 1, DialSlot.Mode.BUTTON);
                showFirstN(Ring.TOOLS, countOf(Ring.TOOLS), DialSlot.Mode.BUTTON);
                showFirstN(Ring.REGISTRIES, effectiveCountOf(Ring.REGISTRIES), DialSlot.Mode.BUTTON);
                showFirstN(Ring.CRYSTALS, effectiveCountOf(Ring.CRYSTALS), DialSlot.Mode.BUTTON);
            }
        }
    }

    private void showFirstN(Ring ring, int n, DialSlot.Mode mode) {
        DialSlot[] arr = rings.get(ring);
        int lim = Math.min(arr.length, Math.max(0, n));
        for (int i = 0; i < arr.length; i++) {
            boolean vis = i < lim;
            arr[i].setVisibleFlag(vis);
            arr[i].setMode(vis ? mode : DialSlot.Mode.HIDDEN);
        }
    }

    /* ---------- フォーム選択アクション（クライアント・サーバー共通） ---------- */
    public void applySelectForm(int formIndex) {
        boolean hasTool = isTool(rings.get(Ring.CENTER)[0].peekRealItem());
        if (formIndex < 0 || !hasTool) {
            pendingFormIndex = -1;
            pendingCrystals.clear();
            uiState = hasTool ? UiState.EDIT_CRYSTALS : UiState.EMPTY;
        } else {
            pendingFormIndex = formIndex;
            pendingCrystals.clear();
            uiState = UiState.FORM_SELECTED;
        }
        layoutSlots();
    }

    public void applyToggleCrystal(int crystalBacking) {
        if (pendingFormIndex < 0) return;
        if (!pendingCrystals.remove((Integer) crystalBacking)) {
            if (pendingCrystals.size() < ToolLoadout.CRYSTAL_SLOTS) {
                pendingCrystals.add(crystalBacking);
            }
        }
    }

    /* ---------- サーバー専用アクション ---------- */
    public void serverSelectForm(int formIndex) {
        applySelectForm(formIndex);
        if (formIndex >= 0) writeDraft(); else clearDraftFromItem();
        broadcastChanges();
    }

    public void serverToggleCrystal(int crystalBacking) {
        applyToggleCrystal(crystalBacking);
        writeDraft();
        broadcastChanges();
    }

    public void serverRegister(int slotIndex) {
        if (pendingFormIndex < 0 || pendingCrystals.size() != ToolLoadout.CRYSTAL_SLOTS) return;

        ItemStack toolStack = rings.get(Ring.CENTER)[0].peekRealItem();
        if (!isTool(toolStack)) return;

        int tier = getToolTier();
        if (slotIndex < 0 || slotIndex >= ToolBase.maxLoadouts(tier)) return;

        boolean isWand = toolStack.getItem() instanceof ToolWand;
        ToolForm[] forms = isWand ? WAND_FORMS : ROD_FORMS;
        if (pendingFormIndex >= forms.length) return;
        ToolForm form = forms[pendingFormIndex];

        int[] crystalIndices = pendingCrystals.stream().mapToInt(Integer::intValue).toArray();
        ToolStats stats = ToolBase.buildStats(form, tier, crystalIndices, backingCrystals);
        ToolLoadout loadout = new ToolLoadout(slotIndex, form, crystalIndices, stats);

        ItemStack modified = toolStack.copy();
        ToolBase.clearDraftLoadout(modified); // 仮登録を削除してから本登録
        if (!ToolBase.setLoadout(modified, loadout)) return;
        ToolBase.setActiveIndex(modified, slotIndex);
        ToolBase.applyComputedStats(modified);

        writeToCenter(modified);
        applySelectForm(-1);
        broadcastChanges();
    }

    /* ---------- インベントリを閉じたときのクリーンアップ ---------- */
    @Override
    public void removed(@NotNull Player player) {
        clearDraftFromItem();
        super.removed(player);
    }

    /* ---------- draft 書き込み / 削除ヘルパー ---------- */

    /** 現在の pendingFormIndex + pendingCrystals で draft ToolLoadout を構築してツールに書き込む。 */
    private void writeDraft() {
        if (pendingFormIndex < 0) return;
        ItemStack tool = rings.get(Ring.CENTER)[0].peekRealItem();
        if (!isTool(tool)) return;

        boolean isWand = tool.getItem() instanceof ToolWand;
        ToolForm[] forms = isWand ? WAND_FORMS : ROD_FORMS;
        if (pendingFormIndex >= forms.length) return;
        ToolForm form = forms[pendingFormIndex];

        int tier = getToolTier();
        int[] crystalIndices = new int[ToolLoadout.CRYSTAL_SLOTS];
        for (int i = 0; i < ToolLoadout.CRYSTAL_SLOTS; i++) {
            crystalIndices[i] = i < pendingCrystals.size() ? pendingCrystals.get(i) : -1;
        }

        ToolStats stats = ToolBase.buildStats(form, tier, crystalIndices, backingCrystals);
        ToolLoadout draft = new ToolLoadout(ToolLoadout.DRAFT_SLOT, form, crystalIndices, stats);

        ItemStack modified = tool.copy();
        ToolBase.setDraftLoadout(modified, draft);
        writeToCenter(modified);
    }

    /** ツールの draft DataComponent を削除してツールに書き戻す。draft がなければ何もしない。 */
    private void clearDraftFromItem() {
        ItemStack tool = rings.get(Ring.CENTER)[0].peekRealItem();
        if (!isTool(tool) || ToolBase.getDraftLoadout(tool) == null) return;
        ItemStack modified = tool.copy();
        ToolBase.clearDraftLoadout(modified);
        writeToCenter(modified);
    }

    private void writeToCenter(ItemStack stack) {
        DialSlot cs = rings.get(Ring.CENTER)[0];
        cs.setMode(DialSlot.Mode.INTERACTIVE);
        cs.set(stack);
    }

    /* ---------- ツール判定 / tier ---------- */
    private boolean isTool(ItemStack s) {
        return (s.getItem() instanceof ToolRod) || (s.getItem() instanceof ToolWand);
    }

    private int getToolTier() {
        ItemStack tool = rings.get(Ring.CENTER)[0].peekRealItem();
        if (tool.getItem() instanceof ToolBase tb) return Math.max(1, Math.min(3, tb.getTier()));
        return 1;
    }

    private void bindCrystalsBacking() {
        ItemStack tool = rings.get(Ring.CENTER)[0].peekRealItem();
        if (isTool(tool)) {
            backingCrystals = ToolInventory.get(tool, crystalCountForTier(getToolTier()), level.registryAccess());
        } else {
            backingCrystals = fallbackEmpty;
        }
    }

    /* ---------- クリスタル backing アクセサ ---------- */
    public int crystalBackingOf(int vi) {
        return vi % Math.max(1, backingCrystals.getSlots());
    }

    /* ---------- 同期 ---------- */
    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, BlockRegistry.JEWELRY_TABLE.get());
    }

    @Override
    public void broadcastChanges() {
        bindCrystalsBacking();

        ItemStack now = rings.get(Ring.CENTER)[0].peekRealItem();
        if (!ItemStack.matches(now, lastCenterTool)) {
            lastCenterTool = now.copy();
            refreshUiState();
            layoutSlots();
        }

        super.broadcastChanges();
    }

    /* ---------- ボタン処理 ---------- */
    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        if (slotId >= 0 && slotId < this.slots.size()) {
            Slot sl = this.slots.get(slotId);
            if (sl instanceof DialSlot rs && rs.mode() == DialSlot.Mode.BUTTON) {
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    /* ---------- シフトクリック ---------- */
    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack in = slot.getItem();
        ItemStack ret = in.copy();

        final int centerFirst   = firstIndex.get(Ring.CENTER);
        final int crystalsFirst = firstIndex.get(Ring.CRYSTALS);
        final int crystalsEnd   = crystalsFirst + effectiveCountOf(Ring.CRYSTALS);

        boolean fromCenter   = (index == centerFirst);
        boolean fromCrystals = (index >= crystalsFirst && index < crystalsEnd);
        boolean fromPlayer   = (index >= playerStartIndex && index < playerEndIndex);

        if (fromCenter || fromCrystals) {
            if (!this.moveItemStackTo(in, playerStartIndex, playerEndIndex, true)) return ItemStack.EMPTY;
        } else if (fromPlayer) {
            if (isTool(in)) {
                if (!this.moveItemStackTo(in, centerFirst, centerFirst + 1, false)) return ItemStack.EMPTY;
            } else if (in.getItem() instanceof Crystal && uiState == UiState.EDIT_CRYSTALS) {
                if (!this.moveItemStackTo(in, crystalsFirst, crystalsEnd, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (in.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return ret;
    }

    /* ---------- util ---------- */
    private static int[] polar(int cx, int cy, int radius, int i, int count, float baseDeg) {
        if (count <= 0) return new int[]{cx, cy};
        double rad = Math.toRadians(-90f + baseDeg + (360f / count) * i);
        return new int[]{
            Math.round(cx + (float)(Math.cos(rad) * radius)),
            Math.round(cy + (float)(Math.sin(rad) * radius))
        };
    }
}
