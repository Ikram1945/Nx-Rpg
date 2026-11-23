package com.ikram.nexus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Pesawat alien dengan AI (Space War).
 * Musuh adalah bot: mereka memilih lawan terdekat — bisa pemain ATAU
 * musuh lain (sesama alien saling serang) — lalu bermanuver menjaga
 * jarak tempur sambil menyamping (strafe), dan menembak beruntun ke
 * arah sasarannya. Badannya BERBELOK mengikuti arah terbangnya
 * (facing, derajat; 0 = kanan). Punya darah seperti pemain dan bar
 * darah digambar di atas pesawatnya begitu darah berkurang.
 *   DRONE  (keroco): cepat & lincah, HP 20, tembakan beruntun 3x.
 *   GUNNER (besar) : lambat & tebal, HP 50, tembakan tunggal terarah.
 * Sprite digambar prosedural sebagai gambar vektor halus lewat
 * SpriteArt; tekstur dipakai bersama oleh semua musuh (statis).
 * Posisi x/y adalah titik TENGAH pesawat. Otak AI (pemilihan sasaran
 * & kemudi) dijalankan oleh SpaceWarGame yang tahu seluruh dunia game;
 * kelas ini menyimpan keadaan, gerak, tembakan, dan gambar.
 */
public class Enemy {

    public static final int DRONE = 0;
    public static final int GUNNER = 1;

    private static Texture[] sharedTextures;
    private static TextureRegion[] sharedRegions;
    private static Texture whiteTex;

