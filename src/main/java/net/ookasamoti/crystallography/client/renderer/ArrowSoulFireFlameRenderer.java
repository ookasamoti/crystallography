package net.ookasamoti.crystallography.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ookasamoti.crystallography.setup.AttachmentTypeRegistry;
import org.joml.Quaternionf;

/**
 * {@link net.ookasamoti.crystallography.mixin.BaseFireBlockMixin} がソウルファイアブロックを
 * 通過したと判定した矢({@code SOUL_FIRE_IGNITED})について、飛行中にバニラが表示する
 * (オレンジの)小さな炎を青に差し替える。
 * <p>
 * {@link SoulFireFlameRenderer} と同じ見た目の差し替えだが、使っているフック
 * ({@link net.neoforged.neoforge.client.event.RenderLivingEvent}) は {@code LivingEntity} 専用で
 * 矢には使えない。NeoForgeには非LivingEntity向けの同等イベントが無いため、代わりに
 * {@link SubmitCustomGeometryEvent}(ワールド空間への汎用描画イベント)を使い、ソウルファイア印の
 * 付いた矢をこちらで探して、カメラ基準に自前で位置・向きを計算して描画する。
 * <p>
 * バニラ自身の(オレンジの)炎提出は止めていない。矢は{@code EntityRenderer}(非Living)経由で
 * 描画されており、{@code displayFireAnimation}を止めるRenderStateModifierは
 * {@link SoulFireFlameRenderer}側で既に全Entity共通で登録済みのため、ここでは青い炎の提出だけを
 * 担当する。
 */
public final class ArrowSoulFireFlameRenderer {
    private static final Identifier FLAME_TEX_0 =
            Identifier.withDefaultNamespace("textures/block/soul_fire_0.png");
    private static final Identifier FLAME_TEX_1 =
            Identifier.withDefaultNamespace("textures/block/soul_fire_1.png");

    private static final int FRAME_COUNT = 32;
    private static final float FRAME_HEIGHT = 1.0F / FRAME_COUNT;
    private static final int[] TEX0_FRAME_ORDER = {
            16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
    };

    private ArrowSoulFireFlameRenderer() {}

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(ArrowSoulFireFlameRenderer::onSubmitCustomGeometry);
    }

    private static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
        Quaternionf rotation = Mth.rotationAroundAxis(
                Mth.Y_AXIS, mc.gameRenderer.getMainCamera().rotation(), new Quaternionf());

        int tick = (int) (mc.level.getGameTime() % FRAME_COUNT);
        float v0Tex0 = TEX0_FRAME_ORDER[tick] * FRAME_HEIGHT;
        float v0Tex1 = tick * FRAME_HEIGHT;

        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();

        for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof AbstractArrow arrow)) continue;
            if (!arrow.isOnFire()) continue;
            if (!Boolean.TRUE.equals(arrow.getData(AttachmentTypeRegistry.SOUL_FIRE_IGNITED))) continue;

            Vec3 worldPos = arrow.getPosition(partialTick);
            Vec3 relative = worldPos.subtract(cameraPos);
            int lightCoords = LightCoordsUtil.withBlock(
                    LevelRenderer.getLightCoords(mc.level, BlockPos.containing(worldPos.x, worldPos.y, worldPos.z)), 15);

            float s = arrow.getBbWidth() * 1.4F;
            float h0 = arrow.getBbHeight() / s;

            poseStack.pushPose();
            poseStack.translate(relative.x, relative.y, relative.z);
            submitFlameLayer(poseStack, collector, RenderTypes.entityCutout(FLAME_TEX_0), 0, s, h0, rotation, lightCoords, v0Tex0);
            submitFlameLayer(poseStack, collector, RenderTypes.entityCutout(FLAME_TEX_1), 1, s, h0, rotation, lightCoords, v0Tex1);
            poseStack.popPose();
        }
    }

    /** {@link SoulFireFlameRenderer#submitFlameLayer} と同一の頂点計算。 */
    private static void submitFlameLayer(
            PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
            int parity, float scale, float h0, Quaternionf rotation, int lightCoords, float frameV0
    ) {
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            pose.scale(scale, scale, scale);
            pose.rotate(rotation);
            pose.translate(0.0F, 0.0F, 0.3F - (int) h0 * 0.02F);

            for (int ss = parity; ; ss += 2) {
                float h = h0 - 0.45F * ss;
                if (h <= 0.0F) break;

                float r = 0.5F * (float) Math.pow(0.9, ss);
                float yo = -0.45F * ss;
                float zo = -0.03F * ss;

                float u0 = 0.0F;
                float v0 = frameV0;
                float u1 = 1.0F;
                float v1 = frameV0 + FRAME_HEIGHT;
                if (ss / 2 % 2 == 0) {
                    float tmp = u1;
                    u1 = u0;
                    u0 = tmp;
                }

                fireVertex(pose, buffer, -r, 0.0F - yo, zo, u1, v1, lightCoords);
                fireVertex(pose, buffer, r, 0.0F - yo, zo, u0, v1, lightCoords);
                fireVertex(pose, buffer, r, 1.4F - yo, zo, u0, v0, lightCoords);
                fireVertex(pose, buffer, -r, 1.4F - yo, zo, u1, v0, lightCoords);
            }
        });
    }

    private static void fireVertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v, int lightCoords) {
        buffer.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setUv1(0, 10).setLight(lightCoords).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
