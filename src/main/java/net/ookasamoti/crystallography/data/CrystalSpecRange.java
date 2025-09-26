package net.ookasamoti.crystallography.data;

import java.util.Set;
import java.util.random.RandomGenerator;

public record CrystalSpecRange(
        int tier,
        FloatRange hardness,
        FloatRange carat,
        FloatRange clarity,
        Set<String> categories) {

    public boolean allFixed() {
        return hardness.isFixed() && carat.isFixed() && clarity.isFixed();
    }

    public CrystalSpecResolved resolve(RandomGenerator rng) {
        float h = hardness.isFixed() ? hardness.min() : lerp(rng.nextFloat(), hardness);
        float c = carat.isFixed()    ? carat.min()    : lerp(rng.nextFloat(), carat);
        float q = clarity.isFixed()  ? clarity.min()  : lerp(rng.nextFloat(), clarity);
        return new CrystalSpecResolved(tier, h, c, q, categories);
    }

    private static float lerp(float t, FloatRange r) {
        return r.min() + (r.max() - r.min()) * t;
    }
}
