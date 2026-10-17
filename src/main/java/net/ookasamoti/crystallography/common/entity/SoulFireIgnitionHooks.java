package net.ookasamoti.crystallography.common.entity;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.ookasamoti.crystallography.setup.AttachmentTypeRegistry;

/**
 * バニラは「何に着火されたか」を区別せず、燃えている間ずっと同じ(オレンジの)炎エフェクトを
 * 描画する。ソウルファイア(青い炎)で着火されたmobだけ見た目を変えるため、
 * {@link AttachmentTypeRegistry#SOUL_FIRE_IGNITED} を使って色を分ける。
 * <p>
 * 判定本体は {@link net.ookasamoti.crystallography.mixin.BaseFireBlockMixin} が
 * {@code BaseFireBlock#entityInside} に直接フックして行っている(接触した実際のBlockStateを
 * その場で見られるため、ダメージイベントや毎tickのブロックスキャンで間接的に推測していた
 * 旧実装より確実)。このクラスが残り二つの役目を持つ：
 * <ol>
 *   <li>燃え尽きたら(延焼tickが切れたら)フラグを解除する後片付け。</li>
 *   <li>継続燃焼ダメージ({@code minecraft:on_fire}、1秒毎に固定1.0)はバニラだと着火元を
 *       区別しないため、ソウルファイア起源なら2倍にする({@code SoulFireBlock}自身の接触
 *       ダメージが通常の2倍なのと同じ比率を再現)。</li>
 * </ol>
 */
public final class SoulFireIgnitionHooks {
    private SoulFireIgnitionHooks() {}

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(SoulFireIgnitionHooks::onOnFireDamage);
        NeoForge.EVENT_BUS.addListener(SoulFireIgnitionHooks::onEntityTick);
    }

    private static void onOnFireDamage(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypes.ON_FIRE)) return;
        LivingEntity victim = event.getEntity();
        if (Boolean.TRUE.equals(victim.getData(AttachmentTypeRegistry.SOUL_FIRE_IGNITED))) {
            event.setAmount(event.getAmount() * 2.0F);
        }
    }

    /** 燃え尽きたら(延焼tickが切れたら)フラグを解除する。発火元イベントが来ない間の後片付け。 */
    private static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living.level().isClientSide()) return;

        if (living.getRemainingFireTicks() <= 0 && Boolean.TRUE.equals(living.getData(AttachmentTypeRegistry.SOUL_FIRE_IGNITED))) {
            living.setData(AttachmentTypeRegistry.SOUL_FIRE_IGNITED, false);
        }
    }
}
