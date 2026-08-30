package net.ookasamoti.crystallography.data;

import com.google.gson.JsonElement;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.item.crystal.CrystalStatsRange;
import net.ookasamoti.crystallography.common.util.LapidaryAnvilOperations;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class CrystalStatsReloader extends SimpleJsonResourceReloadListener<JsonElement> {
    private static final String FOLDER = "crystal/stats";

    public CrystalStatsReloader() { super(ExtraCodecs.JSON, FileToIdConverter.json(FOLDER)); }

    @Override
    protected void apply(Map<Identifier, JsonElement> jsons,
                         @NotNull ResourceManager rm,
                         @NotNull ProfilerFiller profiler) {
        CrystalStatsRegistry.clear();

        for (var e : jsons.entrySet()) {
            var id = e.getKey();
            try {
                var root = e.getValue().getAsJsonObject();

                var itemRL = rlOrNull(root.get("item"));
                if (itemRL == null) {
                    CrystallographyMod.LOGGER.warn("[Stats] {} missing/invalid 'item'", id);
                    continue;
                }

                int tier = getIntOrDefault(root.get("tier"), 0);

                var hardness = parseRangeF(root.get("hardness"));
                var cut      = parseRangeF(root.get("cut"));
                var carat    = parseRangeF(root.get("carat"));
                var clarity  = parseRangeF(root.get("clarity"));

                var categories = parseStringSet(root.get("category"));
                var traits     = parseStringSet(root.get("traits"));

                LapidaryAnvilOperations.CrackResult crack = null;
                if (root.has("crack_result")) {
                    var cr = root.get("crack_result");
                    if (cr.isJsonObject()) {
                        var o = cr.getAsJsonObject();
                        var outItem = rlOrNull(o.get("item"));
                        int count = getIntOrDefault(o.get("count"), 1);
                        if (outItem != null) crack = new LapidaryAnvilOperations.CrackResult(outItem, count);
                    }
                }

                // "color"（新キー）優先、後方互換で "tint" も受け付ける
                JsonElement colorEl = root.has("color") ? root.get("color") : root.get("tint");
                int color = parseHexColor(colorEl, CrystalStatsRange.NO_COLOR);

                CrystalStatsRegistry.put(itemRL, new CrystalStatsRange(
                        tier, hardness, cut, carat, clarity, categories, traits, crack, color
                ));

            } catch (Exception ex) {
                CrystallographyMod.LOGGER.error("[Stats] failed to load {}: {}", id, ex.toString());
            }
        }

        CrystallographyMod.LOGGER.debug("[Stats] loaded {} entries: {}",
                CrystalStatsRegistry.size(),
                CrystalStatsRegistry.keys());
    }

    private static Identifier rlOrNull(JsonElement el) {
        if (el == null) return null;
        try { return Identifier.tryParse(el.getAsString()); }
        catch (Exception ignored) { return null; }
    }

    private static int getIntOrDefault(JsonElement el, int def) {
        if (el == null) return def;
        try { return el.getAsInt(); } catch (Exception ignored) { return def; }
    }

    private static FloatRange parseRangeF(JsonElement el) {
        if (el == null) return new FloatRange(0f, 0f);
        if (el.isJsonObject()) {
            var o = el.getAsJsonObject();
            float min = getFloatOrDefault(o.get("min"), 0f);
            float max = getFloatOrDefault(o.get("max"), min);
            return new FloatRange(min, max);
        }
        float v = getFloatOrDefault(el, 0f);
        return new FloatRange(v, v);
    }

    private static float getFloatOrDefault(JsonElement el, float def) {
        if (el == null) return def;
        try { return el.getAsFloat(); } catch (Exception ignored) { return def; }
    }

    private static int parseHexColor(JsonElement el, int def) {
        if (el == null) return def;
        try {
            String s = el.getAsString().trim();
            if (s.startsWith("#")) s = s.substring(1);
            long v = Long.parseLong(s, 16);
            if (s.length() <= 6) v |= 0xFF000000L;
            return (int) v;
        } catch (Exception ignored) { return def; }
    }

    private static java.util.Set<String> parseStringSet(JsonElement el) {
        var out = new java.util.LinkedHashSet<String>();
        if (el == null) return out;
        if (el.isJsonArray()) {
            for (var v : el.getAsJsonArray()) {
                try { out.add(v.getAsString()); } catch (Exception ignored) {}
            }
        } else {
            try { out.add(el.getAsString()); } catch (Exception ignored) {}
        }
        return out;
    }
}

