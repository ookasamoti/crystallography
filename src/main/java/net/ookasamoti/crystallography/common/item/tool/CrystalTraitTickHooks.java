package net.ookasamoti.crystallography.common.item.tool;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ookasamoti.crystallography.common.item.armor.AmuletStatLogic;
import net.ookasamoti.crystallography.common.item.armor.IAmuletItem;

import java.util.List;

/**
 * 毎tick発動する効果の適用箇所。2種類ある：
 * <ul>
 *   <li>結晶の固有アビリティのうち常時発動するもの（{@link CrystalTraitLogic} の「常時効果系」）。
 *       手に持っている（メイン/オフハンド問わず）だけで発動する点が他の戦闘・採掘フック
 *       （アクティブロードアウトが「そのフォームで使われた瞬間」にのみ発動）と異なる。</li>
 *   <li>アミュレット（防具）に付与されたシジルの {@code worn_effect}（水棲の水中呼吸等）。
 *       装備スロットに装着されているだけで発動する（{@link AmuletStatLogic#applyWornEffects}）。</li>
 * </ul>
 * magnetism は Wiki 上も想定効果の記載が無かったトレイトのため、一般的な「磁力」のイメージ
 * （周囲のドロップアイテム・経験値玉を引き寄せる）で暫定的に実装している。
 */
public final class CrystalTraitTickHooks {
    private CrystalTraitTickHooks() {}

    /** magnetism（効果範囲拡張グループ）：Lvごとの基礎半径。 */
    private static final double MAGNETISM_BASE_RADIUS = 6.0;
    private static final double MAGNETISM_PULL_SPEED = 0.35;
    private static final EquipmentSlot[] ARMOR_SLOTS =
            {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(CrystalTraitTickHooks::onPlayerTick);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        int magnetismLevel = 0;
        for (ItemStack held : List.of(player.getMainHandItem(), player.getOffhandItem())) {
            if (!(held.getItem() instanceof ICrystalTool)) continue;
            var lo = CrystalToolLogic.getActiveLoadout(held);
            if (lo == null || lo.unusable()) continue;

            int tier = CrystalToolLogic.getTier(held);
            var crystalInv = ToolInventory.get(held, CrystalToolLogic.crystalSlotCount(tier), player.level().registryAccess());
            magnetismLevel = Math.max(magnetismLevel,
                    CrystalTraitLogic.traitLevel(lo, crystalInv, CrystalTraitLogic.MAGNETISM, CrystalTraitLogic.SCALING_MAX_LEVEL));
        }

        if (magnetismLevel > 0) pullNearbyPickups(player, MAGNETISM_BASE_RADIUS * magnetismLevel);

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack worn = player.getItemBySlot(slot);
            if (worn.getItem() instanceof IAmuletItem) {
                AmuletStatLogic.applyWornEffects(worn, slot, player);
            }
        }
    }

    private static void pullNearbyPickups(Player player, double radius) {
        AABB area = player.getBoundingBox().inflate(radius);
        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, area)) {
            pullToward(player, item);
        }
        for (ExperienceOrb orb : player.level().getEntitiesOfClass(ExperienceOrb.class, area)) {
            pullToward(player, orb);
        }
    }

    private static void pullToward(Player player, Entity target) {
        Vec3 toPlayer = player.position().subtract(target.position());
        double dist = toPlayer.length();
        if (dist < 0.5) return;
        Vec3 pull = toPlayer.scale(MAGNETISM_PULL_SPEED / dist);
        target.setDeltaMovement(target.getDeltaMovement().add(pull));
        target.hurtMarked = true;
    }
}
