package net.ookasamoti.crystallography.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.item.crystal.CrystalStatsRange;
import net.ookasamoti.crystallography.common.util.LapidaryAnvilOperations;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class CrystalStatsReloader extends SimpleJsonResourceReloadListener {
    private static final Gson G = new GsonBuilder().create();
    private static final String FOLDER = "crystal/stats";

    public CrystalStatsReloader() { super(G, FOLDER); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons,
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
                var carat    = parseRangeF(root.get("carat"));
                var clarity  = parseRangeF(root.get("clarity"));

                var categories = parseStringSet(root.get("category"));

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

                CrystalStatsRegistry.put(itemRL, new CrystalStatsRange(
                        tier, hardness, carat, clarity, categories, crack
                ));

            } catch (Exception ex) {
                CrystallographyMod.LOGGER.error("[Stats] failed to load {}: {}", id, ex.toString());
            }
        }

        CrystallographyMod.LOGGER.debug("[Stats] loaded {} entries: {}",
                CrystalStatsRegistry.size(),
                CrystalStatsRegistry.keys());
    }

    private static ResourceLocation rlOrNull(JsonElement el) {
        if (el == null) return null;
        try { return ResourceLocation.tryParse(el.getAsString()); }
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

