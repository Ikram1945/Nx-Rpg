package com.ikram.nexus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Pesawat pemain (game pertempuran luar angkasa).
 * Sprite digambar prosedural sebagai gambar vektor halus (poligon + elips
 * di kanvas besar, lalu diperkecil dengan filter bilinear sebagai
 * antialias) — bukan lagi peta piksel kasar. Tanpa berkas gambar.
 * Posisi x/y adalah titik TENGAH pesawat.
 */
public class Player {

    public static final int MAX_HP = 3;
    public static final float SPEED = 118f;
    public static final float RADIUS = 7f;

    private final Texture[] textures = new Texture[2];
    private final TextureRegion[] frames = new TextureRegion[2];

    public float x, y;
    public int hp = MAX_HP;
    public boolean dead;
    private float invulnTime;
    private float stateTime;

    public Player(float startX, float startY) {
        textures[0] = buildShipTexture(false);
        textures[1] = buildShipTexture(true);
        for (int i = 0; i < 2; i++) {
            frames[i] = new TextureRegion(textures[i]);
        }
        x = startX;
        y = startY;
    }

    // ---------- Perkakas gambar vektor di atas Pixmap ----------

    /** Isi poligon (scanline). pts = x0,y0,x1,y1,... */
    private static void fillPoly(Pixmap pm, float[] pts, Color c) {
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

    private static float[] scaled(float[] pts, float cx, float cy, float s) {
        float[] out = new float[pts.length];
        for (int i = 0; i < pts.length; i += 2) {
            out[i] = cx + (pts[i] - cx) * s;
            out[i + 1] = cy + (pts[i + 1] - cy) * s;
        }
        return out;
    }

    private static void fillEllipse(Pixmap pm, float cx, float cy, float rx, float ry, Color c) {
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

    /** Perkecil pixmap 2x dengan bilinear: tepi bentuk jadi halus (antialias). */
    private static Texture toTexture(Pixmap big) {
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

    private static Color col(int r, int g, int b, float a) {
        return new Color(r / 255f, g / 255f, b / 255f, a);
    }

    // ---------- Gambar pesawat (kanvas 256x128, hidung ke kanan) ----------

    private static Texture buildShipTexture(boolean longFlame) {
        Pixmap pm = new Pixmap(256, 128, Pixmap.Format.RGBA8888);
        Color outline = col(38, 40, 58, 1f);
        Color hull = col(203, 211, 226, 1f);
        Color steel = col(96, 110, 134, 1f);
        Color steelLight = col(130, 144, 168, 1f);

        // Api mesin (di belakang badan)
        if (longFlame) {
            fillPoly(pm, new float[]{44, 64, 2, 50, 2, 78}, col(255, 150, 40, 0.75f));
            fillPoly(pm, new float[]{44, 64, 16, 55, 16, 73}, col(255, 220, 90, 0.9f));
            fillPoly(pm, new float[]{44, 64, 26, 59, 26, 69}, col(255, 255, 255, 0.95f));
        } else {
            fillPoly(pm, new float[]{44, 64, 14, 52, 14, 76}, col(255, 150, 40, 0.75f));
            fillPoly(pm, new float[]{44, 64, 24, 56, 24, 72}, col(255, 220, 90, 0.9f));
            fillPoly(pm, new float[]{44, 64, 32, 59, 32, 69}, col(255, 255, 255, 0.95f));
        }

        // Sirip ekor kecil atas & bawah
        fillPoly(pm, new float[]{66, 48, 84, 32, 98, 46}, steel);
        fillPoly(pm, new float[]{66, 80, 84, 96, 98, 82}, steel);

        // Sayap belakang atas & bawah
        fillPoly(pm, new float[]{96, 54, 150, 44, 120, 10, 70, 16}, steel);
        fillPoly(pm, new float[]{94, 52, 140, 44, 118, 18, 76, 22}, steelLight);
        fillPoly(pm, new float[]{96, 74, 150, 84, 120, 118, 70, 112}, steel);
        fillPoly(pm, new float[]{94, 76, 140, 84, 118, 110, 76, 106}, steelLight);
        fillEllipse(pm, 121, 14, 4f, 4f, col(120, 230, 255, 0.95f));
        fillEllipse(pm, 121, 114, 4f, 4f, col(120, 230, 255, 0.95f));

        // Badan utama: garis tepi gelap dulu, lalu isi lambung
        float[] body = {246, 64, 196, 45, 120, 39, 66, 48, 52, 64, 66, 80, 120, 89, 196, 83};
        fillPoly(pm, scaled(body, 128, 64, 1.05f), outline);
        fillPoly(pm, body, hull);
        // Bayangan separuh bawah + kilau separuh atas
        fillPoly(pm, new float[]{52, 64, 246, 64, 196, 83, 120, 89, 66, 80},
                col(148, 158, 182, 0.50f));
        fillPoly(pm, new float[]{66, 48, 120, 39, 196, 45, 246, 64, 196, 54, 120, 50, 66, 56},
                col(255, 255, 255, 0.28f));
        // Garis panel
        fillPoly(pm, new float[]{118, 44, 122, 44, 122, 84, 118, 84}, col(38, 40, 58, 0.22f));
        fillPoly(pm, new float[]{148, 42, 152, 42, 152, 86, 148, 86}, col(38, 40, 58, 0.18f));

        // Kokpit berlapis + kilau kaca
        fillEllipse(pm, 172, 64, 30, 12, col(14, 34, 54, 1f));
        fillEllipse(pm, 172, 64, 24, 9, col(38, 120, 160, 1f));
        fillEllipse(pm, 172, 64, 17, 6.5f, col(90, 210, 245, 1f));
        fillEllipse(pm, 182, 60, 7, 2.6f, col(255, 255, 255, 0.95f));

        // Lampu hidung
        fillEllipse(pm, 238, 64, 3.4f, 3.4f, col(255, 230, 140, 1f));

        return toTexture(pm);
    }

    public void update(float delta, float mx, float my, float viewW, float viewH) {
        stateTime += delta;
        if (invulnTime > 0f) invulnTime -= delta;
        if (dead) return;
        float len = (float) Math.sqrt(mx * mx + my * my);
        if (len > 0.01f) {
            float nx = mx / len;
            float ny = my / len;
            float mag = Math.min(1f, len);
            x += nx * SPEED * mag * delta;
            y += ny * SPEED * mag * delta;
        }
        if (x < 14f) x = 14f;
        if (x > viewW - 14f) x = viewW - 14f;
        if (y < 10f) y = 10f;
        if (y > viewH - 10f) y = viewH - 10f;
    }

    /** Titik keluar peluru (ujung hidung pesawat). */
    public float gunX() { return x + 15f; }
    public float gunY() { return y; }

    public boolean isInvulnerable() { return invulnTime > 0f; }

    /** Kena serangan: darah berkurang, kebal sesaat. Return true bila serangan masuk. */
    public boolean takeDamage() {
        if (dead || invulnTime > 0f) return false;
        hp--;
        invulnTime = 1.2f;
        if (hp <= 0) {
            hp = 0;
            dead = true;
        }
        return true;
    }

    public void respawn(float rx, float ry) {
        x = rx;
        y = ry;
        hp = MAX_HP;
        dead = false;
        invulnTime = 2f;
    }

    public void draw(SpriteBatch batch) {
        if (dead) return;
        TextureRegion frame = frames[(int) (stateTime * 14f) % 2];
        // Berkedip tembus pandang selagi kebal setelah kena serangan
        if (invulnTime > 0f && ((int) (invulnTime * 12f)) % 2 == 0) {
            batch.setColor(1f, 1f, 1f, 0.45f);
        }
        batch.draw(frame, x - 19f, y - 9.5f, 38f, 19f);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void dispose() {
        for (Texture t : textures) t.dispose();
    }
}
