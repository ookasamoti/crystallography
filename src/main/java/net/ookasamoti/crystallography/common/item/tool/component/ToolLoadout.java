package net.ookasamoti.crystallography.common.item.tool.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Arrays;

public record ToolLoadout(int slotIndex, ToolForm form, int[] crystalIndices, ToolStats stats,
                          int currentDurability, boolean incomplete) {

    /** 1つのツール登録に使う結晶スロット数（固定）。 */
    public static final int CRYSTAL_SLOTS = 3;

    /** 仮登録スロットインデックスのセンチネル（本登録前の draft 状態）。 */
    public static final int DRAFT_SLOT = -1;

    /** 旧データ移行用センチネル：codec で current_durability が未保存だった場合、満タンとして扱う。 */
    public static final int FULL_DURABILITY_SENTINEL = Integer.MAX_VALUE;

    public ToolLoadout {
        if (crystalIndices.length != CRYSTAL_SLOTS)
            throw new IllegalArgumentException("crystalIndices must have exactly " + CRYSTAL_SLOTS + " elements");
        // クランプ：[0, stats.durability]。
        // - 旧データ（current_durability 未保存）は MAX_VALUE で読み込まれ → ここで stats.durability に丸め込まれる。
        // - マイナスは sticky-zero として 0 に。
        if (currentDurability > stats.durability()) currentDurability = stats.durability();
        if (currentDurability < 0) currentDurability = 0;
    }

    /** 新規登録用：満耐久でロードアウトを構築する。 */
    public static ToolLoadout fresh(int slotIndex, ToolForm form, int[] crystalIndices, ToolStats stats) {
        return new ToolLoadout(slotIndex, form, crystalIndices, stats, stats.durability(), false);
    }

    /** 現在の耐久値だけ差し替えた新ロードアウトを返す。 */
    public ToolLoadout withCurrentDurability(int newCurrent) {
        return new ToolLoadout(slotIndex, form, crystalIndices, stats, newCurrent, incomplete);
    }

    /** stats と現在の耐久値を差し替えた新ロードアウトを返す（結晶差し替え時に使用）。 */
    public ToolLoadout withStatsAndDurability(ToolStats newStats, int newCurrent) {
        return new ToolLoadout(slotIndex, form, crystalIndices, newStats, newCurrent, incomplete);
    }

    /** form を差し替えた新ロードアウトを返す（TRIDENT/MACE への昇格用）。 */
    public ToolLoadout withForm(ToolForm newForm) {
        return new ToolLoadout(slotIndex, newForm, crystalIndices, stats, currentDurability, incomplete);
    }

    /** slotIndex を差し替えた新ロードアウトを返す（REGISTRIES 並べ替え用）。 */
    public ToolLoadout withSlotIndex(int newSlotIndex) {
        return new ToolLoadout(newSlotIndex, form, crystalIndices, stats, currentDurability, incomplete);
    }

    /** incomplete 状態だけを差し替えた新ロードアウトを返す。 */
    public ToolLoadout withIncomplete(boolean newIncomplete) {
        return new ToolLoadout(slotIndex, form, crystalIndices, stats, currentDurability, newIncomplete);
    }

    /** 耐久値0以下＝使用不可（sticky・使用で割った状態）。incomplete とは別概念。 */
    public boolean broken() { return currentDurability <= 0; }

    /** 使用不可（broken または incomplete のいずれか）。アイテム使用時はこちらを使う。 */
    public boolean unusable() { return broken() || incomplete; }

    // int[] はレコードのデフォルト equals/hashCode だと参照比較になるため明示的に定義する
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ToolLoadout lo)) return false;
        return slotIndex == lo.slotIndex && form == lo.form
                && Arrays.equals(crystalIndices, lo.crystalIndices)
                && stats.equals(lo.stats)
                && currentDurability == lo.currentDurability
                && incomplete == lo.incomplete;
    }

    @Override
    public int hashCode() {
        int h = Integer.hashCode(slotIndex);
        h = 31 * h + form.hashCode();
        h = 31 * h + Arrays.hashCode(crystalIndices);
        h = 31 * h + stats.hashCode();
        h = 31 * h + Integer.hashCode(currentDurability);
        h = 31 * h + Boolean.hashCode(incomplete);
        return h;
    }

    // ---- JSON Codec ----
    private static final Codec<int[]> INT_ARRAY_CODEC =
            Codec.INT.listOf().xmap(
                    list -> list.stream().mapToInt(Integer::intValue).toArray(),
                    arr  -> Arrays.stream(arr).boxed().toList()
            );

    public static final Codec<ToolLoadout> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("slot_index", 0)
                    .forGetter(ToolLoadout::slotIndex),
            Codec.STRING.xmap(ToolForm::valueOf, ToolForm::name)
                    .fieldOf("form").forGetter(ToolLoadout::form),
            INT_ARRAY_CODEC
                    .fieldOf("crystal_indices").forGetter(ToolLoadout::crystalIndices),
            ToolStats.CODEC
                    .fieldOf("stats").forGetter(ToolLoadout::stats),
            // 未保存（旧データ）の場合は FULL_DURABILITY_SENTINEL で読み込み、compact constructor で
            // stats.durability にクランプされる。
            Codec.INT.optionalFieldOf("current_durability", FULL_DURABILITY_SENTINEL)
                    .forGetter(ToolLoadout::currentDurability),
            // incomplete は使用中に流動的に変わる runtime 状態。未保存（旧データ）= false。
            Codec.BOOL.optionalFieldOf("incomplete", false)
                    .forGetter(ToolLoadout::incomplete)
    ).apply(i, ToolLoadout::new));

    // ---- Network StreamCodec ----
    private static final StreamCodec<RegistryFriendlyByteBuf, int[]> INT3_STREAM_CODEC =
            StreamCodec.of(
                    (buf, arr) -> { for (int v : arr) buf.writeVarInt(v); },
                    buf -> new int[]{ buf.readVarInt(), buf.readVarInt(), buf.readVarInt() }
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolLoadout> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,                                                   ToolLoadout::slotIndex,
                    ByteBufCodecs.STRING_UTF8.map(ToolForm::valueOf, ToolForm::name),        ToolLoadout::form,
                    INT3_STREAM_CODEC,                                                       ToolLoadout::crystalIndices,
                    ToolStats.STREAM_CODEC,                                                  ToolLoadout::stats,
                    ByteBufCodecs.VAR_INT,                                                   ToolLoadout::currentDurability,
                    ByteBufCodecs.BOOL,                                                      ToolLoadout::incomplete,
                    ToolLoadout::new
            );
}
