package net.ookasamoti.crystallography.client.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.ookasamoti.crystallography.common.item.tool.ToolBase;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Data-driven replacement for the old {@code crystallography:form} {@code ItemProperties} override.
 * Returns the active (or draft) loadout's {@link net.ookasamoti.crystallography.common.item.tool.component.ToolForm}
 * name in lower case (e.g. {@code "pickaxe"}), or {@code null} when no loadout is registered.
 *
 * <p>Used by a {@code minecraft:select} item model with {@code "property": "crystallography:form"};
 * each {@code "when"} case matches a form name and the {@code "fallback"} renders the bare tool base.
 */
public record FormProperty() implements SelectItemModelProperty<String> {

    public static final SelectItemModelProperty.Type<FormProperty, String> TYPE =
            SelectItemModelProperty.Type.create(MapCodec.unit(new FormProperty()), Codec.STRING);

    @Override
    @Nullable
    public String get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner,
                      int seed, ItemDisplayContext displayContext) {
        var draft = ToolBase.getDraftLoadout(stack);
        var lo = draft != null ? draft : ToolBase.getActiveLoadout(stack);
        if (lo == null) return null;
        // 破損したアクティブロードアウトは crashed モデルへ。draft（プレビュー）は通常 form を表示。
        if (draft == null && lo.broken()) return "crashed";
        return lo.form().name().toLowerCase(Locale.ROOT);
    }

    @Override
    public Codec<String> valueCodec() {
        return Codec.STRING;
    }

    @Override
    public SelectItemModelProperty.Type<FormProperty, String> type() {
        return TYPE;
    }
}
