package net.ookasamoti.crystallography.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.ookasamoti.crystallography.common.item.crystal.CrystalSpecRange;
import net.ookasamoti.crystallography.common.util.LapidaryAnvilOperations;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

            Set<String> categories = Set.of();
            if (root.has("category")) {
                var set = new HashSet<String>();
                for (var el : root.getAsJsonArray("category")) set.add(el.getAsString());
                categories = Set.copyOf(set);
            }

            LapidaryAnvilOperations.CrackResult crack_result = null;
            if (root.has("crack_result")) {
                var cr = root.getAsJsonObject("crack_result");
                var outId = ResourceLocation.tryParse(cr.get("item").getAsString());
                int count = cr.has("count") ? cr.get("count").getAsInt() : 1;
                if (outId != null) crack_result = new LapidaryAnvilOperations.CrackResult(outId, count);
            }


            var spec = new CrystalSpecRange(tier, hardness, carat, clarity, categories, crack_result);
            CrystalStatsRegistry.put(itemId, spec);
        }
    }
}
