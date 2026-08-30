package net.ookasamoti.crystallography.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.ookasamoti.crystallography.common.menu.LapidaryAnvilMenu;
import net.ookasamoti.crystallography.common.block.entity.LapidaryAnvilBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LapidaryAnvilBlock extends BaseEntityBlock {

    public static final MapCodec<LapidaryAnvilBlock> CODEC = simpleCodec(LapidaryAnvilBlock::new);
    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public LapidaryAnvilBlock(Properties props) {
        super(props);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state,
                                                        Level level,
                                                        @NotNull BlockPos pos,
                                                        @NotNull Player player,
                                                        @NotNull BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        var be = level.getBlockEntity(pos);
        if (be instanceof LapidaryAnvilBlockEntity anvil) {
            if (player instanceof ServerPlayer sp) {
                MenuProvider provider = new SimpleMenuProvider(
                        (windowId, inv, p) -> new LapidaryAnvilMenu(windowId, inv, p, pos),
                        Component.translatable("block.crystallography.lapidary_anvil")
                );
                sp.openMenu(provider, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected @NotNull InteractionResult useItemOn(@NotNull ItemStack heldStack,
                                                   @NotNull BlockState state,
                                                   @NotNull Level level,
                                                   @NotNull BlockPos pos,
                                                   @NotNull Player player,
                                                   @NotNull InteractionHand hand,
                                                   @NotNull BlockHitResult hit) {
        var r = useWithoutItem(state, level, pos, player, hit);
        // PASS means this wasn't our block entity; let vanilla try the empty-hand interaction.
        return r == InteractionResult.PASS ? InteractionResult.TRY_WITH_EMPTY_HAND : r;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new LapidaryAnvilBlockEntity(pos, state);
    }
}
