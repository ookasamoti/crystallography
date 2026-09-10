package net.ookasamoti.crystallography.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ookasamoti.crystallography.setup.AttachmentTypeRegistry;
import org.joml.Matrix4f;

/**
 * 一人称視点で炎上中に画面いっぱいに表示される炎オーバーレイ({@code ScreenEffectRenderer.renderFire})
 * も、{@code minecraft:block/fire_1} が固定でハードコードされており着火元を区別しない。
 * NeoForgeが用意している {@link RenderBlockScreenEffectEvent}(キャンセル可能)経由で、
 * ソウルファイア起源のプレイヤーだけバニラの描画を止め、同じ構図でソウルファイアの
 * ブロックスプライト({@code minecraft:block/soul_fire_1})を代わりに描画する。
 * <p>
 * こちらは {@link SoulFireFlameRenderer}(mob用の炎)と違い、ブロックアトラス上のスプライトを
 * 正規のAPI({@code SpriteGetter})経由で取得しているため、アトラス自身のティック機構による
 * アニメーションがそのまま効く(自前でフレームを切り替える必要がない)。
 */
public final class SoulFireScreenOverlay {
    private static final SpriteId SOUL_FIRE_1 = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("soul_fire_1");

    private SoulFireScreenOverlay() {}

    public static void register(net.neoforged.bus.api.IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(SoulFireScreenOverlay::onRenderBlockScreenEffect);
    }

    private static void onRenderBlockScreenEffect(RenderBlockScreenEffectEvent event) {
        if (event.getOverlayType() != RenderBlockScreenEffectEvent.OverlayType.FIRE) return;

        Player player = event.getPlayer();
        if (!Boolean.TRUE.equals(player.getData(AttachmentTypeRegistry.SOUL_FIRE_IGNITED))) return;

        event.setCanceled(true);
        TextureAtlasSprite sprite = event.getSprites().get(SOUL_FIRE_1);
        renderFire(event.getPoseStack(), event.getBufferSource(), sprite);
    }

    /** {@code ScreenEffectRenderer.renderFire} と同一の頂点計算(private のため複製)。 */
    private static void renderFire(PoseStack poseStack, MultiBufferSource bufferSource, TextureAtlasSprite sprite) {
        VertexConsumer builder = bufferSource.getBuffer(RenderTypes.fireScreenEffect(sprite.atlasLocation()));
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        for (int i = 0; i < 2; i++) {
            poseStack.pushPose();
            poseStack.translate(-(i * 2 - 1) * 0.24F, -0.3F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees((i * 2 - 1) * 10.0F));
            Matrix4f pose = poseStack.last().pose();
            builder.addVertex(pose, -0.5F, -0.5F, -0.5F).setUv(u1, v1).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            builder.addVertex(pose, 0.5F, -0.5F, -0.5F).setUv(u0, v1).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            builder.addVertex(pose, 0.5F, 0.5F, -0.5F).setUv(u0, v0).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            builder.addVertex(pose, -0.5F, 0.5F, -0.5F).setUv(u1, v0).setColor(1.0F, 1.0F, 1.0F, 0.9F);
            poseStack.popPose();
        }
    }
}
