package net.ookasamoti.crystallography.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class CrystalStatsReloader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String FOLDER = "crystallography/crystal/stats";

    public CrystalStatsReloader() {
        super(GSON, FOLDER);
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> jsons,
            @NotNull ResourceManager resourceManager,
            @NotNull ProfilerFiller profiler
    ) {
        CrystalStatsRegistry.clear();

        for (var entry : jsons.entrySet()) {
            JsonObject root = entry.getValue().getAsJsonObject();

            String itemStr = root.getAsJsonPrimitive("item").getAsString();
            ResourceLocation itemId = ResourceLocation.tryParse(itemStr);
            if (itemId == null) {
                continue;
            }

            int tier = root.has("tier") ? root.get("tier").getAsInt() : 0;

            FloatRange hardness = FloatRange.fromJson(root.getAsJsonObject("hardness"));
            FloatRange carat    = FloatRange.fromJson(root.getAsJsonObject("carat"));
            FloatRange clarity  = FloatRange.fromJson(root.getAsJsonObject("clarity"));

            java.util.Set<String> categories = java.util.Set.of();
            if (root.has("category")) {
                var set = new java.util.HashSet<String>();
                for (var el : root.getAsJsonArray("category")) set.add(el.getAsString());
                categories = java.util.Set.copyOf(set);
            }

            var spec = new CrystalSpecRange(tier, hardness, carat, clarity, categories);
            CrystalStatsRegistry.put(itemId, spec);
        }
    }
}
