package net.ookasamoti.crystallography.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.client.renderer.state.CrystalFishingHookRenderState;
import net.ookasamoti.crystallography.common.item.crystal.CrystalStatsRange;
import net.ookasamoti.crystallography.common.item.tool.CrystalColorHelper;
import net.ookasamoti.crystallography.common.item.tool.form.CrystalFishingRod;
import org.jetbrains.annotations.Nullable;

/**
 * Identical to vanilla's {@code FishingHookRenderer} except the bobber is drawn as an untinted
 * base layer plus a "right"-slot crystal-tinted overlay, and the line itself is colored from the
 * "left" slot instead of hardcoded black. Unlike a thrown trident (whose entity never carries the
 * launching stack back to the client), a fishing hook's owner is still holding the rod, so the
 * live stack — and therefore its crystal colours via {@link CrystalColorHelper} — can simply be
 * read straight from the player in {@link #extractRenderState}; no server-synced visual-data
 * attachment needed (compare {@code CrystalThrownTridentRenderer}/{@code TridentVisualData}).
 */
public class CrystalFishingHookRenderer extends EntityRenderer<FishingHook, CrystalFishingHookRenderState> {
    private static final Identifier BASE_TEXTURE = Identifier.fromNamespaceAndPath(
            CrystallographyMod.MOD_ID, "textures/item/toolwand_fishing_rod_fishing_hook_base.png");
    private static final Identifier RIGHT_TEXTURE = Identifier.fromNamespaceAndPath(
            CrystallographyMod.MOD_ID, "textures/item/toolwand_fishing_rod_fishing_hook_right.png");
    private static final RenderType BASE_RENDER_TYPE = RenderTypes.entityCutoutCull(BASE_TEXTURE);
    private static final RenderType RIGHT_RENDER_TYPE = RenderTypes.entityTranslucent(RIGHT_TEXTURE);

    public CrystalFishingHookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(FishingHook entity, Frustum culler, double camX, double camY, double camZ) {
        return super.shouldRender(entity, culler, camX, camY, camZ) && entity.getPlayerOwner() != null;
    }

