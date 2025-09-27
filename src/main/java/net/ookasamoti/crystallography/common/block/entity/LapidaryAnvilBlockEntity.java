package net.ookasamoti.crystallography.common.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ookasamoti.crystallography.setup.BlockEntitiesRegistry;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class LapidaryAnvilBlockEntity extends BlockEntity {

    public static final int SLOT_ORE   = 0;
    public static final int SLOT_WEDGE = 1;
    public static final int SLOT_PICK  = 2;
    public static final int SLOT_RIGHT_START = 3;
    public static final int SLOT_RIGHT_END   = 20; // inclusive
    public static final int SLOT_COUNT = 21;

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public LapidaryAnvilBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesRegistry.LAPIDARY_ANVIL_BE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("inv", items.serializeNBT(provider));
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("inv")) {
            items.deserializeNBT(provider, tag.getCompound("inv"));
        }
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public void dropAllContents(Level level, BlockPos pos) {
        var container = new SimpleContainer(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) container.setItem(i, items.getStackInSlot(i));
        Containers.dropContents(level, pos, container);
    }

    public boolean hasAllLeftInputs() {
        return !items.getStackInSlot(SLOT_ORE).isEmpty()
                && !items.getStackInSlot(SLOT_WEDGE).isEmpty()
                && !items.getStackInSlot(SLOT_PICK).isEmpty();
    }

    public boolean hasAnyRightItems() {
        for (int i = SLOT_RIGHT_START; i <= SLOT_RIGHT_END; i++) {
            if (!items.getStackInSlot(i).isEmpty()) return true;
        }
        return false;
    }
}
