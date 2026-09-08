package net.ookasamoti.crystallography.client.renderer.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

public class CrystalThrownTridentRenderState extends EntityRenderState {
    public float xRot;
    public float yRot;
    public boolean isFoil;
    public Identifier frameTexture;
    public int centerColor;
    public int leftColor;
    public int rightColor;
}