    @Override
    public void submit(CrystalFishingHookRenderState state, PoseStack poseStack,
                        SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(camera.orientation);
        int lightCoords = state.lightCoords;
        submitNodeCollector.submitCustomGeometry(poseStack, BASE_RENDER_TYPE,
                (pose, buffer) -> bobberQuad(buffer, pose, lightCoords, -1));
        submitNodeCollector.submitCustomGeometry(poseStack, RIGHT_RENDER_TYPE,
                (pose, buffer) -> bobberQuad(buffer, pose, lightCoords, state.rightColor));
        poseStack.popPose();

        float xa = (float) state.lineOriginOffset.x;
        float ya = (float) state.lineOriginOffset.y;
        float za = (float) state.lineOriginOffset.z;
        float width = Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth;
        int lineColor = state.lineColor;
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
            for (int i = 0; i < 16; i++) {
                float a0 = fraction(i, 16);
                float a1 = fraction(i + 1, 16);
                stringVertex(xa, ya, za, buffer, pose, a0, a1, width, lineColor);
                stringVertex(xa, ya, za, buffer, pose, a1, a0, width, lineColor);
            }
        });
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    public CrystalFishingHookRenderState createRenderState() {
        return new CrystalFishingHookRenderState();
    }

    @Override
    public void extractRenderState(FishingHook entity, CrystalFishingHookRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        Player owner = entity.getPlayerOwner();
        if (owner == null) {
            state.lineOriginOffset = Vec3.ZERO;
            return;
        }

        float swing = owner.getAttackAnim(partialTicks);
        float swing2 = Mth.sin(Mth.sqrt(swing) * (float) Math.PI);
        Vec3 playerPos = getPlayerHandPos(owner, swing2, partialTicks);
        Vec3 hookPos = entity.getPosition(partialTicks).add(0.0, 0.25, 0.0);
        state.lineOriginOffset = playerPos.subtract(hookPos);

        ItemStack rod = heldCrystalRod(owner);
        if (rod != null) {
            var registryAccess = owner.level().registryAccess();
            int right = CrystalColorHelper.colorForSlot(rod, registryAccess, 2);
            if (right != CrystalStatsRange.NO_COLOR) state.rightColor = right;
            int left = CrystalColorHelper.colorForSlot(rod, registryAccess, 1);
            if (left != CrystalStatsRange.NO_COLOR) state.lineColor = left;
        }
    }

    @Override
    protected boolean affectedByCulling(FishingHook entity) {
        return false;
    }

    @Nullable
    private static ItemStack heldCrystalRod(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof CrystalFishingRod) return main;
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof CrystalFishingRod) return off;
        return null;
    }

    /** Same hand-position math as vanilla; {@code FishingHookRenderer.getHoldingArm} already
     * resolves correctly for our rod since {@code CrystalFishingRod extends FishingRodItem}. */
    private Vec3 getPlayerHandPos(Player owner, float swing, float partialTicks) {
        int invert = FishingHookRenderer.getHoldingArm(owner) == HumanoidArm.RIGHT ? 1 : -1;
        if (this.entityRenderDispatcher.options.getCameraType().isFirstPerson() && owner == Minecraft.getInstance().player) {
            float fov = this.entityRenderDispatcher.options.fov().get().intValue();
            double viewBobbingScale = 960.0 / fov;
            Vec3 viewVec = this.entityRenderDispatcher
                    .camera
                    .getNearPlane(fov)
                    .getPointOnPlane(invert * 0.525F, -0.1F)
                    .scale(viewBobbingScale)
                    .yRot(swing * 0.5F)
                    .xRot(-swing * 0.7F);
            return owner.getEyePosition(partialTicks).add(viewVec);
        } else {
            float ownerYRot = Mth.lerp(partialTicks, owner.yBodyRotO, owner.yBodyRot) * (float) (Math.PI / 180.0);
            double sin = Mth.sin(ownerYRot);
            double cos = Mth.cos(ownerYRot);
            float playerScale = owner.getScale();
            double rightOffset = invert * 0.35 * playerScale;
            double forwardOffset = 0.8 * playerScale;
            float yOffset = owner.isCrouching() ? -0.1875F : 0.0F;
            return owner.getEyePosition(partialTicks)
                    .add(-cos * rightOffset - sin * forwardOffset, yOffset - 0.45 * playerScale, -sin * rightOffset + cos * forwardOffset);
        }
    }

    private static float fraction(int i, int steps) {
        return (float) i / steps;
    }

    private static void bobberQuad(VertexConsumer buffer, Pose pose, int lightCoords, int color) {
        vertex(buffer, pose, lightCoords, color, 0.0F, 0, 0, 1);
        vertex(buffer, pose, lightCoords, color, 1.0F, 0, 1, 1);
        vertex(buffer, pose, lightCoords, color, 1.0F, 1, 1, 0);
        vertex(buffer, pose, lightCoords, color, 0.0F, 1, 0, 0);
    }

    private static void vertex(VertexConsumer builder, Pose pose, int lightCoords, int color,
                                float x, int y, int u, int v) {
        builder.addVertex(pose, x - 0.5F, (float) y - 0.5F, 0.0F)
                .setColor(color)
                .setUv((float) u, (float) v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(lightCoords)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void stringVertex(float xa, float ya, float za, VertexConsumer stringBuffer, Pose stringPose,
                                      float aa, float nexta, float width, int color) {
        float x = xa * aa;
        float y = ya * (aa * aa + aa) * 0.5F + 0.25F;
        float z = za * aa;
        float nx = xa * nexta - x;
        float ny = ya * (nexta * nexta + nexta) * 0.5F + 0.25F - y;
        float nz = za * nexta - z;
        float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= length;
        ny /= length;
        nz /= length;
        stringBuffer.addVertex(stringPose, x, y, z).setColor(color).setNormal(stringPose, nx, ny, nz).setLineWidth(width);
    }
}
