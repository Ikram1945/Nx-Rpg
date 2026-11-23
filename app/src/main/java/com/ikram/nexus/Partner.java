package com.ikram.nexus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Partner pemain (Space War): pesawat teman yang dikendalikan AI.
 * Terbang dalam formasi di sisi pemain dan MENEMBAK OTOMATIS musuh
 * terdekat di jangkauannya — membasmi musuh bersama pemain. Punya
 * darah sendiri (sistem angka, seperti semua pesawat); bila hancur,
 * ia meledak dan muncul lagi di sisi pemain setelah beberapa detik
 * (diatur oleh SpaceWarGame). Badan berbelok mengikuti arah gerak
 * (facing, derajat; 0 = kanan). Sprite prosedural lewat SpriteArt,
 * warna hijau toska agar mudah dibedakan dari pesawat pemain.
 * Posisi x/y adalah titik TENGAH pesawat.
 * (Rencana masa depan: partner bisa dipasang/dilepas; sekarang ia
 * selalu ikut.)
 */
public class Partner {

    public static final int MAX_HP = 50;
    public static final float RADIUS = 6.5f;
    /** Kecepatan terbang maksimum — lebih cepat dari pemain agar bisa
     *  mengejar formasi, termasuk saat pemain memakai NOS. */
    public static final float MAX_SPEED = 265f;
    private static final float TURN_RATE = 540f;

    private final Texture[] textures = new Texture[2];
    private final TextureRegion[] frames = new TextureRegion[2];
    private Texture whiteTex;

    public float x, y;
    /** Arah hadap pesawat dalam derajat (0 = kanan, CCW). */
    public float facing;
    public int hp = MAX_HP;
    public boolean dead;
    private float invulnTime;
    private float stateTime;
    private float flashTime;

