package net.ookasamoti.crystallography.common.item.tool;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 結晶の固有アビリティのうち、攻撃ヒット時にのみ発現するもの（{@link CrystalTraitLogic} の
 * 「戦闘フック系」）の実際の適用箇所。{@link SigilCombatHooks} と同じ理由
 * （{@code hurtEnemy} は既にダメージ確定後の後処理フックなので間に合わない）で、ダメージ確定前の
 * {@link LivingIncomingDamageEvent} を購読する。
 * <p>
 * amplify（旧 potion_amplify）/ duration（旧 potion_duration）は、このクラスが新たに付与する
 * wither/levitation だけでなく、{@link SigilCombatHooks} が付与するシジル由来の命中時効果にも
 * 適用される（{@link CrystalTraitLogic#amplifyBonus}/{@link CrystalTraitLogic#durationMultiplier}
 * として共有ロジックに切り出し、両クラスから参照する）。
 */
public final class CrystalTraitCombatHooks {
    private CrystalTraitCombatHooks() {}

    private static final float LIFESTEAL_HEAL_FRACTION_PER_LEVEL = 0.10f;
    private static final float ORDER_BONUS_DAMAGE = 3.0f;
    /** sonic：ダメージの一部を防具軽減の対象外にする（0.3 = 通常の防具軽減量の30%を素通しする）。 */
    private static final float SONIC_ARMOR_PIERCE_FRACTION = 0.3f;
    private static final float BREEZING_EXTRA_KNOCKBACK_PER_LEVEL = 0.4f;
    private static final int WITHER_DURATION_TICKS_PER_LEVEL = 100;
    private static final int LEVITATION_DURATION_TICKS_PER_LEVEL = 20;

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(CrystalTraitCombatHooks::onIncomingDamage);
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

        if (CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.ORDER)
                && victim.getType().builtInRegistryHolder().is(EntityTypeTags.ILLAGER)) {
            event.setAmount(event.getAmount() + ORDER_BONUS_DAMAGE);
        }

        if (CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.SONIC)) {
            // 防具によるダメージ軽減量そのものを一部素通しする＝真の意味での防具貫通。
            event.addReductionModifier(DamageContainer.Reduction.ARMOR,
                    (container, reductionIn) -> reductionIn * (1f - SONIC_ARMOR_PIERCE_FRACTION));
        }

        // lifesteal（補正強化グループ）：Lvに比例して回復割合が増える。
        int lifestealLevel = CrystalTraitLogic.traitLevel(lo, crystalInv, CrystalTraitLogic.LIFESTEAL, CrystalTraitLogic.SCALING_MAX_LEVEL);
        if (lifestealLevel > 0) {
            attacker.heal(event.getAmount() * LIFESTEAL_HEAL_FRACTION_PER_LEVEL * lifestealLevel);
        }

        // breezing（補正強化グループ）：Lvに比例して追加ノックバックが強くなる。
        int breezingLevel = CrystalTraitLogic.traitLevel(lo, crystalInv, CrystalTraitLogic.BREEZING, CrystalTraitLogic.SCALING_MAX_LEVEL);
        if (breezingLevel > 0) {
            float yRotRad = attacker.getYRot() * (float) (Math.PI / 180.0);
            victim.knockback(BREEZING_EXTRA_KNOCKBACK_PER_LEVEL * breezingLevel, Mth.sin(yRotRad), -Mth.cos(yRotRad));
        }

        int amplifierBonus = CrystalTraitLogic.amplifyBonus(lo, crystalInv);
        float durationMultiplier = CrystalTraitLogic.durationMultiplier(lo, crystalInv);

        // wither/levitation（効果時間延長グループ）：Lvに比例して持続時間が延びる。
        int witherLevel = CrystalTraitLogic.traitLevel(lo, crystalInv, CrystalTraitLogic.WITHER, CrystalTraitLogic.SCALING_MAX_LEVEL);
        if (witherLevel > 0) {
            victim.addEffect(new MobEffectInstance(MobEffects.WITHER,
                    (int) (WITHER_DURATION_TICKS_PER_LEVEL * witherLevel * durationMultiplier), amplifierBonus), attacker);
        }
        int levitationLevel = CrystalTraitLogic.traitLevel(lo, crystalInv, CrystalTraitLogic.LEVITATION, CrystalTraitLogic.SCALING_MAX_LEVEL);
        if (levitationLevel > 0) {
            victim.addEffect(new MobEffectInstance(MobEffects.LEVITATION,
                    (int) (LEVITATION_DURATION_TICKS_PER_LEVEL * levitationLevel * durationMultiplier), amplifierBonus), attacker);
        }
    }
}
