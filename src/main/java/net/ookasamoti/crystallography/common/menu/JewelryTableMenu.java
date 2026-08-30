package net.ookasamoti.crystallography.common.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.ookasamoti.crystallography.common.block.entity.JewelryTableBlockEntity;
import net.ookasamoti.crystallography.common.item.crystal.Crystal;
import net.ookasamoti.crystallography.common.item.tool.ToolBase;
import net.ookasamoti.crystallography.common.item.tool.ToolInventory;
import net.ookasamoti.crystallography.common.item.tool.ToolRod;
import net.ookasamoti.crystallography.common.item.tool.ToolWand;
import net.ookasamoti.crystallography.common.item.tool.component.ToolForm;
import net.ookasamoti.crystallography.common.item.tool.component.ToolLoadout;
import net.ookasamoti.crystallography.common.item.tool.component.ToolStats;
import net.ookasamoti.crystallography.data.CrystalStatsRegistry;
import net.ookasamoti.crystallography.setup.BlockRegistry;
import net.ookasamoti.crystallography.setup.DataComponentsRegistry;
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
            Ring.CRYSTALS,   new RingDef(18, 64, 78f),
            Ring.REGISTRIES, new RingDef(24, 96, 78f)
    ));

    public static int   countOf (Ring r){ return Objects.requireNonNull(DEF.get(r)).count(); }
    public static int   radiusOf(Ring r){ return Objects.requireNonNull(DEF.get(r)).radius(); }
    public static float baseDegOf(Ring r){ return Objects.requireNonNull(DEF.get(r)).baseDeg(); }

    /**
     * リングが表示するスロット位置数(物理数, tier 非依存・一定間隔)。
     *
     * <p>CRYSTALS=18(20°)、REGISTRIES=24(15°) を全 tier 共通で円周いっぱいに配置し、
     * 無限回転させる。実データ数 D(結晶 6/9/12、登録 8/12/16)より位置数が多い分は
     * {@code vi % D} で円周に繰り返しマップして埋める(tier1/tier2 はシームレス、
     * tier3 のみ継ぎ目が 1 箇所できる)。
     */
    public int effectiveCountOf(Ring r) {
        return countOf(r);
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
    private static final ItemStacksResourceHandler EMPTY_HANDLER = new ItemStacksResourceHandler(1);

    private final ItemStacksResourceHandler fallbackEmpty = new ItemStacksResourceHandler(9);
    private ItemStacksResourceHandler backingCrystals = fallbackEmpty;

    /** 保留中フォームインデックス（-1 = 未選択） */
    public int pendingFormIndex = -1;
    /** 選択済み結晶の backing index 一覧 */
    public final ArrayList<Integer> pendingCrystals = new ArrayList<>();
    /** 編集中の REGISTRIES 登録枠（-1 = 新規登録／未編集）。REGISTRIES 管理機能で使用。 */
    public int editingRegistrySlot = -1;

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
                // 実データ数 D(=crystalSlotCount)で剰余を取る。backingCrystals.size() は
                // 旧 NBT を deserialize すると保存サイズ(18/24等)に膨らむことがあり、
                // それに依存するとサイクル周期がずれるため tier の D を直接使う。
                vi -> vi % Math.max(1, ToolBase.crystalSlotCount(getToolTier())),
                // Crystal クラスのアイテムに限らず、CrystalStatsRegistry に登録されていれば置ける
                // （minecraft:raw_iron 等、原石バッテリー用のバニラアイテムを含む）。
                stack -> stack.getItem() instanceof Crystal || CrystalStatsRegistry.get(stack).isPresent());

        for (DialSlot s : rings.get(Ring.CRYSTALS)) {
            s.setRemovalPenalty(this::shouldPenalizeOreRemoval);
        }

        addRing(Ring.REGISTRIES,
                () -> EMPTY_HANDLER,
                vi -> 0,
                stack -> false);
    }

    /**
     * 原石バッテリー（[[ToolBase#ORE_CATEGORY]]）専用の取り外しペナルティ判定。
     * backingIdx を参照するいずれかのロードアウトが原石バッテリーモードかつ耐久値減少中
     * （currentDurability < stats.durability()）であれば true を返す（DialSlot.remove から呼ばれる）。
     */
    private boolean shouldPenalizeOreRemoval(int backingIdx, ItemStack removedStack) {
        var rangeOpt = CrystalStatsRegistry.get(removedStack);
        if (rangeOpt.isEmpty() || !rangeOpt.get().categories().contains(ToolBase.ORE_CATEGORY)) return false;

        ItemStack tool = rings.get(Ring.CENTER)[0].peekRealItem();
        if (!isTool(tool)) return false;

        for (var lo : ToolBase.getLoadout(tool).entries()) {
            for (int ci : lo.crystalIndices()) {
                if (ci == backingIdx && lo.currentDurability() < lo.stats().durability()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addRing(Ring ring,
                         Supplier<? extends ItemStacksResourceHandler> supplier,
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
    public ItemStacksResourceHandler getBackingCrystals() { return backingCrystals; }

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
            pendingFormIndex = baseFormIndexOf(draft.form(), forms);
            for (int ci : draft.crystalIndices()) {
                if (ci >= 0) pendingCrystals.add(ci);
            }
            uiState = pendingFormIndex >= 0 ? UiState.FORM_SELECTED : UiState.EDIT_CRYSTALS;
        } else {
            uiState = UiState.EDIT_CRYSTALS;
            editingRegistrySlot = -1;
        }
    }

    /**
     * form に対応する TOOLS ボタンの index を返す。TRIDENT/MACE は昇格後の form なので、
     * TOOLS ボタン一覧に無い（元の SPEAR/PICKAXE ボタンとして扱う）。見つからなければ -1。
     */
    private static int baseFormIndexOf(ToolForm form, ToolForm[] forms) {
        ToolForm base = switch (form) {
            case TRIDENT -> ToolForm.SPEAR;
            case MACE    -> ToolForm.PICKAXE;
            default      -> form;
        };
        for (int i = 0; i < forms.length; i++) if (forms[i] == base) return i;
        return -1;
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
            editingRegistrySlot = -1;
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

    /**
     * 初期状態（pendingFormIndex<0）で登録済み REGISTRIES 枠を選択したときの編集開始処理。
     * 既存ロードアウトの form/結晶を pending 状態へ読み込み、FORM_SELECTED 相当に遷移する。
     * 対象枠が空なら何もしない。
     */
    public void applyStartEditRegistry(int slotIndex) {
        ItemStack tool = rings.get(Ring.CENTER)[0].peekRealItem();
        if (!isTool(tool)) return;
        var loOpt = ToolBase.getLoadout(tool).getAtSlot(slotIndex);
        if (loOpt.isEmpty()) return;
        ToolLoadout lo = loOpt.get();

        boolean isWand = tool.getItem() instanceof ToolWand;
        ToolForm[] forms = isWand ? WAND_FORMS : ROD_FORMS;
        int formIdx = baseFormIndexOf(lo.form(), forms);
        if (formIdx < 0) return;

        pendingFormIndex = formIdx;
        pendingCrystals.clear();
        for (int ci : lo.crystalIndices()) if (ci >= 0) pendingCrystals.add(ci);
        editingRegistrySlot = slotIndex;
        uiState = UiState.FORM_SELECTED;
        layoutSlots();
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
        // 結晶構成からトライデント/メイスへの form 昇格を判定。
        form = ToolBase.upgradeFormForCrystals(form, crystalIndices, backingCrystals);
        ToolStats stats = ToolBase.buildStats(form, tier, crystalIndices, backingCrystals);
        ToolLoadout loadout = ToolLoadout.fresh(slotIndex, form, crystalIndices, stats);

        ItemStack modified = toolStack.copy();
        ToolBase.clearDraftLoadout(modified); // 仮登録を削除してから本登録
        if (!ToolBase.setLoadout(modified, loadout)) return;
        ToolBase.setActiveIndex(modified, slotIndex);
        ToolBase.applyComputedStats(modified);

        writeToCenter(modified);
        applySelectForm(-1);
        broadcastChanges();
    }

    public void serverStartEditRegistry(int slotIndex) {
        applyStartEditRegistry(slotIndex);
        if (pendingFormIndex >= 0) writeDraft();
        broadcastChanges();
    }

    /** Esc キャンセル：仮登録を破棄し初期状態に戻す（登録内容には触れない）。 */
    public void serverCancelEdit() {
        applySelectForm(-1);
        clearDraftFromItem();
        broadcastChanges();
    }

    /** Space キー：編集中の REGISTRIES 枠の登録を削除する。 */
    public void serverDeleteRegistry() {
        if (editingRegistrySlot < 0) return;
        ItemStack tool = rings.get(Ring.CENTER)[0].peekRealItem();
        if (!isTool(tool)) return;

        ItemStack modified = tool.copy();
        ToolBase.removeLoadout(modified, editingRegistrySlot);
        ToolBase.clearDraftLoadout(modified);
        writeToCenter(modified);
        applySelectForm(-1);
        broadcastChanges();
    }

    /** 別の REGISTRIES 枠を選択：編集中の枠と登録内容をスワップし初期状態化（並べ替え）。 */
    public void serverSwapRegistry(int otherSlotIndex) {
        if (editingRegistrySlot < 0) return;
        ItemStack tool = rings.get(Ring.CENTER)[0].peekRealItem();
        if (!isTool(tool)) return;

        if (otherSlotIndex != editingRegistrySlot) {
            int tier = getToolTier();
            if (otherSlotIndex < 0 || otherSlotIndex >= ToolBase.maxLoadouts(tier)) return;
            ItemStack modified = tool.copy();
            ToolBase.swapLoadouts(modified, editingRegistrySlot, otherSlotIndex);
            ToolBase.clearDraftLoadout(modified);
            writeToCenter(modified);
        } else {
            clearDraftFromItem();
        }
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

        // draft でも form 昇格をプレビュー表示（モデル/能力プレビューが正しく反映される）。
        form = ToolBase.upgradeFormForCrystals(form, crystalIndices, backingCrystals);
        ToolStats stats = ToolBase.buildStats(form, tier, crystalIndices, backingCrystals);
        ToolLoadout draft = ToolLoadout.fresh(ToolLoadout.DRAFT_SLOT, form, crystalIndices, stats);

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
            // peekRealItem() returns a COPY (the resource API exposes copies, not the live backing
            // stack), so edits to the crystal inventory must be written back into the center slot or
            // they are lost. The onSaved callback pushes the updated tool back into slot 0.
            // 結晶構成の変化に追従して、各ロードアウトの耐久値を「削れた分」継承で再評価する。
            backingCrystals = ToolInventory.get(tool, ToolBase.crystalSlotCount(getToolTier()), level.registryAccess(),
                    updated -> {
                        ToolBase.reconcileLoadouts(updated, getToolTier(), backingCrystals);
                        blockEntity.getItemHandler().set(0, ItemResource.of(updated), Math.max(1, updated.getCount()));
                    });
            // CrystalStats 未解決の結晶（例: minecraft:raw_iron 等、Crystal クラスでない、あるいは
            // onCraftedPostProcess/クラック経由でない手段で入手したもの）をここで解決しておく。
            // buildStats/reconcileLoadouts は CrystalStats が無いと hardness 等を 0 扱いしてしまう。
            resolveUnresolvedCrystalStats();
            // 初回バインド時にも一度 reconcile を実行する。これがないと、以前のビルドや
            // 旧フォーミュラで登録された stale な ToolStats が触らない限り更新されない。
            // tool は peekRealItem() のコピーなので直接書き換え可。差分があれば BE に書き戻す。
            ItemStack before = tool.copy();
            ToolBase.reconcileLoadouts(tool, getToolTier(), backingCrystals);
            if (!ItemStack.matches(before, tool)) {
                blockEntity.getItemHandler().set(0, ItemResource.of(tool), Math.max(1, tool.getCount()));
            }
        } else {
            backingCrystals = fallbackEmpty;
        }
    }

    /** backingCrystals 内の各結晶に CrystalStats が未解決なら解決して書き戻す。 */
    private void resolveUnresolvedCrystalStats() {
        var statsType = DataComponentsRegistry.CRYSTAL_STATS.get();
        int count = ToolBase.crystalSlotCount(getToolTier());
        for (int i = 0; i < count && i < backingCrystals.size(); i++) {
            ItemStack crystal = backingCrystals.getResource(i).toStack(backingCrystals.getAmountAsInt(i));
            if (crystal.isEmpty() || crystal.get(statsType) != null) continue;
            Crystal.resolveStats(crystal, level);
            if (crystal.get(statsType) != null) {
                backingCrystals.set(i, ItemResource.of(crystal), crystal.getCount());
            }
        }
    }

    /* ---------- クリスタル backing アクセサ ---------- */
    public int crystalBackingOf(int vi) {
        return vi % Math.max(1, ToolBase.crystalSlotCount(getToolTier()));
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
    public void clicked(int slotId, int button, @NotNull ContainerInput clickType, @NotNull Player player) {
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
