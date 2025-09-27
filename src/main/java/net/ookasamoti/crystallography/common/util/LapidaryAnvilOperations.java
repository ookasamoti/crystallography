package net.ookasamoti.crystallography.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ookasamoti.crystallography.common.block.entity.LapidaryAnvilBlockEntity;
import net.ookasamoti.crystallography.common.item.crystal.CrystalNBT;
import net.ookasamoti.crystallography.data.CrystalRollsRegistry;

import java.util.List;
import java.util.Random;

public final class LapidaryAnvilOperations {

    private LapidaryAnvilOperations() {}

    public record CrackResult(ResourceLocation item, int count) {}

    public static void crackOre(LapidaryAnvilBlockEntity be, ServerPlayer sp) {
        var level = sp.serverLevel();
        var items = be.getItems();

        var ore   = items.getStackInSlot(LapidaryAnvilBlockEntity.SLOT_ORE);
        var wedge = items.getStackInSlot(LapidaryAnvilBlockEntity.SLOT_WEDGE);
        var pick  = items.getStackInSlot(LapidaryAnvilBlockEntity.SLOT_PICK);
        if (ore.isEmpty() || wedge.isEmpty() || pick.isEmpty()) return;

        var rollListOpt = CrystalRollsRegistry.get(ore);
        if (rollListOpt.isEmpty()) return;

        if (!consumeOne(ore)) return;
        if (level.random.nextFloat() < 0.30f) consumeOne(wedge);

        var entry = pickWeighted(rollListOpt.get(), level.random);
        int multi = rollFortuneMultiplier(level.random, getFortuneLevel(level, pick));

        var outStacks = createOutputStacks(entry, multi, level, pick);
        insertOrDrop(be, outStacks, level, be.getBlockPos());
        be.setChanged();
    }

    public static void crackGems(LapidaryAnvilBlockEntity be, ServerPlayer sp) {
        var level = sp.serverLevel();
        var items = be.getItems();
        for (int i = LapidaryAnvilBlockEntity.SLOT_RIGHT_START; i <= LapidaryAnvilBlockEntity.SLOT_RIGHT_END; i++) {
            var st = items.getStackInSlot(i);
            if (st.isEmpty()) continue;

            var specRangeOpt = net.ookasamoti.crystallography.data.CrystalStatsRegistry.get(st);
            if (specRangeOpt.isEmpty()) continue;
            var crack = specRangeOpt.get().crackResult();
            if (crack == null) continue;

            if (!consumeOne(st)) continue;
            var out = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(crack.item()), crack.count());
            insertOrDrop(be, List.of(out), level, be.getBlockPos());
        }
        be.setChanged();
    }

    private static boolean consumeOne(ItemStack stack) { if (stack.isEmpty()) return false; stack.shrink(1); return true; }

    private static int getFortuneLevel(ServerLevel level, ItemStack pick) {
        Holder<net.minecraft.world.item.enchantment.Enchantment> fortune =
                level.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE);

        return EnchantmentHelper.getTagEnchantmentLevel(fortune, pick);
    }

    private static int rollFortuneMultiplier(net.minecraft.util.RandomSource r, int fortune) {
        if (fortune <= 0) return 1;
        return 1 + r.nextInt(1 + fortune); // 1..1+F
    }

    private static net.ookasamoti.crystallography.data.CrystalRollsRegistry.Entry pickWeighted(
            List<net.ookasamoti.crystallography.data.CrystalRollsRegistry.Entry> list,
            net.minecraft.util.RandomSource r) {
        int total = 0;
        for (var e : list) total += e.weight();
        int roll = r.nextInt(total), acc = 0;
        for (var e : list) { acc += e.weight(); if (roll < acc) return e; }
        return list.getLast();
    }

    private static List<ItemStack> createOutputStacks(
            net.ookasamoti.crystallography.data.CrystalRollsRegistry.Entry out,
            int multi, ServerLevel level, ItemStack pick) {
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(out.item());
        if ("crystal".equals(out.type())) {
            var st = new ItemStack(item, multi);
            CrystalNBT.assignRandomIfNeeded(st, new Random());
            maybeBoostIfMatchingCrystal(pick, st, level.random);
            return List.of(st);
        }
        return List.of(new ItemStack(item, multi));
    }

    private static void insertOrDrop(LapidaryAnvilBlockEntity be, List<ItemStack> stacks, ServerLevel level, BlockPos pos) {
        var handler = be.getItems();
        for (var s : stacks) {
            var remain = ItemHandlerHelper.insertItemStacked(handler, s, false);
            if (!remain.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, remain);
            }
        }
    }

    // TODO: ツールロッドの実装に合わせて差し替え
    private static void maybeBoostIfMatchingCrystal(ItemStack pick, ItemStack producedCrystal, net.minecraft.util.RandomSource r) {
        // isToolRodPickaxe / isSameCrystalAttached をあなたのコードに合わせて埋めてください
    }
}
