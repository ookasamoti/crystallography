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
 * potion_amplify / potion_duration は、この場所で新たに付与する wither / levitation にのみ
 * 効果を及ぼす（{@link SigilCombatHooks} が付与するシジル由来の効果までは触らない、意図的な
 * スコープ限定。両者は別クラス・別リスナーのため、互いのeffectリストを跨いで調整するのは
 * 複雑さに見合わないと判断した）。
 */
public final class CrystalTraitCombatHooks {
    private CrystalTraitCombatHooks() {}

    private static final float LIFESTEAL_HEAL_FRACTION = 0.10f;
    private static final float ORDER_BONUS_DAMAGE = 3.0f;
    /** sonic：ダメージの一部を防具軽減の対象外にする（0.3 = 通常の防具軽減量の30%を素通しする）。 */
    private static final float SONIC_ARMOR_PIERCE_FRACTION = 0.3f;
    private static final float BREEZING_EXTRA_KNOCKBACK = 0.4f;
    private static final int WITHER_DURATION_TICKS = 100;
    private static final int LEVITATION_DURATION_TICKS = 20;
    /** potion_amplify：このクラスが新たに付与する効果（wither/levitation）の増幅レベルへの加算。 */
    private static final int POTION_AMPLIFY_BONUS = 1;
    /** potion_duration：このクラスが新たに付与する効果（wither/levitation）の持続時間への倍率。 */
    private static final float POTION_DURATION_MULTIPLIER = 1.5f;

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

        if (CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.LIFESTEAL)) {
            attacker.heal(event.getAmount() * LIFESTEAL_HEAL_FRACTION);
        }

        if (CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.BREEZING)) {
            float yRotRad = attacker.getYRot() * (float) (Math.PI / 180.0);
            victim.knockback(BREEZING_EXTRA_KNOCKBACK, Mth.sin(yRotRad), -Mth.cos(yRotRad));
        }

        int amplifierBonus = CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.POTION_AMPLIFY)
                ? POTION_AMPLIFY_BONUS : 0;
        float durationMultiplier = CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.POTION_DURATION)
                ? POTION_DURATION_MULTIPLIER : 1f;

        if (CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.WITHER)) {
            victim.addEffect(new MobEffectInstance(MobEffects.WITHER,
                    (int) (WITHER_DURATION_TICKS * durationMultiplier), amplifierBonus), attacker);
        }
        if (CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.LEVITATION)) {
            victim.addEffect(new MobEffectInstance(MobEffects.LEVITATION,
                    (int) (LEVITATION_DURATION_TICKS * durationMultiplier), amplifierBonus), attacker);
        }
    }
}
