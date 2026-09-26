package net.ookasamoti.crystallography.common.item.tool;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.item.tool.component.ToolForm;
import net.ookasamoti.crystallography.setup.AttachmentTypeRegistry;

/**
 * 延焼時間を延長する2つの効果の実際の適用箇所：blazing トレイト（延焼時間1.5倍）と、
 * flame シジルの弓/クロスボウ部分（バニラのFlameエンチャントは着火時間が固定100tickでレベルに
 * 関わらず変化しないため、「Lv2」を実現するにはこの延長処理が必須）。
 * <p>
 * {@code SoulFireIgnitionHooks} と同じ「毎tickの状態変化を見るヒューリスティック」を使う。理由：
 * バニラの火属性・フレイムエンチャントによる着火（{@code LivingEntity#igniteForSeconds}）は、
 * 攻撃側の {@code Player#attack}／矢の命中処理内で、ダメージ確定処理（{@code hurt}、
 * {@link LivingIncomingDamageEvent} もこの中で発火する）が完全に終わった"後"に実行される
 * 後処理の一つなので、{@link LivingIncomingDamageEvent} の時点ではまだ着火前であり、その場で
 * 延焼tickを直接書き換えても後から来る本物の着火処理に上書きされてしまう。
 * <p>
 * そこで {@link LivingIncomingDamageEvent} では「この被害者は次に着火したらこの倍率にする」という
 * 保留倍率（{@link AttachmentTypeRegistry#FIRE_DURATION_PENDING_MULTIPLIER}）だけを立てておき、
 * 同じ tick 内の {@link EntityTickEvent.Post}（＝本物の着火処理も含め、その tick の全処理が
 * 終わった後）で実際に延焼tickへ乗算する。保留倍率は検査した時点で必ずクリアするため、
 * 着火が間に合わなかった場合に後から無関係の着火へ誤爆することはない。
 */
public final class CrystalTraitFireHooks {
    private CrystalTraitFireHooks() {}

    /** blazing（効果時間延長グループ）：Lvごとの延焼時間倍率の増分。 */
    private static final float BLAZING_FIRE_DURATION_BONUS_PER_LEVEL = 0.5f;
    /** flame シジル：バニラのFlame(弓/クロスボウ)は着火時間固定のため、Lv2相当として2倍にする。 */
    private static final float FLAME_SIGIL_FIRE_DURATION_MULTIPLIER = 2.0f;
    private static final Identifier SIGIL_FLAME_ID = Identifier.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "sigil_flame");

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

        float multiplier = 1.0f;
        int blazingLevel = CrystalTraitLogic.traitLevel(lo, crystalInv, CrystalTraitLogic.BLAZING, CrystalTraitLogic.SCALING_MAX_LEVEL);
        if (blazingLevel > 0) {
            multiplier = Math.max(multiplier, 1.0f + BLAZING_FIRE_DURATION_BONUS_PER_LEVEL * blazingLevel);
        }
        if ((lo.form() == ToolForm.BOW || lo.form() == ToolForm.CROSSBOW)
                && CrystalToolLogic.countAttachedSigil(lo, crystalInv, SIGIL_FLAME_ID) > 0) {
            multiplier = Math.max(multiplier, FLAME_SIGIL_FIRE_DURATION_MULTIPLIER);
        }

        if (multiplier > 1.0f) {
            event.getEntity().setData(AttachmentTypeRegistry.FIRE_DURATION_PENDING_MULTIPLIER, multiplier);
        }
    }

    private static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living.level().isClientSide()) return;

        float multiplier = living.getData(AttachmentTypeRegistry.FIRE_DURATION_PENDING_MULTIPLIER);
        if (multiplier <= 1.0f) return;

        living.setData(AttachmentTypeRegistry.FIRE_DURATION_PENDING_MULTIPLIER, 1.0f);
        int fireTicks = living.getRemainingFireTicks();
        if (fireTicks > 0) {
            living.setRemainingFireTicks((int) (fireTicks * multiplier));
        }
    }
}
