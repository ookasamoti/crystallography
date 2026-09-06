package net.ookasamoti.crystallography.common.item.tool;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.ookasamoti.crystallography.common.item.tool.component.ToolForm;
import net.ookasamoti.crystallography.common.item.tool.component.ToolLoadout;
import net.ookasamoti.crystallography.common.item.tool.component.ToolLoadoutList;
import net.ookasamoti.crystallography.common.item.tool.component.ToolStats;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * ロードアウト未登録の「素の状態」のツールベース（{@link ToolRod}/{@link ToolWand}の共通親）。
 * <p>
 * 実際のロジックは全て {@link CrystalToolLogic} に集約されている（フォーム確定後の {@code Crystal*}
 * 系アイテムとも共有するため）。このクラスは {@link Item} が要求するインスタンスメソッドを
 * {@link CrystalToolLogic} へ1行で委譲するだけの薄いシムであり、既存の {@code ToolBase.xxx(...)}
 * という静的呼び出し形も互換性のため維持している。
 */
public abstract class ToolBase extends Item implements ICrystalTool {

    protected final int tier;

    protected ToolBase(Properties props, int tier) {
        super(props);
        this.tier = tier;
    }

    @Override
    public int getTier() { return tier; }

    // ---- 静的ヘルパー（互換のための委譲。実装は CrystalToolLogic 参照） ----

    public static int crystalSlotCount(int tier) { return CrystalToolLogic.crystalSlotCount(tier); }
    public static int maxLoadouts(int tier) { return CrystalToolLogic.maxLoadouts(tier); }

    public static final String ORE_CATEGORY = CrystalToolLogic.ORE_CATEGORY;

    public static int findOreCrystalIndex(int[] crystalIndices, ResourceHandler<ItemResource> crystalInv) {
        return CrystalToolLogic.findOreCrystalIndex(crystalIndices, crystalInv);
    }

    public static boolean isRepairMaterial(int tier, ItemStack stack) { return CrystalToolLogic.isRepairMaterial(tier, stack); }
    public static int repairAmountFor(int tier) { return CrystalToolLogic.repairAmountFor(tier); }

    public static ToolLoadoutList getLoadout(ItemStack stack) { return CrystalToolLogic.getLoadout(stack); }
    public static int getActiveIndex(ItemStack stack) { return CrystalToolLogic.getActiveIndex(stack); }
    public static void setActiveIndex(ItemStack stack, int idx) { CrystalToolLogic.setActiveIndex(stack, idx); }
    public static @Nullable ToolLoadout getActiveLoadout(ItemStack stack) { return CrystalToolLogic.getActiveLoadout(stack); }
    public static boolean addLoadout(ItemStack stack, ToolLoadout add) { return CrystalToolLogic.addLoadout(stack, add); }
    public static boolean setLoadout(ItemStack stack, ToolLoadout add) { return CrystalToolLogic.setLoadout(stack, add); }
    public static void removeLoadout(ItemStack stack, int slotIndex) { CrystalToolLogic.removeLoadout(stack, slotIndex); }
    public static void swapLoadouts(ItemStack stack, int slotA, int slotB) { CrystalToolLogic.swapLoadouts(stack, slotA, slotB); }

    public static @Nullable ToolLoadout getDraftLoadout(ItemStack stack) { return CrystalToolLogic.getDraftLoadout(stack); }
    public static void setDraftLoadout(ItemStack stack, ToolLoadout draft) { CrystalToolLogic.setDraftLoadout(stack, draft); }
    public static void clearDraftLoadout(ItemStack stack) { CrystalToolLogic.clearDraftLoadout(stack); }

    public static ToolStats buildStats(ToolForm form, int rodTier, int[] crystalIndices, ResourceHandler<ItemResource> crystalInv) {
        return CrystalToolLogic.buildStats(form, rodTier, crystalIndices, crystalInv);
    }

    public static ToolForm upgradeFormForCrystals(ToolForm form, int[] crystalIndices, ResourceHandler<ItemResource> crystalInv) {
        return CrystalToolLogic.upgradeFormForCrystals(form, crystalIndices, crystalInv);
    }

    public static @Nullable ToolStats getActiveStats(ItemStack stack) { return CrystalToolLogic.getActiveStats(stack); }
    public static void applyComputedStats(ItemStack stack) { CrystalToolLogic.applyComputedStats(stack); }
    public static void updateActiveCurrentDurability(ItemStack stack, int newCurrent) { CrystalToolLogic.updateActiveCurrentDurability(stack, newCurrent); }

    public static void reconcileLoadouts(ItemStack tool, int rodTier, ResourceHandler<ItemResource> crystalInv) {
        CrystalToolLogic.reconcileLoadouts(tool, rodTier, crystalInv);
    }

    // ---- Item overrides（CrystalToolLogic への委譲） ----

    @Override
    public @NotNull ItemAttributeModifiers getDefaultAttributeModifiers(@NotNull ItemStack stack) {
        return CrystalToolLogic.defaultAttributeModifiers(stack);
    }

    // getDestroySpeed はオーバーライドしない：バニラ既定の Item#getDestroySpeed が
    // DataComponents.TOOL（applyComputedStats が form の採掘対象タグ付きで構築）を読んで
    // 対象タグに一致するブロックだけ speed を返し、それ以外は Tool.defaultMiningSpeed(=1.0=素手相当)
    // にフォールバックする。

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
    public boolean mineBlock(@NotNull ItemStack stack, @NotNull net.minecraft.world.level.Level level,
                             @NotNull net.minecraft.world.level.block.state.BlockState state,
                             @NotNull net.minecraft.core.BlockPos pos, @NotNull LivingEntity owner) {
        if (CrystalToolLogic.isUnusable(stack)) return false;
        return super.mineBlock(stack, level, state, pos, owner);
    }

    @Override
    public boolean canPerformAction(@NotNull ItemInstance stack, @NotNull ItemAbility itemAbility) {
        return CrystalToolLogic.canPerformAction(stack, itemAbility);
    }

    /**
     * NOTE: フォーム確定前の素の状態にはアクティブロードアウトが乗らない想定（登録と同時に
     * 対応する {@code Crystal*} アイテムへ retarget されるため）。このオーバーライドは
     * 移行期間の保険であり、Phase 7 で per-form クラスへの移行が完了すれば実質到達しなくなる。
     */
    @Override
    public @NotNull net.minecraft.world.InteractionResult useOn(@NotNull net.minecraft.world.item.context.UseOnContext context) {
        net.minecraft.world.InteractionResult result = CrystalToolLogic.useOn(context);
        if (result != net.minecraft.world.InteractionResult.PASS) return result;
        return super.useOn(context);
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
