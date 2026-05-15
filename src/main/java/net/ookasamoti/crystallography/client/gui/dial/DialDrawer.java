package net.ookasamoti.crystallography.client.gui.dial;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * 円/リング/弧 などの図形描画（1.21系 MeshData API 対応）。
 */
public final class DialDrawer {
    private DialDrawer(){}


    /** 塗りつぶし円（中心/半径/ARGB） */
    public static void filledCircle(GuiGraphics g, float cx, float cy, float radius, int argb) {
        if (radius <= 0f) return;

        int segments = segForRadius(radius);
        float[] c = argbToRGBA01(argb);
        setupColorShader();

        Matrix4f pose = g.pose().last().pose();
        BufferBuilder buf = Tesselator.getInstance()
                .begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        buf.addVertex(pose, cx, cy, 0).setColor(c[0], c[1], c[2], c[3]);
        for (int i = 0; i <= segments; i++) {
            double ang = Math.PI * 2.0 * i / segments;
            float x = cx + (float)Math.cos(ang) * radius;
            float y = cy + (float)Math.sin(ang) * radius;
            buf.addVertex(pose, x, y, 0).setColor(c[0], c[1], c[2], c[3]);
        }

        MeshData mesh = buf.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }

    /** リング（ドーナツ） */
    public static void filledRing(GuiGraphics g, float cx, float cy, float innerR, float outerR, int argb) {
        arc(g, cx, cy, innerR, outerR, 0f, (float)(Math.PI * 2.0), argb);
    }

    /** 弧（start～end はラジアン） */
    public static void arc(GuiGraphics g, float cx, float cy, float innerR, float outerR,
                           float startRad, float endRad, int argb) {
        float sweep = normalizeSweep(startRad, endRad);
        int segments = Math.max(12, (int)(sweep * 24));
        float[] c = argbToRGBA01(argb);
        setupColorShader();

        Matrix4f pose = g.pose().last().pose();
        BufferBuilder buf = Tesselator.getInstance()
                .begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i <= segments; i++) {
            float t = (float)i / (float)segments;
            float a = startRad + sweep * t;

            float xo = cx + (float)Math.cos(a) * outerR;
            float yo = cy + (float)Math.sin(a) * outerR;
            float xi = cx + (float)Math.cos(a) * innerR;
            float yi = cy + (float)Math.sin(a) * innerR;

            buf.addVertex(pose, xo, yo, 0).setColor(c[0], c[1], c[2], c[3]);
            buf.addVertex(pose, xi, yi, 0).setColor(c[0], c[1], c[2], c[3]);
        }

        MeshData mesh = buf.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }

    /**
     * 円アウトライン。
     * 線は thickness の 20% の細さ、30% の箇所にランダム分散したギャップ、
     * かつ全体が時間とともにゆっくり回転する。
     */
    public static void circleOutline(GuiGraphics g, float cx, float cy, float radius, float thickness, int argb) {
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

    private static int segForRadius(float r) {
        return Mth.clamp((int)Math.ceil(r * 2.5f), 12, 64);
    }

    private static float[] argbToRGBA01(int argb) {
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>> 8)  & 0xFF) / 255f;
        float b = ( argb         & 0xFF) / 255f;
        return new float[]{ r, g, b, a };
    }

    private static void setupColorShader() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
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
