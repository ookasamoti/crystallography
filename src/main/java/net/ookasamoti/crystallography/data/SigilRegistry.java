package net.ookasamoti.crystallography.data;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.ookasamoti.crystallography.common.item.tool.component.ToolForm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * シジルの定義（コスト・効果）をアイテムIDで引けるレジストリ。露天堀り採掘台での
 * 「シジル+結晶→結晶にシジル付与」と、実際のツール性能への反映の両方から参照する。
 */
public final class SigilRegistry {

    /** フォームごとに本物の vanilla エンチャントを付与する1件（招雷/忠誠/無限/重撃/突進等）。
     * フォームによって付与する内容が異なる場合がある（例：突進＝槍にLunge・トライデントにRiptide）
     * ため、フォーム単位のリストにしている。 */
    public record GrantedEnchantment(ToolForm form, ResourceKey<Enchantment> enchantment, int level) {}

    /**
     * アミュレット（防具）の特定スロットに本物の vanilla エンチャントを付与する1件
     * （火炎耐性/凍結の氷渡り/颶風の飛び道具耐性等）。{@code slot} が空なら防具4部位のどれに
     * 付与されていても発動する（颶風/刺突/硬化のように「防具」とだけ指定されているもの）。
     */
    public record ArmorGrant(Optional<EquipmentSlot> slot, ResourceKey<Enchantment> enchantment, int level) {}

    /**
     * @param attackDamageBonus      フォーム一致で常時加算される攻撃力ボーナス（剛力等。無条件）。
     * @param forms                  attackDamageBonus / conditionalDamageBonus を適用する対象フォーム。
     * @param targetTag              このタグに一致する相手への攻撃時のみ {@code conditionalDamageBonus}
     *                               を加算する（浄化＝アンデッド、燻煙＝虫、制圧＝襲撃者等）。無ければ
     *                               条件付きボーナス無し。
     * @param conditionalDamageBonus targetTag 一致時に追加される攻撃力ボーナス。
     * @param onHitEffect            targetTag 一致時に相手へ付与するポーション効果（燻煙の鈍化など）。
     * @param onHitDuration          onHitEffect の付与時間（tick）。
     * @param onHitAmplifier         onHitEffect の増幅レベル（0=I）。
     * @param grants                 本物の vanilla エンチャントとしてツールに付与する一覧（招雷/無限/忠誠/
     *                               重撃/突進等、vanilla 側アイテムクラスの内部ロジックが直接エンチャント
     *                               成分を見て挙動を切り替える「能力ゲート」系はこちらで実現する。
     *                               無条件加算系（力・浄化等）とは別の仕組み）。
     * @param armorGrants            本物の vanilla エンチャントとしてアミュレットに付与する一覧（火炎耐性/
     *                               氷渡り/飛び道具耐性/刺の鎧/水中採掘/ダメージ軽減/スニーク速度上昇等）。
     * @param wornEffectSlot         wornEffect を適用する装備スロット（水棲＝ヘルメット等）。
     * @param wornEffect             このスロットに装着している間ずっと（毎tick）維持し続けるポーション
     *                               効果。水棲の水中呼吸のように、対応する vanilla エンチャントが
     *                               存在しない効果はこちらで実現する。
     * @param wornEffectAmplifier    wornEffect の増幅レベル（0=I）。
     * @param soulFlameIgniteSeconds 0より大きければ、{@code forms} 一致でのヒット時に対象をこの秒数
     *                               ソウルファイア起源として着火する（蒼炎のソウルフレイム）。実際の
     *                               青い炎の見た目・継続燃焼2倍ダメージは既存の
     *                               {@code SoulFireIgnitionHooks} がそのまま面倒を見る。
     */
    public record Definition(int cost, float attackDamageBonus, List<ToolForm> forms,
                              Optional<TagKey<EntityType<?>>> targetTag, float conditionalDamageBonus,
                              Optional<Holder<MobEffect>> onHitEffect, int onHitDuration, int onHitAmplifier,
                              List<GrantedEnchantment> grants, List<ArmorGrant> armorGrants,
                              Optional<EquipmentSlot> wornEffectSlot, Optional<Holder<MobEffect>> wornEffect,
                              int wornEffectAmplifier, int soulFlameIgniteSeconds) {}

    private static final Map<Identifier, Definition> BY_ITEM = new HashMap<>();

    public static void clear() { BY_ITEM.clear(); }
    public static void put(Identifier item, Definition def) { BY_ITEM.put(item, def); }
    public static int size() { return BY_ITEM.size(); }
    public static java.util.Set<Identifier> keys() { return BY_ITEM.keySet(); }

    public static Optional<Definition> get(Identifier item) {
        return Optional.ofNullable(BY_ITEM.get(item));
    }

    public static Optional<Definition> get(ItemStack stack) {
        return get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private SigilRegistry() {}
}
