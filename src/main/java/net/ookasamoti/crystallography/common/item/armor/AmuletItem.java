package net.ookasamoti.crystallography.common.item.armor;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.ookasamoti.crystallography.common.item.tool.CrystalTraitLogic;
import org.jetbrains.annotations.NotNull;

/** アミュレット防具（頭/胴/脚/足）共通の実 Item クラス。{@link IAmuletItem} で宝飾台に識別させる。 */
public class AmuletItem extends Item implements IAmuletItem {
    public AmuletItem(Properties properties) {
        super(properties);
    }

    /**
     * levitation トレイト：ブーツに付いているとき、革の靴と同じ固有効果（粉雪に沈まない）を持つ。
     */
    @Override
    public boolean canWalkOnPowderedSnow(@NotNull ItemStack stack, @NotNull LivingEntity wearer) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.slot() != EquipmentSlot.FEET) return false;
        return AmuletStatLogic.hasSocketedTrait(stack, wearer.level().registryAccess(), CrystalTraitLogic.LEVITATION);
    }
}