    private static void ensureTextures() {
        if (sharedTextures != null) return;
        sharedTextures = new Texture[2];
        sharedRegions = new TextureRegion[2];
        sharedTextures[DRONE] = buildDroneTexture();
        sharedTextures[GUNNER] = buildGunnerTexture();
        for (int i = 0; i < 2; i++) {
            sharedRegions[i] = new TextureRegion(sharedTextures[i]);
        }
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1f, 1f, 1f, 1f);
        pm.fill();
        whiteTex = new Texture(pm);
        pm.dispose();
    }

    public static void disposeShared() {
        if (sharedTextures != null) {
            for (Texture t : sharedTextures) t.dispose();
            sharedTextures = null;
            sharedRegions = null;
        }
        if (whiteTex != null) {
            whiteTex.dispose();
            whiteTex = null;
        }
    }

    // ---------- Drone: piring ungu berkubah (kanvas 192x192) ----------

    private static Texture buildDroneTexture() {
        Pixmap pm = new Pixmap(192, 192, Pixmap.Format.RGBA8888);
        Color outline = SpriteArt.col(38, 40, 58, 1f);
        // Cahaya bawah badan
        SpriteArt.fillEllipse(pm, 96, 130, 58, 13, SpriteArt.col(176, 84, 224, 0.30f));
        // Cakram: tepi gelap, isi ungu, bayangan bawah, kilau atas
        SpriteArt.fillEllipse(pm, 96, 102, 82, 32, outline);
        SpriteArt.fillEllipse(pm, 96, 102, 78, 29, SpriteArt.col(150, 70, 200, 1f));
        SpriteArt.fillEllipse(pm, 96, 110, 76, 23, SpriteArt.col(108, 40, 158, 0.55f));
        SpriteArt.fillEllipse(pm, 96, 92, 58, 15, SpriteArt.col(255, 255, 255, 0.16f));
        // Kubah kaca merah muda
        SpriteArt.fillEllipse(pm, 96, 72, 44, 32, outline);
        SpriteArt.fillEllipse(pm, 96, 72, 40, 29, SpriteArt.col(244, 92, 204, 1f));
        SpriteArt.fillEllipse(pm, 96, 82, 36, 20, SpriteArt.col(200, 60, 170, 0.45f));
        SpriteArt.fillEllipse(pm, 82, 58, 12, 6, SpriteArt.col(255, 255, 255, 0.80f));
        // Mata merah di sisi depan (kiri)
        SpriteArt.fillEllipse(pm, 40, 100, 11, 11, outline);
        SpriteArt.fillEllipse(pm, 40, 100, 7, 7, SpriteArt.col(255, 80, 70, 1f));
        SpriteArt.fillEllipse(pm, 40, 100, 2.5f, 2.5f, SpriteArt.col(255, 255, 255, 1f));
        // Lampu-lampu tepi cakram
        SpriteArt.fillEllipse(pm, 30, 108, 3.5f, 3.5f, SpriteArt.col(120, 230, 255, 0.95f));
        SpriteArt.fillEllipse(pm, 62, 120, 3.5f, 3.5f, SpriteArt.col(120, 230, 255, 0.95f));
        SpriteArt.fillEllipse(pm, 130, 120, 3.5f, 3.5f, SpriteArt.col(120, 230, 255, 0.95f));
        SpriteArt.fillEllipse(pm, 162, 108, 3.5f, 3.5f, SpriteArt.col(120, 230, 255, 0.95f));
        return SpriteArt.toTexture(pm);
    }

    // ---------- Gunner: lambung merah bergarpu, inti menyala (kanvas 256x192) ----------

    private static Texture buildGunnerTexture() {
        Pixmap pm = new Pixmap(256, 192, Pixmap.Format.RGBA8888);
        Color outline = SpriteArt.col(52, 18, 20, 1f);
        // Garpu depan (kiri): atas & bawah, dengan celah menyala
        SpriteArt.fillPoly(pm, new float[]{10, 30, 90, 52, 90, 66, 10, 58}, SpriteArt.col(110, 30, 28, 1f));
        SpriteArt.fillPoly(pm, new float[]{10, 162, 90, 140, 90, 126, 10, 134}, SpriteArt.col(110, 30, 28, 1f));
        SpriteArt.fillPoly(pm, new float[]{16, 52, 88, 60, 88, 63, 16, 57}, SpriteArt.col(255, 150, 40, 0.65f));
        SpriteArt.fillPoly(pm, new float[]{16, 140, 88, 132, 88, 129, 16, 135}, SpriteArt.col(255, 150, 40, 0.65f));
        // Lambung utama: tepi gelap lalu isi merah
        float[] hull = {96, 28, 186, 20, 238, 66, 240, 96, 238, 126, 186, 172, 96, 164, 64, 96};
        SpriteArt.fillPoly(pm, SpriteArt.scaled(hull, 128, 96, 1.05f), outline);
        SpriteArt.fillPoly(pm, hull, SpriteArt.col(206, 74, 60, 1f));
        SpriteArt.fillPoly(pm, new float[]{64, 96, 240, 96, 238, 126, 186, 172, 96, 164},
                SpriteArt.col(156, 44, 40, 0.55f));
        SpriteArt.fillPoly(pm, new float[]{96, 28, 186, 20, 238, 66, 240, 96, 190, 44, 110, 40, 70, 80},
                SpriteArt.col(255, 255, 255, 0.16f));
        // Polong belakang atas & bawah berlampu
        SpriteArt.fillEllipse(pm, 216, 40, 16, 10, SpriteArt.col(70, 60, 80, 1f));
        SpriteArt.fillEllipse(pm, 216, 152, 16, 10, SpriteArt.col(70, 60, 80, 1f));
        SpriteArt.fillEllipse(pm, 216, 40, 8, 3, SpriteArt.col(255, 170, 60, 0.9f));
        SpriteArt.fillEllipse(pm, 216, 152, 8, 3, SpriteArt.col(255, 170, 60, 0.9f));
        // Inti menyala berlapis
        SpriteArt.fillEllipse(pm, 150, 96, 24, 24, outline);
        SpriteArt.fillEllipse(pm, 150, 96, 19, 19, SpriteArt.col(255, 150, 40, 1f));
        SpriteArt.fillEllipse(pm, 150, 96, 12, 12, SpriteArt.col(255, 220, 90, 1f));
        SpriteArt.fillEllipse(pm, 150, 96, 5.5f, 5.5f, SpriteArt.col(255, 255, 255, 1f));
        return SpriteArt.toTexture(pm);
    }

    // ---------- Keadaan bot ----------

    public final int type;
    public float x, y;
    public float vx, vy;
    /** Arah hadap pesawat dalam derajat (0 = kanan, CCW). */
    public float facing = 180f;
    public int hp;
    public final int maxHp;
    public final float radius;
    public final int scoreValue;
    /** Kecepatan gerak maksimum & kecepatan jelajah tanpa sasaran. */
    public final float maxSpeed;
    public final float cruise;
    /** Jarak tempur yang dijaga dari sasarannya. */
    public final float prefRange;
    /** Kecepatan berputar badan (derajat per detik). */
    public final float turnRate;
    public float phase;
    public float time;

    /** True bila yang menghancurkan musuh ini adalah pemain (untuk skor). */
    public boolean killedByPlayer;

    // Sasaran saat ini (diisi oleh AI di SpaceWarGame)
    public boolean hasTarget;
    public boolean targetIsPlayer;
    public Enemy targetEnemy;
    public float targetX, targetY, targetDist;
    public float retargetTimer;
    public float strafeTimer;
    public int strafeDir = 1;

    private float fireCooldown;
    private int burstLeft;
    private float burstTimer;
    private float flashTime;

    public Enemy(int type, float startX, float startY) {
        ensureTextures();
        this.type = type;
        x = startX;
        y = startY;
        phase = (float) (Math.random() * Math.PI * 2.0);
        retargetTimer = (float) Math.random() * 0.5f;
        strafeDir = Math.random() < 0.5 ? -1 : 1;
        if (type == GUNNER) {
            hp = maxHp = 50;
            radius = 10.5f;
            scoreValue = 250;
            maxSpeed = 46f;
            cruise = 26f;
            prefRange = 145f;
            turnRate = 210f;
            fireCooldown = 1.0f + (float) Math.random() * 1.2f;
        } else {
            hp = maxHp = 20;
            radius = 8f;
            scoreValue = 100;
            maxSpeed = 82f;
            cruise = 40f;
            prefRange = 95f;
            turnRate = 320f;
            fireCooldown = 0.7f + (float) Math.random() * 1.0f;
        }
    }

    public void update(float delta, float viewW, float viewH) {
        time += delta;
        if (flashTime > 0f) flashTime -= delta;
        x += vx * delta;
        y += vy * delta;
        if (y < 10f) y = 10f;
        if (y > viewH - 10f) y = viewH - 10f;
        if (x > viewW + 26f) x = viewW + 26f;
        // Badan berbelok mengikuti arah terbang
        float spd2 = vx * vx + vy * vy;
        if (spd2 > 16f) {
            float target = (float) Math.toDegrees(Math.atan2(vy, vx));
            facing = SpriteArt.turnToward(facing, target, turnRate * delta);
        }
    }

    private float cooldownTime() {
        return type == DRONE
                ? 1.6f + (float) Math.random() * 0.9f
                : 1.9f + (float) Math.random() * 0.7f;
    }

    /**
     * Mesin tembak bot. Dipanggil tiap frame; return true tepat pada
     * frame sebuah peluru harus dibuat (game yang mengarahkannya ke
     * sasaran). Drone menembak beruntun 3x, gunner 1x terarah.
     */
    public boolean updateFire(float delta, boolean engaged) {
        if (fireCooldown > 0f) fireCooldown -= delta;
        if (burstLeft > 0) {
            burstTimer -= delta;
            if (burstTimer <= 0f) {
                burstLeft--;
                burstTimer = 0.11f;
                if (burstLeft == 0) fireCooldown = cooldownTime();
                return true;
            }
            return false;
        }
        if (engaged && fireCooldown <= 0f) {
            burstLeft = (type == DRONE ? 3 : 1) - 1;
            burstTimer = 0.11f;
            if (burstLeft == 0) fireCooldown = cooldownTime();
            return true;
        }
        return false;
    }

    /** Terima kerusakan. Return true bila musuh hancur. */
    public boolean damage(int amount) {
        hp -= amount;
        flashTime = 0.12f;
        return hp <= 0;
    }

    /** Musuh dibuang bila keluar batas dunia atau terlalu jauh dari pemain. */
    public boolean isGone(float playerX, float playerY, float worldW, float worldH) {
        if (x < -30f || x > worldW + 30f || y < -30f || y > worldH + 30f) return true;
        float dx = x - playerX;
        float dy = y - playerY;
        return dx * dx + dy * dy > 320f * 320f;
    }

    public void draw(SpriteBatch batch, BitmapFont font, GlyphLayout layout, float worldH) {
        if (flashTime > 0f) {
            batch.setColor(1f, 0.45f, 0.45f, 1f);
        }
        // Sprite digambar berputar sebesar sudut hadap, berporos di tengah.
        // Gambar aslinya menghadap KIRI, jadi putarannya facing + 180.
        float rot = facing + 180f;
        if (type == GUNNER) {
            batch.draw(sharedRegions[type], x - 19f, y - 14.25f,
                    19f, 14.25f, 38f, 28.5f, 1f, 1f, rot);
        } else {
            batch.draw(sharedRegions[type], x - 15f, y - 15f,
                    15f, 15f, 30f, 30f, 1f, 1f, rot);
        }
        batch.setColor(1f, 1f, 1f, 1f);
        // Bar darah + angka HP (tidak ikut berputar), muncul begitu darah
        // berkurang; dekat tepi atas dunia bar & angka pindah ke bawah
        // pesawat supaya tidak keluar peta.
        if (hp < maxHp && hp > 0) {
            boolean diBawah = y > worldH - 22f;
            float barW = radius * 2f;
            float barX = x - barW / 2f;
            float barY = diBawah ? y - radius - 13f : y + radius + 9.5f;
            float frac = (float) hp / (float) maxHp;
            batch.setColor(0f, 0f, 0f, 0.55f);
            batch.draw(whiteTex, barX - 0.6f, barY - 0.6f, barW + 1.2f, 3.6f);
            batch.setColor(Math.min(1f, 1.7f * (1f - frac)), Math.min(1f, 1.7f * frac), 0.15f, 0.95f);
            batch.draw(whiteTex, barX, barY, barW * frac, 2.4f);
            batch.setColor(1f, 1f, 1f, 1f);
            // Angka sisa HP persis di sisi luar bar
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
}
