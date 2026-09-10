package net.ookasamoti.crystallography.data;

import com.google.gson.JsonElement;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.item.tool.component.ToolForm;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SigilReloader extends SimpleJsonResourceReloadListener<JsonElement> {
    private static final String FOLDER = "sigil";

    public SigilReloader() { super(ExtraCodecs.JSON, FileToIdConverter.json(FOLDER)); }

    @Override
    protected void apply(Map<Identifier, JsonElement> jsons,
                         @NotNull ResourceManager rm,
                         @NotNull ProfilerFiller profiler) {
        SigilRegistry.clear();

        for (var e : jsons.entrySet()) {
            var id = e.getKey();
            try {
                var root = e.getValue().getAsJsonObject();

                var itemRL = Identifier.tryParse(root.get("item").getAsString());
                if (itemRL == null) {
                    CrystallographyMod.LOGGER.warn("[Sigil] {} missing/invalid 'item'", id);
                    continue;
                }

                int cost = root.has("cost") ? root.get("cost").getAsInt() : 1;
                float atkBonus = root.has("attack_damage_bonus") ? root.get("attack_damage_bonus").getAsFloat() : 0f;
                float condAtkBonus = root.has("conditional_damage_bonus") ? root.get("conditional_damage_bonus").getAsFloat() : 0f;

                Optional<TagKey<EntityType<?>>> targetTag = Optional.empty();
                if (root.has("target_tag")) {
                    var tagId = Identifier.tryParse(root.get("target_tag").getAsString());
                    if (tagId != null) targetTag = Optional.of(TagKey.create(Registries.ENTITY_TYPE, tagId));
                }

                Optional<Holder<MobEffect>> onHitEffect = Optional.empty();
                int onHitDuration = 0;
                int onHitAmplifier = 0;
                if (root.has("on_hit_effect")) {
                    var effectId = Identifier.tryParse(root.get("on_hit_effect").getAsString());
                    var holderOpt = effectId != null ? BuiltInRegistries.MOB_EFFECT.get(effectId) : Optional.<Holder.Reference<MobEffect>>empty();
                    if (holderOpt.isPresent()) {
                        onHitEffect = Optional.of(holderOpt.get());
                        onHitDuration = root.has("on_hit_duration") ? root.get("on_hit_duration").getAsInt() : 0;
                        onHitAmplifier = root.has("on_hit_amplifier") ? root.get("on_hit_amplifier").getAsInt() : 0;
                    } else {
                        CrystallographyMod.LOGGER.warn("[Sigil] {} has unknown on_hit_effect '{}'", id, root.get("on_hit_effect").getAsString());
                    }
                }

                List<SigilRegistry.GrantedEnchantment> grants = new ArrayList<>();
                if (root.has("grants")) {
                    for (var gEl : root.getAsJsonArray("grants")) {
                        var g = gEl.getAsJsonObject();
                        try {
                            var form = ToolForm.valueOf(g.get("form").getAsString());
                            var enchId = Identifier.tryParse(g.get("enchantment").getAsString());
                            if (enchId == null) {
                                CrystallographyMod.LOGGER.warn("[Sigil] {} has invalid grants[].enchantment '{}'", id, g.get("enchantment").getAsString());
                                continue;
                            }
                            int level = g.has("level") ? g.get("level").getAsInt() : 1;
                            grants.add(new SigilRegistry.GrantedEnchantment(form, ResourceKey.create(Registries.ENCHANTMENT, enchId), level));
                        } catch (IllegalArgumentException ex) {
                            CrystallographyMod.LOGGER.warn("[Sigil] {} has unknown grants[].form '{}'", id, g.get("form").getAsString());
                        }
                    }
                }

                List<ToolForm> forms = new ArrayList<>();
                if (root.has("forms")) {
                    for (var f : root.getAsJsonArray("forms")) {
                        try {
                            forms.add(ToolForm.valueOf(f.getAsString()));
                        } catch (IllegalArgumentException ex) {
                            CrystallographyMod.LOGGER.warn("[Sigil] {} has unknown form '{}'", id, f.getAsString());
                        }
                    }
                }

                SigilRegistry.put(itemRL, new SigilRegistry.Definition(
                        cost, atkBonus, List.copyOf(forms), targetTag, condAtkBonus,
                        onHitEffect, onHitDuration, onHitAmplifier, List.copyOf(grants)));
            } catch (Exception ex) {
                CrystallographyMod.LOGGER.error("[Sigil] failed to load {}: {}", id, ex.toString());
            }
        }

        CrystallographyMod.LOGGER.debug("[Sigil] loaded {} entries: {}", SigilRegistry.size(), SigilRegistry.keys());
    }
}
