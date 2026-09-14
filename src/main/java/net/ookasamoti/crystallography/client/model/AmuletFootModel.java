package net.ookasamoti.crystallography.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.ookasamoti.crystallography.CrystallographyMod;

import java.util.List;

/**
 * 足装備「アミュレット」。{@code tmp/amuletFoot.java}（3回目の再エクスポート版）のジオメトリを移植。
 * 結晶キューブ(cube_r1〜cube_r3)と、それに付随する鎖(cube_r4〜cube_r6)は引き続き
 * 右足({@code right_leg})にだけ追従させる。元エクスポートの "AmuletFootRight" はルート直下
 * オフセット (-1.9, 12, 0) を持っていたが、これは {@code right_leg} 自体のピボットと完全に
 * 一致するため、{@code right_leg} の子にする際はオフセットを (0,0,0) にすればそのまま同じ絶対
 * 位置になる（二重に足さない）。
 * <p>
 * 今回の再エクスポートで追加された "AmuletFootLeft"/"chainLeft"（cube_r7〜cube_r9、結晶を伴わない
 * 純粋な装飾鎖）は左足側の対称パーツ。元エクスポートではルート直下オフセット (0, 24, 0)
 * （プレビュー用「Player」参照リグの胴体ピボットと同じ絶対座標）に置かれていたが、これは
 * "chainRight"（{@code right_leg}→amulet(0,0,0)→chain(1.9,12,0) で絶対位置が (0,24,0) に
 * 戻る構成）と同じ「絶対原点 (0,24,0) を基準に作図する」慣習に従っただけで、実際にどのボーンに
 * 追従させるかは決めていなかったとみられる。左右対称に {@code left_leg} に追従させるため、
 * {@code left_leg}（絶対ピボット (1.9,12,0)）の子として、絶対位置が元の (0,24,0) に戻るよう
 * オフセット (-1.9,12,0) を与えている。
 * <p>
 * cube_r1〜cube_r3 が結晶キューブ、"chain" 配下の cube_r4〜cube_r6 と "chainLeft" 配下の
 * cube_r7〜cube_r9 はいずれも鎖（結晶を持たない装飾）。
 */
public class AmuletFootModel extends HumanoidModel<HumanoidRenderState> implements AmuletCrystalModel {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "amulet_foot"), "main");

    private final List<List<ModelPart>> gemPaths;
    private final List<ModelPart> chainGroups;

    public AmuletFootModel(ModelPart root) {
        super(root);
        ModelPart amulet = this.rightLeg.getChild("amulet");
        this.gemPaths = List.of(
                List.of(amulet, amulet.getChild("cube_r1")),
                List.of(amulet, amulet.getChild("cube_r2")),
                List.of(amulet, amulet.getChild("cube_r3")));
        ModelPart chainRight = amulet.getChild("chain");
        ModelPart chainLeft = this.leftLeg.getChild("chainLeft");
        this.chainGroups = List.of(chainRight, chainLeft);
    }

    @Override
    public ModelPart animatedAnchor() {
        return this.rightLeg;
    }

    @Override
    public List<List<ModelPart>> gemPaths() {
        return gemPaths;
    }

    @Override
    public List<ModelPart> chainGroups() {
        return chainGroups;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));

        PartDefinition amulet = rightLeg.addOrReplaceChild("amulet", CubeListBuilder.create(), PartPose.ZERO);

        amulet.addOrReplaceChild("cube_r1", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2.1F, 7.0F, 2.0F, 0.7854F, 0.0F, 0.0F));
        amulet.addOrReplaceChild("cube_r2", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.1F, 5.0F, -2.0F, 0.0F, -1.5708F, 2.3562F));
        amulet.addOrReplaceChild("cube_r3", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2.1F, 6.0F, -1.0F, 0.7854F, 0.0F, 0.0F));

        PartDefinition chain = amulet.addOrReplaceChild("chain", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
        chain.addOrReplaceChild("cube_r4", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, -6.0F, 2.0F, 0.7854F, 0.0F, 0.0F));
        chain.addOrReplaceChild("cube_r5", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, -7.0F, -1.0F, 0.7854F, 0.0F, 0.0F));
        chain.addOrReplaceChild("cube_r6", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.0F, -8.0F, -2.0F, 0.0F, 0.0F, 0.7854F));

        PartDefinition chainLeft = leftLeg.addOrReplaceChild("chainLeft", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        chainLeft.addOrReplaceChild("cube_r7", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(5.0F, -5.0F, 2.0F, 0.7854F, 0.0F, 0.0F));
        chainLeft.addOrReplaceChild("cube_r8", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(5.0F, -6.0F, -1.0F, 0.7854F, 0.0F, 0.0F));
        chainLeft.addOrReplaceChild("cube_r9", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.0F, -7.0F, -2.0F, 0.0F, 0.0F, 0.7854F));

        return LayerDefinition.create(mesh, 16, 16);
    }
}
