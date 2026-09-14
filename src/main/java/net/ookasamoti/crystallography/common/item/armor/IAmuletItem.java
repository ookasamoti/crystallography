package net.ookasamoti.crystallography.common.item.armor;

/**
 * マーカーインターフェース。アミュレット防具4部位（頭/胴/脚/足）が実装する。
 * {@code ICrystalTool} と違い form/tier の概念は無く、常に固定3枠の結晶インベントリを持つ
 * だけの単純なアイテム（宝飾台の CENTER スロットで {@code ICrystalTool} と並んで受け付けるための
 * 判定に使う。詳細は {@link net.ookasamoti.crystallography.common.menu.JewelryTableMenu}）。
 */
public interface IAmuletItem {
    /** アミュレットが持つ結晶スロット数。常に3（結晶キューブ3個に対応）。 */
    int CRYSTAL_SLOTS = 3;
}
