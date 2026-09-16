package net.ookasamoti.crystallography.common.item.tool;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.ookasamoti.crystallography.common.item.tool.component.ToolForm;
import net.ookasamoti.crystallography.common.item.tool.component.ToolLoadout;
import net.ookasamoti.crystallography.data.CrystalStatsRegistry;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 結晶の「固有アビリティ」＝ stats JSON の {@code traits} フィールド（{@link CrystalStatsRegistry}
 * 経由）の実装本体。シジル（{@link SigilRegistry}、プレイヤーが後付けする）と違い、traits は
 * 結晶そのものに最初から備わっている固定の特性で、コストも消費しない。Wiki の各結晶ページに
 * 「想定効果」として書かれていたものを実装する（2026-09-17）。
 * <p>
 * 効果の適用箇所は種類によって異なる：
 * <ul>
 *   <li>数値ステータスへの折り込み（diamond/pure/bandit）→ {@link CrystalToolLogic#buildStats}</li>
 *   <li>vanilla エンチャント付与（sharp/conduction/golden/fools_gold）→
 *       {@link CrystalToolLogic}（applyGrantedEnchantments。シジルの grants と同じマップに合流し、
 *       同じエンチャントならレベルの大きい方が勝つ）</li>
 *   <li>耐久力エンチャント実効レベルへの加算（boltz）→ {@link CrystalToolLogic}（applyUnbreaking）</li>
 *   <li>シジル付与コストの予算＝carat の底上げ（ward/pinky）→
 *       {@link net.ookasamoti.crystallography.common.util.LapidaryAnvilOperations}</li>
 *   <li>戦闘時の条件付き効果（order/lifesteal/wither/levitation/potion_amplify/potion_duration/
 *       breezing/sonic）→ {@link CrystalTraitCombatHooks}</li>
 *   <li>着火継続時間の延長（blazing）→ {@link CrystalTraitFireHooks}</li>
 *   <li>採掘関連（obsidian_mining/aquatic/smelting/freezing）→ {@link CrystalTraitMiningHooks}</li>
 *   <li>常時効果（magnetism/turtle）→ {@link CrystalTraitTickHooks}</li>
 * </ul>
 * {@code resonance}（採掘時、周囲の同系統ブロックへソナーをリレー）と {@code swap}（遠距離攻撃が
 * ヒットした敵と位置を入れ替える）は、前者は具体的な結果が仕様上定義しきれず、後者は「どの武器が
 * この飛翔体を発射したか」を辿る基盤がまだ無く安全に実装できないため、このクラスでは未実装のまま
 * 残している。{@code mace}/{@code trident} は既存の {@link CrystalToolLogic#upgradeFormForCrystals}
 * が担うフォーム昇格トリガーそのものなので、ここでは何もしない。{@code fire}（黄銅鉱の副特性）は
 * Wiki 上も元々効果の記載が無い。
 */
public final class CrystalTraitLogic {

    // ---- 数値折り込み系（buildStats） ----
    public static final String DIAMOND = "diamond";
    public static final String PURE = "pure";
    public static final String BANDIT = "bandit";
    /** diamond トレイト1個あたりの耐久値ボーナス（tier0/1結晶の基礎hardnessと同程度の値）。 */
    public static final int DIAMOND_DURABILITY_BONUS = 64;
    /** pure トレイトを持つ結晶自身の clarity 寄与に掛かる倍率。 */
    public static final float PURE_CLARITY_MULTIPLIER = 1.2f;
    /** bandit トレイト1個あたりの、AXE フォーム限定の追加攻撃力。 */
    public static final float BANDIT_AXE_DAMAGE_BONUS = 2.0f;

    // ---- vanilla エンチャント付与系（applyGrantedEnchantments） ----
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

    // ---- 戦闘フック系（CrystalTraitCombatHooks） ----
    public static final String ORDER = "order";
    public static final String LIFESTEAL = "lifesteal";
    public static final String WITHER = "wither";
    public static final String LEVITATION = "levitation";
    public static final String POTION_AMPLIFY = "potion_amplify";
    public static final String POTION_DURATION = "potion_duration";
    public static final String BREEZING = "breezing";
    public static final String SONIC = "sonic";

    // ---- 着火フック系（CrystalTraitFireHooks） ----
    public static final String BLAZING = "blazing";

    // ---- 採掘フック系（CrystalTraitMiningHooks） ----
    public static final String OBSIDIAN_MINING = "obsidian_mining";
    public static final String AQUATIC = "aquatic";
    public static final String SMELTING = "smelting";
    public static final String FREEZING = "freezing";

    // ---- 常時効果系（CrystalTraitTickHooks） ----
    public static final String MAGNETISM = "magnetism";
    public static final String TURTLE = "turtle";

    private CrystalTraitLogic() {}

    /** {@code crystal} が指定トレイトを持つか（CrystalStatsRegistry 経由）。 */
    public static boolean stackHasTrait(ItemStack crystal, String trait) {
        return CrystalStatsRegistry.get(crystal).map(r -> r.traits().contains(trait)).orElse(false);
    }

    /** アクティブロードアウトが参照する結晶（最大3個）の中に、指定トレイトを持つものが1つでもあるか。 */
    public static boolean loadoutHasTrait(ToolLoadout lo, ResourceHandler<ItemResource> crystalInv, String trait) {
        return countTrait(lo, crystalInv, trait) > 0;
    }

    /** アクティブロードアウトが参照する結晶のうち、指定トレイトを持つものの個数。 */
    public static int countTrait(ToolLoadout lo, ResourceHandler<ItemResource> crystalInv, String trait) {
        int n = 0;
        for (int idx : lo.crystalIndices()) {
            if (idx < 0 || idx >= crystalInv.size()) continue;
            ItemStack crystal = crystalInv.getResource(idx).toStack(crystalInv.getAmountAsInt(idx));
            if (crystal.isEmpty()) continue;
            if (stackHasTrait(crystal, trait)) n++;
        }
        return n;
    }

    /** {@code crystal} 単体の carat 予算ボーナス（宝石彫刻台でのシジル付与コスト判定用）。 */
    public static int caratBonusFor(ItemStack crystal) {
        int bonus = 0;
        if (stackHasTrait(crystal, WARD)) bonus += WARD_CARAT_BONUS;
        if (stackHasTrait(crystal, PINKY)) bonus += PINKY_CARAT_BONUS;
        return bonus;
    }

    /** トレイトによる vanilla エンチャント付与1件。{@code SigilRegistry.GrantedEnchantment} と同形。 */
    private record TraitGrant(ToolForm form, ResourceKey<Enchantment> enchantment, int level) {}

    private static final Map<String, List<TraitGrant>> ENCHANT_GRANTS = Map.of(
            SHARP, List.of(
                    new TraitGrant(ToolForm.SWORD, Enchantments.SHARPNESS, 1),
                    new TraitGrant(ToolForm.AXE, Enchantments.SHARPNESS, 1),
                    new TraitGrant(ToolForm.SPEAR, Enchantments.SHARPNESS, 1),
                    new TraitGrant(ToolForm.TRIDENT, Enchantments.SHARPNESS, 1),
                    new TraitGrant(ToolForm.MACE, Enchantments.SHARPNESS, 1)
            ),
            CONDUCTION, List.of(
                    new TraitGrant(ToolForm.SWORD, Enchantments.FIRE_ASPECT, 1),
                    new TraitGrant(ToolForm.AXE, Enchantments.FIRE_ASPECT, 1),
                    new TraitGrant(ToolForm.SPEAR, Enchantments.FIRE_ASPECT, 1),
                    new TraitGrant(ToolForm.TRIDENT, Enchantments.FIRE_ASPECT, 1)
            ),
            GOLDEN, List.of(
                    new TraitGrant(ToolForm.PICKAXE, Enchantments.FORTUNE, 1),
                    new TraitGrant(ToolForm.AXE, Enchantments.FORTUNE, 1),
                    new TraitGrant(ToolForm.HOE, Enchantments.FORTUNE, 1),
                    new TraitGrant(ToolForm.SHOVEL, Enchantments.FORTUNE, 1),
                    new TraitGrant(ToolForm.SWORD, Enchantments.LOOTING, 1),
                    new TraitGrant(ToolForm.AXE, Enchantments.LOOTING, 1),
                    new TraitGrant(ToolForm.SPEAR, Enchantments.LOOTING, 1)
            ),
            FOOLS_GOLD, List.of(
                    new TraitGrant(ToolForm.PICKAXE, Enchantments.FORTUNE, 2),
                    new TraitGrant(ToolForm.AXE, Enchantments.FORTUNE, 2),
                    new TraitGrant(ToolForm.HOE, Enchantments.FORTUNE, 2),
                    new TraitGrant(ToolForm.SHOVEL, Enchantments.FORTUNE, 2)
            )
    );

    /**
     * {@code traits} が持つ全トレイトぶんの vanilla エンチャント付与を、フォーム一致分だけ
     * {@code granted} マップへ合流させる（既存キーがあればレベルは大きい方を採用）。
     * {@link CrystalToolLogic#applyGrantedEnchantments} のシジル分の集計と同じマップに対して
     * 追加で呼び出すことで、シジルとトレイトどちらか強い方が反映される。
     */
    public static void mergeEnchantGrants(Set<String> traits, ToolForm form, Map<ResourceKey<Enchantment>, Integer> granted) {
        for (String trait : traits) {
            for (TraitGrant g : ENCHANT_GRANTS.getOrDefault(trait, List.of())) {
                if (g.form() == form) granted.merge(g.enchantment(), g.level(), Math::max);
            }
        }
    }
}
