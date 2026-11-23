package com.ikram.nexus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Pesawat pemain (Space War).
 * Sprite digambar prosedural sebagai gambar vektor halus lewat
 * SpriteArt — tanpa berkas gambar. Pesawat bisa BERBELOK ke segala
 * arah: badannya berputar mengikuti arah gerak (facing, derajat;
 * 0 = kanan), dan peluru ditembakkan ke arah hadap pesawat.
 * Posisi x/y adalah titik TENGAH pesawat.
 */
public class Player {

    public static final int MAX_HP = 200;
    public static final float SPEED = 118f;
    public static final float RADIUS = 7f;
    /** Kecepatan berputar badan pesawat (derajat per detik). */
    private static final float TURN_RATE = 620f;

    private final Texture[] textures = new Texture[2];
    private final TextureRegion[] frames = new TextureRegion[2];

    public float x, y;
    /** Arah hadap pesawat dalam derajat (0 = kanan, CCW). */
    public float facing;
    /** Pengali kecepatan gerak (diatur game; > 1 saat NOS aktif). */
    public float speedScale = 1f;
    /** Bonus darah maks dari upgrade pemain (diatur game). */
    public int hpBonus;
    /** Pengali kecepatan dari upgrade pemain (diatur game). */
    public float speedFactor = 1f;
    /** Sisa waktu perisai awal dari toko (detik); selama > 0 kebal. */
    public float shieldTime;
    public int hp = MAX_HP;
    public boolean dead;

    /** Darah maksimum efektif: dasar + bonus upgrade. */
    public int maxHpEfektif() { return MAX_HP + hpBonus; }
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

    // ---------- Gambar pesawat (kanvas 256x128, hidung ke kanan) ----------

    private static Texture buildShipTexture(boolean longFlame) {
        Pixmap pm = new Pixmap(256, 128, Pixmap.Format.RGBA8888);
        Color outline = SpriteArt.col(38, 40, 58, 1f);
        Color hull = SpriteArt.col(203, 211, 226, 1f);
        Color steel = SpriteArt.col(96, 110, 134, 1f);
        Color steelLight = SpriteArt.col(130, 144, 168, 1f);

        // Api mesin (di belakang badan)
        if (longFlame) {
            SpriteArt.fillPoly(pm, new float[]{44, 64, 2, 50, 2, 78}, SpriteArt.col(255, 150, 40, 0.75f));
            SpriteArt.fillPoly(pm, new float[]{44, 64, 16, 55, 16, 73}, SpriteArt.col(255, 220, 90, 0.9f));
            SpriteArt.fillPoly(pm, new float[]{44, 64, 26, 59, 26, 69}, SpriteArt.col(255, 255, 255, 0.95f));
        } else {
            SpriteArt.fillPoly(pm, new float[]{44, 64, 14, 52, 14, 76}, SpriteArt.col(255, 150, 40, 0.75f));
            SpriteArt.fillPoly(pm, new float[]{44, 64, 24, 56, 24, 72}, SpriteArt.col(255, 220, 90, 0.9f));
            SpriteArt.fillPoly(pm, new float[]{44, 64, 32, 59, 32, 69}, SpriteArt.col(255, 255, 255, 0.95f));
        }

        // Sirip ekor kecil atas & bawah
        SpriteArt.fillPoly(pm, new float[]{66, 48, 84, 32, 98, 46}, steel);
        SpriteArt.fillPoly(pm, new float[]{66, 80, 84, 96, 98, 82}, steel);

        // Sayap belakang atas & bawah
        SpriteArt.fillPoly(pm, new float[]{96, 54, 150, 44, 120, 10, 70, 16}, steel);
        SpriteArt.fillPoly(pm, new float[]{94, 52, 140, 44, 118, 18, 76, 22}, steelLight);
        SpriteArt.fillPoly(pm, new float[]{96, 74, 150, 84, 120, 118, 70, 112}, steel);
        SpriteArt.fillPoly(pm, new float[]{94, 76, 140, 84, 118, 110, 76, 106}, steelLight);
        SpriteArt.fillEllipse(pm, 121, 14, 4f, 4f, SpriteArt.col(120, 230, 255, 0.95f));
        SpriteArt.fillEllipse(pm, 121, 114, 4f, 4f, SpriteArt.col(120, 230, 255, 0.95f));

        // Badan utama: garis tepi gelap dulu, lalu isi lambung
        float[] body = {246, 64, 196, 45, 120, 39, 66, 48, 52, 64, 66, 80, 120, 89, 196, 83};
        SpriteArt.fillPoly(pm, SpriteArt.scaled(body, 128, 64, 1.05f), outline);
        SpriteArt.fillPoly(pm, body, hull);
        // Bayangan separuh bawah + kilau separuh atas
        SpriteArt.fillPoly(pm, new float[]{52, 64, 246, 64, 196, 83, 120, 89, 66, 80},
                SpriteArt.col(148, 158, 182, 0.50f));
        SpriteArt.fillPoly(pm, new float[]{66, 48, 120, 39, 196, 45, 246, 64, 196, 54, 120, 50, 66, 56},
                SpriteArt.col(255, 255, 255, 0.28f));
        // Garis panel
        SpriteArt.fillPoly(pm, new float[]{118, 44, 122, 44, 122, 84, 118, 84}, SpriteArt.col(38, 40, 58, 0.22f));
        SpriteArt.fillPoly(pm, new float[]{148, 42, 152, 42, 152, 86, 148, 86}, SpriteArt.col(38, 40, 58, 0.18f));

        // Kokpit berlapis + kilau kaca
        SpriteArt.fillEllipse(pm, 172, 64, 30, 12, SpriteArt.col(14, 34, 54, 1f));
        SpriteArt.fillEllipse(pm, 172, 64, 24, 9, SpriteArt.col(38, 120, 160, 1f));
        SpriteArt.fillEllipse(pm, 172, 64, 17, 6.5f, SpriteArt.col(90, 210, 245, 1f));
        SpriteArt.fillEllipse(pm, 182, 60, 7, 2.6f, SpriteArt.col(255, 255, 255, 0.95f));

        // Lampu hidung
        SpriteArt.fillEllipse(pm, 238, 64, 3.4f, 3.4f, SpriteArt.col(255, 230, 140, 1f));

        return SpriteArt.toTexture(pm);
    }

