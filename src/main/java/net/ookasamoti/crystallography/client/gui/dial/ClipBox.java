package net.ookasamoti.crystallography.client.gui.dial;

/**
 * GUI座標系の矩形クリップ（半開区間 [x0,x1) [y0,y1)）。
 * ・入力(クリック)判定や scissor の算出などに使える軽量ユーティリティ。
 */
public final class ClipBox {
    public int x0, y0, x1, y1;

    public ClipBox(int x0, int y0, int x1, int y1) { set(x0, y0, x1, y1); }

    public void set(int x0, int y0, int x1, int y1) {
        this.x0 = Math.min(x0, x1);
        this.y0 = Math.min(y0, y1);
        this.x1 = Math.max(x0, x1);
        this.y1 = Math.max(y0, y1);
    }

    /** 1pxでも交差すれば true */
    public boolean overlaps(int rx0, int ry0, int rx1, int ry1) {
        return !(rx1 <= x0 || rx0 >= x1 || ry1 <= y0 || ry0 >= y1);
    }

    /** 交差矩形を out[0..3] に書き込む（交差しない場合は幅<=0/高さ<=0になり得る） */
    public void intersectInto(int rx0, int ry0, int rx1, int ry1, int[] out /*len>=4*/) {
        out[0] = Math.max(x0, rx0);
        out[1] = Math.max(y0, ry0);
        out[2] = Math.min(x1, rx1);
        out[3] = Math.min(y1, ry1);
    }
}
