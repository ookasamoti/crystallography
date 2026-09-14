package net.ookasamoti.crystallography.common.item.armor;

import net.minecraft.world.item.Item;

/** アミュレット防具（頭/胴/脚/足）共通の実 Item クラス。{@link IAmuletItem} で宝飾台に識別させる。 */
public class AmuletItem extends Item implements IAmuletItem {
    public AmuletItem(Properties properties) {
        super(properties);
    }
}
