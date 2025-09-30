// package はご提示に合わせて component に統一
package net.ookasamoti.crystallography.common.item.tool.component;

public enum ToolForm {
    // baseAtk, baseAttackSpeed, baseMiningSpeed
    PICKAXE     (0f, 1.00f, 1.00f),
    SHOVEL      (0f, 1.00f, 1.00f),
    AXE         (3f, 1.00f, 1.00f),
    HOE         (0f, 1.00f, 1.00f),

    SWORD       (2f, 1.00f, 1.00f),
    SPEAR       (2f, 1.00f, 1.00f),
    HAMMER      (0f, 1.00f, 1.00f),
    ROD         (0f, 1.00f, 1.00f),

    KNIFE       (1f, 1.00f, 1.00f),
    BOW         (3f, 1.00f, 1.00f),
    CROSSBOW    (2f, 1.00f, 1.00f),
    BRUSH       (0f, 1.00f, 1.00f),
    SPYGLASS    (0f, 1.00f, 1.00f),
    FISHING_ROD (0f, 1.00f, 1.00f),
    SHIELD      (0f, 1.00f, 1.00f),
    WAND        (0f, 1.00f, 1.00f);

    private final float baseAttack;
    private final float baseAttackSpeed;
    private final float baseMiningSpeed;

    ToolForm(float baseAttack, float baseAttackSpeed, float baseMiningSpeed) {
        this.baseAttack = baseAttack;
        this.baseAttackSpeed = baseAttackSpeed;
        this.baseMiningSpeed = baseMiningSpeed;
    }

    public float baseAttack()      { return baseAttack; }
    public float baseAttackSpeed() { return baseAttackSpeed; }
    public float baseMiningSpeed() { return baseMiningSpeed; }
}



