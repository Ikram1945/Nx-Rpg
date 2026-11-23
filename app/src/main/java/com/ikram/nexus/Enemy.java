package com.ikram.nexus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Pesawat alien dengan AI (game pertempuran luar angkasa).
 * Musuh adalah bot: mereka memilih lawan terdekat — bisa pemain ATAU
 * musuh lain (sesama alien saling serang) — lalu bermanuver menjaga
 * jarak tempur sambil menyamping (strafe), dan menembak beruntun ke
 * arah sasarannya. Punya darah seperti pemain dan bar darah digambar
 * di atas pesawatnya begitu darah berkurang.
 *   DRONE  (keroco): cepat & lincah, 2 darah, tembakan beruntun 3x.
 *   GUNNER (besar) : lambat & tebal, 5 darah, tembakan tunggal terarah.
 * Sprite digambar prosedural sebagai gambar vektor halus; tekstur
 * dipakai bersama oleh semua musuh (dibuat sekali statis).
 * Posisi x/y adalah titik TENGAH pesawat. Otak AI (pemilihan sasaran
 * & kemudi) dijalankan oleh NxRpgGame yang tahu seluruh dunia game;
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

    // ---------- Perkakas gambar vektor di atas Pixmap ----------

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

    // ---------- Drone: piring ungu berkubah (kanvas 192x192) ----------

    private static Texture buildDroneTexture() {
        Pixmap pm = new Pixmap(192, 192, Pixmap.Format.RGBA8888);
        Color outline = col(38, 40, 58, 1f);
        // Cahaya bawah badan
        fillEllipse(pm, 96, 130, 58, 13, col(176, 84, 224, 0.30f));
        // Cakram: tepi gelap, isi ungu, bayangan bawah, kilau atas
        fillEllipse(pm, 96, 102, 82, 32, outline);
        fillEllipse(pm, 96, 102, 78, 29, col(150, 70, 200, 1f));
        fillEllipse(pm, 96, 110, 76, 23, col(108, 40, 158, 0.55f));
        fillEllipse(pm, 96, 92, 58, 15, col(255, 255, 255, 0.16f));
        // Kubah kaca merah muda
        fillEllipse(pm, 96, 72, 44, 32, outline);
        fillEllipse(pm, 96, 72, 40, 29, col(244, 92, 204, 1f));
        fillEllipse(pm, 96, 82, 36, 20, col(200, 60, 170, 0.45f));
        fillEllipse(pm, 82, 58, 12, 6, col(255, 255, 255, 0.80f));
        // Mata merah di sisi depan (kiri)
        fillEllipse(pm, 40, 100, 11, 11, outline);
        fillEllipse(pm, 40, 100, 7, 7, col(255, 80, 70, 1f));
        fillEllipse(pm, 40, 100, 2.5f, 2.5f, col(255, 255, 255, 1f));
        // Lampu-lampu tepi cakram
        fillEllipse(pm, 30, 108, 3.5f, 3.5f, col(120, 230, 255, 0.95f));
        fillEllipse(pm, 62, 120, 3.5f, 3.5f, col(120, 230, 255, 0.95f));
        fillEllipse(pm, 130, 120, 3.5f, 3.5f, col(120, 230, 255, 0.95f));
        fillEllipse(pm, 162, 108, 3.5f, 3.5f, col(120, 230, 255, 0.95f));
        return toTexture(pm);
    }

    // ---------- Gunner: lambung merah bergarpu, inti menyala (kanvas 256x192) ----------

    private static Texture buildGunnerTexture() {
        Pixmap pm = new Pixmap(256, 192, Pixmap.Format.RGBA8888);
        Color outline = col(52, 18, 20, 1f);
        // Garpu depan (kiri): atas & bawah, dengan celah menyala
        fillPoly(pm, new float[]{10, 30, 90, 52, 90, 66, 10, 58}, col(110, 30, 28, 1f));
        fillPoly(pm, new float[]{10, 162, 90, 140, 90, 126, 10, 134}, col(110, 30, 28, 1f));
        fillPoly(pm, new float[]{16, 52, 88, 60, 88, 63, 16, 57}, col(255, 150, 40, 0.65f));
        fillPoly(pm, new float[]{16, 140, 88, 132, 88, 129, 16, 135}, col(255, 150, 40, 0.65f));
        // Lambung utama: tepi gelap lalu isi merah
        float[] hull = {96, 28, 186, 20, 238, 66, 240, 96, 238, 126, 186, 172, 96, 164, 64, 96};
        fillPoly(pm, scaled(hull, 128, 96, 1.05f), outline);
        fillPoly(pm, hull, col(206, 74, 60, 1f));
        fillPoly(pm, new float[]{64, 96, 240, 96, 238, 126, 186, 172, 96, 164},
                col(156, 44, 40, 0.55f));
        fillPoly(pm, new float[]{96, 28, 186, 20, 238, 66, 240, 96, 190, 44, 110, 40, 70, 80},
                col(255, 255, 255, 0.16f));
        // Polong belakang atas & bawah berlampu
        fillEllipse(pm, 216, 40, 16, 10, col(70, 60, 80, 1f));
        fillEllipse(pm, 216, 152, 16, 10, col(70, 60, 80, 1f));
        fillEllipse(pm, 216, 40, 8, 3, col(255, 170, 60, 0.9f));
        fillEllipse(pm, 216, 152, 8, 3, col(255, 170, 60, 0.9f));
        // Inti menyala berlapis
        fillEllipse(pm, 150, 96, 24, 24, outline);
        fillEllipse(pm, 150, 96, 19, 19, col(255, 150, 40, 1f));
        fillEllipse(pm, 150, 96, 12, 12, col(255, 220, 90, 1f));
        fillEllipse(pm, 150, 96, 5.5f, 5.5f, col(255, 255, 255, 1f));
        return toTexture(pm);
    }

    // ---------- Keadaan bot ----------

    public final int type;
    public float x, y;
    public float vx, vy;
    public int hp;
    public final int maxHp;
    public final float radius;
    public final int scoreValue;
    /** Kecepatan gerak maksimum & kecepatan jelajah tanpa sasaran. */
    public final float maxSpeed;
    public final float cruise;
    /** Jarak tempur yang dijaga dari sasarannya. */
    public final float prefRange;
    public float phase;
    public float time;

    /** True bila yang menghancurkan musuh ini adalah pemain (untuk skor). */
    public boolean killedByPlayer;

    // Sasaran saat ini (diisi oleh AI di NxRpgGame)
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
            hp = maxHp = 5;
            radius = 10.5f;
            scoreValue = 250;
            maxSpeed = 46f;
            cruise = 26f;
            prefRange = 145f;
            fireCooldown = 1.0f + (float) Math.random() * 1.2f;
        } else {
            hp = maxHp = 2;
            radius = 8f;
            scoreValue = 100;
            maxSpeed = 82f;
            cruise = 40f;
            prefRange = 95f;
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

    /** Kena satu peluru. */
    public boolean hit() {
        return damage(1);
    }

    public boolean isGone() {
        return x < -24f;
    }

    public void draw(SpriteBatch batch) {
        if (flashTime > 0f) {
            batch.setColor(1f, 0.45f, 0.45f, 1f);
        }
        if (type == GUNNER) {
            batch.draw(sharedRegions[type], x - 19f, y - 14.25f, 38f, 28.5f);
        } else {
            batch.draw(sharedRegions[type], x - 15f, y - 15f, 30f, 30f);
        }
        batch.setColor(1f, 1f, 1f, 1f);
        // Bar darah di atas pesawat, muncul begitu darah berkurang;
        // dekat tepi atas dunia (tinggi 180) bar pindah ke bawah pesawat
        if (hp < maxHp && hp > 0) {
            float barW = radius * 2f;
            float barX = x - barW / 2f;
            float barY = y > 158f ? y - radius - 13f : y + radius + 9.5f;
            float frac = (float) hp / (float) maxHp;
            batch.setColor(0f, 0f, 0f, 0.55f);
            batch.draw(whiteTex, barX - 0.6f, barY - 0.6f, barW + 1.2f, 3.6f);
            batch.setColor(Math.min(1f, 1.7f * (1f - frac)), Math.min(1f, 1.7f * frac), 0.15f, 0.95f);
            batch.draw(whiteTex, barX, barY, barW * frac, 2.4f);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }
}
