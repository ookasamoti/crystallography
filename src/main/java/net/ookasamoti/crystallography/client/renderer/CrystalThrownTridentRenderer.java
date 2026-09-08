package net.ookasamoti.crystallography.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.TridentModel;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.client.renderer.state.CrystalThrownTridentRenderState;
import net.ookasamoti.crystallography.common.entity.TridentVisualData;
import net.ookasamoti.crystallography.setup.AttachmentTypeRegistry;

/**
 * Identical to vanilla's {@code ThrownTridentRenderer} except the frame texture is picked from
 * the thrown stack's crystal tool tier, and three additional crystal-tinted overlay passes are
 * drawn (center/left/right, same technique as {@link CrystalTridentSpecialRenderer}) so a thrown
 * crystal trident keeps reflecting its actual crystal colours in flight.
 */
public class CrystalThrownTridentRenderer extends EntityRenderer<ThrownTrident, CrystalThrownTridentRenderState> {
    private static final SpriteId CENTER_SPRITE = new SpriteId(
        TextureAtlas.LOCATION_ITEMS, Identifier.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "item/toolmodel_trident_center"));
    private static final SpriteId LEFT_SPRITE = new SpriteId(
        TextureAtlas.LOCATION_ITEMS, Identifier.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "item/toolmodel_trident_left"));
    private static final SpriteId RIGHT_SPRITE = new SpriteId(
        TextureAtlas.LOCATION_ITEMS, Identifier.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "item/toolmodel_trident_right"));

    private final TridentModel model;
    private final SpriteGetter sprites;

    public CrystalThrownTridentRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new TridentModel(context.bakeLayer(ModelLayers.TRIDENT));
        this.sprites = context.getSprites();
    }

    public void submit(CrystalThrownTridentRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRot + 90.0F));
        submitNodeCollector.order(0)
            .submitModel(this.model, Unit.INSTANCE, poseStack, state.frameTexture, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        submitOverlay(submitNodeCollector.order(1), poseStack, state, CENTER_SPRITE, state.centerColor);
        submitOverlay(submitNodeCollector.order(2), poseStack, state, LEFT_SPRITE, state.leftColor);
        submitOverlay(submitNodeCollector.order(3), poseStack, state, RIGHT_SPRITE, state.rightColor);
        if (state.isFoil) {
            submitNodeCollector.order(4)
                .submitModel(
                    this.model,
                    Unit.INSTANCE,
                    poseStack,
                    ItemFeatureRenderer.getFoilRenderType(this.model.renderType(state.frameTexture), false),
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    state.outlineColor,
                    null
                );
        }

        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private void submitOverlay(
            OrderedSubmitNodeCollector collector, PoseStack poseStack, CrystalThrownTridentRenderState state, SpriteId sprite, int tint) {
        collector.submitModel(
            this.model,
            Unit.INSTANCE,
            poseStack,
            sprite.renderType(RenderTypes::entityTranslucent),
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            tint,
            this.sprites.get(sprite),
            state.outlineColor,
            null
        );
    }

    public CrystalThrownTridentRenderState createRenderState() {
        return new CrystalThrownTridentRenderState();
    }

    public void extractRenderState(ThrownTrident entity, CrystalThrownTridentRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot(partialTicks);
        state.xRot = entity.getXRot(partialTicks);
        state.isFoil = entity.isFoil();

        // The real weapon stack never reaches the client for this entity (vanilla only syncs a
        // couple of individual bits for ThrownTrident, not the full stack), so tier/colours come
        // from a server-computed, synced attachment instead — see CrystallographyMod#onEntityJoinLevel.
        TridentVisualData visual = entity.getData(AttachmentTypeRegistry.TRIDENT_VISUAL);
        state.frameTexture = frameTextureFor(visual.tier());
        state.centerColor = visual.centerColor();
        state.leftColor = visual.leftColor();
        state.rightColor = visual.rightColor();
    }

    private static Identifier frameTextureFor(int tier) {
        return Identifier.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "textures/item/toolmodel_trident_toolrod_tier" + tier + ".png");
    }
}
