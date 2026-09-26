package net.ookasamoti.crystallography.common.item.tool;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.ookasamoti.crystallography.common.item.tool.component.ToolForm;
import net.ookasamoti.crystallography.common.item.tool.component.ToolLoadout;
import net.ookasamoti.crystallography.data.CrystalStatsRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 結晶の「固有アビリティ」＝ stats JSON の {@code traits} フィールド（{@link CrystalStatsRegistry}
 * 経由）の実装本体。シジル（{@link SigilRegistry}、プレイヤーが後付けする）と違い、traits は
 * 結晶そのものに最初から備わっている固定の特性で、コストも消費しない。
 * <p>
 * 複数の結晶が同じトレイトを持つ場合の「Lv」の数え方は2種類ある：
 * <ul>
 *   <li><b>個数ベース</b>（{@link #countTrait}）：そのトレイトを持つ結晶の枚数（同じ結晶を複数
 *       積んでも増える）。効果範囲拡張・効果時間延長・補正強化グループの大半がこちら。</li>
 *   <li><b>種類ベース</b>（{@link #distinctCrystalTypesWithTrait}）：そのトレイトを持つ「異なる
 *       結晶アイテム」の種類数（同じ結晶を複数積んでも増えない）。golden/fools_gold 専用の特例
 *       （例：金を2〜3個積んでも幸運は上がらないが、金+ブルートゴールドの組み合わせでLv2になる）。</li>
 * </ul>
 * 上限（maxLevel）は特に記載の無いものはLv1（複数積んでも効果は変わらない）、下記の3グループに
 * 属するものだけLv3（個別に上限が定められているものを除く）。
 * <ul>
 *   <li>効果範囲拡張：resonance / freezing / magnetism</li>
 *   <li>効果時間延長：blazing / levitation / wither</li>
 *   <li>補正強化：diamond / breezing / conduction / lifesteal / amplify</li>
 * </ul>
 */
public final class CrystalTraitLogic {

    /** 特に記載の無いトレイトの上限Lv（複数積んでも効果は変わらない）。 */
    public static final int DEFAULT_MAX_LEVEL = 1;
    /** 効果範囲拡張・効果時間延長・補正強化グループの上限Lv。 */
    public static final int SCALING_MAX_LEVEL = 3;

    // ---- 数値折り込み系（buildStats） ----
    public static final String DIAMOND = "diamond";
    public static final String PURE = "pure";
    public static final String BANDIT = "bandit";
    /** diamond トレイト1個あたりの耐久値ボーナス（tier0/1結晶の基礎hardnessと同程度の値）。補正強化。 */
    public static final int DIAMOND_DURABILITY_BONUS = 64;
    /** pure トレイトを持つ結晶自身の clarity 寄与に掛かる倍率。 */
    public static final float PURE_CLARITY_MULTIPLIER = 1.2f;
    /** bandit トレイト1個あたりの、AXE フォーム限定の追加攻撃力。Lv上限1＝複数積んでも+2固定。 */
    public static final float BANDIT_AXE_DAMAGE_BONUS = 2.0f;

    // ---- vanilla エンチャント付与系（CrystalToolLogic#applyGrantedEnchantments） ----
    public static final String SHARP = "sharp";
    public static final String CONDUCTION = "conduction";
    public static final String GOLDEN = "golden";
    public static final String FOOLS_GOLD = "fools_gold";

    // ---- 耐久力エンチャント実効レベル系（applyUnbreaking） ----
    public static final String BOLTZ = "boltz";
    /** boltz トレイトが1つでもあれば、tier由来のUnbreaking実効レベル底上げにさらに+1する。 */
    public static final int BOLTZ_UNBREAKING_BONUS = 1;

    // ---- 宝石彫刻台のcarat予算ボーナス系（LapidaryAnvilOperations） ----
    public static final String WARD = "ward";
    public static final String PINKY = "pinky";
    public static final int WARD_CARAT_BONUS = 2;
    public static final int PINKY_CARAT_BONUS = 1;

    // ---- 戦闘フック系（CrystalTraitCombatHooks / SigilCombatHooks） ----
    public static final String ORDER = "order";
    public static final String LIFESTEAL = "lifesteal";
    public static final String WITHER = "wither";
    public static final String LEVITATION = "levitation";
    /** 旧 potion_amplify。シジル由来の命中時効果にも適用される。 */
    public static final String AMPLIFY = "amplify";
    /** 旧 potion_duration。シジル由来の命中時効果にも適用される。Lv上限1（時間延長率は固定）。 */
    public static final String DURATION = "duration";
    public static final String BREEZING = "breezing";
    public static final String SONIC = "sonic";
    /** 旧 swap。遠距離攻撃：地形にヒットでエンダーパール移動、mobにヒットで位置入れ替え。 */
    public static final String ENDER = "ender";

    // ---- 着火フック系（CrystalTraitFireHooks） ----
    public static final String BLAZING = "blazing";

    // ---- 採掘フック系（CrystalTraitMiningHooks） ----
    /** 旧 obsidian_mining。 */
    public static final String OBSIDIAN_TEAR = "obsidian_tear";
    /** 新規：深層岩グループの採掘速度に補正。 */
    public static final String DEPBORN = "depborn";
    public static final String AQUATIC = "aquatic";
    public static final String SMELTING = "smelting";
    public static final String FREEZING = "freezing";
    /** 新規：採掘時、周囲の同系統ブロックも採掘する（対象はフォームごとに異なる）。 */
    public static final String RESONANCE = "resonance";

    // ---- 常時効果系（CrystalTraitTickHooks / AmuletStatLogic） ----
    public static final String MAGNETISM = "magnetism";
    /** ツールでは aquatic の効果を強化、防具では亀の甲羅ヘルメット効果／水中歩行+1。 */
    public static final String TURTLE = "turtle";

    private CrystalTraitLogic() {}

    /** {@code crystal} が指定トレイトを持つか（CrystalStatsRegistry 経由）。 */
    public static boolean stackHasTrait(ItemStack crystal, String trait) {
        return CrystalStatsRegistry.get(crystal).map(r -> r.traits().contains(trait)).orElse(false);
    }

    /** アクティブロードアウトが参照する結晶（最大3個）の中に、指定トレイトを持つものが1つでもあるか。 */
    public static boolean loadoutHasTrait(ToolLoadout lo, ItemStacksResourceHandler crystalInv, String trait) {
        return countTrait(lo, crystalInv, trait) > 0;
    }

    /** アクティブロードアウトが参照する結晶のうち、指定トレイトを持つものの個数。 */
    public static int countTrait(ToolLoadout lo, ItemStacksResourceHandler crystalInv, String trait) {
        int n = 0;
        for (int idx : lo.crystalIndices()) {
            if (idx < 0 || idx >= crystalInv.size()) continue;
            ItemStack crystal = crystalInv.getResource(idx).toStack(crystalInv.getAmountAsInt(idx));
            if (crystal.isEmpty()) continue;
            if (stackHasTrait(crystal, trait)) n++;
        }
        return n;
    }

    /**
     * アクティブロードアウトが参照する結晶のうち、指定トレイトを持つ「異なる結晶アイテム」の
     * 種類数。golden/fools_gold の特例判定用（同じ結晶を複数積んでも増えない）。
     */
    public static int distinctCrystalTypesWithTrait(ToolLoadout lo, ItemStacksResourceHandler crystalInv, String trait) {
        Set<Item> distinct = new HashSet<>();
        for (int idx : lo.crystalIndices()) {
            if (idx < 0 || idx >= crystalInv.size()) continue;
            ItemStack crystal = crystalInv.getResource(idx).toStack(crystalInv.getAmountAsInt(idx));
            if (crystal.isEmpty()) continue;
            if (stackHasTrait(crystal, trait)) distinct.add(crystal.getItem());
        }
        return distinct.size();
    }

    /** 個数ベースのLv（{@link #countTrait} を {@code maxLevel} でクランプしたもの）。 */
    public static int traitLevel(ToolLoadout lo, ItemStacksResourceHandler crystalInv, String trait, int maxLevel) {
        return Math.min(countTrait(lo, crystalInv, trait), maxLevel);
    }

    /** {@code crystal} 単体の carat 予算ボーナス（宝石彫刻台でのシジル付与コスト判定用）。 */
    public static int caratBonusFor(ItemStack crystal) {
        int bonus = 0;
        if (stackHasTrait(crystal, WARD)) bonus += WARD_CARAT_BONUS;
        if (stackHasTrait(crystal, PINKY)) bonus += PINKY_CARAT_BONUS;
        return bonus;
    }

    /** amplify（旧 potion_amplify、補正強化グループ）：Lvぶんの増幅レベル加算。 */
    public static int amplifyBonus(ToolLoadout lo, ItemStacksResourceHandler crystalInv) {
        return traitLevel(lo, crystalInv, AMPLIFY, SCALING_MAX_LEVEL);
    }

    /** duration（旧 potion_duration、Lv上限1＝倍率は固定）：あれば1.5倍、無ければ1倍。 */
    public static float durationMultiplier(ToolLoadout lo, ItemStacksResourceHandler crystalInv) {
        return loadoutHasTrait(lo, crystalInv, DURATION) ? DURATION_MULTIPLIER : 1f;
    }

    private static final float DURATION_MULTIPLIER = 1.5f;

    // ---- vanilla エンチャント付与（applyGrantedEnchantments から呼ばれる） ----

    private static final List<ToolForm> SHARP_FORMS = List.of(ToolForm.SWORD, ToolForm.AXE, ToolForm.SPEAR, ToolForm.TRIDENT, ToolForm.MACE);
    private static final List<ToolForm> CONDUCTION_FORMS = List.of(ToolForm.SWORD, ToolForm.AXE, ToolForm.SPEAR, ToolForm.TRIDENT);
    private static final List<ToolForm> MINING_FORMS = List.of(ToolForm.PICKAXE, ToolForm.AXE, ToolForm.HOE, ToolForm.SHOVEL);
    private static final List<ToolForm> MELEE_FORMS = List.of(ToolForm.SWORD, ToolForm.AXE, ToolForm.SPEAR);
    /** conduction（補正強化グループ）：結晶の個数でLv1〜3（=火属性I〜III）にスケール。 */
    public static final int CONDUCTION_MAX_LEVEL = SCALING_MAX_LEVEL;

    private static void mergeIfFormMatches(Map<ResourceKey<Enchantment>, Integer> granted, ToolForm form,
                                           List<ToolForm> matchForms, ResourceKey<Enchantment> enchant, int level) {
        if (level > 0 && matchForms.contains(form)) granted.merge(enchant, level, Math::max);
    }

    /**
     * ロードアウト全体の結晶構成から、複数結晶にまたがる判定が必要なトレイト
     * （sharp＝結晶数に関わらずLv1固定、conduction＝結晶の個数でLv1〜3、golden/fools_gold＝
     * 異なる結晶種の数でLv1〜2）の vanilla エンチャント付与を計算し、{@code granted} マップへ
     * 合流させる（既存キーがあればレベルは大きい方を採用）。
     * {@link CrystalToolLogic#applyGrantedEnchantments} のシジル分の集計と同じマップに対して
     * 追加で呼び出すことで、シジルとトレイトどちらか強い方が反映される。
     */
    public static void mergeLoadoutEnchantGrants(ToolLoadout lo, ItemStacksResourceHandler crystalInv,
                                                 Map<ResourceKey<Enchantment>, Integer> granted) {
        ToolForm form = lo.form();

        if (countTrait(lo, crystalInv, SHARP) > 0) {
            mergeIfFormMatches(granted, form, SHARP_FORMS, Enchantments.SHARPNESS, 1);
        }

        int conductionLevel = traitLevel(lo, crystalInv, CONDUCTION, CONDUCTION_MAX_LEVEL);
        mergeIfFormMatches(granted, form, CONDUCTION_FORMS, Enchantments.FIRE_ASPECT, conductionLevel);

        // golden/fools_gold：どちらも「幸運(採掘系)＋ドロップ増加(近接系)」を、異なる結晶種の
        // 数（1個なら片方のみでもLv1、2種類揃えばLv2）ぶん付与する。同じ形の効果を持つ別トレイト
        // なので、両方が同時に成立していても Math.max マージにより強い方だけが反映される。
        int goldenLevel = distinctCrystalTypesWithTrait(lo, crystalInv, GOLDEN);
        mergeIfFormMatches(granted, form, MINING_FORMS, Enchantments.FORTUNE, goldenLevel);
        mergeIfFormMatches(granted, form, MELEE_FORMS, Enchantments.LOOTING, goldenLevel);

        int foolsGoldLevel = distinctCrystalTypesWithTrait(lo, crystalInv, FOOLS_GOLD);
        mergeIfFormMatches(granted, form, MINING_FORMS, Enchantments.FORTUNE, foolsGoldLevel);
        mergeIfFormMatches(granted, form, MELEE_FORMS, Enchantments.LOOTING, foolsGoldLevel);
    }
}
