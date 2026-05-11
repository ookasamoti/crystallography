package net.ookasamoti.crystallography.client.screen;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.ookasamoti.crystallography.common.block.entity.JewelryTableBlockEntity;
import net.ookasamoti.crystallography.common.item.tool.ToolBase;
import net.ookasamoti.crystallography.setup.MenuTypesRegistry;
import org.jetbrains.annotations.NotNull;

public class JewelryTableMenu extends AbstractContainerMenu {

    // Button IDs
    public static final int BTN_SELECT_FORM_BASE    = 0;   // 0–15: ToolForm ordinal
    public static final int BTN_ASSIGN_CRYSTAL_BASE = 16;  // 16–21: visual crystal index 0–5
    public static final int BTN_REGISTER            = 22;
    public static final int BTN_CLEAR               = 23;

    // Slot indices
    public static final int SLOT_TOOLBASE     = 0;
    public static final int SLOT_CRYSTAL_BASE = 1; // 1–6
    public static final int SLOT_PLAYER_BASE  = 7; // 7–42

    // imageHeight = 166  →  dial center (88, 58),  crystal ring r=48
    // Angles: −90°, −30°, 30°, 90°, 150°, 210°  (slots at r=48; only top arc visible)
    // slot top-left = center - 8
    static final int[] CRYSTAL_SLOT_X = {  80, 122, 122,  80, 38, 38 };
    static final int[] CRYSTAL_SLOT_Y = {   2,  26,  74,  98, 74, 26 };

    // Dial clip boundary: slots with center y >= DIAL_CLIP_H are inactive (scissored out)
    static final int DIAL_CLIP_H = 72;

    // ToolBase center slot: center=(88,58) → top-left=(80,50)
    static final int TOOLBASE_SLOT_X = 80;
    static final int TOOLBASE_SLOT_Y = 50;

    // Player inventory: vanilla formula for imageHeight=166 (same as Dispenser)
    //   row0 = 166-82=84,  row1=102,  row2=120,  hotbar = 166-24=142
    static final int INV_ROW_Y  = 84;
    static final int HOTBAR_Y   = 142;

    private final JewelryTableBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    final ContainerData data;

    public JewelryTableMenu(int id, Inventory inv, JewelryTableBlockEntity be, ContainerData data) {
        super(MenuTypesRegistry.JEWELRY_TABLE_MENU.get(), id);
        this.blockEntity = be;
        this.data = data;
        this.access = ContainerLevelAccess.create(inv.player.level(), be.getBlockPos());

        // Slot 0: ToolBase center
        addSlot(new SlotItemHandler(be.getItemHandler(), JewelryTableBlockEntity.SLOT_TOOLBASE,
                TOOLBASE_SLOT_X, TOOLBASE_SLOT_Y) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof ToolBase;
            }
        });

        // Slots 1–6: Crystal staging ring
        for (int i = 0; i < 6; i++) {
            final int vi = i;
            addSlot(new SlotItemHandler(
                    be.getItemHandler(),
                    JewelryTableBlockEntity.SLOT_CRYSTAL_START + i,
                    CRYSTAL_SLOT_X[i], CRYSTAL_SLOT_Y[i]) {

                @Override public boolean isActive() {
                    return CRYSTAL_SLOT_Y[vi] + 8 < DIAL_CLIP_H;
                }
                @Override public boolean mayPlace(@NotNull ItemStack stack) {
                    return isActive() && ToolBase.isCrystal(stack) && formNotSelected();
                }
                @Override public boolean mayPickup(@NotNull Player player) {
                    return isActive() && formNotSelected();
                }
                @Override public boolean isHighlightable() {
                    return isActive() && formNotSelected();
                }

                private boolean formNotSelected() {
                    return JewelryTableMenu.this.data
                            .get(JewelryTableBlockEntity.DATA_SELECTED_FORM) < 0;
                }
            });
        }

        // Slots 7–42: Player inventory (vanilla standard positions)
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, INV_ROW_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, HOTBAR_Y));

        addDataSlots(data);
    }

    // ── server-side button handling ─────────────────────────────────────────

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (!(player instanceof ServerPlayer)) return false;

        if (id >= BTN_SELECT_FORM_BASE && id < BTN_SELECT_FORM_BASE + 16) {
            blockEntity.selectForm(id - BTN_SELECT_FORM_BASE);
            return true;
        }
        if (id >= BTN_ASSIGN_CRYSTAL_BASE && id < BTN_ASSIGN_CRYSTAL_BASE + 6) {
            blockEntity.assignCrystal(id - BTN_ASSIGN_CRYSTAL_BASE);
            return true;
        }
        if (id == BTN_REGISTER) {
            blockEntity.tryRegister();
            return true;
        }
        if (id == BTN_CLEAR) {
            blockEntity.clearSelection();
            return true;
        }
        return false;
    }

    /** Called by ModNet when the client sends RotateRingC2S. */
    public void rotateCrystalsViewServer(int steps) {
        blockEntity.rotateCrystalRing(steps);
    }

    // ── synced state accessors ──────────────────────────────────────────────

    public int getSelectedForm()         { return data.get(JewelryTableBlockEntity.DATA_SELECTED_FORM); }
    public int getCrystalAssign(int pos) { return data.get(JewelryTableBlockEntity.DATA_CRYSTAL_ASSIGN_0 + pos); }
    public int getCrystalOffset()        { return data.get(JewelryTableBlockEntity.DATA_CRYSTAL_OFFSET); }
    public int getFormOffset()           { return data.get(JewelryTableBlockEntity.DATA_FORM_OFFSET); }

    public boolean canRegister() {
        if (getSelectedForm() < 0) return false;
        if (slots.get(SLOT_TOOLBASE).getItem().isEmpty()) return false;
        for (int i = 0; i < 3; i++) if (getCrystalAssign(i) < 0) return false;
        return true;
    }

    // ── shift-click ─────────────────────────────────────────────────────────

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy  = stack.copy();

        if (index < SLOT_PLAYER_BASE) {
            if (!moveItemStackTo(stack, SLOT_PLAYER_BASE, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (stack.getItem() instanceof ToolBase) {
                if (!moveItemStackTo(stack, SLOT_TOOLBASE, SLOT_TOOLBASE + 1, false)) return ItemStack.EMPTY;
            } else if (ToolBase.isCrystal(stack)) {
                if (!moveItemStackTo(stack, SLOT_CRYSTAL_BASE, SLOT_CRYSTAL_BASE + 6, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return access.evaluate((level, pos) ->
                player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
    }
}