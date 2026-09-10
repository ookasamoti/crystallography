package net.ookasamoti.crystallography.common.item.tool;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.ookasamoti.crystallography.data.SigilRegistry;
import net.ookasamoti.crystallography.setup.DataComponentsRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * 「対象タグ一致時のみ加算」系シジル（浄化＝アンデッド特攻など）の実際の適用箇所。
 * このmodのツールはダメージを vanilla の属性（{@code Attributes.ATTACK_DAMAGE}）経由の
 * 無条件加算で扱っており、条件付きボーナスはここでしか正しく実装できない
 * （{@code hurtEnemy} は既にダメージ確定後に呼ばれる後処理フックのため、そこでは間に合わない）。
 */
public final class SigilCombatHooks {
    private SigilCombatHooks() {}

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(SigilCombatHooks::onIncomingDamage);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        ItemStack weapon = attacker.getMainHandItem();
        if (!(weapon.getItem() instanceof ICrystalTool)) return;

        var lo = CrystalToolLogic.getActiveLoadout(weapon);
        if (lo == null || lo.unusable()) return;

        LivingEntity victim = event.getEntity();
        int tier = CrystalToolLogic.getTier(weapon);
        var crystalInv = ToolInventory.get(weapon, CrystalToolLogic.crystalSlotCount(tier), attacker.level().registryAccess());

        float bonus = 0f;
        List<MobEffectInstance> onHitEffects = new ArrayList<>();
        for (int idx : lo.crystalIndices()) {
            if (idx < 0 || idx >= crystalInv.size()) continue;
            var crystal = crystalInv.getResource(idx).toStack(crystalInv.getAmountAsInt(idx));
            if (crystal.isEmpty()) continue;

            var attached = crystal.get(DataComponentsRegistry.ATTACHED_SIGILS.get());
            if (attached == null) continue;

            for (var sigilId : attached.sigils()) {
                var def = SigilRegistry.get(sigilId).orElse(null);
                if (def == null || !def.forms().contains(lo.form())) continue;
                // targetTag が無ければ無条件（凍結の鈍化等）、あれば一致時のみ（浄化＝アンデッド等）。
                boolean applies = def.targetTag().isEmpty()
                        || victim.getType().builtInRegistryHolder().is(def.targetTag().get());
                if (applies) {
                    bonus += def.conditionalDamageBonus();
                    def.onHitEffect().ifPresent(effect ->
                            onHitEffects.add(new MobEffectInstance(effect, def.onHitDuration(), def.onHitAmplifier())));
                }
            }
        }

        if (bonus > 0f) event.setAmount(event.getAmount() + bonus);
        for (var effect : onHitEffects) victim.addEffect(effect, attacker);
    }
}
