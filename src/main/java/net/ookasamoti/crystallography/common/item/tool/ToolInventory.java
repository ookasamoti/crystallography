package net.ookasamoti.crystallography.common.item.tool;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ookasamoti.crystallography.common.item.crystal.Crystal;

public class ToolInventory {

    private static final String TAG_CRYSTAL_INV = "Crystallography:CrystalInv";

    public static IItemHandlerModifiable get(ItemStack carrier, int slots, HolderLookup.Provider lookup) {
        var handler = new StackBackedHandler(carrier, slots, lookup);

        // CustomData から既存の保存内容を復元
        CompoundTag root = readOrCreateCustomTag(carrier);
        CompoundTag invTag = root.getCompound(TAG_CRYSTAL_INV);
        handler.deserializeNBT(lookup, invTag);

        return handler;
    }

    private static CompoundTag readOrCreateCustomTag(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag();
    }

    private static void writeCustomTag(ItemStack stack, CompoundTag root) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static final class StackBackedHandler extends ItemStackHandler {
        private final ItemStack carrier;
        private final HolderLookup.Provider lookup;

        private StackBackedHandler(ItemStack carrier, int slots, HolderLookup.Provider lookup) {
            super(slots);
            this.carrier = carrier;
            this.lookup = lookup;
        }

        @Override
        protected void onContentsChanged(int slot) {
            save();
        }

        private void save() {
            CompoundTag root = readOrCreateCustomTag(carrier);
            CompoundTag inv = this.serializeNBT(lookup);
            root.put(TAG_CRYSTAL_INV, inv);
            writeCustomTag(carrier, root);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // 結晶のみ格納可能（ここに追加のバリデーションを後で拡張可）
            return stack.getItem() instanceof Crystal;
        }
    }
}
