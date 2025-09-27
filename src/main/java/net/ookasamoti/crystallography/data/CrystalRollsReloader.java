package net.ookasamoti.crystallography.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;

import java.util.ArrayList;
import java.util.Map;

public final class  CrystalRollsReloader extends SimpleJsonResourceReloadListener {
    private static final Gson G = new GsonBuilder().create();
    private static final String FOLDER = "crystallography/rolls";

    public CrystalRollsReloader() { super(G, FOLDER); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons,
                         ResourceManager rm,
                         net.minecraft.util.profiling.ProfilerFiller profiler) {
        CrystalRollsRegistry.clear();
        for (var e : jsons.entrySet()) {
            var o = e.getValue().getAsJsonObject();
            var input = ResourceLocation.tryParse(o.get("input").getAsString());
            if (input == null) continue;

            var results = new ArrayList<CrystalRollsRegistry.Entry>();
            for (var rEl : o.getAsJsonArray("results")) {
                var r = rEl.getAsJsonObject();
                var item = ResourceLocation.tryParse(r.get("item").getAsString());
                int weight = r.get("weight").getAsInt();
                String type = r.has("type") ? r.get("type").getAsString() : "vanilla";
                results.add(new CrystalRollsRegistry.Entry(item, weight, type));
            }
            CrystalRollsRegistry.put(input, results);
        }
    }
}