package net.ookasamoti.crystallography.common.item.tool;

import net.minecraft.world.item.Item;
import net.ookasamoti.crystallography.common.item.tool.component.ToolForm;
import net.ookasamoti.crystallography.setup.ItemRegistry;

/**
 * 「フォーム＋tier」から、それに対応する登録済み {@link Item} を引く解決テーブル。
 * ロードアウトのアクティブフォームが変わったときの {@link CrystalToolLogic#retarget} 呼び出しは
 * 全てここを経由する。
 */
public final class ToolFormRegistry {

    private ToolFormRegistry() {}

    /** form・tier に対応するフォーム別 Item を返す。tier は 1..3 にクランプする。 */
    public static Item itemFor(ToolForm form, int tier) {
        var tiers = ItemRegistry.FORM_TIER_ITEMS.get(form);
        if (tiers == null) {
            throw new IllegalArgumentException("No registered item for ToolForm " + form);
        }
        int idx = Math.max(1, Math.min(3, tier)) - 1;
        return tiers[idx].get();
    }

    /** ロードアウト0件（未登録）に戻すときの「素の状態」Item（tier1..3のToolRod/ToolWand）。 */
    public static Item blankFor(ICrystalTool.Kind kind, int tier) {
        int t = Math.max(1, Math.min(3, tier));
        return switch (kind) {
            case ROD -> switch (t) {
                case 1 -> ItemRegistry.TOOLROD_TIER1.get();
                case 2 -> ItemRegistry.TOOLROD_TIER2.get();
                default -> ItemRegistry.TOOLROD_TIER3.get();
            };
            case WAND -> switch (t) {
                case 1 -> ItemRegistry.TOOLWAND_TIER1.get();
                case 2 -> ItemRegistry.TOOLWAND_TIER2.get();
                default -> ItemRegistry.TOOLWAND_TIER3.get();
            };
        };
    }
}
