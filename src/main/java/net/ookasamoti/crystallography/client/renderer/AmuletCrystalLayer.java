package net.ookasamoti.crystallography.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * アミュレットの結晶キューブ個別着色を、バニラの通常の防具レイヤーと全く同じ実行位置
 * （{@code LivingEntityRenderer#submit} 内、体のヨー回転などが {@code poseStack} に
 * 適用済みかつまだ pop されていないタイミング）に描画するための専用レイヤー。
 * <p>
 * 以前は {@code RenderLivingEvent.Post} を使っていたが、この Post イベントは
 * {@code LivingEntityRenderer#submit} が {@code poseStack.popPose()} した「後」に発火するため、
 * 体の回転（{@code setupRotations} で適用される bodyRot 等）がすでに巻き戻された状態の
 * {@code poseStack} しか手に入らず、結晶キューブの座標・向きが正しく再現できなかった。
 * 通常の防具レイヤー（{@code HumanoidArmorLayer}）はこの pop より前、レイヤーリストの
 * ループ内で呼ばれるため正しく描画できていた。{@code EntityRenderersEvent.AddLayers} で
 * このレイヤーを同じレイヤーリストに追加することで、鎖と全く同じタイミング・座標系で
 * 結晶キューブも描画できる。
 */
public final class AmuletCrystalLayer<S extends HumanoidRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    public AmuletCrystalLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        AmuletCrystalRenderer.submitAll(state, poseStack, submitNodeCollector);
    }
}
