package net.ookasamoti.crystallography.client.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ookasamoti.crystallography.client.gui.dial.DialSlot;
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
            Ring.CRYSTALS,   new RingDef(20, 64, 78f),
            Ring.REGISTRIES, new RingDef(26, 96, 78f)
    ));

    public static int   countOf (Ring r){ return Objects.requireNonNull(DEF.get(r)).count(); }
    public static int   radiusOf(Ring r){ return Objects.requireNonNull(DEF.get(r)).radius(); }
    public static float baseDegOf(Ring r){ return Objects.requireNonNull(DEF.get(r)).baseDeg(); }

    /* ===== フォームマッピング ===== */
    private static final ToolForm[] ROD_FORMS  = {
            ToolForm.PICKAXE, ToolForm.SHOVEL, ToolForm.HOE,
            ToolForm.SWORD,   ToolForm.AXE,    ToolForm.SPEAR
    };
    private static final ToolForm[] WAND_FORMS = {
            ToolForm.BOW, ToolForm.CROSSBOW, ToolForm.KNIFE,
            ToolForm.WAND, ToolForm.FISHING_ROD, ToolForm.SHIELD
    };

    /* ===== 状態 ===== */
    public enum UiState { EMPTY, EDIT_CRYSTALS, FORM_SELECTED }

    public final JewelryTableBlockEntity blockEntity;
    private final Level level;

    private final EnumMap<Ring, DialSlot[]> rings = new EnumMap<>(Ring.class);
    private final EnumMap<Ring, Integer> firstIndex = new EnumMap<>(Ring.class);

    private UiState uiState = UiState.EMPTY;

    private int headTools      = 0;
    private int headCrystals   = 0;
    private int headRegistries = 0;
    private int activeCrystalSlots = 0;
    private ItemStack lastCenterTool = ItemStack.EMPTY;

    private int playerStartIndex = -1;
    private int playerEndIndex   = -1;

    private final ItemStackHandler backingTools      = new ItemStackHandler(countOf(Ring.TOOLS));
    private final ItemStackHandler backingRegistries = new ItemStackHandler(countOf(Ring.REGISTRIES));

    private final ItemStackHandler fallbackEmpty = new ItemStackHandler(9);
    private IItemHandler backingCrystals = fallbackEmpty;

    /** クライアント・サーバー双方で管理する保留中フォームインデックス（-1 = 未選択） */
    public int pendingFormIndex = -1;
    /** クライアント・サーバー双方で管理する選択済み結晶のbacking index一覧 */
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
        refreshCrystalCapacity();
        refreshUiState();
        layoutSlots();
    }

    /* ---------- リング生成 ---------- */
    private void initRings() {
        addRing(Ring.CENTER,
                () -> blockEntity.getItemHandler(),
                vi -> 0,
                false);

        addRing(Ring.TOOLS,
                () -> backingTools,
                vi -> {
                    int size = Math.max(1, backingTools.getSlots());
                    int idx = (headTools + vi) % size;
                    if (idx < 0) idx += size;
                    return idx;
                },
                false);

        addRing(Ring.CRYSTALS,
                () -> backingCrystals,
                vi -> {
                    int size = Math.max(1, backingCrystals.getSlots());
                    int idx = (headCrystals + vi) % size;
                    if (idx < 0) idx += size;
                    return idx;
                },
                true);

        addRing(Ring.REGISTRIES,
                () -> backingRegistries,
                vi -> {
                    int size = Math.max(1, backingRegistries.getSlots());
                    int idx = (headRegistries + vi) % size;
                    if (idx < 0) idx += size;
                    return idx;
                },
                false);
    }

    private void addRing(Ring ring,
                            Supplier<IItemHandler> supplier,
                            IntUnaryOperator mapper,
                            boolean crystals) {
        RingDef d = DEF.get(ring);
        int first = this.slots.size();
        DialSlot[] arr = new DialSlot[d.count()];

        for (int v = 0; v < d.count(); v++) {
            int[] xy = polar(TOOL_SLOT_X, TOOL_SLOT_Y, d.radius(), v, d.count(), d.baseDeg());

            DialSlot s = new DialSlot(
                    supplier, mapper,
                    ring.ordinal(), v,
                    xy[0] - 8, xy[1] - 8,
                    DialSlot.Mode.HIDDEN, 16
            ) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    if (ring == Ring.CENTER)  return super.mayPlace(stack) && isTool(stack);
                    if (crystals)             return super.mayPlace(stack) && (stack.getItem() instanceof Crystal);
                    return super.mayPlace(stack);
                }
            };

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
    public int getActiveCrystalSlots() { return activeCrystalSlots; }
    public int getHead(Ring ring) {
        return switch (ring) {
            case TOOLS      -> headTools;
            case CRYSTALS   -> headCrystals;
            case REGISTRIES -> headRegistries;
            default         -> 0;
        };
    }
    public UiState getUiState() { return uiState; }
    public IItemHandler getBackingCrystals() { return backingCrystals; }

    /** クライアント専用: CENTER スロットの変化を検知したときに呼ぶ */
    public void clientSyncState() {
        pendingFormIndex = -1;
        pendingCrystals.clear();
        bindCrystalsBacking();
        refreshCrystalCapacity();
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
        uiState = isTool(tool) ? UiState.EDIT_CRYSTALS : UiState.EMPTY;
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
                showFirstN(Ring.REGISTRIES, 6, DialSlot.Mode.BUTTON);
                showFirstN(Ring.CRYSTALS, activeCrystalSlots, DialSlot.Mode.INTERACTIVE);
            }
            case FORM_SELECTED -> {
                showFirstN(Ring.CENTER, 1, DialSlot.Mode.BUTTON);
                showFirstN(Ring.REGISTRIES, 6, DialSlot.Mode.BUTTON);
                showFirstN(Ring.CRYSTALS, activeCrystalSlots, DialSlot.Mode.BUTTON);
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

    /**
     * フォームを選択する。formIndex=-1 で選択解除。
     * クライアント側でも直接呼んでローカル状態を即時更新する。
     */
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

    /** 結晶選択トグル。クライアント側でも直接呼んで即時更新する。 */
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
        broadcastChanges();
    }

    public void serverToggleCrystal(int crystalBacking) {
        applyToggleCrystal(crystalBacking);
        broadcastChanges();
    }

    /** 結晶3つ選択後、指定フォームでツールに登録する。 */
    public void serverRegister(int formIndex) {
        if (pendingCrystals.size() != ToolLoadout.CRYSTAL_SLOTS) return;

        ItemStack toolStack = rings.get(Ring.CENTER)[0].peekRealItem();
        if (!isTool(toolStack)) return;

        boolean isWand = toolStack.getItem() instanceof ToolWand;
        ToolForm[] forms = isWand ? WAND_FORMS : ROD_FORMS;
        if (formIndex < 0 || formIndex >= forms.length) return;
        ToolForm form = forms[formIndex];

        int tier = getToolTier();
        int[] crystalIndices = pendingCrystals.stream().mapToInt(Integer::intValue).toArray();

        ToolStats stats = ToolBase.buildStats(form, tier, crystalIndices, backingCrystals);
        ToolLoadout loadout = new ToolLoadout(form, crystalIndices, stats);

        ItemStack modified = toolStack.copy();
        int newIndex = ToolBase.getLoadout(modified).entries().size();
        if (!ToolBase.addLoadout(modified, loadout)) return;
        ToolBase.setActiveIndex(modified, newIndex);
        ToolBase.applyComputedStats(modified);

        writeToCenter(modified);
        applySelectForm(-1);
        broadcastChanges();
    }

    /** CENTER スロットのモードを一時的に INTERACTIVE にしてアイテムを書き込む。 */
    private void writeToCenter(ItemStack stack) {
        DialSlot cs = rings.get(Ring.CENTER)[0];
        cs.setMode(DialSlot.Mode.INTERACTIVE);
        cs.set(stack);
        // layoutSlots() 呼び出し後に正しいモードに戻る
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
            int slots = ToolBase.crystalSlotCount(getToolTier());
            backingCrystals = ToolInventory.get(tool, slots, level.registryAccess());
        } else {
            backingCrystals = fallbackEmpty;
        }
    }

    private void refreshCrystalCapacity() {
        activeCrystalSlots = ToolBase.crystalSlotCount(getToolTier());
        int size = Math.max(1, backingCrystals.getSlots());
        headCrystals = mod(headCrystals, size);
    }

    /* ---------- クリスタル backing アクセサ ---------- */
    public int crystalBackingOf(int vi) {
        int size = Math.max(1, backingCrystals.getSlots());
        return mod(headCrystals + vi, size);
    }

    /* ---------- リング回転 ---------- */
    public void rotateToolsView(int steps) {
        if (steps == 0) return;
        headTools = mod(headTools + steps, Math.max(1, backingTools.getSlots()));
        for (var s : rings.get(Ring.TOOLS)) s.setChanged();
    }

    public void rotateToolsViewServer(int steps) {
        if (steps == 0) return;
        headTools = mod(headTools + steps, Math.max(1, backingTools.getSlots()));
        for (var s : rings.get(Ring.TOOLS)) s.setChanged();
        broadcastChanges();
    }

    public void rotateCrystalsView(int steps) {
        if (steps == 0) return;
        headCrystals = mod(headCrystals + steps, Math.max(1, backingCrystals.getSlots()));
        for (var s : rings.get(Ring.CRYSTALS)) s.setChanged();
    }

    public void rotateCrystalsViewServer(int steps) {
        if (steps == 0) return;
        headCrystals = mod(headCrystals + steps, Math.max(1, backingCrystals.getSlots()));
        for (var s : rings.get(Ring.CRYSTALS)) s.setChanged();
        broadcastChanges();
    }

    public void rotateRegistriesView(int steps) {
        if (steps == 0) return;
        headRegistries = mod(headRegistries + steps, Math.max(1, backingRegistries.getSlots()));
        for (var s : rings.get(Ring.REGISTRIES)) s.setChanged();
    }

    public void rotateRegistriesViewServer(int steps) {
        if (steps == 0) return;
        headRegistries = mod(headRegistries + steps, Math.max(1, backingRegistries.getSlots()));
        for (var s : rings.get(Ring.REGISTRIES)) s.setChanged();
        broadcastChanges();
    }

    /* ---------- 同期 ---------- */
    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, BlockRegistry.JEWELRY_TABLE.get());
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        bindCrystalsBacking();
        refreshCrystalCapacity();

        ItemStack now = rings.get(Ring.CENTER)[0].peekRealItem();
        if (!ItemStack.matches(now, lastCenterTool)) {
            lastCenterTool = now.copy();
            refreshUiState();
            layoutSlots();
            super.broadcastChanges();
        }
    }

    /* ---------- ボタン処理（BUTTON スロットは C2S パケット経由、clicked は通常スロットのみ） ---------- */
    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        if (slotId >= 0 && slotId < this.slots.size()) {
            Slot sl = this.slots.get(slotId);
            if (sl instanceof DialSlot rs && rs.mode() == DialSlot.Mode.BUTTON) {
                // BUTTONスロットへのクリックは C2S パケットで処理済みのため何もしない
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
        final int crystalsEnd   = crystalsFirst + activeCrystalSlots;

        boolean fromCenter   = (index == centerFirst);
        boolean fromCrystals = (index >= crystalsFirst && index < crystalsEnd);
        boolean fromPlayer   = (index >= playerStartIndex && index < playerEndIndex);

        // BUTTON モードのスロットは getItem() が EMPTY を返すため、
        // fromCenter/fromCrystals は FORM_SELECTED 中は自動的に false になる
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
    private static int mod(int v, int m) { int r = v % m; return r < 0 ? r + m : r; }

    private static int[] polar(int cx, int cy, int radius, int i, int count, float baseDeg) {
        if (count <= 0) return new int[]{cx, cy};
        double rad = Math.toRadians(-90f + baseDeg + (360f / count) * i);
        return new int[]{
            Math.round(cx + (float)(Math.cos(rad) * radius)),
            Math.round(cy + (float)(Math.sin(rad) * radius))
        };
    }
}