    public void update(float delta, float mx, float my, float viewW, float viewH) {
        stateTime += delta;
        if (invulnTime > 0f) invulnTime -= delta;
        if (shieldTime > 0f) shieldTime -= delta;
        if (dead) return;
        float len = (float) Math.sqrt(mx * mx + my * my);
        if (len > 0.01f) {
            float nx = mx / len;
            float ny = my / len;
            float mag = Math.min(1f, len);
            x += nx * SPEED * speedScale * speedFactor * mag * delta;
            y += ny * SPEED * speedScale * speedFactor * mag * delta;
            // Badan berputar mengikuti arah gerak
            float target = (float) Math.toDegrees(Math.atan2(ny, nx));
            facing = SpriteArt.turnToward(facing, target, TURN_RATE * delta);
        }
        if (x < 14f) x = 14f;
        if (x > viewW - 14f) x = viewW - 14f;
        if (y < 10f) y = 10f;
        if (y > viewH - 10f) y = viewH - 10f;
    }

    /** Titik keluar peluru (ujung hidung, mengikuti arah hadap). */
    public float gunX() {
        return x + (float) Math.cos(Math.toRadians(facing)) * 15f;
    }

    public float gunY() {
        return y + (float) Math.sin(Math.toRadians(facing)) * 15f;
    }

    public boolean isInvulnerable() { return invulnTime > 0f || shieldTime > 0f; }

    /**
     * Kena serangan sebesar amount (angka tetap dari tabel kerusakan di
     * SpaceWarGame): darah berkurang, kebal sesaat. Return true bila
     * serangan masuk.
     */
    public boolean damage(int amount) {
        if (dead || invulnTime > 0f) return false;
        hp -= amount;
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
        hp = maxHpEfektif();
        dead = false;
        invulnTime = 2f;
        facing = 0f;
    }

    /** Hidup lagi di tempat (Nyawa Cadangan dari toko): setengah darah + kebal sesaat. */
    public void revive() {
        dead = false;
        hp = maxHpEfektif() / 2;
        invulnTime = 2.5f;
    }

    /** Bingkai sprite untuk gambar besar di menu utama. */
    public TextureRegion teksturMenu() { return frames[0]; }

    public void draw(SpriteBatch batch) {
        if (dead) return;
        TextureRegion frame = frames[(int) (stateTime * 14f) % 2];
        // Berkedip tembus pandang selagi kebal setelah kena serangan
        if (invulnTime > 0f && ((int) (invulnTime * 12f)) % 2 == 0) {
            batch.setColor(1f, 1f, 1f, 0.45f);
        }
        // Digambar berputar sebesar sudut hadap, berporos di tengah pesawat
        batch.draw(frame, x - 19f, y - 9.5f, 19f, 9.5f, 38f, 19f, 1f, 1f, facing);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void dispose() {
        for (Texture t : textures) t.dispose();
    }
}
