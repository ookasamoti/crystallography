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
 * {@link AttachmentTypeRegistry#SOUL_FIRE_IGNITED} を更新する。
 * <p>
 * バニラに「このブロックが着火元だ」という専用イベントは存在しないが、{@code BaseFireBlock}は
 * 接触ダメージの量そのものでソウルファイア(2.0)と通常の炎(1.0)を区別している
 * ({@code SoulFireBlock}のコンストラクタが{@code fireDamage=2.0}を渡す一方、{@code FireBlock}は
 * 1.0)。この区別はダメージ確定前の生値である{@link LivingIncomingDamageEvent#getOriginalAmount()}
 * にそのまま残っているため、ブロック種別を自前で調べ直す(足元/当たり判定のブロックスキャン)
 * のではなく、バニラ自身がこの瞬間に持っている「どちらの炎か」という情報をそのまま読む。
 * <p>
 * 注意：クリエイティブモードや炎耐性など、炎ダメージそのものに無敵なエンティティは
 * {@code hurt()}の無敵判定でこのダメージイベントが発火せず、判定が一度も走らない
 * （着火=remainingFireTicksの更新自体は無敵判定より前・無条件に行われるため、見た目は
 * 燃えるのに判定だけ素通りし、常に通常の炎として表示される）。サバイバルでは問題なく
 * 動作する(確認済み)ため、無敵エンティティ向けのフォールバックはあえて持たない。
 * <p>
 * また、着火後の継続燃焼ダメージ({@code minecraft:on_fire}、1秒毎に固定1.0)はバニラだと
 * 着火元を区別しないため、ここでも同じ「ソウルファイア=2倍」を再現する。
 */
public final class SoulFireIgnitionHooks {
    private SoulFireIgnitionHooks() {}

    /** {@code SoulFireBlock}の接触ダメージ(2.0)と{@code FireBlock}の接触ダメージ(1.0)の中間値。 */
    private static final float SOUL_FIRE_CONTACT_DAMAGE_THRESHOLD = 1.5f;

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(SoulFireIgnitionHooks::onFireContact);
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

    /** 着火元の判定本体：{@code minecraft:in_fire}接触ダメージの生値でソウル/通常を直接判定する。 */
    private static void onFireContact(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypes.IN_FIRE)) return;
        LivingEntity victim = event.getEntity();
        boolean soulFire = event.getOriginalAmount() > SOUL_FIRE_CONTACT_DAMAGE_THRESHOLD;
        boolean current = victim.getData(AttachmentTypeRegistry.SOUL_FIRE_IGNITED);
        if (soulFire != current) {
            victim.setData(AttachmentTypeRegistry.SOUL_FIRE_IGNITED, soulFire);
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
