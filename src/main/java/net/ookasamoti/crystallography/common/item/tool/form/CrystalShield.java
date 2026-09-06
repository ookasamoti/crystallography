package net.ookasamoti.crystallography.common.item.tool.form;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbility;
import net.ookasamoti.crystallography.common.item.tool.CrystalToolLogic;
import net.ookasamoti.crystallography.common.item.tool.ICrystalTool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * ロードアウトのアクティブフォームが SHIELD のときの実 Item。{@link ShieldItem} はこのバージョンでは
 * 染料での名前表示以外のオーバーライドを持たず、防御自体は {@code DataComponents.BLOCKS_ATTACKS}
 * コンポーネントで駆動される（クラス側の特別なロジックはほぼ無い）。それでも他フォームとの
 * 実装パターン統一のため本物の {@link ShieldItem} を継承する。
 */
public class CrystalShield extends ShieldItem implements ICrystalTool {

    private final int tier;

    public CrystalShield(Properties props, int tier) {
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
        return CrystalToolLogic.canPerformAction(stack, itemAbility);
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
