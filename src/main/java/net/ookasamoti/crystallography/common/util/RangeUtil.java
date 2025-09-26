package net.ookasamoti.crystallography.common.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.RandomSource;

public final class RangeUtil {
    private RangeUtil(){}

    /** JSON の単一数 or {min,max} を [min,max] に正規化 */
    public static float[] parseMinMax(JsonElement el){
        if (el.isJsonPrimitive()) {
            float v = el.getAsFloat();
            return new float[]{v, v};
        }
        JsonObject o = el.getAsJsonObject();
        float min = o.get("min").getAsFloat();
        float max = o.get("max").getAsFloat();
        return new float[]{min, max};
    }

    /** 一様乱数で [min,max] からロール */
    public static float roll(float min, float max, RandomSource r){
        return min + r.nextFloat() * (max - min);
    }
}

