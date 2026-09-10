package net.ookasamoti.crystallography.common.entity;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.ookasamoti.crystallography.setup.AttachmentTypeRegistry;

/**
 * バニラは「何に着火されたか」を区別せず、燃えている間ずっと同じ(オレンジの)炎エフェクトを
 * 描画する。ソウルファイア(青い炎)で着火されたmobだけ見た目を変えるため、毎tick足元の
 * ブロックを見て {@link AttachmentTypeRegistry#SOUL_FIRE_IGNITED} を更新する。
 * <p>
 * バニラに「このブロックが着火元だ」というフックは存在しない(ソウルファイア/通常の焚き火
 * どちらも {@code BaseFireBlock.fireIgnite} という同じ経路を通る)ため、足元のブロックが
 * {@code minecraft:soul_fire} ならソウルファイア起源、{@code minecraft:fire} なら通常の
 * 炎起源とみなして都度上書きする、というヒューリスティックで判定する。どちらでもない間
 * (炎から離れて燃え続けている間)は直前の判定を維持し、燃え尽きたら解除する。
 * <p>
 * また、ソウルファイアブロック自体は接触ダメージがバニラの通常炎(1.0)の2倍(2.0)に
 * 設定されているのに対し、着火後の継続燃焼ダメージ({@code minecraft:on_fire}、1秒毎に固定
 * 1.0)は着火元を区別しない。ここでも同じ「ソウルファイア=2倍」を再現するため、
 * {@link AttachmentTypeRegistry#SOUL_FIRE_IGNITED} が立っている間の on_fire ダメージを倍加する。
 */
public final class SoulFireIgnitionHooks {
    private SoulFireIgnitionHooks() {}

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(SoulFireIgnitionHooks::onEntityTick);
        NeoForge.EVENT_BUS.addListener(SoulFireIgnitionHooks::onIncomingDamage);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypes.ON_FIRE)) return;
        LivingEntity victim = event.getEntity();
        if (Boolean.TRUE.equals(victim.getData(AttachmentTypeRegistry.SOUL_FIRE_IGNITED))) {
            event.setAmount(event.getAmount() * 2.0F);
        }
    }

    private static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living.level().isClientSide()) return;

        int fireTicks = living.getRemainingFireTicks();
        boolean current = living.getData(AttachmentTypeRegistry.SOUL_FIRE_IGNITED);

        if (fireTicks <= 0) {
            if (current) living.setData(AttachmentTypeRegistry.SOUL_FIRE_IGNITED, false);
            return;
        }

        BlockState feet = living.level().getBlockState(living.blockPosition());
        if (feet.is(Blocks.SOUL_FIRE)) {
            if (!current) living.setData(AttachmentTypeRegistry.SOUL_FIRE_IGNITED, true);
        } else if (feet.is(Blocks.FIRE)) {
            if (current) living.setData(AttachmentTypeRegistry.SOUL_FIRE_IGNITED, false);
        }
    }
}
