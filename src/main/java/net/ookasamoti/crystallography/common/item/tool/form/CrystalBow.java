package net.ookasamoti.crystallography.common.item.tool.form;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbility;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.item.tool.CrystalToolLogic;
import net.ookasamoti.crystallography.common.item.tool.ICrystalTool;
import net.ookasamoti.crystallography.common.item.tool.ToolInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * ロードアウトのアクティブフォームが BOW のときの実 Item。本物の {@link BowItem} を継承するため、
 * 弦を引く・矢を放つ挙動は vanilla 実装をそのまま継承して得られる。{@code use}（引き始め）は
 * 使用不可（破損／結晶欠落）ガードを追加する。
 * <p>
 * {@code releaseUsing} だけは haste シジルの引き絞り速度補正のため vanilla の {@link BowItem}
 * 実装を丸ごとポートしている（{@code getPowerForTime} が static かつ非分解のため、部分的な
 * オーバーライドができない）。
 */
public class CrystalBow extends BowItem implements ICrystalTool {

    private static final Identifier SIGIL_HASTE_ID = Identifier.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "sigil_haste");
    /** haste シジル1個（＝ロードアウト中の結晶1個）につき引き絞り速度+15%。 */
    private static final float HASTE_DRAW_SPEED_PER_LEVEL = 0.15f;

    private final int tier;

    public CrystalBow(Properties props, int tier) {
        super(props);
        this.tier = tier;
    }

    @Override public int getTier() { return tier; }
    @Override public Kind getKind() { return Kind.WAND; }

    @Override
    public @NotNull ItemAttributeModifiers getDefaultAttributeModifiers(@NotNull ItemStack stack) {
        return CrystalToolLogic.defaultAttributeModifiers(stack);
    }

    @Override
    public <T extends LivingEntity> int damageItem(@NotNull ItemStack stack, int amount, @Nullable T entity,
                                                   @NotNull Consumer<Item> onBroken) {
        return CrystalToolLogic.damageItem(stack, amount, entity, onBroken);
    }

    @Override
    public void hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity mob, @NotNull LivingEntity attacker) {
        if (CrystalToolLogic.isUnusable(stack)) return;
        super.hurtEnemy(stack, mob, attacker);
    }

    @Override
    public boolean mineBlock(@NotNull ItemStack stack, @NotNull Level level, @NotNull BlockState state,
                             @NotNull BlockPos pos, @NotNull LivingEntity owner) {
        if (CrystalToolLogic.isUnusable(stack)) return false;
        return super.mineBlock(stack, level, state, pos, owner);
    }

    @Override
    public boolean canPerformAction(@NotNull ItemInstance stack, @NotNull ItemAbility itemAbility) {
        return !CrystalToolLogic.isUnusable(stack) && super.canPerformAction(stack, itemAbility);
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (CrystalToolLogic.isUnusable(player.getItemInHand(hand))) return InteractionResult.PASS;
        return super.use(level, player, hand);
    }

    @Override
    public boolean releaseUsing(@NotNull ItemStack itemStack, @NotNull Level level, @NotNull LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player)) return false;
        ItemStack projectile = player.getProjectile(itemStack);
        if (projectile.isEmpty()) return false;

        int timeHeld = this.getUseDuration(itemStack, entity) - remainingTime;
        timeHeld = net.neoforged.neoforge.event.EventHooks.onArrowLoose(itemStack, level, player, timeHeld, !projectile.isEmpty());
        if (timeHeld < 0) return false;

        int hasteLevel = hasteDrawSpeedLevel(itemStack, player);
        if (hasteLevel > 0) {
            timeHeld = Math.round(timeHeld * (1f + HASTE_DRAW_SPEED_PER_LEVEL * hasteLevel));
        }

        float pow = getPowerForTime(timeHeld);
        if (pow < 0.1F) return false;

        List<ItemStack> firedProjectiles = draw(itemStack, projectile, player);
        if (level instanceof ServerLevel serverLevel && !firedProjectiles.isEmpty()) {
            this.shoot(serverLevel, player, player.getUsedItemHand(), itemStack, firedProjectiles, pow * 3.0F, 1.0F, pow == 1.0F, null);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + pow * 0.5F);
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    /** アクティブロードアウトが参照する結晶のうち、haste シジルが付与されているものの個数。 */
    private int hasteDrawSpeedLevel(ItemStack stack, Player player) {
        var lo = CrystalToolLogic.getActiveLoadout(stack);
        if (lo == null || lo.unusable()) return 0;
        int tier = CrystalToolLogic.getTier(stack);
        var crystalInv = ToolInventory.get(stack, CrystalToolLogic.crystalSlotCount(tier), player.level().registryAccess());
        return CrystalToolLogic.countAttachedSigil(lo, crystalInv, SIGIL_HASTE_ID);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack,
                                @NotNull TooltipContext ctx,
                                @NotNull TooltipDisplay display,
                                @NotNull Consumer<Component> out,
                                @NotNull TooltipFlag flag) {
        CrystalToolLogic.appendHoverText(stack, ctx, display, out, flag);
        super.appendHoverText(stack, ctx, display, out, flag);
    }
}
