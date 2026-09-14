package net.ookasamoti.crystallography.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.ookasamoti.crystallography.client.model.AmuletCrystalModel;

import java.util.List;
import java.util.function.Function;

/**
 * アミュレット防具4部位（頭・胴・脚・足）共通の防具モデル差し込み口。バニラの
 * {@code HumanoidArmorLayer} は {@code EquipmentClientInfo}（テクスチャ）を素体防具メッシュに
 * 貼るだけなので、鎖付きの独自ジオメトリ（{@code Amulet*Model}）はこの
 * {@link IClientItemExtensions#getHumanoidArmorModel} 経由で差し替える。返したモデルは
 * {@code HumanoidModel} なので、頭の回転などは {@code getGenericArmorModel} 側が
 * {@code copyModelProperties} で自動転写してくれる。
 * <p>
 * 結晶キューブ（{@link AmuletCrystalModel#gemPaths()}）はここでは常に非表示にする。BlockBench側で
 * 結晶キューブのUVを鎖用テクスチャの領域から動かせなかった名残で、そのままだとこの本体テクスチャ
 * パス（amulet_head.png 等）でも鎖テクスチャの一部が結晶キューブに映り込んでしまう。結晶キューブは
 * {@link AmuletCrystalRenderer} が amulet_crystal.png で個別に塗り分けて描画するので、本体テクスチャ
 * パスからは完全に除外する。{@code visible} は {@code copyModelProperties} が転写する7パーツ
 * （head/hat/body/rightArm/leftArm/rightLeg/leftLeg）には含まれないので、ここで一度falseにすれば
 * 上書きされずに保たれる。
 */
public final class AmuletArmorClientExtensions implements IClientItemExtensions {
    private final ModelLayerLocation layer;
    private final Function<ModelPart, ? extends HumanoidModel<HumanoidRenderState>> factory;
    private HumanoidModel<HumanoidRenderState> model;

    public AmuletArmorClientExtensions(ModelLayerLocation layer, Function<ModelPart, ? extends HumanoidModel<HumanoidRenderState>> factory) {
        this.layer = layer;
        this.factory = factory;
    }

    @Override
    public Model getHumanoidArmorModel(ItemStack itemStack, EquipmentClientInfo.LayerType layerType, Model original) {
        if (this.model == null) {
            this.model = this.factory.apply(Minecraft.getInstance().getEntityModels().bakeLayer(this.layer));
            if (this.model instanceof AmuletCrystalModel gems) {
                for (List<ModelPart> path : gems.gemPaths()) {
                    path.get(path.size() - 1).visible = false;
                }
            }
        }
        return this.model;
    }
}