    public Partner(float startX, float startY) {
        textures[0] = buildShipTexture(false);
        textures[1] = buildShipTexture(true);
        for (int i = 0; i < 2; i++) {
            frames[i] = new TextureRegion(textures[i]);
        }
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1f, 1f, 1f, 1f);
        pm.fill();
        whiteTex = new Texture(pm);
        pm.dispose();
        x = startX;
        y = startY;
    }

    // ---------- Gambar pesawat partner (kanvas 256x128, hidung ke kanan) ----------

    private static Texture buildShipTexture(boolean longFlame) {
        Pixmap pm = new Pixmap(256, 128, Pixmap.Format.RGBA8888);
        Color outline = SpriteArt.col(30, 44, 44, 1f);
        Color hull = SpriteArt.col(150, 226, 205, 1f);
        Color steel = SpriteArt.col(52, 120, 108, 1f);
        Color steelLight = SpriteArt.col(84, 158, 142, 1f);

        // Api mesin
        if (longFlame) {
            SpriteArt.fillPoly(pm, new float[]{44, 64, 2, 50, 2, 78}, SpriteArt.col(255, 150, 40, 0.75f));
            SpriteArt.fillPoly(pm, new float[]{44, 64, 16, 55, 16, 73}, SpriteArt.col(255, 220, 90, 0.9f));
            SpriteArt.fillPoly(pm, new float[]{44, 64, 26, 59, 26, 69}, SpriteArt.col(255, 255, 255, 0.95f));
        } else {
            SpriteArt.fillPoly(pm, new float[]{44, 64, 14, 52, 14, 76}, SpriteArt.col(255, 150, 40, 0.75f));
            SpriteArt.fillPoly(pm, new float[]{44, 64, 24, 56, 24, 72}, SpriteArt.col(255, 220, 90, 0.9f));
            SpriteArt.fillPoly(pm, new float[]{44, 64, 32, 59, 32, 69}, SpriteArt.col(255, 255, 255, 0.95f));
        }

        // Sirip ekor
        SpriteArt.fillPoly(pm, new float[]{66, 48, 84, 32, 98, 46}, steel);
        SpriteArt.fillPoly(pm, new float[]{66, 80, 84, 96, 98, 82}, steel);

        // Sayap belakang
        SpriteArt.fillPoly(pm, new float[]{96, 54, 150, 44, 120, 10, 70, 16}, steel);
        SpriteArt.fillPoly(pm, new float[]{94, 52, 140, 44, 118, 18, 76, 22}, steelLight);
        SpriteArt.fillPoly(pm, new float[]{96, 74, 150, 84, 120, 118, 70, 112}, steel);
        SpriteArt.fillPoly(pm, new float[]{94, 76, 140, 84, 118, 110, 76, 106}, steelLight);
        SpriteArt.fillEllipse(pm, 121, 14, 4f, 4f, SpriteArt.col(255, 230, 140, 0.95f));
        SpriteArt.fillEllipse(pm, 121, 114, 4f, 4f, SpriteArt.col(255, 230, 140, 0.95f));

        // Badan utama
        float[] body = {246, 64, 196, 45, 120, 39, 66, 48, 52, 64, 66, 80, 120, 89, 196, 83};
        SpriteArt.fillPoly(pm, SpriteArt.scaled(body, 128, 64, 1.05f), outline);
        SpriteArt.fillPoly(pm, body, hull);
        SpriteArt.fillPoly(pm, new float[]{52, 64, 246, 64, 196, 83, 120, 89, 66, 80},
                SpriteArt.col(96, 178, 152, 0.50f));
        SpriteArt.fillPoly(pm, new float[]{66, 48, 120, 39, 196, 45, 246, 64, 196, 54, 120, 50, 66, 56},
                SpriteArt.col(255, 255, 255, 0.28f));

        // Kokpit
        SpriteArt.fillEllipse(pm, 172, 64, 30, 12, SpriteArt.col(10, 40, 34, 1f));
        SpriteArt.fillEllipse(pm, 172, 64, 24, 9, SpriteArt.col(28, 130, 100, 1f));
        SpriteArt.fillEllipse(pm, 172, 64, 17, 6.5f, SpriteArt.col(120, 245, 200, 1f));
        SpriteArt.fillEllipse(pm, 182, 60, 7, 2.6f, SpriteArt.col(255, 255, 255, 0.95f));

        // Lampu hidung
        SpriteArt.fillEllipse(pm, 238, 64, 3.4f, 3.4f, SpriteArt.col(255, 230, 140, 1f));

        return SpriteArt.toTexture(pm);
    }

    /** Titik keluar peluru (ujung hidung, mengikuti arah hadap). */
    public float gunX() {
        return x + (float) Math.cos(Math.toRadians(facing)) * 12f;
    }

    public float gunY() {
        return y + (float) Math.sin(Math.toRadians(facing)) * 12f;
    }

    public boolean isInvulnerable() { return invulnTime > 0f; }

    /**
     * Kena serangan sebesar amount (angka tetap dari tabel kerusakan di
     * SpaceWarGame). Return true bila serangan masuk; dead menjadi true
     * tepat saat darah habis.
     */
    public boolean damage(int amount) {
        if (dead || invulnTime > 0f) return false;
        hp -= amount;
        invulnTime = 0.8f;
        flashTime = 0.12f;
        if (hp <= 0) {
            hp = 0;
            dead = true;
        }
        return true;
    }

    /** Muncul (kembali) di titik yang diberikan dengan darah penuh. */
    public void respawn(float rx, float ry) {
        x = rx;
        y = ry;
        hp = MAX_HP;
        dead = false;
        invulnTime = 2f;
        facing = 0f;
    }

    /**
     * Gerak formasi & putaran badan. Kemudi (titik formasi di sisi
     * pemain + tembakan otomatis) dihitung SpaceWarGame; metode ini
     * menerima kecepatan yang diinginkan lalu menerapkannya.
     */
    public void update(float delta, float vx, float vy, float worldW, float worldH) {
        stateTime += delta;
        if (invulnTime > 0f) invulnTime -= delta;
        if (flashTime > 0f) flashTime -= delta;
        if (dead) return;
        x += vx * delta;
        y += vy * delta;
        if (x < 12f) x = 12f;
        if (x > worldW - 12f) x = worldW - 12f;
        if (y < 10f) y = 10f;
        if (y > worldH - 10f) y = worldH - 10f;
        float spd2 = vx * vx + vy * vy;
        if (spd2 > 100f) {
            float target = (float) Math.toDegrees(Math.atan2(vy, vx));
            facing = SpriteArt.turnToward(facing, target, TURN_RATE * delta);
        }
    }

    /** Hadapkan badan ke titik tertentu (dipakai saat diam di formasi). */
    public void faceToward(float tx, float ty, float delta) {
        float target = (float) Math.toDegrees(Math.atan2(ty - y, tx - x));
        facing = SpriteArt.turnToward(facing, target, TURN_RATE * delta);
    }

    public void draw(SpriteBatch batch, BitmapFont font, GlyphLayout layout, float worldH) {
        if (dead) return;
        TextureRegion frame = frames[(int) (stateTime * 14f) % 2];
        if (flashTime > 0f) {
            batch.setColor(1f, 0.5f, 0.5f, 1f);
        } else if (invulnTime > 0f && ((int) (invulnTime * 12f)) % 2 == 0) {
            batch.setColor(1f, 1f, 1f, 0.45f);
        }
        batch.draw(frame, x - 15f, y - 7.5f, 15f, 7.5f, 30f, 15f, 1f, 1f, facing);
        batch.setColor(1f, 1f, 1f, 1f);
        // Bar darah + angka HP di atas pesawat, sama seperti musuh
        if (hp < MAX_HP && hp > 0) {
            boolean diBawah = y > worldH - 22f;
            float barW = RADIUS * 2f;
            float barX = x - barW / 2f;
            float barY = diBawah ? y - RADIUS - 13f : y + RADIUS + 9.5f;
            float frac = (float) hp / (float) MAX_HP;
            batch.setColor(0f, 0f, 0f, 0.55f);
            batch.draw(whiteTex, barX - 0.6f, barY - 0.6f, barW + 1.2f, 3.6f);
            batch.setColor(Math.min(1f, 1.7f * (1f - frac)), Math.min(1f, 1.7f * frac), 0.15f, 0.95f);
            batch.draw(whiteTex, barX, barY, barW * frac, 2.4f);
            batch.setColor(1f, 1f, 1f, 1f);
            if (font != null && layout != null) {
                float skalaLama = font.getData().scaleX;
                font.getData().setScale(0.30f);
                String teks = String.valueOf(hp);
                layout.setText(font, teks);
                font.setColor(1f, 1f, 1f, 0.95f);
                float teksY = diBawah ? barY - 1.2f - layout.height : barY + 4.8f;
                font.draw(batch, teks, x - layout.width / 2f, teksY);
                font.getData().setScale(skalaLama);
                font.setColor(1f, 1f, 1f, 1f);
                batch.setColor(1f, 1f, 1f, 1f);
            }
        }
    }

    public void dispose() {
        for (Texture t : textures) t.dispose();
        if (whiteTex != null) whiteTex.dispose();
    }
}
