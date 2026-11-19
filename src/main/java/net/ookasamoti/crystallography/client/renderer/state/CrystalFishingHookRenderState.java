package net.ookasamoti.crystallography.client.renderer.state;

import net.minecraft.client.renderer.entity.state.FishingHookRenderState;

/**
 * Adds the two crystal-derived colours {@link CrystalFishingHookRenderer} needs on top of
 * vanilla's {@link FishingHookRenderState} (which only carries {@code lineOriginOffset}):
 * the bobber's "right"-slot overlay tint and the line colour (from the "left" slot).
 */
public class CrystalFishingHookRenderState extends FishingHookRenderState {
    /** -1 (white/untinted) when there is no crystal fishing rod or no crystal in the right slot. */
    public int rightColor = -1;
    /** Vanilla's hardcoded black when there is no crystal fishing rod or no crystal in the left slot. */
    public int lineColor = -16777216;
}
