package net.ookasamoti.crystallography.client.gui.dial;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fc;

/**
 * Custom {@link GuiElementRenderState} for the dial menu's procedural shapes (filled circles,
 * rings, arcs). Replaces the old immediate-mode (Tesselator + BufferUploader + shader) drawing,
 * which was removed in the 1.21.5 GUI render-pipeline rewrite.
 *
 * <p>Vertices are stored as interleaved (x, y) pixel coordinates, grouped into quads (4 vertices
 * each) because {@link RenderPipelines#GUI} uses {@code POSITION_COLOR} in {@code QUADS} mode.
 * A flat-coloured shape uses a single {@code argb}. Submit via
 * {@code guiGraphics.submitGuiElementRenderState(...)}.
 */
public record DialShapeRenderState(
        Matrix3x2fc pose,
        float[] verts,
        int argb,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

    @Override
    public void buildVertices(VertexConsumer vc) {
        for (int i = 0; i + 1 < verts.length; i += 2) {
            vc.addVertexWith2DPose(pose, verts[i], verts[i + 1]).setColor(argb);
        }
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }
}
