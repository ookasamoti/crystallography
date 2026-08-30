# Crystal

# バニラツール
|                  | 木     | 石       | 銅       | 鉄     | 金     | ダイヤモンド   | ネザライト  |
| ---------------- | ----- | ------- | ------- | ----- | ----- | -------- | ------ |
| ツルハシ             | 2     | 3       | 3       | 4     | 2     | 5        | 6      |
|                  |       |         |         |       |       |          |        |
| 斧                | 7     | 9       | 9       | 9     | 7     | 9        | 10     |
|                  |       |         |         |       |       |          |        |
| シャベル             | 2     | 3       | 3       | 4     | 2     | 5        | 6      |
|                  |       |         |         |       |       |          |        |
| クワ               | 1     | 1       | 1       | 1     | 1     | 1        | 1      |
|                  |       |         |         |       |       |          |        |
| 剣                | 4 - 6 | 5 - 7.5 | 5 - 7.5 | 6 - 9 | 4 - 6 | 7 - 10.5 | 8 - 12 |
|                  |       |         |         |       |       |          |        |
| mining level     | 0     | 1       | 1       | 2     | 0     | 3        | 4      |
| durability       | 59    | 131     | 191     | 250   | 32    | 1561     | 2031   |
| speed            | 2.0F  | 4.0F    | 5.0F    | 6.0F  | 12.0F | 8.0F     | 9.0F   |
| damage           | 0.0F  | 1.0F    | 1.0F    | 2.0F  | 0.0F  | 3.0F     | 4.0F   |
| enchantmentValue | 15    | 5       | 13      | 14    | 22    | 10       | 15     |

