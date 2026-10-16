package net.ookasamoti.crystallography.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.setup.AttachmentTypeRegistry;

/**
 * バニラの矢は炎を通過すると自身が着火し({@code isOnFire()})、命中した相手も着火させる
 * ({@code AbstractArrow#onHitEntity} が {@code entity.igniteForSeconds} を呼ぶ)。矢は
 * {@code LivingEntity} ではないため、{@link SoulFireIgnitionHooks} が使っている
 * {@code minecraft:in_fire} 接触ダメージ({@link LivingIncomingDamageEvent})は矢自身には
 * 飛んでこない。そのため矢が通過したブロックがソウルファイアかどうかは、燃えている間
 * 毎tick当たり判定を直接調べて {@link AttachmentTypeRegistry#ARROW_SOUL_FLAME} に記録する。
 * 命中時はこの印を見て、着火対象の {@link AttachmentTypeRegistry#SOUL_FIRE_IGNITED} を
 * ヒット本体のダメージイベント内で直接確定させる(無敵判定で素通りする延焼ダメージとは違い、
 * ヒット本体のダメージはほぼ確実に発火する)。
 */
public final class ArrowSoulFireHooks {
    private ArrowSoulFireHooks() {}

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(ArrowSoulFireHooks::onEntityTick);
        NeoForge.EVENT_BUS.addListener(ArrowSoulFireHooks::onHitDamage);
    }

    /** 燃えている矢が、通過中にソウルファイアブロックへ実際に触れたら印を付ける。 */
    private static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (arrow.level().isClientSide()) return;

        AABB box = arrow.getBoundingBox();
        boolean foundSoulFire = false;
        boolean foundNormalFire = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = Mth.floor(box.minX); x <= Mth.floor(box.maxX); x++) {
            for (int y = Mth.floor(box.minY); y <= Mth.floor(box.maxY); y++) {
                for (int z = Mth.floor(box.minZ); z <= Mth.floor(box.maxZ); z++) {
                    BlockState state = arrow.level().getBlockState(cursor.set(x, y, z));
                    if (state.is(Blocks.SOUL_FIRE)) foundSoulFire = true;
                    else if (state.is(Blocks.FIRE)) foundNormalFire = true;
                }
            }
        }
        boolean alreadyFlagged = Boolean.TRUE.equals(arrow.getData(AttachmentTypeRegistry.ARROW_SOUL_FLAME));
        // TODO(diagnostic): 原因特定のため一時的に残しているログ。切り分けできたら削除する。
        CrystallographyMod.LOGGER.info(
                "[arrowsoulfire-debug] tick arrow={} isOnFire={} fireTicks={} delta={} box={} foundSoulFire={} foundNormalFire={} flagged={}",
                arrow.getId(), arrow.isOnFire(), arrow.getRemainingFireTicks(), arrow.getDeltaMovement(), box,
                foundSoulFire, foundNormalFire, alreadyFlagged);
        if (foundSoulFire && !alreadyFlagged) {
            arrow.setData(AttachmentTypeRegistry.ARROW_SOUL_FLAME, true);
            CrystallographyMod.LOGGER.info("[arrowsoulfire-debug] arrow={} flagged ARROW_SOUL_FLAME=true", arrow.getId());
        }
    }

    /** ソウルファイア印の付いた矢が命中した瞬間、着火対象を直接ソウルファイア扱いにする。 */
    private static void onHitDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        boolean isArrow = source.getDirectEntity() instanceof AbstractArrow;
        // TODO(diagnostic): 原因特定のため一時的に残しているログ。切り分けできたら削除する。
        CrystallographyMod.LOGGER.info(
                "[arrowsoulfire-debug] onHitDamage type={} directEntity={} isArrow={}",
                source.getMsgId(), source.getDirectEntity(), isArrow);
        if (!(source.getDirectEntity() instanceof AbstractArrow arrow)) return;
        boolean flagged = Boolean.TRUE.equals(arrow.getData(AttachmentTypeRegistry.ARROW_SOUL_FLAME));
        CrystallographyMod.LOGGER.info(
                "[arrowsoulfire-debug] onHitDamage arrow={} flagged={} victim={}",
                arrow.getId(), flagged, event.getEntity().getName().getString());
        if (!flagged) return;

        event.getEntity().setData(AttachmentTypeRegistry.SOUL_FIRE_IGNITED, true);
    }
}
