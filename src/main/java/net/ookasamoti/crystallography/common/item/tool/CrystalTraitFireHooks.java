package net.ookasamoti.crystallography.common.item.tool;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.ookasamoti.crystallography.setup.AttachmentTypeRegistry;

/**
 * blazing トレイト（延焼時間が1.5倍になる）の実際の適用箇所。{@code SoulFireIgnitionHooks} と
 * 同じ「毎tickの状態変化を見るヒューリスティック」を使う。理由：バニラの火属性エンチャントに
 * よる着火（{@code LivingEntity#igniteForSeconds}）は、攻撃側の {@code Player#attack} 内で
 * ダメージ確定処理（{@code hurt}、{@link LivingIncomingDamageEvent} もこの中で発火する）が
 * 完全に終わった"後"に実行される後処理エンチャント効果の一つなので、
 * {@link LivingIncomingDamageEvent} の時点ではまだ着火前であり、その場で延焼tickを直接
 * 書き換えても後から来る本物の着火処理に上書きされてしまう。
 * <p>
 * そこで {@link LivingIncomingDamageEvent} では「この被害者は次に着火したら1.5倍にする」という
 * 保留フラグ（{@link AttachmentTypeRegistry#BLAZING_PENDING}）だけを立てておき、
 * 同じ tick 内の {@link EntityTickEvent.Post}（＝本物の着火処理も含め、その tick の全処理が
 * 終わった後）で実際に延焼tickを1.5倍にする。フラグは検査した時点で必ずクリアするため、
 * 着火が間に合わなかった場合に後から無関係の着火へ誤爆することはない。
 */
public final class CrystalTraitFireHooks {
    private CrystalTraitFireHooks() {}

    private static final float BLAZING_FIRE_DURATION_MULTIPLIER = 1.5f;

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(CrystalTraitFireHooks::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(CrystalTraitFireHooks::onEntityTick);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        ItemStack weapon = attacker.getMainHandItem();
        if (!(weapon.getItem() instanceof ICrystalTool)) return;

        var lo = CrystalToolLogic.getActiveLoadout(weapon);
        if (lo == null || lo.unusable()) return;

        int tier = CrystalToolLogic.getTier(weapon);
        var crystalInv = ToolInventory.get(weapon, CrystalToolLogic.crystalSlotCount(tier), attacker.level().registryAccess());
        if (CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.BLAZING)) {
            event.getEntity().setData(AttachmentTypeRegistry.BLAZING_PENDING, true);
        }
    }

    private static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living.level().isClientSide()) return;
        if (!Boolean.TRUE.equals(living.getData(AttachmentTypeRegistry.BLAZING_PENDING))) return;

        living.setData(AttachmentTypeRegistry.BLAZING_PENDING, false);
        int fireTicks = living.getRemainingFireTicks();
        if (fireTicks > 0) {
            living.setRemainingFireTicks((int) (fireTicks * BLAZING_FIRE_DURATION_MULTIPLIER));
        }
    }
}
