package net.ookasamoti.crystallography.common.item.tool;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.ookasamoti.crystallography.common.item.crystal.Crystal;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ToolInventory {

    private static final String TAG_CRYSTAL_INV = "Crystallography:CrystalInv";

    public static ItemStacksResourceHandler get(ItemStack carrier, int slots, HolderLookup.Provider lookup) {
        return get(carrier, slots, lookup, null);
    }

    /**
     * @param onSaved invoked with the carrier stack after every save. Because the new resource API
     *               only exposes copies of stored stacks, the carrier here is typically itself a copy
     *               of the real tool (e.g. the jewelry table's center slot). The caller must use this
     *               callback to write the (now-updated) carrier back to its real storage; otherwise
     *               edits to the crystal inventory are lost.
     */
    public static ItemStacksResourceHandler get(ItemStack carrier, int slots, HolderLookup.Provider lookup,
                                                @Nullable Consumer<ItemStack> onSaved) {
        var handler = new StackBackedHandler(carrier, slots, lookup, onSaved);

        // CustomData から既存の保存内容を復元
        CompoundTag root = readOrCreateCustomTag(carrier);
        CompoundTag invTag = root.getCompoundOrEmpty(TAG_CRYSTAL_INV);
        handler.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, lookup, invTag));

        return handler;
    }

    private static CompoundTag readOrCreateCustomTag(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag();
    }

    private static void writeCustomTag(ItemStack stack, CompoundTag root) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static final class StackBackedHandler extends ItemStacksResourceHandler {
        private final ItemStack carrier;
        private final HolderLookup.Provider lookup;
        private final @Nullable Consumer<ItemStack> onSaved;

        private StackBackedHandler(ItemStack carrier, int slots, HolderLookup.Provider lookup,
                                   @Nullable Consumer<ItemStack> onSaved) {
            super(slots);
            this.carrier = carrier;
            this.lookup = lookup;
            this.onSaved = onSaved;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            save();
        }

        private void save() {
            CompoundTag root = readOrCreateCustomTag(carrier);
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, lookup);
            this.serialize(output);
            root.put(TAG_CRYSTAL_INV, output.buildResult());
            writeCustomTag(carrier, root);
            if (onSaved != null) onSaved.accept(carrier);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            // 結晶のみ格納可能（ここに追加のバリデーションを後で拡張可）
            return resource.getItem() instanceof Crystal;
        }
    }
}
