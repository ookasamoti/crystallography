package net.ookasamoti.crystallography.common.item.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * 結晶の固有アビリティのうち、採掘関連（{@link CrystalTraitLogic} の「採掘フック系」）の実際の
 * 適用箇所。obsidian_mining / aquatic は採掘速度そのものへの倍率、smelting / freezing は
 * ブロックを実際に破壊した後の副作用（ドロップの製錬・周囲の水の氷結）。
 */
public final class CrystalTraitMiningHooks {
    private CrystalTraitMiningHooks() {}

    private static final float OBSIDIAN_MINING_SPEED_MULTIPLIER = 2.0f;
    private static final float AQUATIC_MINING_SPEED_MULTIPLIER = 1.5f;
    /** freezing：採掘した位置を中心にこの半径（ブロック）以内の水源ブロックを凍らせる。 */
    private static final int FREEZING_RADIUS = 2;

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(CrystalTraitMiningHooks::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(CrystalTraitMiningHooks::onBlockDrops);
    }

    private static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof ICrystalTool)) return;
        var lo = CrystalToolLogic.getActiveLoadout(tool);
        if (lo == null || lo.unusable()) return;

        int tier = CrystalToolLogic.getTier(tool);
        var crystalInv = ToolInventory.get(tool, CrystalToolLogic.crystalSlotCount(tier), player.level().registryAccess());

        float speed = event.getNewSpeed();
        BlockState state = event.getState();
        if (CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.OBSIDIAN_MINING)
                && (state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN) || state.is(Blocks.ENDER_CHEST))) {
            speed *= OBSIDIAN_MINING_SPEED_MULTIPLIER;
        }
        if (CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.AQUATIC) && player.isUnderWater()) {
            speed *= AQUATIC_MINING_SPEED_MULTIPLIER;
        }
        event.setNewSpeed(speed);
    }

    private static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player)) return; // プレイヤー採掘のみ対象（ホッパー等の自動破壊は対象外）
        ItemStack tool = event.getTool();
        if (!(tool.getItem() instanceof ICrystalTool)) return;
        var lo = CrystalToolLogic.getActiveLoadout(tool);
        if (lo == null || lo.unusable()) return;

        int tier = CrystalToolLogic.getTier(tool);
        var crystalInv = ToolInventory.get(tool, CrystalToolLogic.crystalSlotCount(tier), event.getLevel().registryAccess());

        if (CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.SMELTING)) {
            applySmelting(event);
        }
        if (CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.FREEZING)) {
            freezeNearbyWater(event.getLevel(), event.getPos());
        }
    }

    /** ドロップの各アイテムを、対応する製錬レシピの結果へ差し替える（個数は維持）。 */
    private static void applySmelting(BlockDropsEvent event) {
        ServerLevel level = event.getLevel();
        for (ItemEntity drop : event.getDrops()) {
            ItemStack input = drop.getItem();
            if (input.isEmpty()) continue;
            SingleRecipeInput recipeInput = new SingleRecipeInput(input);
            level.recipeAccess().getRecipeFor(RecipeType.SMELTING, recipeInput, level).ifPresent(holder -> {
                ItemStack smelted = holder.value().assemble(recipeInput);
                if (!smelted.isEmpty()) {
                    smelted.setCount(input.getCount());
                    drop.setItem(smelted);
                }
            });
        }
    }

    /** 中心位置の周囲 {@link #FREEZING_RADIUS} 以内にある水源ブロックを氷に変える。 */
    private static void freezeNearbyWater(ServerLevel level, BlockPos center) {
        for (int dx = -FREEZING_RADIUS; dx <= FREEZING_RADIUS; dx++) {
            for (int dy = -FREEZING_RADIUS; dy <= FREEZING_RADIUS; dy++) {
                for (int dz = -FREEZING_RADIUS; dz <= FREEZING_RADIUS; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
                        level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
                    }
                }
            }
        }
    }
}
