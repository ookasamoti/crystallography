package net.ookasamoti.crystallography.client.gui.dial;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;

import java.util.Random;

/**
 * 円/リング/弧 などの図形描画。
 *
 * <p>1.21.5 の GUI 描画刷新で即時モード(Tesselator/BufferUploader/シェーダー)が廃止されたため、
 * 各図形を QUAD 列に変換し {@link DialShapeRenderState} として
 * {@link GuiGraphicsExtractor#submitGuiElementRenderState} に投入する方式へ移行。
 * 塗りつぶし円は扇形を縮退クワッド、リング/弧は外周↔内周のクワッド帯として emit する。
 */
public final class DialDrawer {
    private DialDrawer(){}

    /** 塗りつぶし円(中心/半径/ARGB)。 */
    public static void filledCircle(GuiGraphicsExtractor g, float cx, float cy, float radius, int argb) {
        if (radius <= 0f) return;
        int segments = segForRadius(radius);
        float[] verts = new float[segments * 8]; // 4 頂点 × 2 成分 × segments
        int p = 0;
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0 * i       / segments;
            double a1 = Math.PI * 2.0 * (i + 1) / segments;
            float x0 = cx + (float)Math.cos(a0) * radius;
            float y0 = cy + (float)Math.sin(a0) * radius;
            float x1 = cx + (float)Math.cos(a1) * radius;
            float y1 = cy + (float)Math.sin(a1) * radius;
            // 内半径0のリング扱い: (outer0, center, center, outer1)。
            // arc()/filledRing()(描画実績あり)と同じ頂点順=同じ巻き順にすることで、
            // GUI パイプラインのカリングで裏面除去されないようにする。
            p = quad(verts, p, x0, y0, cx, cy, cx, cy, x1, y1);
        }
        submit(g, verts, argb, cx, cy, radius);
    }

    /** {@link #filledCircle} の別名(従来 API 互換)。 */
    public static void filledCircleGui(GuiGraphicsExtractor g, float cx, float cy, float radius, int argb) {
        filledCircle(g, cx, cy, radius, argb);
    }

    /** リング(ドーナツ)。 */
    public static void filledRing(GuiGraphicsExtractor g, float cx, float cy, float innerR, float outerR, int argb) {
        arc(g, cx, cy, innerR, outerR, 0f, (float)(Math.PI * 2.0), argb);
    }

    /** 弧(start～end はラジアン)。 */
    public static void arc(GuiGraphicsExtractor g, float cx, float cy, float innerR, float outerR,
                           float startRad, float endRad, int argb) {
        float sweep = normalizeSweep(startRad, endRad);
        int segments = Math.max(12, (int)(sweep * 24));
        float[] verts = new float[segments * 8];
        int p = 0;
        for (int i = 0; i < segments; i++) {
            float a = startRad + sweep * ((float) i / segments);
            float b = startRad + sweep * ((float) (i + 1) / segments);

            float xoa = cx + (float)Math.cos(a) * outerR, yoa = cy + (float)Math.sin(a) * outerR;
            float xia = cx + (float)Math.cos(a) * innerR, yia = cy + (float)Math.sin(a) * innerR;
            float xob = cx + (float)Math.cos(b) * outerR, yob = cy + (float)Math.sin(b) * outerR;
            float xib = cx + (float)Math.cos(b) * innerR, yib = cy + (float)Math.sin(b) * innerR;

            // QUAD = 外周a → 内周a → 内周b → 外周b
            p = quad(verts, p, xoa, yoa, xia, yia, xib, yib, xob, yob);
        }
        submit(g, verts, argb, cx, cy, outerR);
    }

    /**
     * 円アウトライン。
     * 線は thickness の 20% の細さ、30% の箇所にランダム分散したギャップ、
     * かつ全体が時間とともにゆっくり回転する。
     */
    public static void circleOutline(GuiGraphicsExtractor g, float cx, float cy, float radius, float thickness, int argb) {
        float t     = thickness * 0.2f;
        float outer = radius + t * 0.5f;
        float inner = Math.max(0f, radius - t * 0.5f);
        float rot   = (float)((System.currentTimeMillis() % 6000L) / 6000.0 * Math.PI * 2.0);

        // radius を seed にすることで呼び出しごとに独立したパターンを安定生成
        Random rng = new Random((long)(radius * 1000));
        float twoPi = (float)(Math.PI * 2.0);
        int gapCount = 3 + rng.nextInt(2);

        float[] gapFrac = new float[gapCount];
        float sum = 0;
        for (int i = 0; i < gapCount; i++) { gapFrac[i] = rng.nextFloat() + 0.3f; sum += gapFrac[i]; }
        for (int i = 0; i < gapCount; i++) gapFrac[i] = gapFrac[i] / sum * 0.30f;

        float[] arcFrac = new float[gapCount];
        sum = 0;
        for (int i = 0; i < gapCount; i++) { arcFrac[i] = rng.nextFloat() + 0.3f; sum += arcFrac[i]; }
        for (int i = 0; i < gapCount; i++) arcFrac[i] = arcFrac[i] / sum * 0.70f;

        float angle = 0;
        for (int i = 0; i < gapCount; i++) {
            float arcStart = angle;
            angle += arcFrac[i] * twoPi;
            float arcEnd = angle;
            angle += gapFrac[i] * twoPi;
            arc(g, cx, cy, inner, outer, arcStart + rot, arcEnd + rot, argb);
        }
    }

    // ---- helpers ----

    /** 8 成分(頂点4つ分の x,y)を verts[p..] に書き込み、次の書き込み位置を返す。 */
    private static int quad(float[] verts, int p,
                            float x0, float y0, float x1, float y1,
                            float x2, float y2, float x3, float y3) {
        verts[p++] = x0; verts[p++] = y0;
        verts[p++] = x1; verts[p++] = y1;
        verts[p++] = x2; verts[p++] = y2;
        verts[p++] = x3; verts[p++] = y3;
        return p;
    }

    private static void submit(GuiGraphicsExtractor g, float[] verts, int argb, float cx, float cy, float maxR) {
        if (verts.length == 0) return;
        Matrix3x2f pose = new Matrix3x2f(g.pose());
        ScreenRectangle bounds = new ScreenRectangle(
                Mth.floor(cx - maxR), Mth.floor(cy - maxR),
                Mth.ceil(maxR * 2f) + 1, Mth.ceil(maxR * 2f) + 1
        ).transformMaxBounds(pose);
        // 現在のシザースタックを反映：これが無いと enableScissor の効果がカスタム RenderState に伝わらず、
        // 円アウトラインなどがクリップボックスの外にも描画されてしまう。
        g.submitGuiElementRenderState(new DialShapeRenderState(pose, verts, argb, g.peekScissorStack(), bounds));
    }

    private static int segForRadius(float r) {
        return Mth.clamp((int)Math.ceil(r * 2.5f), 12, 64);
    }

    private static float normalizeSweep(float startRad, float endRad) {
        float twoPi = (float)(Math.PI * 2.0);
        float s = wrap(startRad), e = wrap(endRad);
        float d = e - s;
        if (d <= 0) d += twoPi;
        return d;
    }

    private static float wrap(float a) {
        float twoPi = (float)(Math.PI * 2.0);
        a %= twoPi;
        if (a < 0) a += twoPi;
        return a;
    }
}
