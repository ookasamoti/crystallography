package net.ookasamoti.crystallography.common.item.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
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

import java.util.List;

/**
 * 結晶の固有アビリティのうち、採掘関連（{@link CrystalTraitLogic} の「採掘フック系」）の実際の
 * 適用箇所。obsidian_tear / depborn / aquatic（+ turtle 併用時の強化）/ levitation（空中採掘ペナルティ
 * 無効化）/ rubble（即時採掘の抑制）は採掘速度そのものへの倍率、smelting / freezing / resonance /
 * ender（ドロップのインベントリ直接回収）/ wither（対象ブロックのシルクタッチ無効化）はブロックを
 * 実際に破壊した後の副作用。
 */
public final class CrystalTraitMiningHooks {
    private CrystalTraitMiningHooks() {}

    /** 旧 obsidian_mining。黒曜石グループ：黒曜石・エンダーチェスト・エンチャントテーブル。 */
    private static final float OBSIDIAN_TEAR_SPEED_MULTIPLIER = 2.0f;
    /** depborn：深層岩グループ（深層岩本体・その加工品・深層岩鉱石）の採掘速度補正。Lv上限1。 */
    private static final float DEPBORN_SPEED_MULTIPLIER = 1.5f;
    private static final float AQUATIC_MINING_SPEED_MULTIPLIER = 1.5f;
    /** vanillaがonGround()=false時に課す採掘速度ペナルティ（Player#getDestroySpeedでspeed/=5.0F）の逆数。 */
    private static final float AIRBORNE_MINING_PENALTY_DIVISOR = 5.0f;
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
        // フロート：vanillaが空中（onGround()=false）採掘に課す速度ペナルティ（1/5倍）を無効化する。
        // Player#getDestroySpeed はこのイベント発火の直前に speed /= 5.0F を適用済みのため、
        // ここでは単純にその逆数を掛け戻すだけでよい。
        if (!player.onGround() && CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.LEVITATION)) {
            speed *= AIRBORNE_MINING_PENALTY_DIVISOR;
        }
        // 石礫：ブロックが壊れる前に必ず1tick以上かかるよう、即時採掘が発生する閾値の手前まで速度を頭打ちにする
        // （硬度0の元々常に即時採掘のブロックは対象外）。
        if (CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.RUBBLE)) {
            BlockPos pos = event.getPosition().orElse(null);
            if (pos != null) {
                float hardness = state.getDestroySpeed(player.level(), pos);
                if (hardness > 0f) {
                    float instantBreakThreshold = hardness * (player.hasCorrectToolForDrops(state) ? 30f : 100f);
                    speed = Math.min(speed, instantBreakThreshold * CrystalTraitLogic.RUBBLE_SPEED_CAP_FACTOR);
                }
            }
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
                // overtone（振管石限定、resonance 自体は持たない）：resonance の連鎖半径にさらに+1。
                int radius = RESONANCE_BASE_RADIUS * resonanceLevel;
                if (CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.OVERTONE)) {
                    radius += CrystalTraitLogic.OVERTONE_RADIUS_BONUS;
                }
                expandResonance(event.getLevel(), event.getPos(), lo.form(), radius, player, tool);
            }
        }

        boolean hasWither = CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.WITHER);
        if (hasWither) {
            applyWitherNoSilkTouch(event, player, tool, lo.form());
        }
        // ハサミ枠（短剣＝ナイフ）の本来のドロップ可否再計算。クモの巣／葉／羊毛／ツル等の loot table は
        // 「道具が minecraft:shears というアイテムそのものか」を直接見ているため、本 mod 独自の
        // CrystalShears アイテムではこの mod の Tool コンポーネント（採掘速度ルール）をどれだけ整えても
        // ドロップ自体は発生しない。wither 側が既に対象ブロック（葉等）を処理済みならそちらを優先する。
        if (lo.form() == ToolForm.SHEARS && !(hasWither && isWitherTarget(event.getState()))) {
            applyShearsCleanHarvest(event, player, tool);
        }
        if (CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.ENDER)) {
            applyEnderPickup(event, player);
        }
    }

    /** wither：草ブロック・ポドゾル・菌糸ブロック・根付いた土・葉ブロックをシルクタッチ適用外にする（採掘時、植物を枯らす）。 */
    private static boolean isWitherTarget(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.ROOTED_DIRT) || state.is(BlockTags.LEAVES);
    }

    /**
     * {@link #isWitherTarget} に該当するブロックのドロップを、ツールのシルクタッチを無視した
     * 結果（通常ドロップ）へ丸ごと差し替える。シルクタッチの有無でルートテーブルの分岐自体が
     * 変わるため、既存ドロップの変換ではなく {@link Block#getDrops} による再計算で対応する。
     * フォームが SHEARS の場合、葉ブロックの loot table は「道具が本物の {@code minecraft:shears}
     * アイテムか」も見ているため、シルクタッチ除去と同時に {@link #buildShearsSubstitute} で
     * 道具自体も差し替える（さもないとシルクタッチなし・shears判定もなしで何もドロップしなくなる）。
     */
    private static void applyWitherNoSilkTouch(BlockDropsEvent event, Player player, ItemStack tool, ToolForm form) {
        BlockState state = event.getState();
        if (!isWitherTarget(state)) return;

        ServerLevel level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockEntity blockEntity = event.getBlockEntity();

        ItemStack strippedTool = (form == ToolForm.SHEARS) ? buildShearsSubstitute(tool) : tool.copy();
        ItemEnchantments real = strippedTool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (!real.isEmpty()) {
            var mutable = new ItemEnchantments.Mutable(real);
            level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.SILK_TOUCH)
                    .ifPresent(silkTouch -> mutable.removeIf(holder -> holder.is(silkTouch)));
            strippedTool.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
        }

        List<ItemStack> forcedDrops = Block.getDrops(state, level, pos, blockEntity, player, strippedTool);
        event.getDrops().clear();
        for (ItemStack drop : forcedDrops) {
            if (drop.isEmpty()) continue;
            event.getDrops().add(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop));
        }
    }

    /**
     * ハサミ枠（短剣＝ナイフ）：クモの巣／葉／ツル／発光地衣類等の loot table が「道具が本物の
     * {@code minecraft:shears} アイテムそのものか」を直接判定しているため、この mod 独自の
     * {@code CrystalShears} アイテムではタグや Tool コンポーネントをいくら整えても対象にならない。
     * {@link Block#getDrops} を本物の {@link Items#SHEARS}（同じエンチャント付き）で再計算し、
     * そのままドロップを丸ごと差し替えることで実質的に「本物のハサミと同一のドロップ」を再現する。
     */
    private static void applyShearsCleanHarvest(BlockDropsEvent event, Player player, ItemStack tool) {
        ServerLevel level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockEntity blockEntity = event.getBlockEntity();
        BlockState state = event.getState();

        ItemStack substitute = buildShearsSubstitute(tool);
        List<ItemStack> forcedDrops = Block.getDrops(state, level, pos, blockEntity, player, substitute);
        event.getDrops().clear();
        for (ItemStack drop : forcedDrops) {
            if (drop.isEmpty()) continue;
            event.getDrops().add(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop));
        }
    }

    /** 本物の {@link Items#SHEARS} のコピーに、元の道具のエンチャント（幸運等）だけを引き継がせる。 */
    private static ItemStack buildShearsSubstitute(ItemStack tool) {
        ItemStack substitute = new ItemStack(Items.SHEARS);
        ItemEnchantments enchantments = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (!enchantments.isEmpty()) {
            substitute.set(DataComponents.ENCHANTMENTS, enchantments);
        }
        return substitute;
    }

    /**
     * ender（採掘時）：ドロップをプレイヤーのインベントリへ直接テレポートさせる。入りきらない分は
     * 足元（プレイヤーの現在位置）へ落とす。遠距離攻撃時の瞬間移動効果（{@link CrystalTraitProjectileHooks}）
     * とはイベントの発火元が異なるだけで、分岐は不要。
     */
    private static void applyEnderPickup(BlockDropsEvent event, Player player) {
        // この時点のドロップはまだ ServerLevel に spawn されておらず（イベント処理後に呼び出し元が
        // event.getDrops() を走査して addFreshEntity する）、未spawnエンティティへの discard() は
        // 想定外の経路のため、インベントリに収まりきった分はリストから直接取り除く。
        var it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemEntity drop = it.next();
            ItemStack stack = drop.getItem();
            if (stack.isEmpty()) continue;
            // Inventory#add は stack を直接書き換え、入りきらなかった分だけ残す（戻り値は
            // 「1個でも入ったか」であって「全部入ったか」ではないため、判定には使わない）。
            player.getInventory().add(stack);
            if (stack.isEmpty()) {
                it.remove();
            } else {
                drop.setPos(player.getX(), player.getY(), player.getZ());
                drop.setItem(stack);
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
