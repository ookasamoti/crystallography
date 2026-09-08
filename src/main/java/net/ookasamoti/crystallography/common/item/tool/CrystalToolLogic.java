package net.ookasamoti.crystallography.common.item.tool;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.item.tool.component.ToolForm;
import net.ookasamoti.crystallography.common.item.tool.component.ToolLoadout;
import net.ookasamoti.crystallography.common.item.tool.component.ToolLoadoutList;
import net.ookasamoti.crystallography.common.item.tool.component.ToolStats;
import net.ookasamoti.crystallography.data.CrystalStatsRegistry;
import net.ookasamoti.crystallography.setup.DataComponentsRegistry;
import net.ookasamoti.crystallography.setup.ItemRegistry;
import net.ookasamoti.crystallography.setup.ToolComponentsRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 結晶ツール（{@link ToolRod}/{@link ToolWand} と、フォームごとの {@code Crystal*} 系アイテム）が
 * 共有するロジック本体。全て {@link ItemStack}／{@link ItemInstance} を引数に取る static メソッドで、
 * どの具象 Item クラスからも同じ実装を呼べる。
 * <p>
 * Java の仕様上、interface の default メソッドは継承済みの具象メソッド（例：{@code AxeItem#hurtEnemy}）
 * より優先されないため、各具象クラス側は必ずこのクラスへ委譲する1行 override を自分で書く必要がある。
 */
public final class CrystalToolLogic {

    private CrystalToolLogic() {}

    // ---- Attack Damage/Speed 属性修飾子ID ----
    // フォーム固有分は vanilla の Item.BASE_ATTACK_DAMAGE_ID/BASE_ATTACK_SPEED_ID をそのまま使う
    // （applyComputedStats 参照）。結晶合計分はこの独自IDで登録し、AttackAttributeTooltipHooks が
    // ツールチップ上でこのIDを個別スキップして自前フォーマットに差し替える。

    public static final Identifier CRYSTAL_ATK_ID =
            Identifier.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "crystal_atk");
    public static final Identifier CRYSTAL_ATK_SPEED_ID =
            Identifier.fromNamespaceAndPath(CrystallographyMod.MOD_ID, "crystal_atk_speed");

    // ---- Tier定数（設計書準拠） ----

    /** Tier に対応する内部結晶インベントリのスロット数（tier1=6, tier2=9, tier3=12）。 */
    public static int crystalSlotCount(int tier) {
        return switch (tier) { case 1 -> 6; case 2 -> 9; default -> 12; };
    }

    /** Tier に対応する登録ツールの最大数（tier1=8, tier2=12, tier3=16）。 */
    public static int maxLoadouts(int tier) {
        return switch (tier) { case 1 -> 8; case 2 -> 12; default -> 16; };
    }

    // ---- 原石バッテリー（原石系結晶をスタックして耐久値を肩代わりさせる別系統の耐久システム）----

    /** 結晶 JSON の category にこのタグが含まれると「原石系」として原石バッテリーモード扱いになる。 */
    public static final String ORE_CATEGORY = "raw_ore";

    /**
     * crystalIndices が参照する3枠のうち、現在「原石系」(category=raw_ore) の結晶が入っている
     * backing index を返す（複数あれば最初に見つかったもの）。無ければ -1。
     */
    public static int findOreCrystalIndex(int[] crystalIndices, ResourceHandler<ItemResource> crystalInv) {
        for (int idx : crystalIndices) {
            if (idx < 0 || idx >= crystalInv.size()) continue;
            ItemStack crystal = crystalInv.getResource(idx).toStack(1);
            if (crystal.isEmpty()) continue;
            var rangeOpt = CrystalStatsRegistry.get(crystal);
            if (rangeOpt.isPresent() && rangeOpt.get().categories().contains(ORE_CATEGORY)) return idx;
        }
        return -1;
    }

    // ---- 修繕（エンチャント無しでの耐久回復。クラフト台レシピ、[[ToolRepairRecipe]]から利用）----
    // 踏み倒し（結晶差し替えでの水増し）対策のため、必ず「現在値へ固定量を加算し、現在の
    // stats.durability() を上限にクランプ」する方式に限定すること。%回復・満タン回復は、
    // 一時的に高硬度の結晶へ差し替えてから修繕→元に戻す、という往復操作で無限に耐久を
    // 水増しできてしまう（reconcileLoadouts が「削れた分」を絶対値で引き継ぐため）。

    /** tier に対応する修繕素材（tier1=木材, tier2=金インゴット, tier3=黒曜石）。 */
    public static boolean isRepairMaterial(int tier, ItemStack stack) {
        if (stack.isEmpty()) return false;
        return switch (tier) {
            case 1 -> stack.is(ItemTags.PLANKS);
            case 2 -> stack.is(Items.GOLD_INGOT);
            default -> stack.is(Items.OBSIDIAN);
        };
    }

    /**
     * 修繕素材1個あたりの回復量。バニラの木/鉄/ダイヤ「ツール」の耐久値の1/4を採用
     * （tier2 の修繕素材自体は金インゴットだが、回復量の基準は鉄ツール）。
     * ToolMaterial.WOOD/IRON/DIAMOND.durability() = 59/250/1561 → 14/62/390。
     */
    public static int repairAmountFor(int tier) {
        return switch (tier) {
            case 1 -> ToolMaterial.WOOD.durability() / 4;
            case 2 -> ToolMaterial.IRON.durability() / 4;
            default -> ToolMaterial.DIAMOND.durability() / 4;
        };
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
        int max = maxLoadouts(getTier(stack));
        var list = getLoadout(stack);
        if (list.full(max)) return false;
        if (list.getAtSlot(add.slotIndex()).isPresent()) return false;
        var mod = new ArrayList<>(list.entries());
        mod.add(add);
        stack.set(ToolComponentsRegistry.TOOL_LOADOUTS.get(), new ToolLoadoutList(List.copyOf(mod)));
        if (stack.get(ToolComponentsRegistry.TOOL_ACTIVE_INDEX.get()) == null)
            setActiveIndex(stack, add.slotIndex());
        return true;
    }

    /**
     * 指定 slotIndex にロードアウトを登録する。既存エントリがあれば上書きする。
     * slotIndex が Tier 上限外、または新規追加で上限に達している場合は false を返す。
     */
    public static boolean setLoadout(ItemStack stack, ToolLoadout add) {
        int max = maxLoadouts(getTier(stack));
        if (add.slotIndex() < 0 || add.slotIndex() >= max) return false;
        var list = getLoadout(stack);
        var mod = new ArrayList<>(list.entries());
        boolean replaced = mod.removeIf(e -> e.slotIndex() == add.slotIndex());
        if (!replaced && mod.size() >= max) return false;
        mod.add(add);
        stack.set(ToolComponentsRegistry.TOOL_LOADOUTS.get(), new ToolLoadoutList(List.copyOf(mod)));
        if (stack.get(ToolComponentsRegistry.TOOL_ACTIVE_INDEX.get()) == null)
            setActiveIndex(stack, add.slotIndex());
        return true;
    }

    /** 指定 slotIndex のロードアウトを削除する（登録削除）。存在しなければ何もしない。 */
    public static void removeLoadout(ItemStack stack, int slotIndex) {
        var list = getLoadout(stack);
        var mod = new ArrayList<>(list.entries());
        if (!mod.removeIf(e -> e.slotIndex() == slotIndex)) return;
        stack.set(ToolComponentsRegistry.TOOL_LOADOUTS.get(), new ToolLoadoutList(List.copyOf(mod)));
    }

    /**
     * slotA と slotB のロードアウトの登録先を入れ替える（REGISTRIES 並べ替え）。
     * 片方が空きスロットの場合は移動として扱う。アクティブインデックスは移動先へ追従する。
     */
    public static void swapLoadouts(ItemStack stack, int slotA, int slotB) {
        if (slotA == slotB) return;
        var entries = getLoadout(stack).entries();
        ToolLoadout a = null, b = null;
        var rest = new ArrayList<ToolLoadout>(entries.size());
        for (var e : entries) {
            if (e.slotIndex() == slotA) a = e;
            else if (e.slotIndex() == slotB) b = e;
            else rest.add(e);
        }
        if (a == null && b == null) return;
        if (a != null) rest.add(a.withSlotIndex(slotB));
        if (b != null) rest.add(b.withSlotIndex(slotA));
        stack.set(ToolComponentsRegistry.TOOL_LOADOUTS.get(), new ToolLoadoutList(List.copyOf(rest)));

        int active = getActiveIndex(stack);
        if (active == slotA) setActiveIndex(stack, slotB);
        else if (active == slotB) setActiveIndex(stack, slotA);
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
    public static ToolStats buildStats(ToolForm form, int rodTier, int[] crystalIndices, ResourceHandler<ItemResource> crystalInv) {
        var statsType = DataComponentsRegistry.CRYSTAL_STATS.get();

        int hardnessSum = 0;     // 耐久値 = 結晶の hardness の合計
        float cutSum     = 0f;   // 攻撃力 = 結晶の cut の合計
        float claritySum = 0f;   // 採掘速度 = 結晶の clarity の平均
        int tierSum      = 0;    // 採掘ティア = 結晶の tier の平均
        int crystalCount = 0;
        int tierCount    = 0;

        for (int idx : crystalIndices) {
            if (idx < 0 || idx >= crystalInv.size()) continue;
            ItemStack crystal = crystalInv.getResource(idx).toStack(crystalInv.getAmountAsInt(idx));
            if (crystal.isEmpty()) continue;

            var cs = crystal.get(statsType);
            if (cs != null) {
                hardnessSum += cs.hardness();
                cutSum      += cs.cut();
                claritySum  += cs.clarity();
                crystalCount++;
            }

            var rangeOpt = CrystalStatsRegistry.get(crystal);
            if (rangeOpt.isPresent()) {
                tierSum += rangeOpt.get().tier();
                tierCount++;
            }
        }

        float clarityAvg = crystalCount > 0 ? claritySum / crystalCount : 0f;
        int   avgTier    = tierCount    > 0 ? Math.round((float) tierSum / tierCount) : 0;

        // attackDamage = form.baseAttack + Σcut（form 別の武器特性で剣／ピッケル等を差別化）。
        // Σcut が表 damage 相当（満強化で表×1.2）、form ベースで武器形状ごとのボーナスを上乗せ。
        // ただし HOE はバニラ仕様に倣い、全ティア固定で攻撃修飾子 = 0。
        // attackSpeed = form.baseAttackSpeed + (Σclarity − 20)×0.1（小数点二桁未満切り捨て）。
        // miningSpeed = form.baseMiningSpeed × avg(clarity)（現状 baseMiningSpeed=1.0 なので実質 avg(clarity)）。
        float attackDmg = (form == ToolForm.HOE) ? 0f : (form.baseAttack() + cutSum);
        float clarityAtkSpeedBonus = truncateTo2((claritySum - 20f) * 0.1f);

        // 原石バッテリーモード：3枠のいずれかが原石系(category=raw_ore)なら、通常の
        // tier*128+Σhardness とは別系統で「原石の hardness + 残り2枠の hardness(=ボーナス)」
        // = Σhardness そのもの（tier 基礎値なし）を1充填分の耐久上限として扱う。
        // 空になったら原石バッテリーモードでスタックを1消費して全回復する（damageItem 側）。
        boolean oreMode = findOreCrystalIndex(crystalIndices, crystalInv) >= 0;
        int durability = oreMode ? Math.max(1, hardnessSum) : Math.max(1, rodTier * 128 + hardnessSum);

        return new ToolStats(
                avgTier,
                durability,
                attackDmg,
                form.baseAttackSpeed() + clarityAtkSpeedBonus,
                form.baseMiningSpeed() * clarityAvg
        );
    }

    /** 小数点三桁目以降を切り捨てて二桁に丸める（四捨五入ではない）。 */
    private static float truncateTo2(float value) {
        return (float) (Math.floor(value * 100.0) / 100.0);
    }

    /**
     * 登録時に呼び出し、結晶の構成に基づいて form を昇格させる。
     * <ul>
     *   <li>SPEAR + trident_core → TRIDENT</li>
     *   <li>PICKAXE + heavy_core → MACE</li>
     * </ul>
     * 該当しない場合は元の form をそのまま返す。
     */
    public static ToolForm upgradeFormForCrystals(ToolForm form, int[] crystalIndices,
                                                  ResourceHandler<ItemResource> crystalInv) {
        if (form != ToolForm.SPEAR && form != ToolForm.PICKAXE) return form;
        Item tridentCore = ItemRegistry.TRIDENT_CORE.get();
        Item heavyCore   = ItemRegistry.HEAVY_CORE.get();
        for (int idx : crystalIndices) {
            if (idx < 0 || idx >= crystalInv.size()) continue;
            var item = crystalInv.getResource(idx).getItem();
            if (form == ToolForm.SPEAR   && item == tridentCore) return ToolForm.TRIDENT;
            if (form == ToolForm.PICKAXE && item == heavyCore)   return ToolForm.MACE;
        }
        return form;
    }

    /** 採掘ティア → INCORRECT_FOR_*_TOOL タグ。0..4 にクランプ。 */
    private static TagKey<Block> incorrectTagFor(int tier) {
        return switch (Math.min(4, Math.max(0, tier))) {
            case 0 -> BlockTags.INCORRECT_FOR_WOODEN_TOOL;
            case 1 -> BlockTags.INCORRECT_FOR_STONE_TOOL;
            case 2 -> BlockTags.INCORRECT_FOR_IRON_TOOL;
            case 3 -> BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
            default -> BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        };
    }

    /** form → 採掘対象タグ。該当無し（SWORD/SPEAR/TRIDENT/BOW 等）なら null。 */
    private static @Nullable TagKey<Block> mineableTagFor(ToolForm form) {
        return switch (form) {
            case PICKAXE, MACE -> BlockTags.MINEABLE_WITH_PICKAXE;
            case SHOVEL        -> BlockTags.MINEABLE_WITH_SHOVEL;
            case AXE           -> BlockTags.MINEABLE_WITH_AXE;
            case HOE           -> BlockTags.MINEABLE_WITH_HOE;
            default            -> null;
        };
    }

    /**
     * 採掘ティア＋form＋採掘速度から Tool component を構築する。
     * - 第1ルール: deniesDrops(INCORRECT_FOR_<tier>_TOOL) — ティア未満のブロックではドロップ無効。
     * - 第2ルール（form に該当タグがあれば）: minesAndDrops(MINEABLE_*, miningSpeed) — 速度ボーナス。
     * Tool.Rule は順次評価で「最初に一致したルール」が勝つ。
     */
    private static Tool buildToolComponent(int tier, ToolForm form, float miningSpeed) {
        List<Tool.Rule> rules = new ArrayList<>(2);
        rules.add(Tool.Rule.deniesDrops(tagHolderSet(incorrectTagFor(tier))));

        TagKey<Block> mineable = mineableTagFor(form);
        if (mineable != null) {
            rules.add(Tool.Rule.minesAndDrops(tagHolderSet(mineable), Math.max(1f, miningSpeed)));
        }

        // バニラ ToolMaterial#applySwordProperties と同じ特殊ルール（クモの巣は正規ドロップ+高速、
        // 竹は事実上瞬時、SWORD_EFFICIENT タグ(葉/カボチャ/コルリーフ等)は1.5倍速、
        // かつ damagePerBlock=2＝剣で採掘するとツルハシ等の2倍の速さで耐久が減る）。
        // form別の mineableTagFor だけでは剣はどのブロックにも速度ボーナスが無く（素手と同速で正しい）、
        // これらのバニラ準拠の例外だけ別途追加する。
        int damagePerBlock = 1;
        if (form == ToolForm.SWORD) {
            rules.add(Tool.Rule.minesAndDrops(HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()), 15.0f));
            rules.add(Tool.Rule.overrideSpeed(tagHolderSet(BlockTags.SWORD_INSTANTLY_MINES), Float.MAX_VALUE));
            rules.add(Tool.Rule.overrideSpeed(tagHolderSet(BlockTags.SWORD_EFFICIENT), 1.5f));
            damagePerBlock = 2;
        }

        return new Tool(rules, 1.0f, damagePerBlock, false);
    }

    private static HolderSet<Block> tagHolderSet(TagKey<Block> tag) {
        return BuiltInRegistries.BLOCK.get(tag).map(named -> (HolderSet<Block>) named).orElse(HolderSet.empty());
    }

    // ---- 性能取得（ランタイム） ----

    /** アクティブロードアウトの ToolStats を返す。登録なしなら null。 */
    public static @Nullable ToolStats getActiveStats(ItemStack stack) {
        var lo = getActiveLoadout(stack);
        return lo != null ? lo.stats() : null;
    }

    /**
     * アクティブロードアウトの ToolStats を Minecraft DataComponent に反映する。
     * ロードアウト切り替え時・登録時・耐久値変化時に呼び出す。
     * - 通常: MAX_DAMAGE/DAMAGE/ATTRIBUTE_MODIFIERS/Tool/モデルヒントを設定。
     * - 破損時（currentDurability ≤ 0）: 攻撃力・採掘ティア・採掘速度を全て無効化し、crashed モデルへ。
     */
    public static void applyComputedStats(ItemStack stack) {
        var lo = getActiveLoadout(stack);
        if (lo == null) return;
        var s = lo.stats();

        // MAX_DAMAGE と DAMAGE は常に同期（耐久バー表示用）。
        int maxDmg = Math.max(1, s.durability());
        int damageTaken = Math.max(0, Math.min(maxDmg, s.durability() - lo.currentDurability()));
        stack.set(DataComponents.MAX_DAMAGE, maxDmg);
        stack.set(DataComponents.DAMAGE, damageTaken);

        if (lo.broken()) {
            // sticky-broken（耐久 0）：crashed モデル + 完全無効化
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
            stack.remove(DataComponents.TOOL);
            clearSpearComponents(stack);
            var hint = Identifier.parse(CrystallographyMod.MOD_ID + ":form/crashed");
            stack.set(ToolComponentsRegistry.TOOL_ACTIVE_MODEL.get(), hint);
            return;
        }

        if (lo.incomplete()) {
            // 結晶欠落：通常の form モデルのまま（欠落スロットの結晶レイヤーは CrystalTintSource が
            // 白く描画する）、ただし攻撃属性／採掘 Tool は無効化＝使用不可。
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
            stack.remove(DataComponents.TOOL);
            clearSpearComponents(stack);
            var hint = Identifier.parse(CrystallographyMod.MOD_ID + ":form/" + lo.form().name().toLowerCase());
            stack.set(ToolComponentsRegistry.TOOL_ACTIVE_MODEL.get(), hint);
            return;
        }

        // 通常状態
        // Attack Damage/Speed は「フォーム固有分」と「結晶合計分」の2つの AttributeModifier に分けて
        // 登録する。フォーム固有分は vanilla の Item.BASE_ATTACK_DAMAGE_ID/BASE_ATTACK_SPEED_ID を
        // そのまま使うため実際の戦闘計算は vanilla と同じ扱いになる。結晶合計分は CRYSTAL_ATK_ID/
        // CRYSTAL_ATK_SPEED_ID で登録し、AttackAttributeTooltipHooks がツールチップ上でこの2つの
        // modifier IDを個別スキップして [素手 + フォーム + 結晶] 形式の自前表示に差し替える
        // （実際の属性値そのものは vanilla の仕組みのままここで確定させる）。
        float formAtk = lo.form().baseAttack();
        float formSpd = lo.form().baseAttackSpeed();

        ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
        b.add(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, formAtk, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
        b.add(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(CRYSTAL_ATK_ID, s.attackDamage() - formAtk, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
        b.add(Attributes.ATTACK_SPEED,
                new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, formSpd, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
        b.add(Attributes.ATTACK_SPEED,
                new AttributeModifier(CRYSTAL_ATK_SPEED_ID, s.attackSpeed() - formSpd, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, b.build());

        // Tool component（採掘ティアゲート + form 別速度ボーナス）
        stack.set(DataComponents.TOOL, buildToolComponent(s.tier(), lo.form(), s.miningSpeed()));

        // SPEAR: 突き(PiercingWeapon)・突撃(KineticWeapon)・リーチ(AttackRange)等、バニラの
        // 槍が持つ専用コンポーネント一式。CrystalSpear は素の Item を継承する（このバージョンの
        // vanillaにも専用の SpearItem クラスが無く、全てこれらのコンポーネントだけで完結するため）。
        if (lo.form() == ToolForm.SPEAR) {
            applySpearComponents(stack, s.tier());
        } else {
            clearSpearComponents(stack);
        }

        var hint = Identifier.parse(CrystallographyMod.MOD_ID + ":form/" + lo.form().name().toLowerCase());
        stack.set(ToolComponentsRegistry.TOOL_ACTIVE_MODEL.get(), hint);
    }

    /**
     * バニラの木/石/鉄/ダイヤ/ネザライト槍が持つ {@code Item.Properties#spear(...)} 引数一式
     * （attackDuration, damageMultiplier, delay, dismountTime, dismountThreshold, knockbackTime,
     * knockbackThreshold, damageTime, damageThreshold）。他フォームの {@code incorrectTagFor}/
     * {@code avgTier} と同じ 0=木 / 1=石 / 2=鉄 / 3=ダイヤ / 4=ネザライト の5段階に対応させ、
     * 結晶構成から算出した平均tierでどのプリセットを使うか動的に決める。
     */
    private static final float[][] SPEAR_TIER_PRESETS = {
            // duration, dmgMult, delay, dismountT, dismountThr, knockbackT, knockbackThr, damageT, damageThr
            {0.65f, 0.70f,  0.75f, 5.0f, 14.0f, 10.0f, 5.1f, 15.0f,  4.6f}, // 0: wood
            {0.75f, 0.82f,  0.70f, 4.5f, 13.0f,  9.0f, 5.1f, 13.75f, 4.6f}, // 1: stone
            {0.95f, 0.95f,  0.60f, 2.5f, 11.0f,  6.75f,5.1f, 11.25f, 4.6f}, // 2: iron
            {1.05f, 1.075f, 0.50f, 3.0f, 10.0f,  6.5f, 5.1f, 10.0f,  4.6f}, // 3: diamond
            {1.15f, 1.20f,  0.40f, 2.5f,  9.0f,  5.5f, 5.1f,  8.75f, 4.6f}, // 4: netherite
    };

    private static void applySpearComponents(ItemStack stack, int avgTier) {
        float[] p = SPEAR_TIER_PRESETS[Math.max(0, Math.min(SPEAR_TIER_PRESETS.length - 1, avgTier))];
        float attackDuration = p[0], damageMultiplier = p[1], delay = p[2],
                dismountTime = p[3], dismountThreshold = p[4],
                knockbackTime = p[5], knockbackThreshold = p[6],
                damageTime = p[7], damageThreshold = p[8];

        stack.set(DataComponents.KINETIC_WEAPON, new KineticWeapon(
                10,
                (int) (delay * 20.0f),
                KineticWeapon.Condition.ofAttackerSpeed((int) (dismountTime * 20.0f), dismountThreshold),
                KineticWeapon.Condition.ofAttackerSpeed((int) (knockbackTime * 20.0f), knockbackThreshold),
                KineticWeapon.Condition.ofRelativeSpeed((int) (damageTime * 20.0f), damageThreshold),
                0.38f,
                damageMultiplier,
                Optional.of(SoundEvents.SPEAR_USE),
                Optional.of(SoundEvents.SPEAR_HIT)
        ));
        stack.set(DataComponents.PIERCING_WEAPON, new PiercingWeapon(
                true, false,
                Optional.of(SoundEvents.SPEAR_ATTACK),
                Optional.of(SoundEvents.SPEAR_HIT)
        ));
        stack.set(DataComponents.ATTACK_RANGE, new AttackRange(2.0f, 4.5f, 2.0f, 6.5f, 0.125f, 0.5f));
        stack.set(DataComponents.MINIMUM_ATTACK_CHARGE, 1.0f);
        stack.set(DataComponents.SWING_ANIMATION, new SwingAnimation(SwingAnimationType.STAB, (int) (attackDuration * 20.0f)));
        stack.set(DataComponents.WEAPON, new Weapon(1));
        // 突撃（チャージ中の全力スプリント）を可能にする：デフォルトのUSE_EFFECTSは走行不可+減速。
        stack.set(DataComponents.USE_EFFECTS, new UseEffects(true, false, 1.0f));
    }

    /** SPEAR以外へフォームが変わった／使用不可になったときに、槍専用コンポーネントの残留を防ぐ。 */
    private static void clearSpearComponents(ItemStack stack) {
        stack.remove(DataComponents.KINETIC_WEAPON);
        stack.remove(DataComponents.PIERCING_WEAPON);
        stack.remove(DataComponents.ATTACK_RANGE);
        stack.remove(DataComponents.MINIMUM_ATTACK_CHARGE);
        stack.remove(DataComponents.SWING_ANIMATION);
        stack.remove(DataComponents.WEAPON);
        stack.remove(DataComponents.USE_EFFECTS);
    }

    /**
     * アクティブロードアウトが存在しない（0件になった／未登録の「素の状態」に戻る）ときに、
     * {@link #applyComputedStats} が書き込んだ耐久値・攻撃属性・採掘 Tool・モデルヒントの
     * 各コンポーネントを消し去る。これを呼ばずに retarget だけ行うと、前のフォームで計算された
     * Tool コンポーネント等が「素の状態」の Item にそのまま残ってしまい、本来ロードアウト無しでは
     * 使えないはずの採掘ボーナス等が残留してしまう。
     */
    public static void clearComputedStats(ItemStack stack) {
        stack.remove(DataComponents.MAX_DAMAGE);
        stack.remove(DataComponents.DAMAGE);
        stack.remove(DataComponents.ATTRIBUTE_MODIFIERS);
        stack.remove(DataComponents.TOOL);
        stack.remove(ToolComponentsRegistry.TOOL_ACTIVE_MODEL.get());
        clearSpearComponents(stack);
    }

    // ---- 耐久値管理 ----

    /**
     * アクティブロードアウトの currentDurability を newCurrent に置き換え、不変リストを更新する。
     * 0 以下にはならない（sticky-zero）。
     */
    public static void updateActiveCurrentDurability(ItemStack stack, int newCurrent) {
        var list = getLoadout(stack);
        int activeIdx = getActiveIndex(stack);
        int clamped = Math.max(0, newCurrent);
        var updated = new ArrayList<ToolLoadout>(list.entries().size());
        for (var e : list.entries()) {
            if (e.slotIndex() == activeIdx) {
                updated.add(e.withCurrentDurability(clamped));
            } else {
                updated.add(e);
            }
        }
        stack.set(ToolComponentsRegistry.TOOL_LOADOUTS.get(), new ToolLoadoutList(List.copyOf(updated)));
    }

    /**
     * 結晶の入れ替え／取り外しに伴い、各ロードアウトを再評価する。状態は以下の3種類：
     * <ul>
     *   <li><b>broken (sticky)</b>: currentDurability=0。耐久を使い切った状態。復活しない。</li>
     *   <li><b>incomplete</b>: 参照する結晶スロットの少なくとも1つが空。使用不可だが復元可能
     *       （結晶が戻れば normal に復帰）。stats と currentDurability は欠落前の値を保持する。</li>
     *   <li><b>normal</b>: 全結晶あり、使用可。</li>
     * </ul>
     * 全結晶が揃っている場合は stats を結晶構成で再計算し、damageTaken = oldStats.durability - oldCurrent
     * を引き継いで newCurrent = newStats.durability - damageTaken を適用。結果がマイナスなら 0（sticky-broken）。
     */
    public static void reconcileLoadouts(ItemStack tool, int rodTier,
                                         ResourceHandler<ItemResource> crystalInv) {
        var list = getLoadout(tool);
        if (list.entries().isEmpty()) return;

        var updated = new ArrayList<ToolLoadout>(list.entries().size());
        boolean anyChanged = false;

        for (var lo : list.entries()) {
            // sticky-broken は触らない（incomplete フラグだけは現状に追従させて整合を保つ）。
            // 例外：原石バッテリーモード（原石系結晶を含む）は tier*128 の恒久破損とは別モデルなので、
            // 原石が補充されていれば復活させる（damageItem 側で原石を使い切って0になった状態）。
            if (lo.broken()) {
                boolean nowIncomplete = isAnyCrystalMissing(lo.crystalIndices(), crystalInv);
                if (!nowIncomplete && findOreCrystalIndex(lo.crystalIndices(), crystalInv) >= 0) {
                    ToolForm reform = upgradeFormForCrystals(lo.form(), lo.crystalIndices(), crystalInv);
                    ToolStats newStats = buildStats(reform, rodTier, lo.crystalIndices(), crystalInv);
                    updated.add(new ToolLoadout(lo.slotIndex(), reform, lo.crystalIndices(),
                            newStats, newStats.durability(), false));
                    anyChanged = true;
                    continue;
                }
                if (lo.incomplete() != nowIncomplete) {
                    updated.add(lo.withIncomplete(nowIncomplete));
                    anyChanged = true;
                } else {
                    updated.add(lo);
                }
                continue;
            }

            boolean nowIncomplete = isAnyCrystalMissing(lo.crystalIndices(), crystalInv);

            if (nowIncomplete) {
                // 欠落中：stats／耐久は復元のために据え置き、incomplete のみ立てる。
                if (!lo.incomplete()) {
                    updated.add(lo.withIncomplete(true));
                    anyChanged = true;
                } else {
                    updated.add(lo);
                }
                continue;
            }

            // 全結晶あり：stats 再計算 + 耐久値の「削れた分」引き継ぎ + form 昇格 + incomplete 解除。
            ToolForm reform = upgradeFormForCrystals(lo.form(), lo.crystalIndices(), crystalInv);
            ToolStats newStats = buildStats(reform, rodTier, lo.crystalIndices(), crystalInv);

            int damageTaken = lo.stats().durability() - lo.currentDurability();
            int newCurrent  = newStats.durability() - damageTaken;
            int clamped     = Math.max(0, newCurrent);  // < 0 → 0 (sticky-broken)

            if (!newStats.equals(lo.stats()) || clamped != lo.currentDurability()
                    || reform != lo.form() || lo.incomplete()) {
                updated.add(new ToolLoadout(lo.slotIndex(), reform, lo.crystalIndices(),
                        newStats, clamped, false));
                anyChanged = true;
            } else {
                updated.add(lo);
            }
        }

        if (anyChanged) {
            tool.set(ToolComponentsRegistry.TOOL_LOADOUTS.get(), new ToolLoadoutList(List.copyOf(updated)));
            applyComputedStats(tool);
        }
    }

    private static boolean isAnyCrystalMissing(int[] crystalIndices, ResourceHandler<ItemResource> crystalInv) {
        for (int idx : crystalIndices) {
            if (idx < 0 || idx >= crystalInv.size()) return true;
            if (crystalInv.getResource(idx).isEmpty()) return true;
        }
        return false;
    }

    // ---- retarget（アクティブフォームに応じた Item への差し替え） ----

    /**
     * stack の Item が既に newItem なら何もせず返す。そうでなければ、stack の全コンポーネント
     * パッチ（ロードアウト一覧・結晶インベントリ・耐久値等）を引き継いだ newItem の新しい
     * ItemStack を作って返す。ItemStack の Item は生成後不変のため、フォーム変更は必ずこの
     * 「新しいスタックを作って呼び出し元のスロット/手に書き戻す」形になる。
     */
    public static ItemStack retarget(ItemStack stack, Item newItem) {
        if (stack.getItem() == newItem) return stack;
        return new ItemStack(newItem.builtInRegistryHolder(), stack.getCount(), stack.getComponentsPatch());
    }

    /**
     * stack のアクティブロードアウトのフォームに対応する Item へ retarget する（既に一致していれば
     * 何もしない）。アクティブロードアウトが無ければ「素の状態」の Item（ロッド/ワンド × tier）へ戻し、
     * その際は前フォームの Tool/攻撃属性コンポーネントも {@link #clearComputedStats} で消す。
     * 宝飾台での登録・削除・並べ替え・結晶編集（フォーム昇格を含む）と、ホットキー+ホイールの
     * ロードアウト切り替えの、アクティブフォームが変わりうる箇所は全てここを通す。
     */
    public static ItemStack retargetToActiveForm(ItemStack stack, int tier) {
        var lo = getActiveLoadout(stack);
        Item target;
        if (lo != null) {
            target = ToolFormRegistry.itemFor(lo.form(), tier);
        } else {
            clearComputedStats(stack);
            ICrystalTool.Kind kind = (stack.getItem() instanceof ICrystalTool ct) ? ct.getKind() : ICrystalTool.Kind.ROD;
            target = ToolFormRegistry.blankFor(kind, tier);
        }
        return retarget(stack, target);
    }

    /**
     * 宝飾台での見た目用 retarget。仮登録中（draft）があればその form を優先して見た目だけ
     * 反映し（耐久値・攻撃属性等の実データはアクティブロードアウトのまま触らない＝
     * {@link #applyComputedStats} は呼ばない）、draft が無ければ {@link #retargetToActiveForm}
     * と同じくアクティブロードアウトの form に従う。
     * <p>
     * {@code bindCrystalsBacking} の reconcile 後retarget等、宝飾台の中央スロットを書き戻す
     * 箇所は {@link #retargetToActiveForm} ではなくこちらを使うこと。そうしないと、REGISTRIES
     * 編集でdraftを別formにプレビュー中でも、reconcile起因の再retargetが毎回「本来のアクティブ
     * ロードアウトのform」へ巻き戻してしまう。
     */
    public static ItemStack retargetForDisplay(ItemStack stack, int tier) {
        var draft = getDraftLoadout(stack);
        if (draft != null) {
            return retarget(stack, ToolFormRegistry.itemFor(draft.form(), tier));
        }
        return retargetToActiveForm(stack, tier);
    }

    // ---- Item override 委譲用ロジック ----

    /** tier を {@link ICrystalTool} 経由で取得する。該当しなければ 1。 */
    public static int getTier(ItemStack stack) {
        return (stack.getItem() instanceof ICrystalTool ct) ? ct.getTier() : 1;
    }

    public static ItemAttributeModifiers defaultAttributeModifiers(ItemStack stack) {
        return ItemAttributeModifiers.EMPTY;
    }

    /**
     * アクティブロードアウトが未登録・結晶欠落・破損のいずれかで使用不可かどうか。
     * {@link ItemInstance} を受け取る（{@link ItemStack} はそのサブタイプなのでそのまま渡せる）ため、
     * {@code canPerformAction}（引数が {@code ItemInstance}）と {@code hurtEnemy}/{@code mineBlock}/
     * {@code useOn}/{@code use}（引数が {@code ItemStack}）のどちらのガードにも共通して使える。
     */
    public static boolean isUnusable(ItemInstance stack) {
        ToolLoadoutList list = stack.get(ToolComponentsRegistry.TOOL_LOADOUTS.get());
        Integer activeIdx = stack.get(ToolComponentsRegistry.TOOL_ACTIVE_INDEX.get());
        if (list == null || activeIdx == null) return true;
        var lo = list.getAtSlot(activeIdx).orElse(null);
        return lo == null || lo.unusable();
    }

    /**
     * NeoForge の耐久値ダメージフック本体。デフォルトはバニラの DAMAGE 加算＋上限到達で破壊。
     * ここではアクティブロードアウトの currentDurability を直接管理し、0 以下に達したら破損固定。
     * 0 を返してバニラ側の DAMAGE 自動加算・破壊処理を完全に肩代わりする。
     */
    public static <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity,
                                                           Consumer<Item> onBroken) {
        if (amount <= 0) return 0;
        var lo = getActiveLoadout(stack);
        if (lo == null) return 0;
        if (lo.unusable()) return 0; // 破損 or 結晶欠落中 → 耐久を減らさない

        int newCurrent = lo.currentDurability() - amount;
        if (newCurrent <= 0 && entity != null) {
            newCurrent = tryOreBatteryRefill(stack, lo, entity, newCurrent);
        }
        newCurrent = Math.max(0, newCurrent);
        updateActiveCurrentDurability(stack, newCurrent);
        // 表示用 DAMAGE と、必要なら破損切替（モデル/属性）を反映。
        applyComputedStats(stack);
        // バニラ側にはダメージ無し（破壊もしない）。
        return 0;
    }

    /**
     * 原石バッテリーモード（アクティブロードアウトが原石系結晶を含む）で耐久が尽きた瞬間、
     * 原石スタックから1個消費して全回復させる（「それが0になると原石を一つ消費して
     * 全回復からやり直し」）。原石スタックが最後の1個だった場合は消費した上で0のまま返す
     * （＝使用不可。次に宝飾台を開いたとき reconcileLoadouts が結晶欠落=incomplete として検出する）。
     * 原石バッテリーでなければ元の newCurrent をそのまま返す（通常の sticky-broken）。
     */
    private static int tryOreBatteryRefill(ItemStack stack, ToolLoadout lo, LivingEntity entity, int newCurrent) {
        int tier = getTier(stack);
        var crystalInv = ToolInventory.get(stack, crystalSlotCount(tier), entity.level().registryAccess());
        int oreIdx = findOreCrystalIndex(lo.crystalIndices(), crystalInv);
        if (oreIdx < 0) return newCurrent;

        ItemStack ore = crystalInv.getResource(oreIdx).toStack(crystalInv.getAmountAsInt(oreIdx));
        if (ore.isEmpty()) return newCurrent;
        ore.shrink(1);
        crystalInv.set(oreIdx, ItemResource.of(ore), ore.getCount());
        return ore.isEmpty() ? 0 : lo.stats().durability();
    }

    /**
     * アクティブロードアウトの form に応じたバニラの道具アクション（AxeItem/HoeItem 等の
     * 専用クラスが持つ機能）を有効化する。
     */
    public static boolean canPerformAction(ItemInstance stack, ItemAbility itemAbility) {
        ToolLoadoutList list = stack.get(ToolComponentsRegistry.TOOL_LOADOUTS.get());
        Integer activeIdx = stack.get(ToolComponentsRegistry.TOOL_ACTIVE_INDEX.get());
        if (list == null || activeIdx == null) return false;
        var lo = list.getAtSlot(activeIdx).orElse(null);
        if (lo == null || lo.unusable()) return false;
        return abilitiesFor(lo.form()).contains(itemAbility);
    }

    private static Set<ItemAbility> abilitiesFor(ToolForm form) {
        return switch (form) {
            case AXE    -> ItemAbilities.DEFAULT_AXE_ACTIONS;
            case HOE    -> ItemAbilities.DEFAULT_HOE_ACTIONS;
            case SHOVEL -> ItemAbilities.DEFAULT_SHOVEL_ACTIONS;
            case SWORD  -> Set.of(ItemAbilities.SWORD_SWEEP);
            case SHEARS -> ItemAbilities.DEFAULT_SHEARS_ACTIONS;
            default     -> Set.of();
        };
    }

    /**
     * form 別のバニラ右クリック挙動（AxeItem/HoeItem/ShovelItem#useOn の移植）。
     * いずれも {@link BlockState#getToolModifiedState} が対応ブロックか判定＋実際の状態変更まで
     * 行うので、ここでは「呼んで結果を適用する」だけの薄いラッパーで済む。
     * <p>
     * NOTE: AXE/HOE/SHOVEL がそれぞれの実 vanilla クラス（{@code AxeItem}等）を継承するフォーム別
     * クラスへ移行した後は、この分岐は到達不能になる想定（Phase 7 で削除予定の暫定実装）。
     */
    public static InteractionResult useOn(UseOnContext context) {
        var lo = getActiveLoadout(context.getItemInHand());
        if (lo != null && !lo.unusable()) {
            InteractionResult result = switch (lo.form()) {
                case AXE    -> tryAxeUseOn(context);
                case HOE    -> tryHoeTill(context);
                case SHOVEL -> tryShovelUseOn(context);
                default     -> InteractionResult.PASS;
            };
            if (result != InteractionResult.PASS) return result;
        }
        return InteractionResult.PASS;
    }

    /** AxeItem#useOn の移植：原木剥ぎ→銅の風化戻し→ミツロウ落としの順に試す。 */
    private static InteractionResult tryAxeUseOn(UseOnContext context) {
        Player player = context.getPlayer();
        // オフハンドが盾等の「ブロック中」アイテムを構えている間は、バニラの斧と同様に反応しない。
        if (context.getHand() == InteractionHand.MAIN_HAND
                && player != null
                && player.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS)
                && !player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState oldState = level.getBlockState(pos);

        BlockState stripped = oldState.getToolModifiedState(context, ItemAbilities.AXE_STRIP, false);
        if (stripped != null) {
            level.playSound(player, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0f, 1.0f);
            return applyAxeResult(context, stripped);
        }
        BlockState scraped = oldState.getToolModifiedState(context, ItemAbilities.AXE_SCRAPE, false);
        if (scraped != null) {
            spawnAxeSoundAndParticle(level, pos, player, oldState, SoundEvents.AXE_SCRAPE, 3005);
            return applyAxeResult(context, scraped);
        }
        BlockState waxOff = oldState.getToolModifiedState(context, ItemAbilities.AXE_WAX_OFF, false);
        if (waxOff != null) {
            spawnAxeSoundAndParticle(level, pos, player, oldState, SoundEvents.AXE_WAX_OFF, 3004);
            return applyAxeResult(context, waxOff);
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult applyAxeResult(UseOnContext context, BlockState newState) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        level.setBlock(pos, newState, 11);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, newState));
        if (player != null) {
            context.getItemInHand().hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
        }
        return InteractionResult.SUCCESS;
    }

    /** AxeItem#spawnSoundAndParticle の移植（銅の風化戻し／ミツロウ落とし用。二連チェストの追従含む）。 */
    private static void spawnAxeSoundAndParticle(Level level, BlockPos pos, @Nullable Player player,
                                                 BlockState oldState, SoundEvent sound, int particle) {
        level.playSound(player, pos, sound, SoundSource.BLOCKS, 1.0f, 1.0f);
        level.levelEvent(player, particle, pos, 0);
        if (oldState.getBlock() instanceof ChestBlock && oldState.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos neighborPos = ChestBlock.getConnectedBlockPos(pos, oldState);
            level.gameEvent(GameEvent.BLOCK_CHANGE, neighborPos, GameEvent.Context.of(player, level.getBlockState(neighborPos)));
            level.levelEvent(player, particle, neighborPos, 0);
        }
    }

    /** HoeItem#useOn の移植：耕作（開墾地化・根付いた土のハンギングルーツ回収は getToolModifiedState 内で処理）。 */
    private static InteractionResult tryHoeTill(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState tilled = level.getBlockState(pos).getToolModifiedState(context, ItemAbilities.HOE_TILL, false);
        if (tilled == null) return InteractionResult.PASS;

        Player player = context.getPlayer();
        level.playSound(player, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0f, 1.0f);
        if (!level.isClientSide()) {
            level.setBlock(pos, tilled, 11);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, tilled));
            if (player != null) {
                context.getItemInHand().hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** ShovelItem#useOn の移植：道整地、無ければ焚き火消火（消火の効果音・演出は getToolModifiedState 内で処理）。 */
    private static InteractionResult tryShovelUseOn(UseOnContext context) {
        if (context.getClickedFace() == Direction.DOWN) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState blockState = level.getBlockState(pos);
        Player player = context.getPlayer();

        BlockState updatedState;
        BlockState flattened = blockState.getToolModifiedState(context, ItemAbilities.SHOVEL_FLATTEN, false);
        if (flattened != null && level.getBlockState(pos.above()).isAir()) {
            level.playSound(player, pos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0f, 1.0f);
            updatedState = flattened;
        } else {
            updatedState = blockState.getToolModifiedState(context, ItemAbilities.SHOVEL_DOUSE, false);
            if (updatedState != null && !level.isClientSide()) {
                level.levelEvent(null, 1009, pos, 0);
            }
        }

        if (updatedState == null) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            level.setBlock(pos, updatedState, 11);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, updatedState));
            if (player != null) {
                context.getItemInHand().hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** 仮登録中／登録済みロードアウトのツールチップ本文。 */
    public static void appendHoverText(ItemStack stack,
                                       Item.TooltipContext ctx,
                                       TooltipDisplay display,
                                       Consumer<Component> out,
                                       TooltipFlag flag) {
        // 仮登録中（FORM_SELECTED）: 組み立て中のステータスを表示
        var draft = getDraftLoadout(stack);
        if (draft != null) {
            int selected = (int) java.util.Arrays.stream(draft.crystalIndices()).filter(i -> i >= 0).count();
            out.accept(Component.literal("[一時登録中: " + draft.form().name() + "]")
                    .withStyle(ChatFormatting.YELLOW));
            out.accept(Component.literal("結晶: " + selected + " / " + ToolLoadout.CRYSTAL_SLOTS)
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        var list = getLoadout(stack).entries();
        int idx  = getActiveIndex(stack);
        out.accept(Component.literal("Loadout: " + list.size() + "  [Active " + idx + "]")
                .withStyle(ChatFormatting.GRAY));

        var lo = getActiveLoadout(stack);
        if (lo != null) {
            out.accept(Component.literal("- " + lo.form().name()).withStyle(ChatFormatting.AQUA));
        } else {
            out.accept(Component.literal("No registered tool").withStyle(ChatFormatting.RED));
        }
    }
}
