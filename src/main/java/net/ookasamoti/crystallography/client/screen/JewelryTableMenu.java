package net.ookasamoti.crystallography.client.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.ookasamoti.crystallography.common.block.entity.JewelryTableBlockEntity;
import net.ookasamoti.crystallography.common.item.crystal.Crystal;
import net.ookasamoti.crystallography.common.item.tool.ToolBase;
import net.ookasamoti.crystallography.common.item.tool.ToolInventory;
import net.ookasamoti.crystallography.common.item.tool.ToolRod;
import net.ookasamoti.crystallography.common.item.tool.ToolWand;
import net.ookasamoti.crystallography.setup.BlockRegistry;
import net.ookasamoti.crystallography.setup.MenuTypesRegistry;
import org.jetbrains.annotations.NotNull;

public class JewelryTableMenu extends AbstractContainerMenu {

    private static final int TOOL_SLOT_X = 80, TOOL_SLOT_Y = 35;
    private static final int CRYSTAL_BASE_X = 53, CRYSTAL_Y = 14, CRYSTAL_SPACING = 18;
    private static final int TOTAL_CRYSTAL_SLOTS = 9;

    private static final int IDX_TOOL = 0;
    private static final int IDX_CRYSTAL_START = 1;
    private static final int IDX_CRYSTAL_END   = IDX_CRYSTAL_START + TOTAL_CRYSTAL_SLOTS; // [1,10)

    public final JewelryTableBlockEntity blockEntity;
    private final Level level;

    private final Slot toolSlot;
    private final Slot[] crystalSlots = new Slot[TOTAL_CRYSTAL_SLOTS];

    private final DelegatingHandler crystalHandler;

    private ItemStack lastToolRef = ItemStack.EMPTY;

    private boolean rebinding = false;

    public JewelryTableMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (JewelryTableBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(0));
    }

    public JewelryTableMenu(int containerId, Inventory inv, JewelryTableBlockEntity be, ContainerData data) {
        super(MenuTypesRegistry.JEWELRY_TABLE_MENU.get(), containerId);
        this.blockEntity = be;
        this.level = inv.player.level();

        this.toolSlot = this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 0, TOOL_SLOT_X, TOOL_SLOT_Y) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return isTool(stack); }
            @Override public void setChanged() {
                super.setChanged();
                rebindCrystalHandler();
            }
        });

        this.crystalHandler = new DelegatingHandler(new ItemStackHandler(TOTAL_CRYSTAL_SLOTS));
        for (int i = 0; i < TOTAL_CRYSTAL_SLOTS; i++) {
            final int slotIdx = i; // capture
            int x = CRYSTAL_BASE_X + i * CRYSTAL_SPACING;
            int y = CRYSTAL_Y;

            this.crystalSlots[i] = this.addSlot(new SlotItemHandler(this.crystalHandler, i, x, y) {
                @Override public boolean mayPlace(@NotNull ItemStack stack) {
                    return hasTool() && stack.getItem() instanceof Crystal;
                }
                @Override public boolean isActive() {
                    return hasTool() && slotIdx < getActiveCrystalCount();
                }
            });
        }

        addPlayerInventorySlots(inv);

        rebindCrystalHandler();
        super.broadcastChanges();
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 9; ++j)
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        for (int k = 0; k < 9; ++k)
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
    }

    private boolean isTool(ItemStack s) {
        return (s.getItem() instanceof ToolRod) || (s.getItem() instanceof ToolWand);
    }
    private boolean hasTool() { return isTool(toolSlot.getItem()); }

    private int getToolTier() {
        ItemStack tool = toolSlot.getItem();
        if (tool.getItem() instanceof ToolBase tb) {
            return Math.max(1, Math.min(3, tb.getTier()));
        }
        return 1;
    }

    private int getActiveCrystalCount() {
        return switch (getToolTier()) {
            case 1 -> 5;
            case 2 -> 7;
            default -> 9;
        };
    }

    private void rebindCrystalHandler() {
        if (rebinding) return;
        rebinding = true;
        try {
            ItemStack cur = toolSlot.getItem();
            if (ItemStack.isSameItemSameComponents(cur, lastToolRef)) {
                return;
            }
            lastToolRef = cur.copy();

            IItemHandlerModifiable next = hasTool()
                    ? ToolInventory.get(cur, TOTAL_CRYSTAL_SLOTS, level.registryAccess())
                    : new ItemStackHandler(TOTAL_CRYSTAL_SLOTS);

            this.crystalHandler.setDelegate(next);

            for (Slot s : crystalSlots) s.setChanged();

            super.broadcastChanges();
        } finally {
            rebinding = false;
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack ret = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ret;

        ItemStack in = slot.getItem();
        ret = in.copy();

        final int plStart = IDX_CRYSTAL_END;
        final int plEnd   = this.slots.size();

        int activeEnd = IDX_CRYSTAL_START + getActiveCrystalCount();

        if (index == IDX_TOOL) {
            if (!this.moveItemStackTo(in, plStart, plEnd, true)) return ItemStack.EMPTY;
        } else if (index >= IDX_CRYSTAL_START && index < IDX_CRYSTAL_END) {
            if (!this.moveItemStackTo(in, plStart, plEnd, true)) return ItemStack.EMPTY;
        } else {
            if (isTool(in)) {
                if (!this.moveItemStackTo(in, IDX_TOOL, IDX_TOOL + 1, false)) return ItemStack.EMPTY;
            } else if (in.getItem() instanceof Crystal) {
                if (!hasTool()) return ItemStack.EMPTY;
                if (!this.moveItemStackTo(in, IDX_CRYSTAL_START, activeEnd, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (in.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return ret;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        ItemStack tool = toolSlot.getItem();
        if (!tool.isEmpty()) {
            toolSlot.set(ItemStack.EMPTY);
            if (!player.addItem(tool)) player.drop(tool, false);
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, BlockRegistry.JEWELRY_TABLE.get());
    }

    private static final class DelegatingHandler implements IItemHandlerModifiable {
        private IItemHandlerModifiable delegate;
        DelegatingHandler(IItemHandlerModifiable initial) { this.delegate = initial; }
        void setDelegate(IItemHandlerModifiable next)     { this.delegate = next;   }

        @Override public int getSlots() { return delegate.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return delegate.getStackInSlot(slot); }
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { delegate.setStackInSlot(slot, stack); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return delegate.insertItem(slot, stack, simulate); }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return delegate.extractItem(slot, amount, simulate); }
        @Override public int getSlotLimit(int slot) { return delegate.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return delegate.isItemValid(slot, stack); }
    }
}
