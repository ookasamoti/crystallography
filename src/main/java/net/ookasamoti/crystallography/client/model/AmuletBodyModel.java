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
 * 胴装備「アミュレット」。{@code tmp/amuletBody.java}（2回目のモデル刷新版）のジオメトリを移植。
 * "AmuletBody" はルート直下(offset 0,0,0)に置かれていたが、胴({@code body})に追従させたいので
 * {@code body} の子として追加している（{@code body} のピボットは原点＝ルートと同じ位置なので、
 * オフセットはそのまま流用できる）。
 * <p>
 * cube_r1〜cube_r3 が結晶キューブ（{@link AmuletCrystalRenderer} が amulet_crystal.png で
 * 個別に染色する）、"chain" 配下の cube_r4〜cube_r6 が鎖（amulet_chain.png、無染色）。
 * 今回のモデルはどちらも通常サイズの実キューブなので、縮小スケールも面カリングも不要。
 */
public class AmuletBodyModel extends HumanoidModel<HumanoidRenderState> implements AmuletCrystalModel {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "amulet_body"), "main");

    private final List<List<ModelPart>> gemPaths;
    private final ModelPart chainGroup;

    public AmuletBodyModel(ModelPart root) {
        super(root);
        ModelPart amulet = this.body.getChild("amulet");
        this.gemPaths = List.of(
                List.of(amulet, amulet.getChild("cube_r1")),
                List.of(amulet, amulet.getChild("cube_r2")),
                List.of(amulet, amulet.getChild("cube_r3")));
        this.chainGroup = amulet.getChild("chain");
    }

    @Override
    public ModelPart animatedAnchor() {
        return this.body;
    }

    @Override
    public List<List<ModelPart>> gemPaths() {
        return gemPaths;
    }

    @Override
    public List<ModelPart> chainGroups() {
        return List.of(chainGroup);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));

        PartDefinition amulet = body.addOrReplaceChild("amulet", CubeListBuilder.create(), PartPose.ZERO);

        amulet.addOrReplaceChild("cube_r1", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.0F, 3.0F, -3.0F, 0.0F, 1.5708F, 0.7854F));
        amulet.addOrReplaceChild("cube_r2", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.0F, 3.0F, -3.0F, 0.0F, 1.5708F, 0.7854F));
        amulet.addOrReplaceChild("cube_r3", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 4.0F, -3.0F, 0.0F, 1.5708F, 0.7854F));

        PartDefinition chain = amulet.addOrReplaceChild("chain", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        chain.addOrReplaceChild("cube_r4", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.0F, -22.0F, -2.0F, 0.0F, 0.0F, 0.7854F));
        chain.addOrReplaceChild("cube_r5", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.0F, -22.0F, -2.0F, 0.0F, 0.0F, 0.7854F));
        chain.addOrReplaceChild("cube_r6", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -21.0F, -2.0F, 0.0F, 0.0F, 0.7854F));

        return LayerDefinition.create(mesh, 16, 16);
    }
}
