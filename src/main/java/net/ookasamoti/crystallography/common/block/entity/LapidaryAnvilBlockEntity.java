package net.ookasamoti.crystallography.common.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.ookasamoti.crystallography.setup.BlockEntitiesRegistry;
import net.ookasamoti.crystallography.setup.BlockRegistry;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jetbrains.annotations.NotNull;

public class LapidaryAnvilBlockEntity extends BlockEntity {

    public static final int SLOT_PICK  = 0;
    public static final int SLOT_WEDGE = 1;
    public static final int SLOT_ORE   = 2;

    public static final int SLOT_RIGHT_START = 3;
    public static final int SLOT_RIGHT_END   = 17;

    public static final int SLOT_COUNT = 18;

    private final ItemStacksResourceHandler items = new ItemStacksResourceHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
        }

        // スロット制限：ツルハシスロットにはツルハシ系のみ、wedgeスロットにはwedgeのみ。
        // ResourceHandlerSlot.mayPlace（プレイヤー操作）と insert（生成物の自動挿入）の両方に効く。
        @Override
        public boolean isValid(int index, ItemResource resource) {
            if (resource.isEmpty()) return true;
            if (index == SLOT_PICK)  return resource.getItem().builtInRegistryHolder().is(ItemTags.PICKAXES);
            if (index == SLOT_WEDGE) return resource.is(BlockRegistry.WEDGE.get().asItem());
            return true;
        }

        // deserialize は保存リストのサイズにハンドラを縮める。旧セーブ（スロット数違い）を読むと
        // size が SLOT_COUNT 未満になり、メニュー（18スロット）を開く際に getResource() が
        // IndexOutOfBounds で落ちる。読み込み後は常に SLOT_COUNT に正規化する。
        @Override
        public void deserialize(net.minecraft.world.level.storage.ValueInput input) {
            super.deserialize(input);
            if (size() != SLOT_COUNT) {
                NonNullList<ItemStack> fixed = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
                int n = Math.min(size(), SLOT_COUNT);
                for (int i = 0; i < n; i++) {
                    fixed.set(i, getResource(i).toStack(getAmountAsInt(i)));
                }
                setStacks(fixed);
            }
        }
    };

    public LapidaryAnvilBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesRegistry.LAPIDARY_ANVIL_BE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput output) {
        super.saveAdditional(output);
        items.serialize(output.child("inv"));
    }

    @Override
    protected void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        input.child("inv").ifPresent(items::deserialize);
    }

    public ItemStacksResourceHandler getItems() {
        return items;
    }

    @Override
    public void preRemoveSideEffects(@NotNull BlockPos pos, @NotNull BlockState state) {
        if (level != null) dropAllContents(level, pos);
        super.preRemoveSideEffects(pos, state);
    }

    public void dropAllContents(Level level, BlockPos pos) {
        var container = new SimpleContainer(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) container.setItem(i, items.getResource(i).toStack(items.getAmountAsInt(i)));
        Containers.dropContents(level, pos, container);
    }

    public boolean hasAllLeftInputs() {
        return !items.getResource(SLOT_ORE).isEmpty()
                && !items.getResource(SLOT_WEDGE).isEmpty()
                && !items.getResource(SLOT_PICK).isEmpty();
    }

    public boolean hasAnyRightItems() {
        for (int i = SLOT_RIGHT_START; i <= SLOT_RIGHT_END; i++) {
            if (!items.getResource(i).isEmpty()) return true;
        }
        return false;
    }
}
