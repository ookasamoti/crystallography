package net.ookasamoti.crystallography.client.screen;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.ookasamoti.crystallography.common.blocks.JewelryTableBlockEntity;
import net.ookasamoti.crystallography.common.items.crystals.Crystal;
import net.ookasamoti.crystallography.common.items.tool.ToolBase;
import net.ookasamoti.crystallography.common.items.tool.ToolRod;
import net.ookasamoti.crystallography.common.items.tool.ToolWand;
import net.ookasamoti.crystallography.common.items.tool.ToolInventory;
import net.ookasamoti.crystallography.setup.BlockRegistry;
import net.ookasamoti.crystallography.setup.MenuTypesRegistry;
import org.jetbrains.annotations.NotNull;

public class JewelryTableMenu extends AbstractContainerMenu {

    public final JewelryTableBlockEntity blockEntity;
    private final Level level;

    // --- レイアウト定数 ---
    private static final int TOOL_SLOT_X = 80;
    private static final int TOOL_SLOT_Y = 35;

    private static final int CRYSTAL_BASE_X = 53;
    private static final int CRYSTAL_Y      = 14;
    private static final int CRYSTAL_SPACING = 18;

    // --- スロット構成 ---
    private static final int IDX_TOOL = 0;

    // 物理的な保存領域は常に 9（tier で有効枠だけ解放）
    private static final int TOTAL_CRYSTAL_SLOTS = 9;
    private static final int IDX_CRYSTAL_START = 1;                               // 1..9
    private static final int IDX_CRYSTAL_END   = IDX_CRYSTAL_START + TOTAL_CRYSTAL_SLOTS; // 10（+1終端）

    private final Slot toolSlot;
    private final Slot[] crystalSlots = new Slot[TOTAL_CRYSTAL_SLOTS];

    public JewelryTableMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(MenuTypesRegistry.JEWELRY_TABLE_MENU.get(), containerId);
        checkContainerSize(inv, 1);
        this.blockEntity = (JewelryTableBlockEntity) entity;
        this.level = inv.player.level();

        addPlayerInventorySlots(inv);

        this.toolSlot = this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 0, TOOL_SLOT_X, TOOL_SLOT_Y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof ToolRod || stack.getItem() instanceof ToolWand;
            }
            @Override
            public void setChanged() {
                super.setChanged();
                // ツールが入れ替わったら、有効枠の見え方を更新
                updateCrystalSlotsActive();
            }
        });

        // 結晶スロット（保存は常に9、tierで isActive() を切り替え）
        IItemHandlerModifiable handler = getCrystalHandler();
        for (int i = 0; i < TOTAL_CRYSTAL_SLOTS; i++) {
            final int slotIdx = i;
            int x = CRYSTAL_BASE_X + i * CRYSTAL_SPACING;
            int y = CRYSTAL_Y;

            crystalSlots[i] = this.addSlot(new SlotItemHandler(handler, i, x, y) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return stack.getItem() instanceof Crystal;
                }
                @Override
                public boolean isActive() {
                    return hasTool() && slotIdx < getActiveCrystalCount();
                }
            });
        }

        updateCrystalSlotsActive();
    }

    private boolean hasTool() {
        ItemStack tool = toolSlot.getItem();
        return tool.getItem() instanceof ToolRod || tool.getItem() instanceof ToolWand;
    }

    /** ツール（Rod/Wand）の tier を取得（未挿入や非対応なら 1 扱い） */
    private int getToolTier() {
        ItemStack tool = toolSlot.getItem();
        if (!(tool.getItem() instanceof ToolBase tb)) return 1;
        return Math.max(1, Math.min(3, tb.getTier()));
    }

    /** tier → 有効な結晶スロット数（5/7/9） */
    private int getActiveCrystalCount() {
        return switch (getToolTier()) {
            case 1 -> 5;
            case 2 -> 7;
            default -> 9;
        };
    }

    /** 結晶用のハンドラを取得（ツールがあればツール保存、無ければ一時の空ハンドラ） */
    private IItemHandlerModifiable getCrystalHandler() {
        ItemStack tool = toolSlot == null ? ItemStack.EMPTY : toolSlot.getItem();
        if (tool.isEmpty() || !(tool.getItem() instanceof ToolBase)) {
            return new ItemStackHandler(TOTAL_CRYSTAL_SLOTS);
        }
        return ToolInventory.get(tool, TOTAL_CRYSTAL_SLOTS, level.registryAccess());
    }

    /** isActive() 再評価のための通知。必要に応じて再構築もここで可能。 */
    private void updateCrystalSlotsActive() {
        this.broadcastChanges();
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack ret = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ret;

        ItemStack in = slot.getItem();
        ret = in.copy();

        int activeEnd = IDX_CRYSTAL_START + getActiveCrystalCount();

        if (index == IDX_TOOL) {
            if (!this.moveItemStackTo(in, IDX_CRYSTAL_END, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (index >= IDX_CRYSTAL_START && index < IDX_CRYSTAL_END) {
            if (!this.moveItemStackTo(in, IDX_CRYSTAL_END, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (in.getItem() instanceof ToolRod || in.getItem() instanceof ToolWand) {
                if (!this.moveItemStackTo(in, IDX_TOOL, IDX_TOOL + 1, false)) return ItemStack.EMPTY;
            } else if (in.getItem() instanceof Crystal) {
                if (!this.moveItemStackTo(in, IDX_CRYSTAL_START, activeEnd, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (in.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return ret;
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 9; ++j)
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));

        for (int k = 0; k < 9; ++k)
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, BlockRegistry.JEWELRY_TABLE.get());
    }
}
