package net.ookasamoti.crystallography.common.item.tool;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * ender トレイト（旧 swap）：遠距離攻撃のヒットした場所にプレイヤーをテレポートさせる
 * （エンダーパールの効果）、mobにヒットした場合は相互の位置を入れ替える。
 * <p>
 * {@link Projectile} は発射に使われた武器アイテムを保持しないため、着弾時点での射手の
 * メイン/オフハンドの持ち物を確認する近似で判定する（射手が持ち替えていた場合など、厳密には
 * 発射時の武器と一致しない可能性があるが、実用上は十分）。
 */
public final class CrystalTraitProjectileHooks {
    private CrystalTraitProjectileHooks() {}

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(CrystalTraitProjectileHooks::onProjectileImpact);
    }

    private static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile.level() instanceof ServerLevel level)) return;
        if (!(projectile.getOwner() instanceof LivingEntity shooter)) return;
        if (weaponWithEnder(shooter) == null) return;

        HitResult hit = event.getRayTraceResult();
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
            swapPositions(level, shooter, target);
        } else if (hit instanceof BlockHitResult) {
            teleportToImpact(level, shooter, projectile.position());
        }
    }

    private static @Nullable ItemStack weaponWithEnder(LivingEntity shooter) {
        for (ItemStack held : List.of(shooter.getMainHandItem(), shooter.getOffhandItem())) {
            if (!(held.getItem() instanceof ICrystalTool)) continue;
            var lo = CrystalToolLogic.getActiveLoadout(held);
            if (lo == null || lo.unusable()) continue;

            int tier = CrystalToolLogic.getTier(held);
            var crystalInv = ToolInventory.get(held, CrystalToolLogic.crystalSlotCount(tier), shooter.level().registryAccess());
            if (CrystalTraitLogic.loadoutHasTrait(lo, crystalInv, CrystalTraitLogic.ENDER)) return held;
        }
        return null;
    }

    private static void teleportToImpact(ServerLevel level, LivingEntity shooter, Vec3 impactPos) {
        spawnPortalParticles(level, shooter.position());
        shooter.teleportTo(impactPos.x, impactPos.y, impactPos.z);
        shooter.resetFallDistance();
        spawnPortalParticles(level, impactPos);
        playTeleportSound(level, impactPos);
    }

    private static void swapPositions(ServerLevel level, LivingEntity shooter, LivingEntity target) {
        Vec3 shooterPos = shooter.position();
        Vec3 targetPos = target.position();
        spawnPortalParticles(level, shooterPos);
        spawnPortalParticles(level, targetPos);
        shooter.teleportTo(targetPos.x, targetPos.y, targetPos.z);
        target.teleportTo(shooterPos.x, shooterPos.y, shooterPos.z);
        shooter.resetFallDistance();
        target.resetFallDistance();
        playTeleportSound(level, shooterPos);
        playTeleportSound(level, targetPos);
    }

    private static void spawnPortalParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y + 1.0, pos.z, 32, 0.5, 1.0, 0.5, 0.0);
    }

    private static void playTeleportSound(ServerLevel level, Vec3 pos) {
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}
