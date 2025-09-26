package net.ookasamoti.crystallography.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ookasamoti.crystallography.setup.BlockRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Wedge extends Block implements SimpleWaterloggedBlock {
    public static final IntegerProperty WEDGES = IntegerProperty.create("wedges", 1, 4);
    public static final IntegerProperty HITS = IntegerProperty.create("hits", 0, 3); // 叩かれた回数
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);

    public Wedge(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(WEDGES, 1)
                .setValue(HITS, 0)
                .setValue(WATERLOGGED, false)
                .setValue(FACE, AttachFace.WALL)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        FluidState fluidState = level.getFluidState(pos);

        BlockState blockState = this.defaultBlockState()
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER)
                .setValue(FACING, context.getHorizontalDirection().getOpposite());

        if (clickedFace == Direction.UP) {
            return blockState.setValue(FACE, AttachFace.FLOOR);
        } else if (clickedFace == Direction.DOWN) {
            return blockState.setValue(FACE, AttachFace.CEILING);
        } else {
            return blockState.setValue(FACE, AttachFace.WALL).setValue(FACING, clickedFace);
        }
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        int hits = state.getValue(HITS);
        AttachFace face = state.getValue(BlockStateProperties.ATTACH_FACE);

        return switch (face) {
            case FLOOR -> switch (hits) {
                case 1 -> Block.box(0.0, 0.0, 0.0, 16.0, 5.0, 16.0);
                case 2 -> Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0);
                case 3 -> Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);
                default -> Block.box(0.0, 0.0, 0.0, 16.0, 7.0, 16.0);
            };
            case CEILING -> switch (hits) {
                case 1 -> Block.box(0.0, 11.0, 0.0, 16.0, 16.0, 16.0);
                case 2 -> Block.box(0.0, 13.0, 0.0, 16.0, 16.0, 16.0);
                case 3 -> Block.box(0.0, 15.0, 0.0, 16.0, 16.0, 16.0);
                default -> Block.box(0.0, 9.0, 0.0, 16.0, 16.0, 16.0);
            };
            case WALL -> {
                Direction facing = state.getValue(FACING);
                yield switch (facing) {
                    case NORTH -> switch (hits) {
                        case 1 -> Block.box(0.0, 0.0, 11.0, 16.0, 16.0, 16.0);
                        case 2 -> Block.box(0.0, 0.0, 13.0, 16.0, 16.0, 16.0);
                        case 3 -> Block.box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0);
                        default -> Block.box(0.0, 0.0, 9.0, 16.0, 16.0, 16.0);
                    };
                    case SOUTH -> switch (hits) {
                        case 1 -> Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 5.0);
                        case 2 -> Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 3.0);
                        case 3 -> Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);
                        default -> Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 7.0);
                    };
                    case EAST -> switch (hits) {
                        case 1 -> Block.box(0.0, 0.0, 0.0, 5.0, 16.0, 16.0);
                        case 2 -> Block.box(0.0, 0.0, 0.0, 3.0, 16.0, 16.0);
                        case 3 -> Block.box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0);
                        default -> Block.box(0.0, 0.0, 0.0, 7.0, 16.0, 16.0);
                    };
                    case WEST -> switch (hits) {
                        case 1 -> Block.box(11.0, 0.0, 0.0, 16.0, 16.0, 16.0);
                        case 2 -> Block.box(13.0, 0.0, 0.0, 16.0, 16.0, 16.0);
                        case 3 -> Block.box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);
                        default -> Block.box(9.0, 0.0, 0.0, 16.0, 16.0, 16.0);
                    };
                    default -> Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
                };
            }
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WEDGES, HITS, WATERLOGGED, FACE, FACING);
    }

    @Override
    public @NotNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    private Direction getAttachDirection(BlockState state) {
        AttachFace face = state.getValue(FACE);
        return switch (face) {
            case FLOOR -> Direction.DOWN;
            case CEILING -> Direction.UP;
            case WALL -> state.getValue(FACING).getOpposite();
        };
    }

    @Override
    public boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        Direction attachDirection = getAttachDirection(state);
        BlockPos supportPos = pos.relative(attachDirection);
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, attachDirection.getOpposite());
    }

    public @NotNull ItemInteractionResult useItemOn(ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (stack.getItem() instanceof PickaxeItem) {
            int currentHits = state.getValue(HITS);
            int wedgeCount = state.getValue(WEDGES);
            Direction attachDirection = getAttachDirection(state);
            BlockPos targetPos = pos.relative(attachDirection);

            if (currentHits < 3) {
                level.setBlock(pos, state.setValue(HITS, currentHits + 1), 3);
                level.playSound(null, pos, SoundEvents.METAL_HIT, SoundSource.BLOCKS, 1.0F, 1.0F);
            } else {
                if (level.getBlockState(targetPos).isFaceSturdy(level, targetPos, attachDirection.getOpposite())) {
                    attemptWedgeMining(level, targetPos, pos, wedgeCount, player);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        } else if (stack.getItem() == BlockRegistry.WEDGE.get().asItem() && state.getValue(WEDGES) < 4) {
            int currentWedges = state.getValue(WEDGES);
            level.setBlock(pos, state.setValue(WEDGES, currentWedges + 1), 3);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        Direction attachDirection = getAttachDirection(state);

        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        if (attachDirection == direction && !state.canSurvive(level, pos)) {
            int wedgeCount = state.getValue(WEDGES);
            int hits = state.getValue(HITS);
            double dropChance = 1.0 - (hits * 0.1);

            for (int i = 0; i < wedgeCount; i++) {
                if (RandomSource.create().nextDouble() < dropChance) {
                    popResource((Level) level, pos, new ItemStack(this));
                }
            }
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    private void attemptWedgeMining(Level level, BlockPos targetPos, BlockPos wedgePos, int wedgeCount, Player player) {
        BlockState targetState = level.getBlockState(targetPos);
        BlockEntity blockEntity = level.getBlockEntity(targetPos);
        ItemStack blockStack = new ItemStack(targetState.getBlock());

        boolean canHarvest = targetState.canHarvestBlock(level, targetPos, player);
        double blockDropChance = 0.2 + (wedgeCount * 0.2);
        boolean canDropBlock = RandomSource.create().nextDouble() < blockDropChance;

        if (level.getBlockState(targetPos).getDestroySpeed(level, targetPos) >= 0 && canHarvest) {

            if (!isMultiParts(level, targetPos) && canDropBlock) {
                if (blockEntity != null) {
                    CompoundTag blockEntityData = blockEntity.saveWithFullMetadata(level.registryAccess());
                    BlockItem.setBlockEntityData(blockStack, blockEntity.getType(), blockEntityData);
                }
                Block.popResource(level, targetPos, blockStack);
            }
            level.removeBlock(targetPos, false);
        } else {
            level.playSound(null, targetPos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.removeBlock(wedgePos, false);
        }
    }

    private boolean isMultiParts(Level level, BlockPos targetPos){
        boolean result = false;
        BlockState targetState = level.getBlockState(targetPos);
        if (level instanceof ServerLevel serverLevel) {
            for (Direction side : Direction.values()) {
                BlockPos adjacentPos = targetPos.relative(side);
                BlockState adjacentState = level.getBlockState(adjacentPos);

                if (adjacentState.getBlock() == targetState.getBlock()) {
                    List<ItemStack> drops = Block.getDrops(targetState, serverLevel, targetPos, null);
                    if (drops.isEmpty()) {
                        result = true;
                    }
                    break;
                }
            }
        }
        return result;
    }
}