D:\Development\Minecraft_mod\Crystallography\Crystallography_1.21.1-NeoForge\build\moddev\artifacts\neoforge-21.1.209-merged.jar!\net\minecraft\world\item\Tiers.java

    public enum Tiers implements Tier {
        WOOD(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 59, 2.0F, 0.0F, 15, () -> Ingredient.of(ItemTags.PLANKS)),
        STONE(BlockTags.INCORRECT_FOR_STONE_TOOL, 131, 4.0F, 1.0F, 5, () -> Ingredient.of(ItemTags.STONE_TOOL_MATERIALS)),
        IRON(BlockTags.INCORRECT_FOR_IRON_TOOL, 250, 6.0F, 2.0F, 14, () -> Ingredient.of(Items.IRON_INGOT)),
        DIAMOND(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1561, 8.0F, 3.0F, 10, () -> Ingredient.of(Items.DIAMOND)),
        GOLD(BlockTags.INCORRECT_FOR_GOLD_TOOL, 32, 12.0F, 0.0F, 22, () -> Ingredient.of(Items.GOLD_INGOT)),
        NETHERITE(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2031, 9.0F, 4.0F, 15, () -> Ingredient.of(Items.NETHERITE_INGOT));



| 内部tier | 対応 ToolMaterial / タグ         | 掘れるようになるブロック              | 備考                               |
| ------ | ---------------------------- | ------------------------- | -------------------------------- |
| 0      | INCORRECT_FOR_WOODEN_TOOL    | 石ティア未満                    | GOLDもゲートはここ(速いだけ)                |
| 1      | INCORRECT_FOR_STONE_TOOL     | needs_stone(鉄鉱石など)        | COPPERもゲートは石と同じ(needs_copperは無い) |
| 2      | INCORRECT_FOR_IRON_TOOL      | needs_iron(金/ダイヤ/エメラルド鉱石) |                                  |
| 3      | INCORRECT_FOR_DIAMOND_TOOL   | needs_diamond(黒曜石/古代の残骸)  |                                  |
| 4      | INCORRECT_FOR_NETHERITE_TOOL | ダイヤと同じ                    | 数字の差はmod独自ブロック用                  |

# 結晶




## パラメーター
- ティア：採掘ティア
- 硬度(hardness)：耐久値
- カット(cut)：攻撃力に寄与
- カラット(carat)：エンチャントの上限値などに寄与
- 純度(clarity)：採掘速度や攻撃速度、クールタイムなどに寄与
- カテゴリ(category)：大まかなカテゴリ　同カテゴリのもの二つをツールに使うとセット効果が発現する
- 特性(traits)：素材ごとの特性
- crack_result：結晶を割ったときに手に入るアイテムと個数


    {
      "item": "crystallography:pink_diamond",
      "tier": 3,
      "hardness": 512,
      "cut":     { "min": 1.0, "max": 1.2 },
      "carat":   { "min": 8, "max": 12 },
      "clarity": { "min": 8.0, "max": 9.6 },
      "category": "jewel",
      "traits": [ "diamond", "pinky" ],
      "crack_result": { "item": "minecraft:diamond", "count": 1 },
      "color": "#FF74E9"
    }


### 値の対応（実装）

下表の値は base 値で、stats JSON では次のレンジに展開される（ツール性能への寄与も併記）。

- **durability** → `hardness`（整数・固定）。3結晶の**合計**がツール耐久に加算（`tier*128 + Σhardness`）。
- **damage** → `cut { min: 表/3, max: 表×0.4 }`。3結晶の**合計**がツール攻撃力（満強化で表×1.2）。
- **speed** → `clarity { min: 表, max: 表×1.2 }`。3結晶の**平均**がツール採掘速度（満強化で表×1.2）。
- **max enchantment** → `carat { min: 表−4, max: 表 }`（整数・強化不可）。結晶ごとのシジル予算。
- **color** → 着色 `#RRGGBB`（下表 color 列）。
- ティア（mining level）→ `tier`（整数）。

> 強化システム（使用に応じて speed/damage が max へ成長）は未実装。レンジは base(min)/cap(max) として用意済み。



| name(japanese) | name(english)                            | get from                        | mining level | durability | speed | damage | enchantmentValue | max enchantment  | category    | ability                                                 | crack_result              | color  |
| -------------- | ---------------------------------------- | ------------------------------- | ------------ | ---------- | ----- | ------ | ---------------- | ---------------- | ----------- | ------------------------------------------------------- | ------------------------- | ------ |
| 黄銅鉱            | Chalcopyrite<br>chalcopyrite             | 銅鉱石                             | 1.0          | 64         | 5.0F  | 1.0F   | -                | 8                | metal(金属)   | 伝導<br>火系エンチャントに補正                                       | 銅の原石                      | C7A24A |
| 磁鉄鉱            | Magnetite<br>magnetite                   | 鉄鉱石                             | 2.0          | 128        | 6.0F  | 2.0F   | -                | 9                | metal(金属)   | 磁力                                                      | 鉄の原石                      | 4C4C55 |
| 黄鉄鉱            | Pyrite<br>pyrite                         | 鉄鉱石                             | 2.0          | 128        | 6.0F  | 2.0F   | -                | 11               | metal(金属)   | 愚者の黄金<br>幸運+2の効果を固定で持たされる                               | 鉄の原石                      | CDAE3B |
| 石炭             | Anthracite<br>anthracite                 | 石炭鉱石                            | 0.0          | 16         | 4.0F  | 0.0F   | -                | 8                | stone(石)    | 鉱炉<br>採掘時に製錬                                            | 石炭                        | 2A2A2E |
| 金              | Gold<br>gold                             | 金鉱石                             | 0.0          | 16         | 12.0F | 0.0F   | -                | 13               | metal(金属)   | 黄金<br>幸運に補正                                             | 金の原石                      | F6CF3B |
| ブルートゴールド       | Brute Gold<br>brute_gold                 | ピグリンブルートからドロップ                  | 0.0          | 256        | 12.0F | 3.0F   | -                | 13               | metal(金属)   | 黄金<br>幸運に補正<br><br>蛮族(bandit)<br>斧の攻撃力に補正               | 金の原石*5                    | C98A2E |
| 青氷結晶           | Blue Ice Crystal<br>blue_ice_crystal     | 青氷                              | 0.0          | 64         | 4.0F  | 1.0F   | -                | 9                | crystal(結晶) | 凍氷<br>採掘時、周囲の水を凍らせる                                     | 青色の染料                     | 8FD3FF |
| プリズマリンクリスタル    | Prismarine Crystal<br>prismarine_crystal | エルダーガーディアンからドロップ                | 2.0          | 256        | 5.0F  | 2.0F   | -                | 11               | crystal(結晶) | 水中での採掘速度が上がる                                            | プリズマリンクリスタル<br>プリズマリンシャード | 57C7B8 |
| レッドストーン        | Redstone<br>redstone                     | レッドストーン鉱石                       | 2.0          | 128        | 6.0F  | 2.0F   | -                | 11               | stone(石)    | ポーション効果の時間延長                                            | レッドストーンダスト                | D32F2F |
| ラピスラズリ         | Lapis Lazuli<br>lapis_lazuli             | ラピスラズリ鉱石                        | 1.0          | 64         | 5.0F  | 1.0F   | -                | 12(abilityにより14) | jewel(宝石)   | ワード<br>エンチャント上限レベル合計数+2                                 | ラピスラズリ                    | 2A52BE |
| アメジスト          | Amethyst<br>amethyst                     | アメジストブロック                       | 1.0          | 64         | 4.0F  | 0.0F   | -                | 11               | quartz(水晶)  | 共振<br>採掘時、周囲の同系統のブロックにソナーをリレーする                         | アメジスト                     | A472D6 |
| ダイヤモンド         | Diamond<br>diamond                       | ダイヤモンド鉱石                        | 3.0          | 512        | 8.0F  | 3.0F   | -                | 12               | jewel(宝石)   | ダイヤモンド<br>耐久力に補正<br><br>純粋<br>純度に1.2倍の補正                | ダイヤモンド                    | B9F2FF |
| ピンクダイヤモンド      | Pink Diamond<br>pink_diamond             | ダイヤモンド鉱石                        | 3.0          | 512        | 8.0F  | 3.0F   | -                | 12               | jewel(宝石)   | ダイヤモンド<br>耐久力に補正<br><br>ピンキー<br>エンチャント上限レベル合計数+1        | ダイヤモンド                    | FF74E9 |
| ブラックダイヤモンド     | Black Diamond<br>black_diamond           | 深層ダイヤモンド鉱石                      | 4.0          | 512        | 8.0F  | 3.0F   | -                | 12               | jewel(宝石)   | ダイヤモンド<br>耐久力に補正<br><br>ボルツ<br>耐久力エンチャントに+1の補正          | ダイヤモンド                    | 2B2A33 |
| エメラルド          | Emerald<br>emerald                       | エメラルド鉱石                         | 3.0          | 512        | 8.0F  | 3.0F   | -                | 12               | jewel(宝石)   | 秩序<br>ピリジャー特攻に補正                                        | エメラルド                     | 1FA85B |
| トラピッチェエメラルド    | Trapiche Emerald<br>trapiche_emerald     | 深層エメラルド鉱石                       | 3.0          | 512        | 8.0F  | 3.0F   | -                | 12               | jewel(宝石)   | *霊亀*<br>水系エンチャントに補正                                     |                           | 1C8A4E |
| ネザークォーツ        | Nether Quartz<br>nether_quartz           | ネザークォーツ                         | 2.0          | 128        | 6.0F  | 3.0F   | -                | 10               | quartz(水晶)  | 鋭利<br>攻撃力エンチャントに補正                                      | ネザークォーツ                   | E8DCCB |
| グロウストーン        | Glowstone<br>glowstone                   | グロウストーン                         | 1.0          | 32         | 7.0F  | 1.0F   | -                | 9                | stone(石)    | ポーション効果の段階を強化                                           |                           | FFD34D |
| ウィザーローズクォーツ    | Wither Rose Quartz<br>wither_rose_quartz | ウィザーからドロップ                      | 4.0          | 512        | 10.0F | 4.0F   | -                | 13               | quartz(水晶)  | 鋭利<br>攻撃力エンチャントに補正<br><br>ウィザー<br>ヒット時にウィザー状態を付与        |                           | 3B2F36 |
| ブレイズクリスタル      | Blaze Crystal<br>blaze_crystal           | ブレイズロッド                         | 2.0          | 128        | 7.0F  | 2.0F   | -                | 9                | crystal(結晶) | ブレイジング<br>延焼時間が1.5倍に                                    | ブレイズパウダー* 5               | FF9A1F |
| ブリーズクリスタル      | Breeze Crystal<br>breeze_crystal         | ブリーズロッド                         | 2.0          | 128        | 7.0F  | 2.0F   | -                | 9                | crystal(結晶) | ブリージング<br>ノックバックが二倍に                                    |                           | B6F0D6 |
| エンドクリスタル       | End Crystal<br>end_crystal               | エンダードラゴンを回復中のエンドクリスタルを破壊によりドロップ | 3.0          | 256        | 9.0F  | 2.0F   | -                | 10               | crystal(結晶) | 攻撃ヒット時に回復する                                             | エンドクリスタル                  | E173C9 |
| エンダーパール        | Ender Pearl<br>ender_pearl               | エンダーパール                         | 1.0          | 128        | 5.0F  | 2.0F   | -                | 10               | pearl(真珠)   | 遠距離攻撃がヒットした敵と位置を入れ替える                                   |                           | 19A38C |
| シュルカーパール       | Shulker Pearl<br>shulker_pearl           | シュルカーからドロップ                     | 2.0          | 512        | 7.0F  | 2.0F   | -                | 13               | pearl(真珠)   | フロート<br>ヒット時浮遊効果を付与<br>遠距離攻撃が重力で落ちない                    |                           | C7A9D6 |
| 黒曜石            | Obsidian<br>obsidian                     | 黒曜石                             | 4.0          | 1024       | 8.0F  | 3.0F   | -                | 9                | stone(石)    | 鋭利<br>攻撃力エンチャントに補正                                      |                           | 1E1726 |
| 黒曜石の涙          | Obsidian Tear<br>obsidian_tear           | 泣く黒曜石                           | 4.0          | 1024       | 8.0F  | 3.0F   | -                | 11               | stone(石)    | 黒曜石・エンダーチェストの採掘速度に二倍の補正                                 |                           | 6A4BB5 |
| 振管石(ソナーシャード)   | Sonar Shard<br>sonar_shard               | ウォーデンからドロップ                     | 4.0          | 1024       | 11.0F | 4.0F   | -                | 13               | stone(石)    | 共振<br>採掘時、周囲の同系統のブロックにソナーをリレーする<br><br>振撃<br>アーマー貫通ダメージ |                           | 18BBC4 |
| トライコア          | Trident Core<br>trident_core             | トライデント                          | 3.0          | 512        | 8.0F  | 3.0F   | -                | 14               | pearl(真珠)   | 槍に組み込むとトライデントに                                          |                           | 3FA8A0 |
| ヘビーコア          | Heavy Core<br>heavy_core                 | ヘビーコア                           | 4.0          | 1024       | 8.0F  | 4.0F   | -                | 14               | pearl(真珠)   | ツルハシに組み込むとメイスに                                          |                           | B8BBC4 |


カテゴリ(category)

ツールに二つ存在するとセット効果発動

| カテゴリ        | セット効果               |
| ----------- | ------------------- |
| metal(金属)   | 耐久値計算に1.2倍の補正       |
| quartz(水晶)  | 攻撃力計算に1.2倍の補正       |
| stone(石)    | 水晶・結晶のセット効果にカウントできる |
| jewel(宝石)   | エンチャントの上限値に1.2倍の補正  |
| crystal(結晶) | 採掘速度計算に1.2倍の補正      |
| pearl(真珠)   | クールタイム計算に1.2倍の補正    |




## シジル

エンチャント本をクラフトすることでエンチャントをバラした「シジル」という紙片を手に入れる

結晶とシジルを金床で合成し、シジルのコストが結晶のカラット(carat)を上回らない限り、結晶にシジルを付与する




| name        | クラフト元                   | 取得元エンチャント本                 |                                                                           | コスト |
| ----------- | ----------------------- | -------------------------- | ------------------------------------------------------------------------- | --- |
| 力           | 黒曜石                     | 攻撃力・射撃ダメージ増加               | 剣/斧/短剣/槍：攻撃力(5)<br>弓/クロスボウ：射撃ダメージ増加(5)                                    | 1   |
| 浄化          |                         | アンデッド特攻                    | 剣/斧/短剣/槍：アンデッド特攻(5)                                                       | 1   |
| 香草          |                         | 虫特攻                        | 剣/斧/短剣/槍：虫特攻(5)                                                           | 1   |
| 制圧          | エメラルド                   |                            | 剣/斧/短剣/槍：ピリジャー特攻(5)                                                       | 1   |
| 水棲          | カメの甲羅                   | 水中呼吸・水中歩行・激流               | ヘルメット：水中呼吸(3)                                                             | 1   |
| 炎           | ブレイズロッド<br>ブレイズクリスタル(3) | 火属性・フレイム・火炎耐性              | 剣/斧/短剣/槍：火属性(2)<br>弓/クロスボウ：フレイム(2)<br>チェストプレート：火炎耐性(4)                    | 1   |
| 蒼炎<br>wisp  | -                       | ソウルスピード・ソウルフレイム(炎を青炎に)     | ブーツ：ソウルスピード(3)<br>剣/斧/短剣/槍/弓/クロスボウ：ソウルフレイム(1)                             | 1   |
| 凍結          | 青氷                      | 氷渡り                        | ブーツ：氷渡り(2)<br>剣/斧/短剣/槍：鈍化(2)                                              | 1   |
| 颶風          | ブリーズロッド<br>ブリーズクリスタル(3) | ノックバック・パンチ・飛び道具耐性・ウィンドバースト | 剣/斧/短剣/槍：ノックバック(2)<br>弓：パンチ(2)<br>防具：飛び道具耐性(4)<br>メイス：ウィンドバースト(3)         | 1   |
| 雷           | -                       | 招雷                         | トライデント：招雷(1)                                                              | 2   |
| 落下耐性        |                         | 落下耐性                       | ブーツ：落下耐性(4)                                                               | 1   |
| 加護          |                         | 幸運・ドロップ増加・宝釣り              | ツルハシ/斧/クワ/シャベル：幸運(3)<br>剣/斧/短剣/槍：ドロップ増加(3)<br>釣り竿：宝釣り(3)                  | 1   |
| 抽出          | 振管石                     | シルクタッチ                     | ツルハシ/斧/クワ/シャベル：シルクタッチ(1)<br>剣/斧/短剣/槍：抽出(3)                                | 2   |
| 刺突          | プリズマリンクリスタル             | 刺の鎧・防具貫通・貫通・水棲特攻           | 防具：刺の鎧(3)<br>剣/斧/メイス：防具貫通<br>弓/クロスボウ：貫通<br>槍：水棲特攻                         | 1   |
| 突進          |                         | 突進・激流                      | 槍：突進<br>トライデント：激流                                                         | 1   |
| 加速<br>haste |                         | 効率強化・入れ食い・高速装填・水中採掘        | ツルハシ/斧/クワ/シャベル：効率強化(5)<br>釣り竿：入れ食い(3)<br>弓/クロスボウ：高速装填(3)<br>ヘルメット：水中採掘(1) | 1   |
| 広範          |                         | 範囲ダメージ増加・拡散                | 剣：範囲ダメージ増加(3)<br>弓/クロスボウ：拡散(2)                                            | 1   |
| 硬化          |                         | ダメージ軽減                     | 防具：ダメージ軽減(4)                                                              | 1   |
| 爬行          | -                       | スニーク速度上昇                   | ブーツ：スニーク速度上昇(3)                                                           | 1   |
| 無限          |                         | 無限                         | 弓/クロスボウ：無限(1)                                                             | 2   |
| 忠誠          |                         | 忠誠                         | トライデント：忠誠(1)                                                              | 2   |
| 重撃          |                         | 重撃                         | メイス：重撃(5)                                                                 | 1   |
| 耐久力         |                         | 耐久力                        | ツールロッド/ツールワンド：耐久力(3)                                                      | -   |
| 修繕          |                         | 修繕                         | ツールロッド/ツールワンド：修繕(2)                                                       | -   |








その他









