package net.ookasamoti.crystallography.common.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ookasamoti.crystallography.client.screen.JewelryTableMenu;
import net.ookasamoti.crystallography.common.item.tool.ToolBase;
import net.ookasamoti.crystallography.common.item.tool.component.ToolForm;
import net.ookasamoti.crystallography.common.item.tool.component.ToolLoadout;
import net.ookasamoti.crystallography.setup.BlockEntitiesRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class JewelryTableBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_TOOLBASE = 0;
    public static final int SLOT_CRYSTAL_START = 1;
    public static final int SLOT_CRYSTAL_COUNT = 6;
    public static final int SLOT_TOTAL = 7;

    public static final int DATA_SELECTED_FORM    = 0;
    public static final int DATA_CRYSTAL_ASSIGN_0 = 1;
    public static final int DATA_CRYSTAL_ASSIGN_1 = 2;
    public static final int DATA_CRYSTAL_ASSIGN_2 = 3;
    public static final int DATA_CRYSTAL_OFFSET   = 4;
    public static final int DATA_FORM_OFFSET      = 5;
    public static final int DATA_COUNT            = 6;

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_TOTAL) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null) level.invalidateCapabilities(getBlockPos());
        }
    };

    private int selectedForm = -1;
    private final int[] crystalAssign = {-1, -1, -1};
    private int crystalOffset = 0;
    private int formOffset = 0;

    private final ContainerData containerData = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case DATA_SELECTED_FORM    -> selectedForm;
                case DATA_CRYSTAL_ASSIGN_0 -> crystalAssign[0];
                case DATA_CRYSTAL_ASSIGN_1 -> crystalAssign[1];
                case DATA_CRYSTAL_ASSIGN_2 -> crystalAssign[2];
                case DATA_CRYSTAL_OFFSET   -> crystalOffset;
                case DATA_FORM_OFFSET      -> formOffset;
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {
            switch (i) {
                case DATA_SELECTED_FORM    -> selectedForm    = v;
                case DATA_CRYSTAL_ASSIGN_0 -> crystalAssign[0] = v;
                case DATA_CRYSTAL_ASSIGN_1 -> crystalAssign[1] = v;
                case DATA_CRYSTAL_ASSIGN_2 -> crystalAssign[2] = v;
                case DATA_CRYSTAL_OFFSET   -> crystalOffset   = v;
                case DATA_FORM_OFFSET      -> formOffset      = v;
            }
        }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public JewelryTableBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesRegistry.JEWELRY_TABLE_BE.get(), pos, state);
    }

    public ContainerData getContainerData() { return containerData; }
    public IItemHandler getItemHandler()    { return itemHandler; }

    // ── form selection ──────────────────────────────────────────────────────

    public void selectForm(int formOrdinal) {
        if (selectedForm == formOrdinal) {
            selectedForm = -1;
        } else {
            selectedForm = formOrdinal;
        }
        Arrays.fill(crystalAssign, -1);
        setChanged();
    }

    // ── crystal assignment ──────────────────────────────────────────────────

    /** visualIndex = which ring position was clicked (0-5). Maps to actual slot via crystalOffset. */
    public void assignCrystal(int visualIndex) {
        int actualSlot = Math.floorMod(visualIndex + crystalOffset, SLOT_CRYSTAL_COUNT);
        ItemStack crystal = itemHandler.getStackInSlot(SLOT_CRYSTAL_START + actualSlot);
        if (crystal.isEmpty()) return;

        // Toggle off if already assigned
        for (int i = 0; i < 3; i++) {
            if (crystalAssign[i] == actualSlot) {
                crystalAssign[i] = -1;
                setChanged();
                return;
            }
        }
        // Assign to first empty position
        for (int i = 0; i < 3; i++) {
            if (crystalAssign[i] < 0) {
                crystalAssign[i] = actualSlot;
                setChanged();
                return;
            }
        }
    }

    // ── register loadout ────────────────────────────────────────────────────

    public boolean tryRegister() {
        ItemStack toolStack = itemHandler.getStackInSlot(SLOT_TOOLBASE);
        if (toolStack.isEmpty() || !(toolStack.getItem() instanceof ToolBase)) return false;
        if (selectedForm < 0) return false;
        for (int a : crystalAssign) if (a < 0) return false;

        // Verify all 3 slots have crystals
        for (int i = 0; i < 3; i++) {
            if (itemHandler.getStackInSlot(SLOT_CRYSTAL_START + crystalAssign[i]).isEmpty()) return false;
        }

        ItemStack[] crystals = new ItemStack[3];
        for (int i = 0; i < 3; i++) {
            int si = SLOT_CRYSTAL_START + crystalAssign[i];
            crystals[i] = itemHandler.getStackInSlot(si).copyWithCount(1);
        }

        ToolLoadout loadout = new ToolLoadout(ToolForm.values()[selectedForm], crystals);
        ItemStack modifiedTool = toolStack.copy();
        if (!ToolBase.addLoadout(modifiedTool, loadout)) return false;

        // Consume crystals
        for (int i = 0; i < 3; i++) {
            itemHandler.extractItem(SLOT_CRYSTAL_START + crystalAssign[i], 1, false);
        }
        itemHandler.setStackInSlot(SLOT_TOOLBASE, modifiedTool);
        Arrays.fill(crystalAssign, -1);
        selectedForm = -1;
        setChanged();
        return true;
    }

    public void clearSelection() {
        selectedForm = -1;
        Arrays.fill(crystalAssign, -1);
        setChanged();
    }

    // ── ring rotation ───────────────────────────────────────────────────────

    public void rotateCrystalRing(int steps) {
        crystalOffset = Math.floorMod(crystalOffset + steps, SLOT_CRYSTAL_COUNT);
        setChanged();
    }

    public void rotateFormRing(int steps) {
        formOffset = Math.floorMod(formOffset + steps, ToolForm.values().length);
        setChanged();
    }

    // ── drops / lifecycle ───────────────────────────────────────────────────

    public void drops() {
        if (level == null || level.isClientSide) return;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    itemHandler.getStackInSlot(i));
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.jewelry_table");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player) {
        return new JewelryTableMenu(id, inv, this, containerData);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("selectedForm", selectedForm);
        tag.putIntArray("crystalAssign", crystalAssign.clone());
        tag.putInt("crystalOffset", crystalOffset);
        tag.putInt("formOffset", formOffset);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        selectedForm = tag.getInt("selectedForm");
        int[] arr = tag.getIntArray("crystalAssign");
        if (arr.length == 3) System.arraycopy(arr, 0, crystalAssign, 0, 3);
        crystalOffset = tag.getInt("crystalOffset");
        formOffset    = tag.getInt("formOffset");
    }
}
