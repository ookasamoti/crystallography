package net.ookasamoti.crystallography.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.ookasamoti.crystallography.common.block.entity.LapidaryAnvilBlockEntity;
import net.ookasamoti.crystallography.data.CrystalRollsRegistry;
import net.ookasamoti.crystallography.data.CrystalStatsRegistry;

import java.util.List;

public final class LapidaryAnvilOperations {

    private LapidaryAnvilOperations() {}

    public record CrackResult(Identifier item, int count) {}

    public static void crackOre(LapidaryAnvilBlockEntity be, ServerPlayer sp) {
        var level = sp.level();
        var items = be.getItems();

        var ore   = stackAt(items, LapidaryAnvilBlockEntity.SLOT_ORE);
        var wedge = stackAt(items, LapidaryAnvilBlockEntity.SLOT_WEDGE);
        var pick  = stackAt(items, LapidaryAnvilBlockEntity.SLOT_PICK);
        if (ore.isEmpty() || wedge.isEmpty() || pick.isEmpty()) {
            // サーバー側スロットが空。クライアントにアイテムが見えているのにこれが出る場合は同期ずれ。
            sp.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("message.crystallography.lapidary.need_inputs")));
            return;
        }

        var rollListOpt = CrystalRollsRegistry.get(ore);
        if (rollListOpt.isEmpty() || rollListOpt.get().isEmpty()) {
            // この鉱石にはロール定義が無い（＝割っても何も出ない）。無反応だと壊れて見えるので通知する。
            sp.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("message.crystallography.lapidary.no_rolls")));
            return;
        }

        if (!consumeOne(items, LapidaryAnvilBlockEntity.SLOT_ORE)) return;
        if (level.getRandom().nextFloat() < 0.30f) consumeOne(items, LapidaryAnvilBlockEntity.SLOT_WEDGE);

        var entry = pickWeighted(rollListOpt.get(), level.getRandom());
        int multi = rollFortuneMultiplier(level.getRandom(), getFortuneLevel(level, pick));

        var outStacks = createOutputStacks(entry, multi, level);
        insertOrDrop(be, outStacks, level, be.getBlockPos());
        level.playSound(null, be.getBlockPos(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6f, 1.2f);
        be.setChanged();
    }

    public static void crackGems(LapidaryAnvilBlockEntity be, ServerPlayer sp) {
        var level = sp.level();
        var items = be.getItems();
        for (int i = LapidaryAnvilBlockEntity.SLOT_RIGHT_START; i <= LapidaryAnvilBlockEntity.SLOT_RIGHT_END; i++) {
            var st = stackAt(items, i);
            if (st.isEmpty()) continue;

            var specRangeOpt = CrystalStatsRegistry.get(st);
            if (specRangeOpt.isEmpty()) continue;

            var crack = specRangeOpt.get().crackResult();
            if (crack == null) continue;

            if (!consumeOne(items, i)) continue;
            var out = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(crack.item()), crack.count());
            insertOrDrop(be, List.of(out), level, be.getBlockPos());
        }
        be.setChanged();
    }

    /** Reads a slot's contents as an ItemStack (a copy; mutating it does NOT affect the handler). */
    private static ItemStack stackAt(ItemStacksResourceHandler items, int slot) {
        return items.getResource(slot).toStack(items.getAmountAsInt(slot));
    }

    /** Shrinks the given slot's stack by one, writing the result back to the handler. */
    private static boolean consumeOne(ItemStacksResourceHandler items, int slot) {
        var res = items.getResource(slot);
        if (res.isEmpty()) return false;
        int n = items.getAmountAsInt(slot) - 1;
        if (n <= 0) items.set(slot, ItemResource.EMPTY, 0);
        else        items.set(slot, res, n);
        return true;
    }

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

    private static java.util.List<ItemStack> createOutputStacks(
            net.ookasamoti.crystallography.data.CrystalRollsRegistry.Entry out,
            int multi, ServerLevel level) {

        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(out.item());

        if ("crystal".equals(out.type())) {
            var st = new ItemStack(item, multi);
            // Crystal クラスのアイテムに限らず、CrystalStatsRegistry に登録されていれば解決する
            // （minecraft:raw_iron 等、バニラアイテムを原石系結晶として使うケースを含む）。
            net.ookasamoti.crystallography.common.item.crystal.Crystal.resolveStats(st, level);
            return java.util.List.of(st);
        }

        return java.util.List.of(new ItemStack(item, multi));
    }

    /** 生成物は右側（ジェム）スロットにのみ入れる。入力スロット(pick/wedge/ore)は汚染しない。 */
    private static void insertOrDrop(LapidaryAnvilBlockEntity be, List<ItemStack> stacks, ServerLevel level, BlockPos pos) {
        var handler = be.getItems();
        for (var s : stacks) {
            if (s.isEmpty()) continue;
            var res = ItemResource.of(s);
            int remain = s.getCount();
            try (Transaction tx = Transaction.open(null)) {
                for (int i = LapidaryAnvilBlockEntity.SLOT_RIGHT_START;
                     i <= LapidaryAnvilBlockEntity.SLOT_RIGHT_END && remain > 0; i++) {
                    remain -= handler.insert(i, res, remain, tx);
                }
                tx.commit();
            }
            if (remain > 0) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        res.toStack(remain));
            }
        }
    }
}
