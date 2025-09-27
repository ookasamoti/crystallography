package net.ookasamoti.crystallography.client.screen;

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
import net.ookasamoti.crystallography.data.CrystalRollsRegistry;
import net.ookasamoti.crystallography.setup.MenuTypesRegistry;
import org.jetbrains.annotations.NotNull;

public class LapidaryAnvilMenu extends AbstractContainerMenu {

    public static final int BTN_CRACK_ORE  = 0;
    public static final int BTN_CRACK_GEMS = 1;

    private final ContainerLevelAccess access;
    private final LapidaryAnvilBlockEntity be;

    public LapidaryAnvilMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, inv.player, buf.readBlockPos());
    }

    public LapidaryAnvilMenu(int id, Inventory inv, Player player, BlockPos pos) {
        super(MenuTypesRegistry.LAPIDARY_ANVIL_MENU.get(), id);
        this.access = ContainerLevelAccess.create(player.level(), pos);
        this.be = (LapidaryAnvilBlockEntity) player.level().getBlockEntity(pos);

        if (this.be == null) throw new IllegalStateException("LapidaryAnvil BE not found at " + pos);

        // left side
        addSlot(new SlotItemHandler(be.getItems(), LapidaryAnvilBlockEntity.SLOT_ORE,   20, 20));
        addSlot(new SlotItemHandler(be.getItems(), LapidaryAnvilBlockEntity.SLOT_WEDGE, 20, 44));
        addSlot(new SlotItemHandler(be.getItems(), LapidaryAnvilBlockEntity.SLOT_PICK,  20, 68));

        // right side
        int index = LapidaryAnvilBlockEntity.SLOT_RIGHT_START;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 6; x++) {
                addSlot(new SlotItemHandler(be.getItems(), index++, 116 + x * 18, 20 + y * 18));
            }
        }

        // player inventory
        int invY = 102;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new Slot(inv, x + y * 9 + 9, 8 + x * 18, invY + y * 18));
            }
        }
        // Hot bar
        for (int x = 0; x < 9; x++) addSlot(new Slot(inv, x, 8 + x * 18, invY + 58));
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
            LapidaryAnvilOperations.crackOre(be, sp);
            return true;
        }
        if (id == BTN_CRACK_GEMS) {
            LapidaryAnvilOperations.crackGems(be, sp);
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

        int beStart = 0;
        int beEnd   = 21; // exclusive
        int plEnd   = this.slots.size();

        if (index < beEnd) {
            if (!this.moveItemStackTo(in, beEnd, plEnd, true)) return ItemStack.EMPTY;
        } else {
            if (!this.moveItemStackTo(in, LapidaryAnvilBlockEntity.SLOT_RIGHT_START,
                    LapidaryAnvilBlockEntity.SLOT_RIGHT_END + 1, false)) return ItemStack.EMPTY;
        }

        if (in.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    public boolean canCrackOre()  { return be.hasAllLeftInputs(); }
    public boolean canCrackGems() { return be.hasAnyRightItems(); }
}
