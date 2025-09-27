package net.ookasamoti.crystallography.common.item.crystal;

import net.minecraft.resources.ResourceLocation;
import java.util.List;

public record CrystalSpec(
        int tier,
        int hardness,
        float caratMin,  float caratMax,
        float clarityMin,float clarityMax,
        List<ResourceLocation> categories
) {
    public boolean caratFixed()   { return Float.compare(caratMin,   caratMax)   == 0; }
    public boolean clarityFixed() { return Float.compare(clarityMin, clarityMax) == 0; }
}


