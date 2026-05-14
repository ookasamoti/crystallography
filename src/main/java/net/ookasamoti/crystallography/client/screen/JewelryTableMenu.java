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
import net.ookasamoti.crystallography.setup.BlockRegistry;
import net.ookasamoti.crystallography.setup.MenuTypesRegistry;
import org.jetbrains.annotations.NotNull;

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

    /* ===== 状態 ===== */
    public enum UiState { EMPTY, EDIT_CRYSTALS, REGISTER_SETUP }

    public final JewelryTableBlockEntity blockEntity;
    private final Level level;

    private final EnumMap<Ring, DialSlot[]> rings = new EnumMap<>(Ring.class);
    private final EnumMap<Ring, Integer> firstIndex = new EnumMap<>(Ring.class);

    private UiState uiState = UiState.EMPTY;

    private int headTools      = 0;
    private int headCrystals   = 0;
    private int headRegistries = 0;
    private int activeCrystalSlots = 0;
    private ItemStack selectedCrystal = ItemStack.EMPTY;
    private ItemStack lastCenterTool = ItemStack.EMPTY;

    private int playerStartIndex = -1;
    private int playerEndIndex   = -1;

    private final ItemStackHandler backingTools      = new ItemStackHandler(countOf(Ring.TOOLS));
    private final ItemStackHandler backingRegistries = new ItemStackHandler(countOf(Ring.REGISTRIES));

    private final ItemStackHandler fallbackEmpty = new ItemStackHandler(9);
    private IItemHandler backingCrystals = fallbackEmpty;

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
                    int shift = (vi == countOf(Ring.CRYSTALS) - 1) ? size - 1 : vi;
                    int idx = (headCrystals + shift) % size;
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

    /** クライアント専用: CENTER スロットの変化を検知したときに呼ぶ */
    public void clientSyncState() {
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
        uiState = isTool(tool) ? UiState.EDIT_CRYSTALS : UiState.EMPTY;
    }

    private void layoutSlots() {
        for (Ring r : Ring.values())
            for (DialSlot s : rings.get(r)) { s.setMode(DialSlot.Mode.HIDDEN); s.setVisibleFlag(false); }

        showFirstN(Ring.CENTER, 1,
                (uiState == UiState.REGISTER_SETUP) ? DialSlot.Mode.BUTTON : DialSlot.Mode.INTERACTIVE);

        if (uiState == UiState.EMPTY) {
            selectedCrystal = ItemStack.EMPTY;
            return;
        }

        showFirstN(Ring.TOOLS, countOf(Ring.TOOLS),
                (uiState == UiState.EDIT_CRYSTALS) ? DialSlot.Mode.BUTTON : DialSlot.Mode.HIDDEN);

        showFirstN(Ring.REGISTRIES, 3, DialSlot.Mode.DECORATION);

        DialSlot.Mode crystalMode = (uiState == UiState.EDIT_CRYSTALS) ? DialSlot.Mode.INTERACTIVE : DialSlot.Mode.BUTTON;
        showFirstN(Ring.CRYSTALS, activeCrystalSlots, crystalMode);

        DialSlot[] crystalArr = rings.get(Ring.CRYSTALS);
        DialSlot lastCrystal = crystalArr[crystalArr.length - 1];
        lastCrystal.setVisibleFlag(true);
        lastCrystal.setMode(DialSlot.Mode.DECORATION);

        if (uiState != UiState.REGISTER_SETUP) {
            selectedCrystal = ItemStack.EMPTY;
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
        backingCrystals = isTool(tool)
                ? ToolInventory.get(tool, 9, level.registryAccess())
                : fallbackEmpty;
    }

    private void refreshCrystalCapacity() {
        activeCrystalSlots = switch (getToolTier()) { case 1 -> 5; case 2 -> 7; default -> 9; };
        int size = Math.max(1, backingCrystals.getSlots());
        headCrystals = mod(headCrystals, size);
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

    /* ---------- ボタン処理 ---------- */
    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        if (slotId >= 0 && slotId < this.slots.size()) {
            Slot sl = this.slots.get(slotId);
            if (sl instanceof DialSlot rs && rs.mode() == DialSlot.Mode.BUTTON) {
                if (uiState == UiState.EMPTY) return;
                Ring ring = Ring.values()[rs.ring()];

                if (uiState == UiState.EDIT_CRYSTALS && ring == Ring.TOOLS) {
                    return;
                }

                if (uiState == UiState.REGISTER_SETUP) {
                    if (ring == Ring.CRYSTALS) {
                        ItemStack stack = rs.peekRealItem();
                        if (!stack.isEmpty() && stack.getItem() instanceof Crystal) {
                            selectedCrystal = ItemStack.matches(selectedCrystal, stack)
                                    ? ItemStack.EMPTY
                                    : stack.copyWithCount(1);
                            super.broadcastChanges();
                        }
                        return;
                    }
                    if (ring == Ring.CENTER) {
                        uiState = UiState.EDIT_CRYSTALS;
                        selectedCrystal = ItemStack.EMPTY;
                        layoutSlots();
                        super.broadcastChanges();
                        return;
                    }
                }
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

        final int centerFirst  = firstIndex.get(Ring.CENTER);
        final int crystalsFirst = firstIndex.get(Ring.CRYSTALS);
        final int crystalsEnd   = crystalsFirst + activeCrystalSlots;

        boolean fromCenter   = (index == centerFirst);
        boolean fromCrystals = (index >= crystalsFirst && index < crystalsEnd);
        boolean fromPlayer   = (index >= playerStartIndex && index < playerEndIndex);

        if (fromCenter || fromCrystals) {
            if (!this.moveItemStackTo(in, playerStartIndex, playerEndIndex, true)) return ItemStack.EMPTY;
        } else if (fromPlayer) {
            if (isTool(in)) {
                if (!this.moveItemStackTo(in, centerFirst, centerFirst + 1, false)) return ItemStack.EMPTY;
            } else if (in.getItem() instanceof Crystal && uiState != UiState.EMPTY) {
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
