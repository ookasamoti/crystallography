package net.ookasamoti.crystallography.common.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.ookasamoti.crystallography.common.menu.JewelryTableMenu;
import net.ookasamoti.crystallography.setup.BlockEntitiesRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JewelryTableBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStacksResourceHandler itemHandler = new ItemStacksResourceHandler(1) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
            if (level != null) {
                level.invalidateCapabilities(getBlockPos());
            }
        }
    };

    private ContainerData data = new SimpleContainerData(1);

    public JewelryTableBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesRegistry.JEWELRY_TABLE_BE.get(), pos, state);
    }

    public ContainerData getContainerData() { return this.data; }

    public void drops() {
        assert level != null;
        if (!level.isClientSide()) {
            for (int i = 0; i < itemHandler.size(); i++) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                        itemHandler.getResource(i).toStack(itemHandler.getAmountAsInt(i)));
            }
        }
    }

    public ItemStacksResourceHandler getItemHandler() { return itemHandler; }

    @Override
    public void preRemoveSideEffects(@NotNull BlockPos pos, @NotNull BlockState state) {
        drops();
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput output) {
        super.saveAdditional(output);
        itemHandler.serialize(output.child("inventory"));
    }

    @Override
    protected void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        input.child("inventory").ifPresent(itemHandler::deserialize);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.jewelry_table");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player) {
        return new JewelryTableMenu(id, inv, this, this.data);
    }
}
