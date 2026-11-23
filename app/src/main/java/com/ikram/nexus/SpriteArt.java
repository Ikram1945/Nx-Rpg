package com.ikram.nexus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

/**
 * Perkakas gambar vektor prosedural bersama (dipakai Player & Enemy):
 * menggambar poligon & elips di atas Pixmap kanvas besar, lalu
 * memperkecilnya 2x dengan filter bilinear sebagai antialias, sehingga
 * sprite tampak halus — bukan piksel kotak-kotak. Koordinat gambar
 * mengikuti Pixmap: x ke kanan, y ke BAWAH (baris 0 = atas).
 */
public final class SpriteArt {

    private SpriteArt() {
    }

    public static Color col(int r, int g, int b, float a) {
        return new Color(r / 255f, g / 255f, b / 255f, a);
    }

    /** Isi poligon (scanline, even-odd). pts = x0,y0,x1,y1,... */
    public static void fillPoly(Pixmap pm, float[] pts, Color c) {
        int n = pts.length / 2;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            minY = Math.min(minY, pts[i * 2 + 1]);
            maxY = Math.max(maxY, pts[i * 2 + 1]);
        }
        pm.setColor(c);
        int y0 = Math.max(0, (int) Math.floor(minY));
        int y1 = Math.min(pm.getHeight() - 1, (int) Math.ceil(maxY));
        float[] xs = new float[n];
        for (int y = y0; y <= y1; y++) {
            float yc = y + 0.5f;
            int cnt = 0;
            for (int i = 0; i < n; i++) {
                float ax = pts[i * 2], ay = pts[i * 2 + 1];
                float bx = pts[((i + 1) % n) * 2], by = pts[((i + 1) % n) * 2 + 1];
                if ((ay <= yc && by > yc) || (by <= yc && ay > yc)) {
                    xs[cnt++] = ax + (yc - ay) / (by - ay) * (bx - ax);
                }
            }
            for (int i = 0; i < cnt; i++) {
                for (int j = i + 1; j < cnt; j++) {
                    if (xs[j] < xs[i]) { float t = xs[i]; xs[i] = xs[j]; xs[j] = t; }
                }
            }
            for (int i = 0; i + 1 < cnt; i += 2) {
                int xa = Math.max(0, (int) Math.ceil(xs[i] - 0.5f));
                int xb = Math.min(pm.getWidth() - 1, (int) Math.floor(xs[i + 1] - 0.5f));
                for (int px = xa; px <= xb; px++) pm.drawPixel(px, y);
            }
        }
    }

    /** Salin titik-titik poligon yang diskalakan terhadap titik pusat. */
    public static float[] scaled(float[] pts, float cx, float cy, float s) {
        float[] out = new float[pts.length];
        for (int i = 0; i < pts.length; i += 2) {
            out[i] = cx + (pts[i] - cx) * s;
            out[i + 1] = cy + (pts[i + 1] - cy) * s;
        }
        return out;
    }

    /** Isi elips penuh berpusat (cx, cy) berjari-jari (rx, ry). */
    public static void fillEllipse(Pixmap pm, float cx, float cy, float rx, float ry, Color c) {
        pm.setColor(c);
        int x0 = Math.max(0, (int) (cx - rx)), x1 = Math.min(pm.getWidth() - 1, (int) (cx + rx));
        int y0 = Math.max(0, (int) (cy - ry)), y1 = Math.min(pm.getHeight() - 1, (int) (cy + ry));
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                float dx = (x + 0.5f - cx) / rx, dy = (y + 0.5f - cy) / ry;
                if (dx * dx + dy * dy <= 1f) pm.drawPixel(x, y);
            }
        }
    }

    /** Perkecil pixmap 2x dengan bilinear (antialias) lalu jadikan tekstur Linear. */
    public static Texture toTexture(Pixmap big) {
        Pixmap small = new Pixmap(big.getWidth() / 2, big.getHeight() / 2, Pixmap.Format.RGBA8888);
        big.setFilter(Pixmap.Filter.BiLinear);
        small.drawPixmap(big, 0, 0, big.getWidth(), big.getHeight(),
                0, 0, small.getWidth(), small.getHeight());
        Texture t = new Texture(small);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        small.dispose();
        big.dispose();
        return t;
    }

    /**
     * Putar sudut 'current' menuju 'target' (derajat) paling jauh
     * 'maxStep', lewat jalan memutar terpendek. Sudut 0 = kanan,
     * berlawanan arah jarum jam (sesuai dunia libGDX yang y-nya ke atas).
     */
    public static float turnToward(float current, float target, float maxStep) {
        float diff = (target - current) % 360f;
        if (diff > 180f) diff -= 360f;
        if (diff < -180f) diff += 360f;
        if (diff > maxStep) diff = maxStep;
        if (diff < -maxStep) diff = -maxStep;
        return current + diff;
    }
}
