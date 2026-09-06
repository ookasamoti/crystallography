package net.ookasamoti.crystallography.common.item.tool;

/**
 * 「結晶ツール」であることを示すマーカーインターフェース。
 * ロードアウト未登録の素の状態（{@link ToolRod}/{@link ToolWand}）と、
 * フォームごとに実クラスへ乗り換えた後の各アイテム（{@code Crystal*} 系）の両方が実装する。
 * <p>
 * 従来の {@code instanceof ToolBase}（tier 取得）/{@code instanceof ToolRod}・{@code instanceof ToolWand}
 * （ロッド系かワンド系かの判別）に散在していた判定を、この1つのインターフェースへ集約する。
 */
public interface ICrystalTool {

    int getTier();

    Kind getKind();

    enum Kind { ROD, WAND }
}
