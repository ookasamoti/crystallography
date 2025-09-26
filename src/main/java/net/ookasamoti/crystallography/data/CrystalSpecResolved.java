package net.ookasamoti.crystallography.data;

import java.util.Set;

public record CrystalSpecResolved(
        int tier,
        float hardness,
        float carat,
        float clarity,
        Set<String> categories) {}
