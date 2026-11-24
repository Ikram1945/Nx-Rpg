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
    /** Kapal induk (boss): raksasa bersenjata laser & bom, HP 500.
     *  Hanya SATU dalam setiap permainan — kemunculan & senjatanya
     *  diatur SpaceWarGame. */
    public static final int MOTHERSHIP = 2;
    /** Alien roket (hijau): menjaga jarak dan menembakkan MINI ROKET
     *  auto-aim (pengejar) ke sasarannya — peluncuran & roketnya
     *  diatur SpaceWarGame. */
    public static final int ROCKETEER = 3;
    /** Alien roket ELITE (hijau gelap, jauh lebih besar): menembakkan
     *  ROKET BESAR — lebih cepat, lebih lincah memburu, damage jauh
     *  lebih besar dan meledak beradius. Jarang muncul. */
    public static final int ELITE_ROCKET = 4;

    private static Texture[] sharedTextures;
    private static TextureRegion[] sharedRegions;
    private static Texture whiteTex;

    private static void ensureTextures() {
        if (sharedTextures != null) return;
        sharedTextures = new Texture[5];
        sharedRegions = new TextureRegion[5];
        sharedTextures[DRONE] = buildDroneTexture();
        sharedTextures[GUNNER] = buildGunnerTexture();
        sharedTextures[MOTHERSHIP] = buildMothershipTexture();
        sharedTextures[ROCKETEER] = buildRocketeerTexture();
        sharedTextures[ELITE_ROCKET] = buildEliteRocketTexture();
        for (int i = 0; i < 5; i++) {
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

    // ---------- Kapal induk: lambung raksasa berinti besar (kanvas 416x288, hidung ke kiri) ----------

    private static Texture buildMothershipTexture() {
        Pixmap pm = new Pixmap(416, 288, Pixmap.Format.RGBA8888);
        Color outline = SpriteArt.col(30, 26, 44, 1f);
        Color baja = SpriteArt.col(88, 84, 116, 1f);
        Color bajaGelap = SpriteArt.col(58, 54, 84, 1f);
        // Polong mesin belakang (kanan) atas & bawah + cahaya mesin
        SpriteArt.fillEllipse(pm, 356, 62, 52, 26, outline);
        SpriteArt.fillEllipse(pm, 356, 62, 46, 21, bajaGelap);
        SpriteArt.fillEllipse(pm, 356, 226, 52, 26, outline);
        SpriteArt.fillEllipse(pm, 356, 226, 46, 21, bajaGelap);
        SpriteArt.fillEllipse(pm, 388, 62, 14, 8, SpriteArt.col(120, 230, 255, 0.9f));
        SpriteArt.fillEllipse(pm, 388, 226, 14, 8, SpriteArt.col(120, 230, 255, 0.9f));
        // Sayap besar menyapu ke belakang
        SpriteArt.fillPoly(pm, new float[]{150, 108, 330, 30, 372, 44, 240, 128}, bajaGelap);
        SpriteArt.fillPoly(pm, new float[]{150, 180, 330, 258, 372, 244, 240, 160}, bajaGelap);
        SpriteArt.fillPoly(pm, new float[]{150, 108, 318, 40, 240, 122}, baja);
        SpriteArt.fillPoly(pm, new float[]{150, 180, 318, 248, 240, 166}, baja);
        // Lambung utama: tepi gelap lalu isi baja
        float[] hull = {26, 144, 84, 92, 196, 66, 300, 84, 330, 144, 300, 204, 196, 222, 84, 196};
        SpriteArt.fillPoly(pm, SpriteArt.scaled(hull, 208, 144, 1.045f), outline);
        SpriteArt.fillPoly(pm, hull, baja);
        SpriteArt.fillPoly(pm, new float[]{26, 144, 330, 144, 300, 204, 196, 222, 84, 196},
                SpriteArt.col(40, 38, 62, 0.45f));
        SpriteArt.fillPoly(pm, new float[]{84, 92, 196, 66, 300, 84, 330, 144, 240, 96, 120, 96, 60, 128},
                SpriteArt.col(255, 255, 255, 0.13f));
        // Garis panel lambung
        SpriteArt.fillPoly(pm, new float[]{150, 76, 156, 76, 156, 212, 150, 212}, SpriteArt.col(20, 18, 34, 0.35f));
        SpriteArt.fillPoly(pm, new float[]{238, 80, 244, 80, 244, 208, 238, 208}, SpriteArt.col(20, 18, 34, 0.30f));
        // Kubah meriam atas & bawah (sumber laser di hidung kiri)
        SpriteArt.fillEllipse(pm, 92, 96, 26, 18, outline);
        SpriteArt.fillEllipse(pm, 92, 96, 21, 14, SpriteArt.col(150, 60, 70, 1f));
        SpriteArt.fillEllipse(pm, 92, 192, 26, 18, outline);
        SpriteArt.fillEllipse(pm, 92, 192, 21, 14, SpriteArt.col(150, 60, 70, 1f));
        // Moncong laser di hidung (kiri)
        SpriteArt.fillEllipse(pm, 30, 144, 15, 15, outline);
        SpriteArt.fillEllipse(pm, 30, 144, 10, 10, SpriteArt.col(255, 70, 60, 1f));
        SpriteArt.fillEllipse(pm, 30, 144, 4.5f, 4.5f, SpriteArt.col(255, 255, 255, 1f));
        // Inti tenaga raksasa berlapis di tengah
        SpriteArt.fillEllipse(pm, 208, 144, 46, 46, outline);
        SpriteArt.fillEllipse(pm, 208, 144, 38, 38, SpriteArt.col(255, 120, 50, 1f));
        SpriteArt.fillEllipse(pm, 208, 144, 25, 25, SpriteArt.col(255, 200, 80, 1f));
        SpriteArt.fillEllipse(pm, 208, 144, 11, 11, SpriteArt.col(255, 255, 255, 1f));
        // Lampu-lampu lambung
        float[][] lampu = {{110, 128}, {110, 160}, {272, 108}, {272, 180}, {180, 92}, {180, 196}};
        for (float[] l : lampu) {
            SpriteArt.fillEllipse(pm, l[0], l[1], 4.5f, 4.5f, SpriteArt.col(255, 90, 80, 0.95f));
        }
        return SpriteArt.toTexture(pm);
    }

    // ---------- Alien roket: piring hijau + dua roket siap tembak ----------

    private static Texture buildRocketeerTexture() {
        Pixmap pm = new Pixmap(256, 192, Pixmap.Format.RGBA8888);
        Color outline = SpriteArt.col(26, 52, 30, 1f);
        // Cahaya bawah badan
        SpriteArt.fillEllipse(pm, 128, 130, 64, 12, SpriteArt.col(120, 255, 140, 0.28f));
        // Cakram hijau: tepi gelap, isi, kilau atas
        SpriteArt.fillEllipse(pm, 128, 100, 94, 35, outline);
        SpriteArt.fillEllipse(pm, 128, 97, 90, 32, SpriteArt.col(56, 148, 66, 1f));
        SpriteArt.fillEllipse(pm, 128, 90, 82, 25, SpriteArt.col(92, 196, 104, 1f));
        SpriteArt.fillEllipse(pm, 110, 82, 40, 10, SpriteArt.col(255, 255, 255, 0.18f));
        // Kubah kemudi hijau muda
        SpriteArt.fillEllipse(pm, 140, 74, 27, 18, outline);
        SpriteArt.fillEllipse(pm, 140, 72, 23, 15, SpriteArt.col(178, 240, 150, 1f));
        SpriteArt.fillEllipse(pm, 132, 66, 8, 5, SpriteArt.col(255, 255, 255, 0.9f));
        // Dua mini roket terpasang di atas & bawah, hidung ke KIRI
        gambarRoketKecil(pm, 66, 48);
        gambarRoketKecil(pm, 66, 140);
        // Lampu-lampu kecil
        SpriteArt.fillEllipse(pm, 182, 96, 5, 5, SpriteArt.col(255, 90, 70, 1f));
        SpriteArt.fillEllipse(pm, 88, 102, 4, 4, SpriteArt.col(220, 255, 160, 1f));
        return SpriteArt.toTexture(pm);
    }

    /** Mini roket horizontal: badan terang, hidung oranye ke kiri, sirip hijau. */
    private static void gambarRoketKecil(Pixmap pm, float cx, float cy) {
        Color outline = SpriteArt.col(26, 52, 30, 1f);
        SpriteArt.fillEllipse(pm, cx, cy, 31, 9, outline);
        SpriteArt.fillEllipse(pm, cx + 2, cy, 28, 7, SpriteArt.col(224, 230, 238, 1f));
        // Hidung menunjuk KIRI
        SpriteArt.fillPoly(pm, new float[]{cx - 46, cy, cx - 27, cy - 8, cx - 27, cy + 8},
                SpriteArt.col(240, 120, 40, 1f));
        // Sirip ekor atas & bawah
        SpriteArt.fillPoly(pm, new float[]{cx + 20, cy - 4, cx + 33, cy - 13, cx + 25, cy - 1},
                SpriteArt.col(56, 148, 66, 1f));
        SpriteArt.fillPoly(pm, new float[]{cx + 20, cy + 4, cx + 33, cy + 13, cx + 25, cy + 1},
                SpriteArt.col(56, 148, 66, 1f));
    }

    // ---------- Alien roket ELITE: lambung hijau gelap berlapis baja + kubah emas ----------

    private static Texture buildEliteRocketTexture() {
        Pixmap pm = new Pixmap(320, 240, Pixmap.Format.RGBA8888);
        Color outline = SpriteArt.col(16, 34, 20, 1f);
        // Cahaya bawah badan
        SpriteArt.fillEllipse(pm, 160, 162, 84, 15, SpriteArt.col(120, 255, 140, 0.25f));
        // Cakram besar hijau gelap berlapis
        SpriteArt.fillEllipse(pm, 160, 124, 122, 46, outline);
        SpriteArt.fillEllipse(pm, 160, 120, 118, 43, SpriteArt.col(30, 96, 42, 1f));
        SpriteArt.fillEllipse(pm, 160, 112, 108, 34, SpriteArt.col(44, 128, 58, 1f));
        // Sabuk baja di tengah lambung
        SpriteArt.fillEllipse(pm, 160, 126, 118, 16, SpriteArt.col(52, 62, 58, 1f));
        SpriteArt.fillEllipse(pm, 160, 124, 118, 12, SpriteArt.col(74, 88, 82, 1f));
        // Baut-baut baja di sabuk
        for (int i = 0; i < 6; i++) {
            float bx = 66f + i * 38f;
            SpriteArt.fillEllipse(pm, bx, 124, 4.5f, 4.5f, SpriteArt.col(150, 165, 158, 1f));
        }
        // Kubah kemudi EMAS
        SpriteArt.fillEllipse(pm, 172, 88, 36, 24, outline);
        SpriteArt.fillEllipse(pm, 172, 85, 31, 20, SpriteArt.col(240, 188, 74, 1f));
        SpriteArt.fillEllipse(pm, 162, 77, 11, 7, SpriteArt.col(255, 246, 205, 0.95f));
        // Dua roket besar terpasang di atas & bawah, hidung ke KIRI
        gambarRoketKecil(pm, 92, 56);
        gambarRoketKecil(pm, 92, 176);
        // Lampu peringatan merah berderet
        SpriteArt.fillEllipse(pm, 226, 118, 6, 6, SpriteArt.col(255, 70, 55, 1f));
        SpriteArt.fillEllipse(pm, 100, 122, 5, 5, SpriteArt.col(255, 70, 55, 1f));
        SpriteArt.fillEllipse(pm, 160, 138, 5, 5, SpriteArt.col(255, 190, 80, 1f));
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
    /** Koin yang didapat pemain bila menghancurkan musuh ini. */
    public final int coinValue;
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
        if (type == MOTHERSHIP) {
            hp = maxHp = 500;
            radius = 30f;
            scoreValue = 2000;
            coinValue = 500;
            maxSpeed = 24f;
            cruise = 14f;
            prefRange = 215f;
            turnRate = 55f;
            fireCooldown = 3.0f;
        } else if (type == GUNNER) {
            hp = maxHp = 50;
            radius = 10.5f;
            scoreValue = 250;
            coinValue = 25;
            maxSpeed = 46f;
            cruise = 26f;
            prefRange = 145f;
            turnRate = 210f;
            fireCooldown = 1.0f + (float) Math.random() * 1.2f;
        } else if (type == ROCKETEER) {
            hp = maxHp = 35;
            radius = 9.5f;
            scoreValue = 175;
            coinValue = 18;
            maxSpeed = 55f;
            cruise = 32f;
            prefRange = 175f;
            turnRate = 260f;
            fireCooldown = 1.2f + (float) Math.random() * 1.2f;
        } else if (type == ELITE_ROCKET) {
            hp = maxHp = 120;
            radius = 14f;
            scoreValue = 400;
            coinValue = 45;
            maxSpeed = 38f;
            cruise = 24f;
            prefRange = 190f;
            turnRate = 180f;
            fireCooldown = 1.4f + (float) Math.random() * 1.0f;
        } else {
            hp = maxHp = 20;
            radius = 8f;
            scoreValue = 100;
            coinValue = 10;
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

    /**
     * Khusus alien roket: dipanggil tiap frame; return true tepat pada
     * frame sebuah mini roket harus diluncurkan (game yang membuat &
     * mengemudikan roketnya). Jeda antar luncuran 2,6-3,4 detik.
     */
    public boolean siapRoket(float delta, boolean engaged) {
        if (fireCooldown > 0f) fireCooldown -= delta;
        if (engaged && fireCooldown <= 0f) {
            fireCooldown = 2.6f + (float) Math.random() * 0.8f;
            return true;
        }
        return false;
    }

    /**
     * Khusus alien roket ELITE: sama seperti siapRoket, tapi jeda
     * serangannya jauh lebih singkat (1,6-2,2 detik) — serangannya
     * terasa terus-menerus menekan.
     */
    public boolean siapRoketBesar(float delta, boolean engaged) {
        if (fireCooldown > 0f) fireCooldown -= delta;
        if (engaged && fireCooldown <= 0f) {
            fireCooldown = 1.6f + (float) Math.random() * 0.6f;
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
        // Ambang jauh dibuat longgar (560) agar gerombolan musuh yang
        // bertarung agak jauh dari pemain tidak lenyap begitu saja —
        // jumlah musuh hidup harus bisa mencapai puluhan.
        return dx * dx + dy * dy > 560f * 560f;
    }

    public void draw(SpriteBatch batch, BitmapFont font, GlyphLayout layout, float worldH) {
        if (flashTime > 0f) {
            batch.setColor(1f, 0.45f, 0.45f, 1f);
        }
        // Sprite digambar berputar sebesar sudut hadap, berporos di tengah.
        // Gambar aslinya menghadap KIRI, jadi putarannya facing + 180.
        float rot = facing + 180f;
        if (type == MOTHERSHIP) {
            batch.draw(sharedRegions[type], x - 52f, y - 36f,
                    52f, 36f, 104f, 72f, 1f, 1f, rot);
        } else if (type == GUNNER) {
            batch.draw(sharedRegions[type], x - 19f, y - 14.25f,
                    19f, 14.25f, 38f, 28.5f, 1f, 1f, rot);
        } else if (type == ROCKETEER) {
            batch.draw(sharedRegions[type], x - 20f, y - 15f,
                    20f, 15f, 40f, 30f, 1f, 1f, rot);
        } else if (type == ELITE_ROCKET) {
            batch.draw(sharedRegions[type], x - 28f, y - 21f,
                    28f, 21f, 56f, 42f, 1f, 1f, rot);
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
