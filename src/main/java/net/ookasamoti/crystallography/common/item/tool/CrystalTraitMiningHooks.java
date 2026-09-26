package net.ookasamoti.crystallography.common.item.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.ookasamoti.crystallography.common.item.tool.component.ToolForm;

/**
 * 結晶の固有アビリティのうち、採掘関連（{@link CrystalTraitLogic} の「採掘フック系」）の実際の
 * 適用箇所。obsidian_tear / depborn / aquatic（+ turtle 併用時の強化）は採掘速度そのものへの倍率、
 * smelting / freezing / resonance はブロックを実際に破壊した後の副作用（ドロップの製錬・周囲の水の
 * 氷結・周囲の同系統ブロックの連鎖採掘）。
 */
public final class CrystalTraitMiningHooks {
    private CrystalTraitMiningHooks() {}

    /** 旧 obsidian_mining。黒曜石グループ：黒曜石・エンダーチェスト・エンチャントテーブル。 */
    private static final float OBSIDIAN_TEAR_SPEED_MULTIPLIER = 2.0f;
    /** depborn：深層岩グループ（深層岩本体・その加工品・深層岩鉱石）の採掘速度補正。Lv上限1。 */
    private static final float DEPBORN_SPEED_MULTIPLIER = 1.5f;
    private static final float AQUATIC_MINING_SPEED_MULTIPLIER = 1.5f;
    /** turtle：ツールに付いているとき、同ロードアウトの aquatic の効果（速度ボーナス分）を倍増する。 */
    private static final float TURTLE_AQUATIC_BONUS_MULTIPLIER = 2.0f;
    /** freezing（効果範囲拡張グループ）：Lvごとの基礎半径（ブロック）。 */
    private static final int FREEZING_BASE_RADIUS = 2;
    /** resonance（効果範囲拡張グループ）：Lvごとの基礎半径（ブロック）。 */
    private static final int RESONANCE_BASE_RADIUS = 1;

    /** resonance の連鎖採掘中、内部で再帰的に onBlockDrops が呼ばれても再展開しないためのガード。 */
    private static final ThreadLocal<Boolean> RESONANCE_EXPANDING = ThreadLocal.withInitial(() -> false);

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
        if (CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.OBSIDIAN_TEAR) && isObsidianGroup(state)) {
            speed *= OBSIDIAN_TEAR_SPEED_MULTIPLIER;
        }
        if (CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.DEPBORN) && isDeepslateGroup(state)) {
            speed *= DEPBORN_SPEED_MULTIPLIER;
        }
        if (CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.AQUATIC) && player.isUnderWater()) {
            float bonus = AQUATIC_MINING_SPEED_MULTIPLIER - 1f;
            if (CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.TURTLE)) {
                bonus *= TURTLE_AQUATIC_BONUS_MULTIPLIER;
            }
            speed *= 1f + bonus;
        }
        event.setNewSpeed(speed);
    }

    private static boolean isObsidianGroup(BlockState state) {
        return state.is(Blocks.OBSIDIAN) || state.is(Blocks.ENDER_CHEST) || state.is(Blocks.ENCHANTING_TABLE);
    }

    /** 深層岩本体・加工品・深層岩鉱石すべて（レジストリ名に "deepslate" を含むブロック）。 */
    private static boolean isDeepslateGroup(BlockState state) {
        return state.getBlock().builtInRegistryHolder().key().identifier().getPath().contains("deepslate");
    }

    private static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player player)) return; // プレイヤー採掘のみ対象（ホッパー等の自動破壊は対象外）
        ItemStack tool = event.getTool();
        if (!(tool.getItem() instanceof ICrystalTool)) return;
        var lo = CrystalToolLogic.getActiveLoadout(tool);
        if (lo == null || lo.unusable()) return;

        int tier = CrystalToolLogic.getTier(tool);
        var crystalInv = ToolInventory.get(tool, CrystalToolLogic.crystalSlotCount(tier), event.getLevel().registryAccess());

        if (CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.SMELTING)) {
            applySmelting(event);
        }
        int freezingLevel = CrystalTraitLogic.traitLevel(lo, crystalInv, CrystalTraitLogic.FREEZING, CrystalTraitLogic.SCALING_MAX_LEVEL);
        if (freezingLevel > 0) {
            freezeNearbyWater(event.getLevel(), event.getPos(), FREEZING_BASE_RADIUS * freezingLevel);
        }

        if (!RESONANCE_EXPANDING.get()) {
            int resonanceLevel = CrystalTraitLogic.traitLevel(lo, crystalInv, CrystalTraitLogic.RESONANCE, CrystalTraitLogic.SCALING_MAX_LEVEL);
            if (resonanceLevel > 0 && matchesResonanceCategory(lo.form(), event.getState())) {
                expandResonance(event.getLevel(), event.getPos(), lo.form(), RESONANCE_BASE_RADIUS * resonanceLevel, player, tool);
            }
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

    /** 中心位置の周囲 {@code radius} 以内にある水源ブロックを氷に変える。 */
    private static void freezeNearbyWater(ServerLevel level, BlockPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
                        level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
                    }
                }
            }
        }
    }

    /** resonance：フォームごとの対象カテゴリ（ピッケル＝鉱石／斧＝原木／シャベル＝シャベル適正／クワ＝クワ適正）。 */
    private static boolean matchesResonanceCategory(ToolForm form, BlockState state) {
        return switch (form) {
            case PICKAXE -> isOreBlock(state);
            case AXE -> state.is(BlockTags.LOGS);
            case SHOVEL -> state.is(BlockTags.MINEABLE_WITH_SHOVEL);
            case HOE -> state.is(BlockTags.MINEABLE_WITH_HOE);
            default -> false;
        };
    }

    private static boolean isOreBlock(BlockState state) {
        return state.is(BlockTags.COAL_ORES) || state.is(BlockTags.IRON_ORES) || state.is(BlockTags.DIAMOND_ORES)
                || state.is(BlockTags.REDSTONE_ORES) || state.is(BlockTags.LAPIS_ORES) || state.is(BlockTags.EMERALD_ORES)
                || state.is(BlockTags.COPPER_ORES) || state.is(BlockTags.GOLD_ORES);
    }

    /** 中心位置の周囲 {@code radius} 以内にある同カテゴリのブロックを、道具で採掘したのと同様に連鎖破壊する。 */
    private static void expandResonance(ServerLevel level, BlockPos center, ToolForm form, int radius, Player player, ItemStack tool) {
        RESONANCE_EXPANDING.set(true);
        try {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos pos = center.offset(dx, dy, dz);
                        BlockState state = level.getBlockState(pos);
                        if (!matchesResonanceCategory(form, state)) continue;
                        mineBonusBlock(level, pos, player, tool);
                    }
                }
            }
        } finally {
            RESONANCE_EXPANDING.set(false);
        }
    }

    /**
     * {@link net.minecraft.world.level.Level#destroyBlock} 相当だが、実際に使用中の道具を
     * ドロップ計算（幸運等のエンチャント判定）へ渡し、耐久値も通常の採掘と同じ経路で消費させる
     * （vanilla の {@code destroyBlock} は常に空スタックしか渡さず、両方が素通りしてしまうため）。
     */
    private static void mineBonusBlock(ServerLevel level, BlockPos pos, Player player, ItemStack tool) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        level.levelEvent(2001, pos, Block.getId(state));
        Block.dropResources(state, level, pos, blockEntity, player, tool);
        level.removeBlock(pos, false);
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
        tool.hurtAndBreak(1, level, player, item -> {});
    }
}
