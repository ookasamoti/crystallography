package net.ookasamoti.crystallography.common.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.ookasamoti.crystallography.common.block.entity.LapidaryAnvilBlockEntity;
import net.ookasamoti.crystallography.common.util.LapidaryAnvilOperations;
import net.ookasamoti.crystallography.setup.MenuTypesRegistry;
import org.jetbrains.annotations.NotNull;

public class LapidaryAnvilMenu extends AbstractContainerMenu {

    public static final int BTN_CRACK_ORE  = 0;
    public static final int BTN_CRACK_GEMS = 1;

    private final ContainerLevelAccess access;
    private final LapidaryAnvilBlockEntity blockEntity;

    public LapidaryAnvilMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, inv.player, buf.readBlockPos());
    }

    public LapidaryAnvilMenu(int id, Inventory inv, Player player, BlockPos pos) {
        super(MenuTypesRegistry.LAPIDARY_ANVIL_MENU.get(), id);
        this.access = ContainerLevelAccess.create(player.level(), pos);
        this.blockEntity = (LapidaryAnvilBlockEntity) player.level().getBlockEntity(pos);
        if (this.blockEntity == null) throw new IllegalStateException("LapidaryAnvil BE not found at " + pos);

        final int rightX = 80;
        final int rightY = 20;

        addSlot(new SlotItemHandler(blockEntity.getItems(), LapidaryAnvilBlockEntity.SLOT_PICK,  20, rightY));
        addSlot(new SlotItemHandler(blockEntity.getItems(), LapidaryAnvilBlockEntity.SLOT_WEDGE, 20, rightY + 18));
        addSlot(new SlotItemHandler(blockEntity.getItems(), LapidaryAnvilBlockEntity.SLOT_ORE,  20, rightY + 36));

        int index = LapidaryAnvilBlockEntity.SLOT_RIGHT_START;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 5; x++) {
                addSlot(new SlotItemHandler(blockEntity.getItems(), index++,
                        rightX + x * 18, rightY + y * 18));
            }
        }

        addPlayerInventorySlots(inv);
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        final int baseY = 116;
        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 9; ++j)
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, baseY + i * 18));

        final int hotbarY = 174;
        for (int k = 0; k < 9; ++k)
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, hotbarY));
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return access.evaluate((level, pos) -> player.distanceToSqr(
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (!(player instanceof ServerPlayer sp)) return true;
        if (id == BTN_CRACK_ORE) {
            LapidaryAnvilOperations.crackOre(blockEntity, sp);
            return true;
        }
        if (id == BTN_CRACK_GEMS) {
            LapidaryAnvilOperations.crackGems(blockEntity, sp);
            return true;
        }
        return false;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return empty;
        ItemStack in = slot.getItem();
        ItemStack copy = in.copy();

        int beEnd   = LapidaryAnvilBlockEntity.SLOT_COUNT;
        int plEnd   = this.slots.size();

        if (index < beEnd) {
            if (!this.moveItemStackTo(in, beEnd, plEnd, true)) return ItemStack.EMPTY;
        } else {
            if (!this.moveItemStackTo(in,
                    LapidaryAnvilBlockEntity.SLOT_RIGHT_START,
                    LapidaryAnvilBlockEntity.SLOT_RIGHT_END + 1, false)) return ItemStack.EMPTY;
        }

        if (in.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    public boolean canCrackOre() {
        return hasItemInSlot(0) && hasItemInSlot(1) && hasItemInSlot(2);
    }

    public boolean canCrackGems() {
        for (int i = 3; i <= 17; i++) if (hasItemInSlot(i)) return true;
        return false;
    }

    private boolean hasItemInSlot(int idx) {
        if (idx < 0 || idx >= this.slots.size()) return false;
        var s = this.slots.get(idx);
        return s.hasItem();
    }
}
