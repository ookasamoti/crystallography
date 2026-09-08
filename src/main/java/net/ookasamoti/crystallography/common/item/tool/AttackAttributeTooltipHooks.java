package net.ookasamoti.crystallography.common.item.tool;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Util;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.ookasamoti.crystallography.common.item.tool.component.ToolLoadout;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddAttributeTooltipsEvent;
import net.neoforged.neoforge.event.GatherSkippedAttributeTooltipsEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Replaces the vanilla/NeoForge auto-generated Attack Damage/Speed tooltip lines for our tools
 * with a [bare hand + form + crystal] breakdown, using NeoForge's own extension points for this
 * ({@link GatherSkippedAttributeTooltipsEvent} to suppress the two per-modifier auto lines this
 * mod's own {@code Item.BASE_ATTACK_DAMAGE_ID}/{@code CRYSTAL_ATK_ID} pair would otherwise
 * generate, {@link AddAttributeTooltipsEvent} to inject the replacement in the same spot) rather
 * than a separate custom tooltip section.
 */
public final class AttackAttributeTooltipHooks {
    private AttackAttributeTooltipHooks() {}

    private static final DecimalFormat FORMAT = Util.make(
            new DecimalFormat("#.##"),
            fmt -> fmt.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT)));

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(AttackAttributeTooltipHooks::onGatherSkipped);
        NeoForge.EVENT_BUS.addListener(AttackAttributeTooltipHooks::onAddTooltips);
    }

    private static void onGatherSkipped(GatherSkippedAttributeTooltipsEvent event) {
        if (activeLoadout(event.getStack()) == null) return;
        event.skipId(Item.BASE_ATTACK_DAMAGE_ID);
        event.skipId(CrystalToolLogic.CRYSTAL_ATK_ID);
        event.skipId(Item.BASE_ATTACK_SPEED_ID);
        event.skipId(CrystalToolLogic.CRYSTAL_ATK_SPEED_ID);
    }

    private static void onAddTooltips(AddAttributeTooltipsEvent event) {
        if (!event.shouldShow()) return;
        var lo = activeLoadout(event.getStack());
        if (lo == null) return;

        var s = lo.stats();
        float formAtk = lo.form().baseAttack();
        float formSpd = lo.form().baseAttackSpeed();

        event.addTooltipLines(
                breakdownLine(Attributes.ATTACK_DAMAGE, formAtk, s.attackDamage() - formAtk),
                breakdownLine(Attributes.ATTACK_SPEED, formSpd, s.attackSpeed() - formSpd)
        );
    }

    @Nullable
    private static ToolLoadout activeLoadout(ItemStack stack) {
        if (!(stack.getItem() instanceof ICrystalTool)) return null;
        return ToolBase.getActiveLoadout(stack);
    }

    private static Component breakdownLine(Holder<Attribute> attribute, double formBonus, double crystalBonus) {
        double entityBase = attribute.value().getDefaultValue();
        double total = entityBase + formBonus + crystalBonus;

        MutableComponent bracket = Component.literal("[" + FORMAT.format(entityBase))
                .append(signedTerm(formBonus))
                .append(signedTerm(crystalBonus))
                .append(Component.literal("]"))
                .withStyle(ChatFormatting.GRAY);

        return Component.literal(FORMAT.format(total) + " ")
                .append(Component.translatable(attribute.value().getDescriptionId()))
                .append(Component.literal(" "))
                .append(bracket)
                .withStyle(ChatFormatting.DARK_GREEN);
    }

    private static MutableComponent signedTerm(double value) {
        return Component.literal(value >= 0
                ? " + " + FORMAT.format(value)
                : " - " + FORMAT.format(-value));
    }
}
