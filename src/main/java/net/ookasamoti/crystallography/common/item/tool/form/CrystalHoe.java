package net.ookasamoti.crystallography.common.item.tool.form;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbility;
import net.ookasamoti.crystallography.common.item.tool.CrystalToolLogic;
import net.ookasamoti.crystallography.common.item.tool.ICrystalTool;
import net.ookasamoti.crystallography.common.item.tool.component.ToolForm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * ロードアウトのアクティブフォームが HOE のときの実 Item。本物の {@link HoeItem} を継承し、
 * 耕作は vanilla 実装をそのまま継承する。{@link HoeItem} 自身はロードアウトの使用可否を見ないため、
 * {@code useOn}/{@code canPerformAction} はここでガードを追加する。
 */
public class CrystalHoe extends HoeItem implements ICrystalTool {

    private final int tier;

    public CrystalHoe(Properties props, int tier) {
        super(ToolMaterial.DIAMOND, ToolForm.HOE.baseAttack(), ToolForm.HOE.baseAttackSpeed(), props);
        this.tier = tier;
    }

    @Override public int getTier() { return tier; }
    @Override public Kind getKind() { return Kind.ROD; }

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
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        if (CrystalToolLogic.isUnusable(context.getItemInHand())) return InteractionResult.PASS;
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
