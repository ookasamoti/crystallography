package net.ookasamoti.crystallography.common.item.armor;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;
import net.ookasamoti.crystallography.CrystallographyMod;
import net.ookasamoti.crystallography.common.item.tool.ToolInventory;
import net.ookasamoti.crystallography.setup.DataComponentsRegistry;

/**
 * アミュレット防具4部位の装備効果を、装着中の結晶3個の {@code CrystalStats}
 * （hardness/cut/carat/clarity）から算出する。ツール側の {@code CrystalToolLogic.buildStats}
 * と対になる、防具側の性能計算本体。
 * <p>
 * ツールと違い、アミュレットには「ロードアウト」も「ツールベースのtier」も無く、3枠の結晶
 * インベントリがそのまま最終的な装備効果に直結する（宝飾台での結晶入れ替えのたびに
 * {@link #applyComputedStats} を呼び直すだけでよい）。cut（切削の鋭さ＝攻撃向けの特性）は
 * ツール専用の意味合いが強いため、防具側では意図的に使用しない。
 * <ul>
 *   <li>hardness（硬度）→ 防御力・靭性。tier4結晶3個満タン（Σhardness=3072）で quality=1.0
 *       となり、部位別の基礎値（netherite一式相当：頭3/胴8/脚6/足3、靭性は各3）に達する。</li>
 *   <li>carat（重量・大きさ）→ ノックバック耐性。結晶の質量が衝撃を吸収するイメージ。</li>
 *   <li>clarity（純度）→ 移動速度（{@code ADD_MULTIPLIED_BASE}）。混じり気のない結晶ほど
 *       身が軽い、というイメージ。</li>
 * </ul>
 */
public final class AmuletStatLogic {

    // Σhardness の正規化上限（tier4結晶=hardness1024 を3個装着した場合の合計）。
    private static final float HARDNESS_QUALITY_CAP = 3072f;
    private static final float BASE_TOUGHNESS = 3f;

    private static final float KNOCKBACK_CARAT_DIVISOR = 420f;
    private static final float KNOCKBACK_CAP = 0.1f;

    private static final float SPEED_CLARITY_DIVISOR = 1500f;
    private static final float SPEED_CAP = 0.03f;

    private static final Identifier ARMOR_ID = id("amulet_armor");
    private static final Identifier TOUGHNESS_ID = id("amulet_toughness");
    private static final Identifier KNOCKBACK_ID = id("amulet_knockback_resistance");
    private static final Identifier SPEED_ID = id("amulet_speed");

    private AmuletStatLogic() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(CrystallographyMod.MOD_ID, path);
    }

    /** 部位別の基礎防御力（netherite一式の配分：頭3/胴8/脚6/足3、合計20）。 */
    private static float baseArmorFor(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 3f;
            case CHEST -> 8f;
            case LEGS -> 6f;
            case FEET -> 3f;
            default -> 0f;
        };
    }

    /**
     * 装着中の結晶3個から装備効果を計算し、{@code stack} の {@code ATTRIBUTE_MODIFIERS} に反映する。
     * 宝飾台で結晶構成が変わるたびに（{@code onSaved} コールバック経由で）呼び出すこと。
     */
    public static void applyComputedStats(ItemStack stack, HolderLookup.Provider lookup) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) return;
        EquipmentSlot slot = equippable.slot();
        EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(slot);

        var statsType = DataComponentsRegistry.CRYSTAL_STATS.get();
        var inv = ToolInventory.get(stack, IAmuletItem.CRYSTAL_SLOTS, lookup);

        int hardnessSum = 0;
        float caratSum = 0f;
        float claritySum = 0f;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack crystal = inv.getResource(i).toStack(inv.getAmountAsInt(i));
            if (crystal.isEmpty()) continue;
            var cs = crystal.get(statsType);
            if (cs == null) continue;
            hardnessSum += cs.hardness();
            caratSum    += cs.carat();
            claritySum  += cs.clarity();
        }

        float quality = Math.min(1f, hardnessSum / HARDNESS_QUALITY_CAP);
        float armor = baseArmorFor(slot) * quality;
        float toughness = BASE_TOUGHNESS * quality;
        float knockbackResist = Math.min(KNOCKBACK_CAP, caratSum / KNOCKBACK_CARAT_DIVISOR);
        float speed = Math.min(SPEED_CAP, claritySum / SPEED_CLARITY_DIVISOR);

        ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
        if (armor > 0) {
            b.add(Attributes.ARMOR,
                    new AttributeModifier(ARMOR_ID, armor, AttributeModifier.Operation.ADD_VALUE), group);
        }
        if (toughness > 0) {
            b.add(Attributes.ARMOR_TOUGHNESS,
                    new AttributeModifier(TOUGHNESS_ID, toughness, AttributeModifier.Operation.ADD_VALUE), group);
        }
        if (knockbackResist > 0) {
            b.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(KNOCKBACK_ID, knockbackResist, AttributeModifier.Operation.ADD_VALUE), group);
        }
        if (speed > 0) {
            b.add(Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(SPEED_ID, speed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), group);
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, b.build());
    }
}
