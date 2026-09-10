package net.ookasamoti.crystallography.client.renderer;

import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.setup.AttachmentTypeRegistry;
import org.joml.Quaternionf;

/**
 * バニラの炎エフェクト({@code FlameFeatureRenderer})は、着火元がソウルファイアかどうかに
 * 関わらず常に同じ(オレンジの){@code minecraft:block/fire_0}/{@code fire_1} を描画する。
 * これを「ソウルファイアで着火されたmobは青い炎で燃える」ように差し替えるためのクラス。
 * <p>
 * バニラの炎描画自体を上書きするフックは存在しない({@link net.minecraft.client.renderer.feature.FlameFeatureRenderer}
 * はテクスチャ選択が完全にハードコードされている)ため、ミキシン等は使わず次の2段構えで実現する。
 * <ol>
 *   <li>{@link RegisterRenderStateModifiersEvent} でエンティティのレンダー状態抽出後に
 *       {@link AttachmentTypeRegistry#SOUL_FIRE_IGNITED} を読み取ってレンダー状態に焼き込み、
 *       該当エンティティのバニラ炎表示({@code displayFireAnimation})を止める。</li>
 *   <li>{@link RenderLivingEvent.Post}(バニラの炎提出より前に発火する)で、ソウルファイア起源
 *       のエンティティにだけ、{@code FlameFeatureRenderer.renderFlame} と同じ頂点計算を、
 *       バニラのソウルファイア(ブロック用)テクスチャを単体テクスチャとして流用しつつ再現し
 *       提出する(専用テクスチャができるまでの暫定措置)。</li>
 * </ol>
 */
public final class SoulFireFlameRenderer {
    private static final ContextKey<Boolean> SOUL_FIRE_KEY =
            new ContextKey<>(Identifier.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "soul_fire_ignited"));

    // 専用テクスチャができるまでの暫定実装として、バニラのソウルファイア(ブロック用)テクスチャを
    // そのまま単体テクスチャとして流用する。アトラス経由ではないため、これらの .png.mcmeta が
    // 本来持っているアトラス内アニメーションは効かない(各ファイルの1枚目の見た目で静止する)が、
    // 2枚を層ごとに交互利用することでバニラの炎描画に近い見た目にはなる。
    private static final Identifier FLAME_TEX_0 =
            Identifier.withDefaultNamespace("textures/block/soul_fire_0.png");
    private static final Identifier FLAME_TEX_1 =
            Identifier.withDefaultNamespace("textures/block/soul_fire_1.png");

    // soul_fire_0/1.png はどちらも 16x512(16x16のフレームが32枚縦に並んだアトラス用アニメ
    // シート)。アトラスを経由しないためアトラス側の自動ティックは効かないが、同じ画像から
    // 自前でフレームを切り出して毎tick切り替えることで同じアニメーションを再現する。
    private static final int FRAME_COUNT = 32;
    private static final float FRAME_HEIGHT = 1.0F / FRAME_COUNT;
    // soul_fire_0.png.mcmeta の "frames" 配列(0〜31を並べ替えたもの)をそのまま再現。
    private static final int[] TEX0_FRAME_ORDER = {
            16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
    };
    // soul_fire_1.png.mcmeta には "frames" の指定が無く、0〜31の連番再生になる。

    private SoulFireFlameRenderer() {}

    public static void register(net.neoforged.bus.api.IEventBus modBus) {
        modBus.addListener(SoulFireFlameRenderer::onRegisterRenderStateModifiers);
        NeoForge.EVENT_BUS.addListener(SoulFireFlameRenderer::onRenderLivingPost);
    }

    private static void onRegisterRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<EntityRenderer<Entity, EntityRenderState>>() {}, (Entity entity, EntityRenderState state) -> {
            boolean soulFire = Boolean.TRUE.equals(entity.getData(AttachmentTypeRegistry.SOUL_FIRE_IGNITED));
            state.setRenderData(SOUL_FIRE_KEY, soulFire);
            if (soulFire) {
                // バニラの(オレンジの)炎提出はこのフラグを見て行われる。ここで止めて、代わりに
                // 下の RenderLivingEvent.Post 側で青い炎を提出する。
                state.displayFireAnimation = false;
            }
        });
    }

    private static void onRenderLivingPost(RenderLivingEvent.Post<?, ?, ?> event) {
        EntityRenderState state = event.getRenderState();
        if (!Boolean.TRUE.equals(state.getRenderData(SOUL_FIRE_KEY))) return;

        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();

        float s = state.boundingBoxWidth * 1.4F;
        float h0 = state.boundingBoxHeight / s;
        int lightCoords = LightCoordsUtil.withBlock(state.lightCoords, 15);
        Quaternionf rotation = Mth.rotationAroundAxis(
                Mth.Y_AXIS, Minecraft.getInstance().gameRenderer.getMainCamera().rotation(), new Quaternionf());

        // バニラの soul_fire_0/1.png はデフォルトのフレーム時間(1tick/フレーム)でアニメする
        // ので、ワールドのtick数を32(=フレーム数)で割った余りを共通のアニメ位置として使う。
        int tick = (int) (Minecraft.getInstance().level.getGameTime() % FRAME_COUNT);
        float v0Tex0 = TEX0_FRAME_ORDER[tick] * FRAME_HEIGHT;
        float v0Tex1 = tick * FRAME_HEIGHT;

        submitFlameLayer(poseStack, collector, RenderTypes.entityCutout(FLAME_TEX_0), 0, s, h0, rotation, lightCoords, v0Tex0);
        submitFlameLayer(poseStack, collector, RenderTypes.entityCutout(FLAME_TEX_1), 1, s, h0, rotation, lightCoords, v0Tex1);
    }

    /**
     * {@code FlameFeatureRenderer.renderFlame} と同じ頂点計算を、指定した偶奇(段ごとに交互に
     * 使われる2枚のテクスチャのどちらか一方)の段だけ再現する。r/yo/zo は段番号 ss だけから
     * 決まる閉じた式で求めており、元の実装の「1段描画するごとに変数を更新する」逐次計算と
     * 同じ値になる。
     */
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
