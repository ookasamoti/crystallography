package net.ookasamoti.crystallography.client.color;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.ookasamoti.crystallography.common.item.tool.ToolBase;
import net.ookasamoti.crystallography.common.item.tool.ToolInventory;
import net.ookasamoti.crystallography.data.CrystalStatsRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * Data-driven replacement for the old {@code RegisterColorHandlersEvent.Item} color handler.
 * Resolves the tint colour for one of a tool's three crystal model layers.
 *
 * <p>Referenced from an item model definition's {@code tints} array as
 * {@code {"type": "crystallography:crystal", "crystal_slot": N}} where {@code N} is
 * 0 (center), 1 (left) or 2 (right) — matching {@link net.ookasamoti.crystallography.common.item.tool.component.ToolLoadout#crystalIndices()}.
 * Layer 0 (the base tier texture) is left untinted via a {@code minecraft:constant} source.
 */
public record CrystalTintSource(int crystalSlot) implements ItemTintSource {

    public static final MapCodec<CrystalTintSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Codec.intRange(0, 2).fieldOf("crystal_slot").forGetter(CrystalTintSource::crystalSlot)
            ).apply(i, CrystalTintSource::new));

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        var draft = ToolBase.getDraftLoadout(stack);
        var lo = draft != null ? draft : ToolBase.getActiveLoadout(stack);
        if (lo == null) return -1;

        int[] indices = lo.crystalIndices();
        if (crystalSlot >= indices.length) return -1;
        int crystalIndex = indices[crystalSlot];
        if (crystalIndex < 0) return -1;

        if (level == null) return -1;
        int tier = (stack.getItem() instanceof ToolBase tb) ? tb.getTier() : 1;
        var inv = ToolInventory.get(stack, ToolBase.crystalSlotCount(tier), level.registryAccess());
        var crystal = inv.getResource(crystalIndex).toStack(inv.getAmountAsInt(crystalIndex));
        if (crystal.isEmpty()) return -1;

        return CrystalStatsRegistry.get(crystal).map(r -> r.color()).orElse(-1);
    }

    @Override
    public MapCodec<CrystalTintSource> type() {
        return MAP_CODEC;
    }
}
