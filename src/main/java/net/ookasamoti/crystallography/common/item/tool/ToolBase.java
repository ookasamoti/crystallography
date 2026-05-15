package net.ookasamoti.crystallography.common.item.tool;

import net.minecraft.ChatFormatting;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.item.tool.component.ToolForm;
import net.ookasamoti.crystallography.common.item.tool.component.ToolLoadout;
import net.ookasamoti.crystallography.common.item.tool.component.ToolLoadoutList;
import net.ookasamoti.crystallography.common.item.tool.component.ToolStats;
import net.ookasamoti.crystallography.data.CrystalStatsRegistry;
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

    public int getTier() { return tier; }

    // ---- Tier定数（設計書準拠） ----

    /** Tier に対応する内部結晶インベントリのスロット数（tier1=6, tier2=9, tier3=12）。 */
    public static int crystalSlotCount(int tier) {
        return switch (tier) { case 1 -> 6; case 2 -> 9; default -> 12; };
    }

    /** Tier に対応する登録ツールの最大数（tier1=8, tier2=12, tier3=16）。 */
    public static int maxLoadouts(int tier) {
        return switch (tier) { case 1 -> 8; case 2 -> 12; default -> 16; };
    }

    // ---- ロードアウト操作 ----

    public static ToolLoadoutList getLoadout(ItemStack stack) {
        ToolLoadoutList list = stack.get(ToolComponentsRegistry.TOOL_LOADOUTS.get());
        return list != null ? list : new ToolLoadoutList(List.of());
    }

    public static int getActiveIndex(ItemStack stack) {
        Integer i = stack.get(ToolComponentsRegistry.TOOL_ACTIVE_INDEX.get());
        return i != null ? i : 0;
    }

    public static void setActiveIndex(ItemStack stack, int idx) {
        stack.set(ToolComponentsRegistry.TOOL_ACTIVE_INDEX.get(), Math.max(0, idx));
    }

    public static @Nullable ToolLoadout getActiveLoadout(ItemStack stack) {
        int slotIdx = getActiveIndex(stack);
        return getLoadout(stack).getAtSlot(slotIdx).orElse(null);
    }

    /**
     * ツールにロードアウトを追加する。
     * スロット重複または Tier 上限（8/12/16）に達した場合は追加せず false を返す。
     */
    public static boolean addLoadout(ItemStack stack, ToolLoadout add) {
        int max = maxLoadouts(getRodTier(stack));
        var list = getLoadout(stack);
        if (list.full(max)) return false;
        if (list.getAtSlot(add.slotIndex()).isPresent()) return false;
        var mod = new java.util.ArrayList<>(list.entries());
        mod.add(add);
        stack.set(ToolComponentsRegistry.TOOL_LOADOUTS.get(), new ToolLoadoutList(java.util.List.copyOf(mod)));
        if (stack.get(ToolComponentsRegistry.TOOL_ACTIVE_INDEX.get()) == null)
            setActiveIndex(stack, add.slotIndex());
        return true;
    }

    /**
     * 指定 slotIndex にロードアウトを登録する。既存エントリがあれば上書きする。
     * slotIndex が Tier 上限外、または新規追加で上限に達している場合は false を返す。
     */
    public static boolean setLoadout(ItemStack stack, ToolLoadout add) {
        int max = maxLoadouts(getRodTier(stack));
        if (add.slotIndex() < 0 || add.slotIndex() >= max) return false;
        var list = getLoadout(stack);
        var mod = new java.util.ArrayList<>(list.entries());
        boolean replaced = mod.removeIf(e -> e.slotIndex() == add.slotIndex());
        if (!replaced && mod.size() >= max) return false;
        mod.add(add);
        stack.set(ToolComponentsRegistry.TOOL_LOADOUTS.get(), new ToolLoadoutList(java.util.List.copyOf(mod)));
        if (stack.get(ToolComponentsRegistry.TOOL_ACTIVE_INDEX.get()) == null)
            setActiveIndex(stack, add.slotIndex());
        return true;
    }

    // ---- ドラフトロードアウト操作 ----

    /** 仮登録中の ToolLoadout を返す。なければ null。 */
    public static @Nullable ToolLoadout getDraftLoadout(ItemStack stack) {
        return stack.get(ToolComponentsRegistry.TOOL_DRAFT_LOADOUT.get());
    }

    /** 仮登録ロードアウトを書き込む。 */
    public static void setDraftLoadout(ItemStack stack, ToolLoadout draft) {
        stack.set(ToolComponentsRegistry.TOOL_DRAFT_LOADOUT.get(), draft);
    }

    /** 仮登録ロードアウトを削除する（取り消し・本登録完了時）。 */
    public static void clearDraftLoadout(ItemStack stack) {
        stack.remove(ToolComponentsRegistry.TOOL_DRAFT_LOADOUT.get());
    }

    // ---- 性能計算（登録時のみ呼ぶ） ----

    /**
     * 宝飾台での登録時に呼び出し、ToolStats を生成する。
     * crystalInv は {@link ToolInventory#get} で取得したインスタンスを渡す。
     * 結果は {@link ToolLoadout} に格納し、以後は変更しない。
     */
    public static ToolStats buildStats(ToolForm form, int rodTier, int[] crystalIndices, IItemHandler crystalInv) {
        var statsType = DataComponentsRegistry.CRYSTAL_STATS.get();

        int hardnessSum = 0;
        float caratSum  = 0f;
        float claritySum = 0f;
        int maxCrystalTier = 0;

        for (int idx : crystalIndices) {
            if (idx < 0 || idx >= crystalInv.getSlots()) continue;
            ItemStack crystal = crystalInv.getStackInSlot(idx);
            if (crystal.isEmpty()) continue;

            var cs = crystal.get(statsType);
            if (cs != null) {
                hardnessSum += cs.hardness();
                caratSum    += cs.carat();
                claritySum  += cs.clarity();
            }

            var rangeOpt = CrystalStatsRegistry.get(crystal);
            if (rangeOpt.isPresent()) {
                maxCrystalTier = Math.max(maxCrystalTier, rangeOpt.get().tier());
            }
        }

        return new ToolStats(
                maxCrystalTier,
                Math.max(1, rodTier * 128 + hardnessSum),
                form.baseAttack()      + caratSum,
                form.baseAttackSpeed() + claritySum,
                form.baseMiningSpeed() + claritySum
        );
    }

    // ---- 性能取得（ランタイム） ----

    /** アクティブロードアウトの ToolStats を返す。登録なしなら null。 */
    public static @Nullable ToolStats getActiveStats(ItemStack stack) {
        var lo = getActiveLoadout(stack);
        return lo != null ? lo.stats() : null;
    }

    /**
     * アクティブロードアウトの ToolStats を Minecraft DataComponent に反映する。
     * ロードアウト切り替え時・登録時に呼び出す。
     */
    public static void applyComputedStats(ItemStack stack) {
        var lo = getActiveLoadout(stack);
        if (lo == null) return;
        var s = lo.stats();

        stack.set(DataComponents.MAX_DAMAGE, s.durability());

        var idAtk = ResourceLocation.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "atk");
        var idSpd = ResourceLocation.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "atk_speed");

        ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
        b.add(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(idAtk, s.attackDamage(), AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
        b.add(Attributes.ATTACK_SPEED,
                new AttributeModifier(idSpd, s.attackSpeed(), AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, b.build());

        var hint = ResourceLocation.parse(CrystallographyMod.MOD_ID + ":form/" + lo.form().name().toLowerCase());
        stack.set(ToolComponentsRegistry.TOOL_ACTIVE_MODEL.get(), hint);
    }

    // ---- Item overrides ----

    @Override
    public @NotNull ItemAttributeModifiers getDefaultAttributeModifiers(@NotNull ItemStack stack) {
        return ItemAttributeModifiers.EMPTY;
    }

    @Override
    public float getDestroySpeed(@NotNull ItemStack stack,
                                 net.minecraft.world.level.block.state.@NotNull BlockState state) {
        var s = getActiveStats(stack);
        return s != null ? Math.max(1f, s.miningSpeed()) : 1f;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack,
                                @NotNull TooltipContext ctx,
                                @NotNull List<Component> out,
                                @NotNull TooltipFlag flag) {
        // 仮登録中（FORM_SELECTED）: 組み立て中のステータスを表示
        var draft = getDraftLoadout(stack);
        if (draft != null) {
            int selected = (int) java.util.Arrays.stream(draft.crystalIndices()).filter(i -> i >= 0).count();
            out.add(Component.literal("[一時登録中: " + draft.form().name() + "]")
                    .withStyle(ChatFormatting.YELLOW));
            out.add(Component.literal("結晶: " + selected + " / " + ToolLoadout.CRYSTAL_SLOTS)
                    .withStyle(ChatFormatting.GRAY));
            if (selected > 0) {
                var s = draft.stats();
                out.add(Component.literal("Durability " + s.durability())
                        .withStyle(ChatFormatting.DARK_GREEN));
                out.add(Component.literal(
                        "Atk " + String.format(java.util.Locale.ROOT, "%.2f", s.attackDamage()) +
                        "  Spd " + String.format(java.util.Locale.ROOT, "%.2f", s.attackSpeed()))
                        .withStyle(ChatFormatting.DARK_GREEN));
            }
            super.appendHoverText(stack, ctx, out, flag);
            return;
        }

        var list = getLoadout(stack).entries();
        int idx  = getActiveIndex(stack);
        out.add(Component.literal("Loadout: " + list.size() + "  [Active " + idx + "]")
                .withStyle(ChatFormatting.GRAY));

        var lo = getActiveLoadout(stack);
        if (lo != null) {
            var s = lo.stats();
            out.add(Component.literal("- " + lo.form().name()).withStyle(ChatFormatting.AQUA));
            out.add(Component.literal("Durability " + s.durability())
                    .withStyle(ChatFormatting.DARK_GREEN));
            out.add(Component.literal(
                    "Atk " + String.format(java.util.Locale.ROOT, "%.2f", s.attackDamage()) +
                    "  Spd " + String.format(java.util.Locale.ROOT, "%.2f", s.attackSpeed()))
                    .withStyle(ChatFormatting.DARK_GREEN));
        } else {
            out.add(Component.literal("No registered tool").withStyle(ChatFormatting.RED));
        }
        super.appendHoverText(stack, ctx, out, flag);
    }

    // ---- helpers ----

    private static int getRodTier(ItemStack stack) {
        return (stack.getItem() instanceof ToolBase tb) ? tb.getTier() : 1;
    }
}
