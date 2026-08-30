package net.ookasamoti.crystallography.data;

import com.google.gson.JsonElement;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.common.advancements.critereon.ItemAbilityPredicate;
import net.ookasamoti.crystallography.CrystallographyMod;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Map;

import static net.ookasamoti.crystallography.data.CrystalRollsRegistry.BY_INPUT;

// CrystalRollsReloader.java

public final class CrystalRollsReloader extends SimpleJsonResourceReloadListener<JsonElement> {
    private static final String FOLDER = "crystal/rolls";
    public CrystalRollsReloader() { super(ExtraCodecs.JSON, FileToIdConverter.json(FOLDER)); }

    @Override
    protected void apply(Map<Identifier, JsonElement> jsons,
                         ResourceManager rm,
                         ProfilerFiller profiler) {
        CrystalRollsRegistry.clear();
        for (var e : jsons.entrySet()) {
            var id = e.getKey(); // 例: crystallography:diamond_ore
            var o = e.getValue().getAsJsonObject();

            var input = Identifier.tryParse(o.get("input").getAsString());
            if (input == null) {
                CrystallographyMod.LOGGER.warn("[Rolls] {} has no valid 'input'", id);
                continue;
            }

            var results = new ArrayList<CrystalRollsRegistry.Entry>();
            for (var rEl : o.getAsJsonArray("results")) {
                var r = rEl.getAsJsonObject();
                var item = Identifier.tryParse(r.get("item").getAsString());
                int weight = r.get("weight").getAsInt();
                String type = r.has("type") ? r.get("type").getAsString() : "vanilla";
                results.add(new CrystalRollsRegistry.Entry(item, weight, type));
            }
            CrystalRollsRegistry.put(input, results);
            CrystallographyMod.LOGGER.debug("[Rolls] put {} -> {} entries", input, results.size());
        }
        CrystallographyMod.LOGGER.debug("[Rolls] loaded {} inputs: {}", BY_INPUT.size(), BY_INPUT.keySet().stream().map(Identifier::toString).toList());
    }
}
