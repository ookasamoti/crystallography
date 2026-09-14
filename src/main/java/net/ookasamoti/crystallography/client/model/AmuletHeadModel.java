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
 * 頭装備「アミュレット」。{@code tmp/amuletHead.java}（再エクスポート版、Body/Leg/Footと同じ
 * 「結晶3個＋鎖のサブグループ」構成）のジオメトリを移植。
 * <p>
 * "AmuletHead" はルート直下(offset 0,0,0)に置かれていたが、頭({@code head})に追従させたいので
 * {@code head} の子として追加している（{@code head} のピボットも原点なので、オフセットは
 * そのまま流用できる）。
 * <p>
 * cube_r1〜cube_r3 が結晶キューブ、"bone" 配下の cube_r4 が鎖（今回は1個だけ）。
 */
public class AmuletHeadModel extends HumanoidModel<HumanoidRenderState> implements AmuletCrystalModel {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "amulet_head"), "main");

    private final List<List<ModelPart>> gemPaths;
    private final ModelPart chainGroup;

    public AmuletHeadModel(ModelPart root) {
        super(root);
        ModelPart amulet = this.head.getChild("amulet");
        this.gemPaths = List.of(
                List.of(amulet, amulet.getChild("cube_r1")),
                List.of(amulet, amulet.getChild("cube_r2")),
                List.of(amulet, amulet.getChild("cube_r3")));
        this.chainGroup = amulet.getChild("chain");
    }

    @Override
    public ModelPart animatedAnchor() {
        return this.head;
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
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));

        PartDefinition amulet = head.addOrReplaceChild("amulet", CubeListBuilder.create(), PartPose.ZERO);

        amulet.addOrReplaceChild("cube_r1", CubeListBuilder.create()
                        .texOffs(1, 0).addBox(0.0F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, -1.0F, -1.0F, 0.7854F, 0.0F, 0.0F));
        amulet.addOrReplaceChild("cube_r2", CubeListBuilder.create()
                        .texOffs(1, 0).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, 0.0F, -1.0F, 0.7854F, 0.0F, 0.0F));
        amulet.addOrReplaceChild("cube_r3", CubeListBuilder.create()
                        .texOffs(1, 0).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, -2.0F, -1.0F, 0.7854F, 0.0F, 0.0F));

        PartDefinition chain = amulet.addOrReplaceChild("chain", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        chain.addOrReplaceChild("cube_r4", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(0.0F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, -26.0F, -1.0F, 0.7854F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 16, 16);
    }
}
