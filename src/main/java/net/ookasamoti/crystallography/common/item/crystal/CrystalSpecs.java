// net.ookasamoti.crystallography.common.item.crystal.CrystalSpecs
package net.ookasamoti.crystallography.common.item.crystal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;

import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.ookasamoti.crystallography.CrystallographyMod;

import org.jetbrains.annotations.NotNull;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static net.ookasamoti.crystallography.common.util.RangeUtil.parseMinMax;

public final class CrystalSpecs extends SimplePreparableReloadListener<Map<Item, CrystalSpec>> {

    // data/crystallography/crystal/stats/*.json
    private static final String FOLDER = "crystal/stats";
    private static volatile Map<Item, CrystalSpec> SPECS = Map.of();

    private CrystalSpecs(){}

    public static Optional<CrystalSpec> find(Item item){
        return Optional.ofNullable(SPECS.get(item));
    }

    public static void onReload(AddReloadListenerEvent e){
        e.addListener(new CrystalSpecs());
    }

    @Override
    protected @NotNull Map<Item, CrystalSpec> prepare(@NotNull ResourceManager rm, @NotNull ProfilerFiller profiler) {
        Map<Item, CrystalSpec> out = new HashMap<>();

        Map<ResourceLocation, Resource> resources = rm.listResources(FOLDER, id -> id.getPath().endsWith(".json"));
        resources.forEach((resId, res) -> {
            if (!CrystallographyMod.MOD_ID.equals(resId.getNamespace())) return;

            try (BufferedReader r = new BufferedReader(new InputStreamReader(res.open(), StandardCharsets.UTF_8))) {
                JsonObject root = JsonParser.parseReader(r).getAsJsonObject();

                ResourceLocation itemId = ResourceLocation.parse(root.get("item").getAsString());
                Item item = BuiltInRegistries.ITEM.get(itemId);

                int tier = root.get("tier").getAsInt();
                int hardness = root.get("hardness").getAsInt();

                float[] carat   = parseMinMax(root.get("carat"));
                float[] clarity = parseMinMax(root.get("clarity"));

                List<ResourceLocation> categories = new ArrayList<>();
                if (root.has("categories")) {
                    root.getAsJsonArray("categories").forEach(el ->
                            categories.add(ResourceLocation.parse(el.getAsString()))
                    );
                }

                out.put(item, new CrystalSpec(
                        tier, hardness,
                        carat[0],   carat[1],
                        clarity[0], clarity[1],
                        categories
                ));
            } catch (Exception ex) {
                CrystallographyMod.LOGGER.error("CrystalSpecs: failed to load {}", resId, ex);
            }
        });

        return out;
    }

    @Override
    protected void apply(@NotNull Map<Item, CrystalSpec> prepared, @NotNull ResourceManager rm, @NotNull ProfilerFiller profiler) {
        SPECS = Map.copyOf(prepared);
        CrystallographyMod.LOGGER.info("CrystalSpecs loaded {} entries", SPECS.size());
    }
}
