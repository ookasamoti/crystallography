package net.ookasamoti.crystallography.data;

import com.google.gson.JsonObject;

public record FloatRange(float min, float max) {
    public static FloatRange of(float min, float max) { return new FloatRange(min, max); }
    public boolean isFixed() { return Float.compare(min, max) == 0; }

    public static FloatRange fromJson(JsonObject o) {
        if (o == null) return FloatRange.of(0f, 0f);
        float min = o.has("min") ? o.get("min").getAsFloat() : 0f;
        float max = o.has("max") ? o.get("max").getAsFloat() : min;
        return FloatRange.of(min, max);
    }
}
