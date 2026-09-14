package net.ookasamoti.crystallography.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.client.model.AmuletBodyModel;
import net.ookasamoti.crystallography.client.model.AmuletCrystalModel;
import net.ookasamoti.crystallography.client.model.AmuletFootModel;
import net.ookasamoti.crystallography.client.model.AmuletHeadModel;
import net.ookasamoti.crystallography.client.model.AmuletLegModel;
import net.ookasamoti.crystallography.common.item.armor.AmuletColorHelper;
import net.ookasamoti.crystallography.common.item.armor.IAmuletItem;

import java.util.List;
import java.util.function.Function;

/**
 * アミュレット4部位が持つ「結晶3個」をそれぞれ個別の色で塗り分ける処理本体。
 * {@link AmuletCrystalLayer} から {@link #submitAll} が呼ばれる（{@code RenderLivingEvent.Post}
 * ではなく専用の {@code RenderLayer} を使っている理由は同クラスのコメント参照）。
 * <p>
 * バニラの装備アセットJSON（{@code equipment/amulet_*.json}）の仕組みは「1レイヤー=1色」しか
 * 表現できず、3個の結晶キューブを別々の色にはできない。かといって同じモデルインスタンスを
 * 使い回して {@code visible} を毎パスごとに切り替えながら複数回 {@code submitModel} するのは
 * 安全ではない（submitは描画を遅延させるだけで、実際に {@code visible} が読まれるのは後段の
 * {@code ModelFeatureRenderer} 処理時点なので、切り替えのタイミングと競合してどのパスも
 * 最後に設定した状態で描画されてしまう）。
 * <p>
 * そのため、部位ごとに「結晶スロットN専用（そのキューブだけ visible=true、鎖と他の2個は
 * visible=false固定）」のモデルインスタンスを3個ずつ用意し、それぞれを独立した
 * {@code submitModel} 呼び出しで描画する。
 */
public final class AmuletCrystalRenderer {
    private static final Identifier CRYSTAL_TEXTURE =
            Identifier.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "textures/entity/equipment/humanoid/amulet_crystal.png");

    private static AmuletHeadModel[] headModels;
    private static AmuletBodyModel[] bodyModels;
    private static AmuletLegModel[] legModels;
    private static AmuletFootModel[] footModels;

    private AmuletCrystalRenderer() {
    }

    public static void submitAll(HumanoidRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        RegistryAccess registryAccess = level.registryAccess();

        renderIfAmulet(state.headEquipment, headModels(), state, poseStack, collector, registryAccess);
        renderIfAmulet(state.chestEquipment, bodyModels(), state, poseStack, collector, registryAccess);
        renderIfAmulet(state.legsEquipment, legModels(), state, poseStack, collector, registryAccess);
        renderIfAmulet(state.feetEquipment, footModels(), state, poseStack, collector, registryAccess);
    }

    private static <M extends HumanoidModel<HumanoidRenderState>> void renderIfAmulet(
            ItemStack equipped, M[] slotModels, HumanoidRenderState state,
            PoseStack poseStack, SubmitNodeCollector collector, RegistryAccess registryAccess
    ) {
        if (!(equipped.getItem() instanceof IAmuletItem)) return;

        int[] colors = AmuletColorHelper.colorsForAllSlots(equipped, registryAccess);
        RenderType renderType = RenderTypes.armorCutoutNoCull(CRYSTAL_TEXTURE);

        for (int i = 0; i < slotModels.length; i++) {
            int argb = colors[i] == AmuletColorHelper.NO_COLOR
                    ? AmuletColorHelper.EMPTY_SLOT_COLOR
                    : (0xFF000000 | (colors[i] & 0xFFFFFF));
            collector.submitModel(
                    slotModels[i], state, poseStack, renderType,
                    state.lightCoords, OverlayTexture.NO_OVERLAY, argb, null, state.outlineColor, null);
        }
    }

    /** {@code model} の鎖と結晶スロット{@code slot}以外の結晶キューブを永久に非表示にする。 */
    private static void configureSlotOnly(AmuletCrystalModel model, int slot) {
        for (ModelPart chain : model.chainGroups()) {
            chain.visible = false;
        }
        List<List<ModelPart>> paths = model.gemPaths();
        for (int i = 0; i < paths.size(); i++) {
            List<ModelPart> path = paths.get(i);
            path.get(path.size() - 1).visible = (i == slot);
        }
    }

    private static <M extends HumanoidModel<HumanoidRenderState> & AmuletCrystalModel> M[] bakeThreeSlots(
            M[] array, Function<ModelPart, M> factory, ModelLayerLocation layer
    ) {
        for (int i = 0; i < array.length; i++) {
            M model = factory.apply(Minecraft.getInstance().getEntityModels().bakeLayer(layer));
            configureSlotOnly(model, i);
            array[i] = model;
        }
        return array;
    }

    private static AmuletHeadModel[] headModels() {
        if (headModels == null) headModels = bakeThreeSlots(new AmuletHeadModel[3], AmuletHeadModel::new, AmuletHeadModel.LAYER_LOCATION);
        return headModels;
    }

    private static AmuletBodyModel[] bodyModels() {
        if (bodyModels == null) bodyModels = bakeThreeSlots(new AmuletBodyModel[3], AmuletBodyModel::new, AmuletBodyModel.LAYER_LOCATION);
        return bodyModels;
    }

    private static AmuletLegModel[] legModels() {
        if (legModels == null) legModels = bakeThreeSlots(new AmuletLegModel[3], AmuletLegModel::new, AmuletLegModel.LAYER_LOCATION);
        return legModels;
    }

    private static AmuletFootModel[] footModels() {
        if (footModels == null) footModels = bakeThreeSlots(new AmuletFootModel[3], AmuletFootModel::new, AmuletFootModel.LAYER_LOCATION);
        return footModels;
    }
}
