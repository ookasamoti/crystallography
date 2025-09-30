package net.ookasamoti.crystallography.common.item.tool;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.item.tool.component.ToolLoadout;
import net.ookasamoti.crystallography.common.item.tool.component.ToolLoadoutList;
import net.ookasamoti.crystallography.setup.DataComponentsRegistry;
import net.ookasamoti.crystallography.setup.ToolComponentsRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class ToolBase extends Item {

    protected final int tier;

    protected ToolBase(Properties props, int tier) {
        super(props);
        this.tier = tier;
    }

    public int getTier(){ return tier; }

    public static ToolLoadoutList getLoadout(ItemStack stack){
        ToolLoadoutList list = stack.get(ToolComponentsRegistry.TOOL_LOADOUTS.get());
        return list != null ? list : new ToolLoadoutList(List.of());
    }
    public static int getActiveIndex(ItemStack stack){
        Integer i = stack.get(ToolComponentsRegistry.TOOL_ACTIVE_INDEX.get());
        return i != null ? i : 0;
    }
    public static void setActiveIndex(ItemStack stack, int idx){
        stack.set(ToolComponentsRegistry.TOOL_ACTIVE_INDEX.get(), Math.max(0, idx));
    }
    public static @Nullable ToolLoadout getActiveLoadout(ItemStack stack){
        var list = getLoadout(stack).entries();
        int idx = Math.min(Math.max(0, getActiveIndex(stack)), Math.max(0, list.size()-1));
        return list.isEmpty() ? null : list.get(idx);
    }
    public static boolean addLoadout(ItemStack stack, ToolLoadout add){
        var list = getLoadout(stack);
        if (list.full()) return false;
        var mod = new java.util.ArrayList<>(list.entries());
        mod.add(add);
        stack.set(ToolComponentsRegistry.TOOL_LOADOUTS.get(), new ToolLoadoutList(java.util.List.copyOf(mod)));
        if (stack.get(ToolComponentsRegistry.TOOL_ACTIVE_INDEX.get()) == null) setActiveIndex(stack, 0);
        return true;
    }

    public record Computed(float attackDamage, float attackSpeed, int durability, float miningSpeed){}

    public static Computed compute(ItemStack stack){
        var lo = getActiveLoadout(stack);
        if (lo == null) return new Computed(1f, 1f, 1, 1f);

        var form = lo.form();
        float baseAtk         = form.baseAttack();
        float baseAtkSpeed    = form.baseAttackSpeed();
        float baseMiningSpeed = form.baseMiningSpeed();

        int   hardnessSum = 0;
        float caratSum    = 0f;
        float claritySum  = 0f;

        var statsType = DataComponentsRegistry.CRYSTAL_STATS.get();
        for (var c : lo.crystals()) {
            if (c == null || c.isEmpty()) continue;
            var s = c.get(statsType);
            if (s == null) continue;
            hardnessSum += s.hardness();
            caratSum    += s.weight();
            claritySum  += s.purity();
        }

        int rodBase = (getRodTier(stack) * 128);
        int durability   = Math.max(1, rodBase + hardnessSum);
        float atk        = baseAtk + caratSum;
        float atkSpeed   = baseAtkSpeed + claritySum;
        float miningSpd  = baseMiningSpeed + claritySum;

        return new Computed(atk, atkSpeed, durability, miningSpd);
    }

    private static int getRodTier(ItemStack stack){
        Item i = stack.getItem();
        return (i instanceof ToolBase tb) ? tb.getTier() : 1;
    }

    public static void applyComputedStats(ItemStack stack, @Nullable HolderLookup.Provider lookup){
        Computed c = compute(stack);

        stack.set(DataComponents.MAX_DAMAGE, c.durability());

        ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();

        var idAtk  = ResourceLocation.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "atk");
        var idSpd  = ResourceLocation.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "atk_speed");

        b.add(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(idAtk, c.attackDamage(), AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);

        b.add(Attributes.ATTACK_SPEED,
                new AttributeModifier(idSpd, c.attackSpeed(), AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);

        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, b.build());

        var lo = getActiveLoadout(stack);
        if (lo != null) {
            var hint = ResourceLocation.parse(CrystallographyMod.MOD_ID + ":form/" + lo.form().name().toLowerCase());
            stack.set(ToolComponentsRegistry.TOOL_ACTIVE_MODEL.get(), hint);
        }
    }

    @Override
    public @NotNull ItemAttributeModifiers getDefaultAttributeModifiers(@NotNull ItemStack stack) {
        return ItemAttributeModifiers.EMPTY;
    }

    public float getDestroySpeed(@NotNull ItemStack stack, net.minecraft.world.level.block.state.@NotNull BlockState state) {
        return Math.max(1.0f, compute(stack).miningSpeed());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack,
                                @NotNull TooltipContext ctx,
                                @NotNull List<Component> out,
                                @NotNull TooltipFlag flag) {
        var list = getLoadout(stack).entries();
        int idx = getActiveIndex(stack);
        out.add(Component.literal("Loadout: " + list.size() + "  [Active " + idx + "]").withStyle(ChatFormatting.GRAY));

        var lo = getActiveLoadout(stack);
        if (lo != null) {
            out.add(Component.literal("- " + lo.form().name()).withStyle(ChatFormatting.AQUA));
            Computed c = compute(stack);
            out.add(Component.literal("Durability " + c.durability()).withStyle(ChatFormatting.DARK_GREEN));
            out.add(Component.literal("Atk " + String.format(java.util.Locale.ROOT,"%.2f", c.attackDamage()) +
                            "  Spd " + String.format(java.util.Locale.ROOT,"%.2f", c.attackSpeed()))
                    .withStyle(ChatFormatting.DARK_GREEN));
        } else {
            out.add(Component.literal("No registered tool").withStyle(ChatFormatting.RED));
        }
        super.appendHoverText(stack, ctx, out, flag);
    }

    public static boolean isCrystal(ItemStack s){
        return s.getItem() instanceof net.ookasamoti.crystallography.common.item.crystal.Crystal;
    }
}
