package net.ookasamoti.crystallography.common.item.tool.component;

public enum ToolForm {
    // ---- ロッドフォーム ----
    // baseAtk, baseAttackSpeed, baseMiningSpeed
    // baseAtk は vanilla diamond-tier baseline (attackDamageBaseline) に合わせる：
    //   tool damage modifier = baseline + ToolMaterial.attackDamageBonus(=Σcut 相当)
    // baseAttackSpeed は vanilla baseline（ATTACK_SPEED 基礎値 4.0 への加算、負値で遅くなる）：
    //   displayed attack speed = 4.0 + baseAttackSpeed
    //   例：SWORD = -2.4 → 1.6 attacks/sec、PICKAXE = -2.8 → 1.2 attacks/sec
    PICKAXE     (1.0f,  -2.8f,  1.00f),  // vanilla diamond pickaxe (1.0, -2.8)
    SHOVEL      (1.5f,  -3.0f,  1.00f),  // vanilla diamond shovel  (1.5, -3.0)
    HOE         (0f,     0.0f,  1.00f),  // vanilla diamond hoe     (-3.0, 0.0)  ※attackDamage は特例 0 固定
    AXE         (5.0f,  -3.0f,  1.00f),  // vanilla diamond axe     (5.0, -3.0)
    SWORD       (3.0f,  -2.4f,  1.00f),  // vanilla diamond sword   (3.0, -2.4)
    SPEAR       (2.0f,  -2.9f,  1.00f),  // バニラ非対応：trident 同等の attack speed

    // 派生フォーム：SPEAR + trident_core → TRIDENT、PICKAXE + heavy_core → MACE。
    // TRIDENT/MACE は vanilla だとティア無し固定値。attack speed もそのまま流用。
    TRIDENT     (5.0f,  -2.9f,  1.00f),  // 手動調整: vanilla trident の base は8.0だが強すぎるため5.0に調整
    MACE        (5.0f,  -3.4f,  1.00f),  // vanilla mace    (5.0, -3.4)

    // ---- ワンドフォーム ----
    // バニラに対応するアイテム属性はない（SHEARS を除く）。仮の値として「遠隔／補助系は遅め」とした暫定値。
    BOW         (3f,    -3.0f,  1.00f),
    CROSSBOW    (2f,    -3.0f,  1.00f),
    // 短剣（ナイフ）としての武器性能：手動調整値。エンティティリーチのみ剣（＝vanilla基礎値のまま）
    // の0.8倍（entity_interaction_range: 3.0→2.4）に短縮。ブロックリーチ（設置/採掘対象への距離）は
    // vanilla基礎値のまま変更しない。
    SHEARS      (1.0f,  -1.2f,  1.00f, -0.6f, 0f),
    SPYGLASS    (0f,    -3.0f,  1.00f),
    FISHING_ROD (0f,    -3.0f,  1.00f),
    SHIELD      (0f,    -3.0f,  1.00f);

    private final float baseAttack;
    private final float baseAttackSpeed;
    private final float baseMiningSpeed;
    // vanilla基礎値（entity_interaction_range=3.0, block_interaction_range=4.5）からの加算分。
    // 他フォームは0＝vanillaデフォルトのまま。
    private final float entityReach;
    private final float blockReach;

    ToolForm(float baseAttack, float baseAttackSpeed, float baseMiningSpeed) {
        this(baseAttack, baseAttackSpeed, baseMiningSpeed, 0f, 0f);
    }

    ToolForm(float baseAttack, float baseAttackSpeed, float baseMiningSpeed, float entityReach, float blockReach) {
        this.baseAttack = baseAttack;
        this.baseAttackSpeed = baseAttackSpeed;
        this.baseMiningSpeed = baseMiningSpeed;
        this.entityReach = entityReach;
        this.blockReach = blockReach;
    }

    public float baseAttack()      { return baseAttack; }
    public float baseAttackSpeed() { return baseAttackSpeed; }
    public float baseMiningSpeed() { return baseMiningSpeed; }
    public float entityReach()     { return entityReach; }
    public float blockReach()      { return blockReach; }
}
