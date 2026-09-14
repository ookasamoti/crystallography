package net.ookasamoti.crystallography.client.model;

import net.minecraft.client.model.geom.ModelPart;

import java.util.List;

/**
 * アミュレット防具4部位共通：装着中の結晶3個をそれぞれ個別の色で塗り分けるために
 * {@code AmuletCrystalRenderer} が必要とする情報を公開するインターフェース。
 * <p>
 * {@link #animatedAnchor()} は {@code head}/{@code body}/{@code right_leg} など、
 * {@code HumanoidModel#setupAnim} によって毎フレーム回転が更新される「アニメーションされる
 * 基点パーツ」。
 */
public interface AmuletCrystalModel {
    /** アニメーションされる基点パーツ（このパーツの回転だけは毎フレーム変わる）。 */
    ModelPart animatedAnchor();

    /**
     * 結晶スロット0/1/2に対応する3本のパス。各パスは
     * {@code animatedAnchor()} の子から結晶キューブ本体までの中間パーツを順に並べたもので、
     * 最後の要素が実際にレンダリングする結晶キューブの {@link ModelPart}。
     */
    List<List<ModelPart>> gemPaths();

    /**
     * 鎖（結晶キューブ以外の装飾）のルートパーツ。結晶専用の描画パスではこれを全て非表示にする。
     * 通常は1個だが、Footのように左右両足に鎖装飾があるなど部位によっては複数になりうる。
     */
    List<ModelPart> chainGroups();
}
