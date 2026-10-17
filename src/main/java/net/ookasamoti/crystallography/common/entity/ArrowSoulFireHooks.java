package net.ookasamoti.crystallography.common.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.ookasamoti.crystallography.setup.AttachmentTypeRegistry;

/**
 * バニラの矢は炎を通過すると自身が着火し({@code isOnFire()})、命中した相手も着火させる
 * ({@code AbstractArrow#onHitEntity} が {@code entity.igniteForSeconds} を呼ぶ)。矢が
 * ソウルファイアを通過したかどうかは {@link net.ookasamoti.crystallography.mixin.BaseFireBlockMixin}
 * が矢自身の {@link AttachmentTypeRegistry#SOUL_FIRE_IGNITED} に直接書き込む(矢は
 * {@code LivingEntity} ではないが、mixinのフック先である{@code entityInside}はあらゆる
 * {@code Entity}を対象に呼ばれるため、同じ仕組みがそのまま使える)。
 * <p>
 * このクラスの役目は、その印の付いた矢が命中した瞬間、着火対象にも同じ色を伝播させることだけ。
 */
public final class ArrowSoulFireHooks {
    private ArrowSoulFireHooks() {}

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(ArrowSoulFireHooks::onHitDamage);
    }

    /** ソウルファイアを通過した矢が命中した瞬間、着火対象を直接ソウルファイア扱いにする。 */
    private static void onHitDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getDirectEntity() instanceof AbstractArrow arrow)) return;
        if (!Boolean.TRUE.equals(arrow.getData(AttachmentTypeRegistry.SOUL_FIRE_IGNITED))) return;

        event.getEntity().setData(AttachmentTypeRegistry.SOUL_FIRE_IGNITED, true);
    }
}
