package net.ookasamoti.crystallography.client.gui.dial;

/**
 * ダイヤル配置の幾何情報（Screen座標系で使う想定）。
 *
 * - center(cx,cy) : 円の中心（Screen座標）
 * - radius        : 半径
 * - count         : 目盛り数（配置数）
 * - baseAngleDeg  : 基準角（デフォルト -90=上向き）
 * - angularOffset : 任意オフセット（UI側の調整）
 * - clockwise     : 時計回り
 * - iconDiameter  : スロット/アイコンの想定直径（16推奨）
 */
public final class DialLayout {
    private int cx, cy;
    private int radius;
    private int count;
    private boolean clockwise = true;
    private float baseAngleDeg = -90f;
    private float angularOffsetDeg = 0f;
    private int iconDiameter = 16;

    public DialLayout center(int cx, int cy){ this.cx = cx; this.cy = cy; return this; }
    public DialLayout radius(int r){ this.radius = r; return this; }
    public DialLayout count(int n){ this.count = Math.max(0, n); return this; }
    public DialLayout clockwise(boolean b){ this.clockwise = b; return this; }
    public DialLayout startAngleDeg(float deg){ this.baseAngleDeg = deg; return this; }
    public DialLayout angularOffsetDeg(float deg){ this.angularOffsetDeg = deg; return this; }
    public DialLayout iconDiameter(int px){ this.iconDiameter = Math.max(1, px); return this; }

    public int centerX(){ return cx; }
    public int centerY(){ return cy; }
    public int radius(){ return radius; }
    public int count(){ return count; }
    public boolean clockwise(){ return clockwise; }
    public float baseAngleDeg(){ return baseAngleDeg; }
    public float angularOffsetDeg(){ return angularOffsetDeg; }
    public int iconDiameter(){ return iconDiameter; }

    /** smoothShift=0 の座標 */
    public int[] positionOf(int i){
        return positionOf(i, 0f);
    }

    /**
     * i=論理インデックス, smoothShift=-1..+1（1コマぶんの見た目補間）
     * Screen座標で返す。
     */
    public int[] positionOf(int i, float smoothShift){
        if (count <= 0) return new int[]{cx, cy};

        float step = 360f / (float)count;
        float dir = clockwise ? 1f : -1f;

        // smoothShift は「次の目盛り方向へどれだけ滑らせるか」
        float logical = i + smoothShift * dir;

        float ang = baseAngleDeg + angularOffsetDeg + dir * step * logical;
        double rad = Math.toRadians(ang);

        int x = Math.round(cx + (float)(Math.cos(rad) * radius));
        int y = Math.round(cy + (float)(Math.sin(rad) * radius));
        return new int[]{x, y};
    }
}
