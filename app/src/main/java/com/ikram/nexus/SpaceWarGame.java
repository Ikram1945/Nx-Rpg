package com.ikram.nexus;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

/**
 * Space War — pertempuran luar angkasa.
 * Pesawat pemain melawan bot-bot alien (drone & gunner) yang saling
 * menyerang juga. Semua pesawat bisa berbelok ke segala arah mengikuti
 * arah terbangnya. Kendali: joystick tetap di kiri bawah (sentuh +
 * geser) atau WASD / panah untuk bergerak, tombol tembak di kanan bawah
 * (tahan untuk menembak terus) atau SPASI; tembakan mengikuti arah
 * hadap pesawat. Semua sprite digambar prosedural.
 */
public class SpaceWarGame extends ApplicationAdapter {

    private static final float VIEW_W = 320f;
    private static final float VIEW_H = 180f;
    /** Ukuran dunia (peta) — jauh lebih besar dari layar; kamera mengikuti pemain. */
    private static final float WORLD_W = 1280f;
    private static final float WORLD_H = 720f;
    private static final float FIRE_INTERVAL = 0.14f;
    private static final float PLAYER_BULLET_SPEED = 300f;
    // NOS (ledakan kecepatan untuk pemain): durasi, jeda isi ulang, pengali laju
    private static final float NOS_DURATION = 1.6f;
    private static final float NOS_COOLDOWN = 4f;
    private static final float NOS_MULT = 2.1f;
    // Skill roket pemain: roket pemburu auto-aim ke musuh terdekat
    private static final float ROCKET_COOLDOWN = 5f;
    private static final float ROCKET_SPEED = 215f;
    private static final float ROCKET_TURN = 300f;   // derajat per detik
    private static final float ROCKET_LIFE = 4f;
    private static final int ROCKET_DAMAGE = 20;
    private static final float ROCKET_SPLASH_RADIUS = 30f;
    private static final int ROCKET_SPLASH_DAMAGE = 10;
    // Tabel kerusakan TETAP (tercatat, tidak pernah acak): semua peluru
    // (pemain maupun musuh) 10, tabrakan badan 20 untuk kedua pihak.
    private static final int BULLET_DAMAGE = 10;
    private static final int RAM_DAMAGE = 20;
    // Komposisi musuh: alien ungu (drone) 70%, alien kuning (gunner) 30%
    private static final float PELUANG_KUNING = 0.30f;
    // Bot musuh yang hidup bersamaan dibatasi segini
    private static final int MAX_BOT_HIDUP = 7;
    private static final float ROCKET_LOCK_RANGE = 260f;

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private OrthographicCamera screenCam;
    private SpriteBatch uiBatch;
    private BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();

    private Texture joyBaseTex, joyKnobTex, fireBtnTex, nosBtnTex, rocketBtnTex, rocketTex, whiteTex, softDotTex;
    private TextureRegion rocketRegion;

    private Player player;
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Bullet> bullets = new ArrayList<>();
    private final ArrayList<Rocket> rockets = new ArrayList<>();
    private final ArrayList<Particle> particles = new ArrayList<>();

    // Bintang latar: 3 lapis paralaks [x, y, kecepatan, ukuran, terang]
    private final ArrayList<float[]> stars = new ArrayList<>();

    // Nebula: gumpalan cahaya lembut raksasa [x, y, kecepatan, ukuran, r, g, b, alfa]
    private final ArrayList<float[]> nebula = new ArrayList<>();

    private Preferences prefs;
    private int score;
    private int hiScore;

    private static final int STATE_PLAY = 0;
    private static final int STATE_OVER = 1;
    private int state = STATE_PLAY;
    private float overTimer;
    private float elapsed;
    private float spawnTimer = 0.8f;
    private float fireTimer;
    private float hitFlash;

    // Penampil error: bila aktif, game berhenti & layar error yang digambar
    private final ErrorScreen errorScreen = new ErrorScreen();

    // Joystick tetap (piksel layar, y ke atas)
    private boolean joyActive = false;
    private int joyPointer = -1;
    private float joyCenterX, joyCenterY;
    private float joyBaseSize, joyKnobSize, joyTravel;
    private float knobX, knobY;
    private float moveX, moveY;

    // Tombol tembak (piksel layar, y ke atas)
    private int firePointer = -1;
    private float fireCenterX, fireCenterY, fireBtnSize;
    private boolean fireHeld = false;

    // Tombol NOS (piksel layar) + keadaan ledakan kecepatan
    private int nosPointer = -1;
    private float nosCenterX, nosCenterY, nosBtnSize;
    private boolean nosHeld = false;
    private float nosTime;
    private float nosCooldown;

    // Kamera pengikut: titik tengah pandangan saat ini di dunia
    private float camX, camY, prevCamX, prevCamY;
    private boolean camInit;

    // Tombol roket (piksel layar) + jeda isi ulang skill
    private int rocketPointer = -1;
    private float rocketCenterX, rocketCenterY, rocketBtnSize;
    private boolean rocketHeld = false;
    private float rocketCooldown;

    private static class Bullet {
        float x, y, vx, vy;
        boolean friendly;
        boolean dead;
        /** Penembaknya untuk peluru musuh (agar tidak kena dirinya sendiri). */
        Enemy owner;
        Bullet(float x, float y, float vx, float vy, boolean friendly, Enemy owner) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.friendly = friendly; this.owner = owner;
        }
    }

    /** Roket pemain: meluncur lalu memburu musuh yang dikunci (auto-aim). */
    private static class Rocket {
        float x, y, vx, vy;
        float angle;   // arah gerak saat ini (derajat)
        float life;    // sisa umur terbang sebelum roket gugur sendiri
        Enemy target;  // musuh yang dikunci; null = terbang lurus / cari kunci baru
        boolean dead;
        Rocket(float x, float y, float angleDeg, Enemy target) {
            this.x = x;
            this.y = y;
            this.angle = angleDeg;
            this.target = target;
            this.life = ROCKET_LIFE;
            float rad = (float) Math.toRadians(angleDeg);
            this.vx = (float) Math.cos(rad) * ROCKET_SPEED;
            this.vy = (float) Math.sin(rad) * ROCKET_SPEED;
        }
    }

    private static class Particle {
        float x, y, vx, vy, life, maxLife, size;
        float r, g, b;
        Particle(float x, float y, float vx, float vy, float life, float size,
                 float r, float g, float b) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.life = life; this.maxLife = life; this.size = size;
            this.r = r; this.g = g; this.b = b;
        }
    }

    @Override
    public void create() {
        // Perlengkapan layar error dibuat paling dulu & terpisah, supaya
        // error apa pun sesudah ini tetap bisa DITAMPILKAN di layar game
        // (bukan force close diam-diam). Bila bahkan ini gagal, penangkap
        // error di MainActivity yang mengambil alih.
        screenCam = new OrthographicCamera();
        uiBatch = new SpriteBatch();
        font = new BitmapFont();
        // Font bawaan libGDX bertekstur piksel; haluskan tampilannya agar
        // terlihat modern: filter Linear pada tekstur font + posisi gambar
        // tidak dibulatkan ke piksel utuh.
        font.getRegion().getTexture().setFilter(
                Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        font.setUseIntegerPositions(false);
        try {
            whiteTex = makeWhiteTexture();
        } catch (Throwable abaikan) {
            whiteTex = null;
        }
        try {
            createGame();
        } catch (Throwable t) {
            errorScreen.showError(t);
        }
    }

    private void createGame() {
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(VIEW_W, VIEW_H, camera);
        batch = new SpriteBatch();

        joyBaseTex = new Texture(Gdx.files.internal("ui/joy_base.png"));
        joyKnobTex = new Texture(Gdx.files.internal("ui/joy_knob.png"));
        fireBtnTex = makeFireButtonTexture();
        nosBtnTex = makeNosButtonTexture();
        rocketBtnTex = makeRocketButtonTexture();
        rocketTex = makeRocketTexture();
        rocketRegion = new TextureRegion(rocketTex);
        softDotTex = makeSoftDotTexture();

        prefs = Gdx.app.getPreferences("nx_space");
        hiScore = prefs.getInteger("hi", 0);
    }

    private Texture makeWhiteTexture() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1f, 1f, 1f, 1f);
        pm.fill();
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    /** Titik cahaya lembut (gradasi radial) untuk nebula, pijar peluru, partikel. */
    private Texture makeSoftDotTexture() {
        Pixmap pm = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                double d = Math.sqrt((x - 31.5) * (x - 31.5) + (y - 31.5) * (y - 31.5)) / 32.0;
                if (d < 1.0) {
                    float a = (float) Math.pow(1.0 - d, 2.0);
                    pm.setColor(1f, 1f, 1f, a);
                    pm.drawPixel(x, y);
                }
            }
        }
        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return t;
    }

    private Texture makeFireButtonTexture() {
        Pixmap pm = new Pixmap(96, 96, Pixmap.Format.RGBA8888);
        for (int y = 0; y < 96; y++) {
            for (int x = 0; x < 96; x++) {
                double d = Math.sqrt((x - 47.5) * (x - 47.5) + (y - 47.5) * (y - 47.5));
                if (d >= 41 && d <= 47) {
                    pm.setColor(0.55f, 0.90f, 1f, 0.85f);
                    pm.drawPixel(x, y);
                } else if (d < 41) {
                    pm.setColor(0.35f, 0.75f, 0.95f, 0.16f);
                    pm.drawPixel(x, y);
                }
                // Ikon peluru mendatar di tengah
                boolean boltBody = y >= 43 && y <= 52 && x >= 28 && x <= 58;
                boolean boltTip = x >= 59 && x <= 67 && Math.abs(y - 47.5) < (67 - x) * 0.62;
                if (boltBody || boltTip) {
                    pm.setColor(0.65f, 0.95f, 1f, 1f);
                    pm.drawPixel(x, y);
                }
            }
        }
        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return t;
    }

    private void layoutJoystick() {
        float density = Gdx.graphics.getDensity();
        joyBaseSize = 118f * density;
        joyKnobSize = 52f * density;
        joyTravel = joyBaseSize * 0.30f;
        float margin = 22f * density;
        joyCenterX = margin + joyBaseSize / 2f;
        joyCenterY = margin + joyBaseSize / 2f;
        knobX = joyCenterX;
        knobY = joyCenterY;
        fireBtnSize = 92f * density;
        fireCenterX = Gdx.graphics.getWidth() - margin - fireBtnSize / 2f;
        fireCenterY = margin + fireBtnSize / 2f;
        // Tombol NOS tepat di atas tombol tembak
        nosBtnSize = 62f * density;
        nosCenterX = fireCenterX;
        nosCenterY = fireCenterY + fireBtnSize / 2f + 12f * density + nosBtnSize / 2f;
        // Tombol roket di sebelah kiri tombol tembak
        rocketBtnSize = 62f * density;
        rocketCenterX = fireCenterX - fireBtnSize / 2f - 14f * density - rocketBtnSize / 2f;
        rocketCenterY = fireCenterY;
    }

    private void initStars(float viewW, float viewH) {
        stars.clear();
        int[] counts = {36, 26, 16};
        float[] speeds = {6f, 14f, 26f};
        float[] sizes = {1f, 1.6f, 2.4f};
        float[] brights = {0.35f, 0.6f, 1f};
        for (int layer = 0; layer < 3; layer++) {
            for (int i = 0; i < counts[layer]; i++) {
                stars.add(new float[]{
                        (float) Math.random() * viewW,
                        (float) Math.random() * viewH,
                        speeds[layer], sizes[layer], brights[layer]});
            }
        }
    }

    private void initNebula(float viewW, float viewH) {
        nebula.clear();
        float[][] tints = {
                {0.30f, 0.28f, 0.75f}, {0.16f, 0.55f, 0.65f},
                {0.55f, 0.25f, 0.70f}, {0.25f, 0.35f, 0.85f}};
        for (int i = 0; i < 4; i++) {
            float[] t = tints[i];
            nebula.add(new float[]{
                    (float) Math.random() * viewW,
                    (float) Math.random() * viewH,
                    2f + (float) Math.random() * 2.5f,
                    90f + (float) Math.random() * 80f,
                    t[0], t[1], t[2],
                    0.05f + (float) Math.random() * 0.04f});
        }
    }

    @Override
    public void resize(int width, int height) {
        if (viewport == null) {
            // createGame gagal sebelum viewport jadi: cukup siapkan kamera
            // layar error, sisanya ditangani penampil error.
            screenCam.setToOrtho(false, width, height);
            return;
        }
        viewport.update(width, height, true);
        screenCam.setToOrtho(false, width, height);
        layoutJoystick();
        initStars(viewport.getWorldWidth(), viewport.getWorldHeight());
        initNebula(viewport.getWorldWidth(), viewport.getWorldHeight());
        if (player == null) {
            player = new Player(WORLD_W * 0.42f, WORLD_H / 2f);
        }
    }

    /** Tekstur tombol NOS: cincin oranye + ikon petir (label teks digambar HUD). */
    private Texture makeNosButtonTexture() {
        Pixmap pm = new Pixmap(96, 96, Pixmap.Format.RGBA8888);
        for (int y = 0; y < 96; y++) {
            for (int x = 0; x < 96; x++) {
                double d = Math.sqrt((x - 47.5) * (x - 47.5) + (y - 47.5) * (y - 47.5));
                if (d >= 41 && d <= 47) {
                    pm.setColor(1f, 0.62f, 0.25f, 0.9f);
                    pm.drawPixel(x, y);
                } else if (d < 41) {
                    pm.setColor(1f, 0.55f, 0.20f, 0.16f);
                    pm.drawPixel(x, y);
                }
                // Ikon petir di tengah (poligon kasar per piksel)
                boolean bolt = (x >= 52 && x <= 62 && y >= 18 && y <= 46)
                        || (x >= 40 && x <= 56 && y >= 42 && y <= 52)
                        || (x >= 36 && x <= 46 && y >= 48 && y <= 76);
                if (bolt) {
                    pm.setColor(1f, 0.80f, 0.35f, 1f);
                    pm.drawPixel(x, y);
                }
            }
        }
        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return t;
    }

    /** Tekstur tombol roket: cincin merah + ikon roket tegak. */
    private Texture makeRocketButtonTexture() {
        Pixmap pm = new Pixmap(96, 96, Pixmap.Format.RGBA8888);
        for (int y = 0; y < 96; y++) {
            for (int x = 0; x < 96; x++) {
                double d = Math.sqrt((x - 47.5) * (x - 47.5) + (y - 47.5) * (y - 47.5));
                if (d >= 41 && d <= 47) {
                    pm.setColor(1f, 0.50f, 0.38f, 0.9f);
                    pm.drawPixel(x, y);
                } else if (d < 41) {
                    pm.setColor(1f, 0.42f, 0.30f, 0.16f);
                    pm.drawPixel(x, y);
                }
                // Ikon roket tegak: ujung di atas, api di bawah
                boolean ujung = y >= 22 && y <= 34
                        && Math.abs(x - 47.5) <= (y - 22) * 0.48;
                boolean badan = y >= 34 && y <= 60 && x >= 42 && x <= 53;
                boolean jendela = y >= 41 && y <= 47 && x >= 45 && x <= 50;
                boolean sirip = y >= 52 && y <= 64
                        && (Math.abs(x - 47.5) >= 6
                        && Math.abs(x - 47.5) <= 6 + (64 - y) * 0.75);
                boolean api = y >= 62 && y <= 72
                        && Math.abs(x - 47.5) <= (72 - y) * 0.42;
                if (ujung || badan || sirip) {
                    pm.setColor(1f, 0.74f, 0.58f, 1f);
                    pm.drawPixel(x, y);
                }
                if (jendela) {
                    pm.setColor(0.60f, 0.90f, 1f, 1f);
                    pm.drawPixel(x, y);
                }
                if (api) {
                    pm.setColor(1f, 0.55f, 0.20f, 1f);
                    pm.drawPixel(x, y);
                }
            }
        }
        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return t;
    }

    /** Sprite roket terbang (hidung ke kanan), digambar vektor lewat SpriteArt. */
    private Texture makeRocketTexture() {
        Pixmap pm = new Pixmap(60, 24, Pixmap.Format.RGBA8888);
        // Sirip ekor (merah-oranye)
        SpriteArt.fillPoly(pm, new float[]{6, 7, 17, 7, 10, 1.5f}, SpriteArt.col(220, 90, 40, 1f));
        SpriteArt.fillPoly(pm, new float[]{6, 17, 17, 17, 10, 22.5f}, SpriteArt.col(220, 90, 40, 1f));
        // Badan utama (abu terang)
        SpriteArt.fillPoly(pm, new float[]{10, 7, 30, 5, 52, 8, 59, 12, 52, 16, 30, 19, 10, 17},
                SpriteArt.col(205, 214, 228, 1f));
        // Pita ekor gelap
        SpriteArt.fillPoly(pm, new float[]{10, 7, 15, 6.4f, 15, 17.6f, 10, 17},
                SpriteArt.col(90, 100, 115, 1f));
        // Kerucut hidung (merah)
        SpriteArt.fillPoly(pm, new float[]{43, 7.6f, 59, 12, 43, 16.4f},
                SpriteArt.col(232, 72, 50, 1f));
        // Jendela (biru muda)
        SpriteArt.fillEllipse(pm, 35, 12, 3.4f, 2.6f, SpriteArt.col(120, 220, 255, 1f));
        Texture t = SpriteArt.toTexture(pm);
        pm.dispose();
        return t;
    }

    private void readInput() {
        moveX = 0f;
        moveY = 0f;
        fireHeld = Gdx.input.isKeyPressed(Input.Keys.SPACE);
        nosHeld = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
        rocketHeld = Gdx.input.isKeyPressed(Input.Keys.E);

        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) moveX -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) moveX += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) moveY += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) moveY -= 1f;

        int screenH = Gdx.graphics.getHeight();
        for (int i = 0; i < 5; i++) {
            if (!Gdx.input.isTouched(i)) {
                if (joyPointer == i) { joyPointer = -1; joyActive = false; }
                if (firePointer == i) { firePointer = -1; }
                if (nosPointer == i) { nosPointer = -1; }
                if (rocketPointer == i) { rocketPointer = -1; }
                continue;
            }
            float sx = Gdx.input.getX(i);
            float sy = screenH - Gdx.input.getY(i);
            // Sentuhan yang belum punya peran ditentukan dari posisinya:
            // dekat tombol tembak -> penembak, separuh kiri layar -> joystick.
            // Syaratnya hanya "slotku kosong & sentuhan ini bukan milik peran
            // lain" — JANGAN mensyaratkan slot lain kosong juga, karena pemain
            // hampir selalu menahan joystick sambil menembak; syarat lama
            // membuat tombol tembak tidak merespons selama joystick ditahan.
            if (firePointer == -1 && joyPointer != i && nosPointer != i && rocketPointer != i) {
                float fdx = sx - fireCenterX;
                float fdy = sy - fireCenterY;
                if (Math.sqrt(fdx * fdx + fdy * fdy) <= fireBtnSize * 0.85f) {
                    firePointer = i;
                }
            }
            if (nosPointer == -1 && joyPointer != i && firePointer != i && rocketPointer != i) {
                float ndx = sx - nosCenterX;
                float ndy = sy - nosCenterY;
                if (Math.sqrt(ndx * ndx + ndy * ndy) <= nosBtnSize * 0.85f) {
                    nosPointer = i;
                }
            }
            if (rocketPointer == -1 && joyPointer != i && firePointer != i && nosPointer != i) {
                float rdx = sx - rocketCenterX;
                float rdy = sy - rocketCenterY;
                if (Math.sqrt(rdx * rdx + rdy * rdy) <= rocketBtnSize * 0.85f) {
                    rocketPointer = i;
                }
            }
            if (joyPointer == -1 && firePointer != i && nosPointer != i && rocketPointer != i
                    && sx < Gdx.graphics.getWidth() * 0.55f) {
                joyPointer = i;
                joyActive = true;
            }
            if (firePointer == i) fireHeld = true;
            if (nosPointer == i) nosHeld = true;
            if (rocketPointer == i) rocketHeld = true;
            if (joyPointer == i) {
                float dx = sx - joyCenterX;
                float dy = sy - joyCenterY;
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                if (len > joyTravel) {
                    dx = dx / len * joyTravel;
                    dy = dy / len * joyTravel;
                }
                knobX = joyCenterX + dx;
                knobY = joyCenterY + dy;
                if (len > 4f) {
                    moveX = dx / joyTravel;
                    moveY = dy / joyTravel;
                }
            }
        }
        if (!joyActive) {
            knobX = joyCenterX;
            knobY = joyCenterY;
        }
    }

    /** Musuh muncul tepat di luar pandangan kamera (tepi acak), di dalam dunia. */
    private void spawnEnemy(float viewW, float viewH) {
        float left = camX - viewW / 2f;
        float bottom = camY - viewH / 2f;
        float x;
        float y;
        double sisi = Math.random();
        if (sisi < 0.40) {          // kanan (paling sering)
            x = left + viewW + 26f;
            y = bottom + (float) Math.random() * viewH;
        } else if (sisi < 0.60) {   // atas
            x = left + (float) Math.random() * viewW;
            y = bottom + viewH + 26f;
        } else if (sisi < 0.80) {   // bawah
            x = left + (float) Math.random() * viewW;
            y = bottom - 26f;
        } else {                    // kiri
            x = left - 26f;
            y = bottom + (float) Math.random() * viewH;
        }
        // Jepit ke dalam dunia (dekat perbatasan dunia titik bisa terlihat)
        if (x < 16f) x = 16f;
        if (x > WORLD_W - 16f) x = WORLD_W - 16f;
        if (y < 16f) y = 16f;
        if (y > WORLD_H - 16f) y = WORLD_H - 16f;
        int type = (Math.random() < PELUANG_KUNING)
                ? Enemy.GUNNER : Enemy.DRONE;
        enemies.add(new Enemy(type, x, y));
    }

    /**
     * Otak bot musuh: pilih lawan terdekat (pemain diberi sedikit
     * prioritas lewat bias jarak 0.8, tapi sesama alien yang lebih
     * dekat tetap diserang), lalu kemudikan pesawatnya menjaga jarak
     * tempur sambil menyamping (strafe) dan menghindari kerumunan.
     */
    private void updateEnemyAi(Enemy e, float delta, float viewW, float viewH) {
        // Sasaran lama gugur? Lupakan.
        if (e.hasTarget) {
            if (e.targetIsPlayer && player.dead) e.hasTarget = false;
            if (!e.targetIsPlayer && (e.targetEnemy == null
                    || e.targetEnemy.hp <= 0 || !enemies.contains(e.targetEnemy))) {
                e.hasTarget = false;
            }
        }
        e.retargetTimer -= delta;
        if (e.retargetTimer <= 0f || !e.hasTarget) {
            e.retargetTimer = 1.2f + (float) Math.random() * 1.0f;
            float best = Float.MAX_VALUE;
            e.hasTarget = false;
            if (!player.dead) {
                float dx = player.x - e.x;
                float dy = player.y - e.y;
                float d = (float) Math.sqrt(dx * dx + dy * dy) * 0.8f;
                if (d < best) {
                    best = d;
                    e.targetIsPlayer = true;
                    e.targetEnemy = null;
                    e.hasTarget = true;
                }
            }
            for (Enemy o : enemies) {
                if (o == e || o.hp <= 0) continue;
                float dx = o.x - e.x;
                float dy = o.y - e.y;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d < best) {
                    best = d;
                    e.targetIsPlayer = false;
                    e.targetEnemy = o;
                    e.hasTarget = true;
                }
            }
        }

        float desiredX;
        float desiredY;
        if (e.hasTarget) {
            float tx = e.targetIsPlayer ? player.x : e.targetEnemy.x;
            float ty = e.targetIsPlayer ? player.y : e.targetEnemy.y;
            e.targetX = tx;
            e.targetY = ty;
            float dx = tx - e.x;
            float dy = ty - e.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            e.targetDist = dist;
            float ux = dist > 1f ? dx / dist : 1f;
            float uy = dist > 1f ? dy / dist : 0f;
            // Radial: mendekat bila terlalu jauh, mundur bila terlalu dekat
            float radial = 0f;
            if (dist > e.prefRange + 18f) radial = 1f;
            else if (dist < e.prefRange - 18f) radial = -0.8f;
            // Strafe: bergerak tegak lurus sasaran, arah berganti-ganti
            e.strafeTimer -= delta;
            if (e.strafeTimer <= 0f) {
                e.strafeTimer = 1.4f + (float) Math.random() * 1.6f;
                if (Math.random() < 0.45) e.strafeDir = -e.strafeDir;
            }
            float px = -uy * e.strafeDir;
            float py = ux * e.strafeDir;
            desiredX = (ux * radial + px * 0.7f) * e.maxSpeed - 10f;
            desiredY = (uy * radial + py * 0.7f) * e.maxSpeed;
        } else {
            // Tanpa sasaran: jelajah ke kiri dengan gelombang ringan
            e.targetDist = Float.MAX_VALUE;
            desiredX = -e.cruise;
            desiredY = (float) Math.sin(e.time * 2.2f + e.phase) * 14f;
        }
        // Separasi: jangan menumpuk dengan bot lain
        for (Enemy o : enemies) {
            if (o == e) continue;
            float dx = e.x - o.x;
            float dy = e.y - o.y;
            float d2 = dx * dx + dy * dy;
            if (d2 < 28f * 28f && d2 > 0.01f) {
                float d = (float) Math.sqrt(d2);
                desiredX += dx / d * (28f - d) * 2.2f;
                desiredY += dy / d * (28f - d) * 2.2f;
            }
        }
        // Jauhi tepi atas/bawah layar
        if (e.y < 26f) desiredY += (26f - e.y) * 3f;
        if (e.y > viewH - 26f) desiredY -= (e.y - (viewH - 26f)) * 3f;
        // Batasi kecepatan maksimum, lalu ratakan kemudi
        float sp = (float) Math.sqrt(desiredX * desiredX + desiredY * desiredY);
        if (sp > e.maxSpeed) {
            desiredX = desiredX / sp * e.maxSpeed;
            desiredY = desiredY / sp * e.maxSpeed;
        }
        float k = Math.min(1f, 5f * delta);
        e.vx += (desiredX - e.vx) * k;
        e.vy += (desiredY - e.vy) * k;
    }

    /** Buat peluru bot yang diarahkan ke sasarannya saat ini. */
    /**
     * Meluncurkan roket skill: auto-aim mengunci musuh terdekat dari
     * pemain sebagai sasaran awal (roket memburunya selama terbang);
     * bila tidak ada musuh, roket meluncur ke arah hadap pesawat dan
     * bisa mengunci musuh yang masuk jangkauannya di tengah jalan.
     */
    private void luncurkanRoket() {
        Enemy sasaran = null;
        float terbaik = Float.MAX_VALUE;
        for (Enemy e : enemies) {
            if (e.hp <= 0) continue;
            float dx = e.x - player.x;
            float dy = e.y - player.y;
            float d = dx * dx + dy * dy;
            if (d < terbaik) {
                terbaik = d;
                sasaran = e;
            }
        }
        float arah = player.facing;
        if (sasaran != null) {
            arah = (float) Math.toDegrees(
                    Math.atan2(sasaran.y - player.y, sasaran.x - player.x));
        }
        rockets.add(new Rocket(player.gunX(), player.gunY(), arah, sasaran));
    }

    /** Ledakan roket: sasaran utama kena ROCKET_DAMAGE, musuh lain dalam radius percikan kena lebih ringan. */
    private void ledakanRoket(float x, float y, Enemy utama) {
        explosion(x, y, true);
        if (utama.damage(ROCKET_DAMAGE)) utama.killedByPlayer = true;
        for (Enemy e : enemies) {
            if (e == utama || e.hp <= 0) continue;
            float dx = e.x - x;
            float dy = e.y - y;
            if (dx * dx + dy * dy <= ROCKET_SPLASH_RADIUS * ROCKET_SPLASH_RADIUS) {
                if (e.damage(ROCKET_SPLASH_DAMAGE)) e.killedByPlayer = true;
            }
        }
    }

    private void spawnEnemyBullet(Enemy e) {
        float dx = e.targetX - e.x;
        float dy = e.targetY - e.y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) return;
        float spread = e.type == Enemy.DRONE ? 0.09f : 0.02f;
        float ang = (float) Math.atan2(dy, dx)
                + (float) (Math.random() * 2.0 - 1.0) * spread;
        float spd = e.type == Enemy.DRONE ? 122f : 105f;
        float ca = (float) Math.cos(ang);
        float sa = (float) Math.sin(ang);
        bullets.add(new Bullet(e.x + ca * (e.radius + 2f), e.y + sa * (e.radius + 2f),
                ca * spd, sa * spd, false, e));
    }

    private void explosion(float x, float y, boolean big) {
        int count = big ? 26 : 14;
        float[][] colors = {
                {1f, 0.62f, 0.16f}, {1f, 0.86f, 0.35f},
                {1f, 1f, 1f}, {0.92f, 0.35f, 0.25f}};
        for (int i = 0; i < count; i++) {
            double ang = Math.random() * Math.PI * 2.0;
            float spd = (big ? 30f : 20f) + (float) Math.random() * (big ? 100f : 70f);
            float[] c = colors[(int) (Math.random() * colors.length)];
            particles.add(new Particle(x, y,
                    (float) Math.cos(ang) * spd, (float) Math.sin(ang) * spd,
                    0.35f + (float) Math.random() * 0.45f,
                    1.8f + (float) Math.random() * 1.8f, c[0], c[1], c[2]));
        }
    }

    private void resetRun(float viewW, float viewH) {
        enemies.clear();
        bullets.clear();
        score = 0;
        elapsed = 0f;
        spawnTimer = 0.8f;
        fireTimer = 0f;
        nosTime = 0f;
        nosCooldown = 0f;
        rockets.clear();
        rocketCooldown = 0f;
        player.respawn(WORLD_W * 0.42f, WORLD_H / 2f);
        camInit = false; // kamera langsung melompat ke pemain
        state = STATE_PLAY;
    }

    private void saveHiScore() {
        if (score > hiScore) {
            hiScore = score;
            prefs.putInteger("hi", hiScore);
            prefs.flush();
        }
    }

    @Override
    public void render() {
        if (errorScreen.isActive()) {
            errorScreen.draw(uiBatch, font, screenCam, whiteTex);
            return;
        }
        try {
            renderGame();
        } catch (Throwable t) {
            errorScreen.showError(t);
            errorScreen.draw(uiBatch, font, screenCam, whiteTex);
        }
    }

    private void renderGame() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);
        float viewW = viewport.getWorldWidth();
        float viewH = viewport.getWorldHeight();

        readInput();

        // Bintang selalu bergerak
        for (float[] s : stars) {
            s[0] -= s[2] * delta;
            if (s[0] < -3f) {
                s[0] = viewW + 3f;
                s[1] = (float) Math.random() * viewH;
            }
        }
        // Nebula melayang lebih lambat di belakang bintang
        for (float[] nb : nebula) {
            nb[0] -= nb[2] * delta;
            if (nb[0] < -nb[3]) {
                nb[0] = viewW + nb[3];
                nb[1] = (float) Math.random() * viewH;
            }
        }

        if (hitFlash > 0f) hitFlash -= delta;

        if (state == STATE_PLAY) {
            elapsed += delta;

            // NOS: tekan tombol/SHIFT saat jeda habis -> ledakan kecepatan
            // 1,6 detik (laju 2,1x), lalu jeda isi ulang 4 detik.
            if (nosCooldown > 0f) nosCooldown -= delta;
            if (nosTime > 0f) {
                nosTime -= delta;
                if (nosTime <= 0f) nosCooldown = NOS_COOLDOWN;
            } else if (nosHeld && nosCooldown <= 0f && !player.dead) {
                nosTime = NOS_DURATION;
            }
            player.speedScale = nosTime > 0f ? NOS_MULT : 1f;
            player.update(delta, moveX, moveY, WORLD_W, WORLD_H);
            // Jejak asap cahaya di ekor pesawat selama NOS menyala
            if (nosTime > 0f && !player.dead) {
                float rad = (float) Math.toRadians(player.facing);
                float tx = player.x - (float) Math.cos(rad) * 15f;
                float ty = player.y - (float) Math.sin(rad) * 15f;
                for (int i = 0; i < 2; i++) {
                    particles.add(new Particle(
                            tx + (float) (Math.random() * 4f - 2f),
                            ty + (float) (Math.random() * 4f - 2f),
                            -(float) Math.cos(rad) * 22f + (float) (Math.random() * 8f - 4f),
                            -(float) Math.sin(rad) * 22f + (float) (Math.random() * 8f - 4f),
                            0.32f, 2.2f, 0.55f, 0.85f, 1f));
                }
            }

            // Menembak (tahan tombol / spasi)
            fireTimer -= delta;
            if (fireHeld && fireTimer <= 0f && !player.dead) {
                float rad = (float) Math.toRadians(player.facing);
                bullets.add(new Bullet(player.gunX(), player.gunY(),
                        (float) Math.cos(rad) * PLAYER_BULLET_SPEED,
                        (float) Math.sin(rad) * PLAYER_BULLET_SPEED, true, null));
                fireTimer = FIRE_INTERVAL;
            }

            // Skill roket (tombol roket / E): luncurkan roket pemburu,
            // lalu jeda isi ulang sebelum bisa menembak lagi.
            if (rocketCooldown > 0f) rocketCooldown -= delta;
            if (rocketHeld && rocketCooldown <= 0f && !player.dead) {
                luncurkanRoket();
                rocketCooldown = ROCKET_COOLDOWN;
            }

            // Gelombang musuh (maksimal 7 bot hidup bersamaan)
            spawnTimer -= delta;
            if (spawnTimer <= 0f) {
                if (enemies.size() < MAX_BOT_HIDUP) {
                    spawnEnemy(viewW, viewH);
                    float interval = Math.max(0.42f, 1.05f - elapsed * 0.008f);
                    spawnTimer = interval * (0.7f + (float) Math.random() * 0.6f);
                } else {
                    spawnTimer = 0.35f;
                }
            }

            // Musuh: AI memilih sasaran & mengemudi, lalu bergerak & menembak
            for (Enemy e : enemies) {
                updateEnemyAi(e, delta, WORLD_W, WORLD_H);
                e.update(delta, WORLD_W, WORLD_H);
                boolean engaged = e.hasTarget && e.targetDist <= 205f;
                if (e.updateFire(delta, engaged)) {
                    spawnEnemyBullet(e);
                }
            }

            // Peluru bergerak (dibuang bila keluar dunia)
            for (Bullet b : bullets) {
                b.x += b.vx * delta;
                b.y += b.vy * delta;
                if (b.x < -16f || b.x > WORLD_W + 16f || b.y < -16f || b.y > WORLD_H + 16f) {
                    b.dead = true;
                }
            }

            // Tabrakan: peluru pemain vs musuh
            for (Bullet b : bullets) {
                if (!b.friendly || b.dead) continue;
                for (Enemy e : enemies) {
                    if (e.hp <= 0) continue;
                    float dx = b.x - e.x;
                    float dy = b.y - e.y;
                    float rr = e.radius + 3f;
                    if (dx * dx + dy * dy < rr * rr) {
                        b.dead = true;
                        if (e.damage(BULLET_DAMAGE)) e.killedByPlayer = true;
                        break;
                    }
                }
            }
            // Roket pemburu: kunci ulang bila sasaran gugur, belok mengejar
            // sasarannya, tinggalkan jejak asap, dan meledak saat mengenai
            // musuh (ledakan besar + percikan ke musuh di sekitarnya).
            for (Rocket r : rockets) {
                if (r.target != null && (r.target.hp <= 0 || !enemies.contains(r.target))) {
                    r.target = null;
                }
                if (r.target == null) {
                    float terbaik = ROCKET_LOCK_RANGE * ROCKET_LOCK_RANGE;
                    for (Enemy e : enemies) {
                        if (e.hp <= 0) continue;
                        float dx = e.x - r.x;
                        float dy = e.y - r.y;
                        float d = dx * dx + dy * dy;
                        if (d < terbaik) {
                            terbaik = d;
                            r.target = e;
                        }
                    }
                }
                if (r.target != null) {
                    float ingin = (float) Math.toDegrees(
                            Math.atan2(r.target.y - r.y, r.target.x - r.x));
                    r.angle = SpriteArt.turnToward(r.angle, ingin, ROCKET_TURN * delta);
                }
                float rad = (float) Math.toRadians(r.angle);
                r.vx = (float) Math.cos(rad) * ROCKET_SPEED;
                r.vy = (float) Math.sin(rad) * ROCKET_SPEED;
                r.x += r.vx * delta;
                r.y += r.vy * delta;
                r.life -= delta;
                particles.add(new Particle(
                        r.x - (float) Math.cos(rad) * 13f,
                        r.y - (float) Math.sin(rad) * 13f,
                        -(float) Math.cos(rad) * 14f + (float) (Math.random() * 6f - 3f),
                        -(float) Math.sin(rad) * 14f + (float) (Math.random() * 6f - 3f),
                        0.35f, 2.4f, 0.78f, 0.80f, 0.86f));
                if (r.life <= 0f || r.x < -12f || r.x > WORLD_W + 12f
                        || r.y < -12f || r.y > WORLD_H + 12f) {
                    explosion(r.x, r.y, false);
                    r.dead = true;
                    continue;
                }
                for (Enemy e : enemies) {
                    if (e.hp <= 0) continue;
                    float dx = e.x - r.x;
                    float dy = e.y - r.y;
                    float rr = e.radius + 5f;
                    if (dx * dx + dy * dy < rr * rr) {
                        ledakanRoket(r.x, r.y, e);
                        r.dead = true;
                        break;
                    }
                }
            }
            Iterator<Rocket> rit = rockets.iterator();
            while (rit.hasNext()) {
                if (rit.next().dead) rit.remove();
            }

            // Musuh yang hancur -> ledakan; skor hanya bila pemain yang membunuh
            Iterator<Enemy> eit = enemies.iterator();
            while (eit.hasNext()) {
                Enemy e = eit.next();
                if (e.hp <= 0) {
                    explosion(e.x, e.y, e.type == Enemy.GUNNER);
                    if (e.killedByPlayer) score += e.scoreValue;
                    eit.remove();
                    continue;
                }
                if (e.isGone(player.x, player.y, WORLD_W, WORLD_H)) {
                    eit.remove();
                    continue;
                }
                // Tabrakan badan musuh vs pesawat: kedua pihak kena
                // RAM_DAMAGE. Gunner yang tebal bisa selamat dari tabrakan.
                if (!player.dead && !player.isInvulnerable()) {
                    float dx = e.x - player.x;
                    float dy = e.y - player.y;
                    float rr = e.radius + Player.RADIUS;
                    if (dx * dx + dy * dy < rr * rr) {
                        if (e.damage(RAM_DAMAGE)) {
                            e.killedByPlayer = true;
                            explosion(e.x, e.y, e.type == Enemy.GUNNER);
                            score += e.scoreValue;
                            eit.remove();
                        }
                        if (player.damage(RAM_DAMAGE)) onPlayerHit(viewW, viewH);
                    }
                }
            }
            // Peluru musuh vs pesawat DAN vs musuh lain (sesama alien bisa kena,
            // kecuali penembaknya sendiri)
            for (Bullet b : bullets) {
                if (b.friendly || b.dead) continue;
                if (!player.dead && !player.isInvulnerable()) {
                    float dx = b.x - player.x;
                    float dy = b.y - player.y;
                    float rr = Player.RADIUS + 3f;
                    if (dx * dx + dy * dy < rr * rr) {
                        b.dead = true;
                        if (player.damage(BULLET_DAMAGE)) onPlayerHit(viewW, viewH);
                        continue;
                    }
                }
                for (Enemy e : enemies) {
                    if (e == b.owner || e.hp <= 0) continue;
                    float dx = b.x - e.x;
                    float dy = b.y - e.y;
                    float rr = e.radius + 3f;
                    if (dx * dx + dy * dy < rr * rr) {
                        b.dead = true;
                        e.damage(BULLET_DAMAGE); // hancurnya disapu pada iterasi musuh berikutnya
                        break;
                    }
                }
            }
            Iterator<Bullet> bit = bullets.iterator();
            while (bit.hasNext()) {
                if (bit.next().dead) bit.remove();
            }
        } else {
            // GAME OVER: musuh melayang keluar layar, lalu mulai lagi sendiri
            for (Enemy e : enemies) {
                e.vx = -e.cruise;
                e.vy = 0f;
                e.update(delta, WORLD_W, WORLD_H);
            }
            overTimer -= delta;
            if (overTimer <= 0f) resetRun(viewW, viewH);
        }

        // Kamera mengikuti pemain (dijepit ke batas dunia) + paralaks latar:
        // bintang & nebula digeser berlawanan arah gerak kamera sesuai
        // kedalaman lapisannya, jadi latar terasa jauh di belakang dunia.
        if (player != null && state == STATE_PLAY) {
            float targetX = Math.max(viewW / 2f, Math.min(WORLD_W - viewW / 2f, player.x));
            float targetY = Math.max(viewH / 2f, Math.min(WORLD_H - viewH / 2f, player.y));
            if (!camInit) {
                camX = targetX;
                camY = targetY;
                prevCamX = camX;
                prevCamY = camY;
                camInit = true;
            } else {
                float ikuti = Math.min(1f, 7f * delta);
                camX += (targetX - camX) * ikuti;
                camY += (targetY - camY) * ikuti;
            }
            float geserX = camX - prevCamX;
            float geserY = camY - prevCamY;
            prevCamX = camX;
            prevCamY = camY;
            if (geserX != 0f || geserY != 0f) {
                for (float[] s : stars) {
                    float faktor = s[2] / 26f * 0.75f;
                    s[0] -= geserX * faktor;
                    s[1] -= geserY * faktor;
                    if (s[0] < -3f) s[0] += viewW + 6f;
                    if (s[0] > viewW + 3f) s[0] -= viewW + 6f;
                    if (s[1] < -3f) s[1] += viewH + 6f;
                    if (s[1] > viewH + 3f) s[1] -= viewH + 6f;
                }
                for (float[] nb : nebula) {
                    nb[0] -= geserX * 0.12f;
                    nb[1] -= geserY * 0.12f;
                    if (nb[0] < -nb[3]) nb[0] += viewW + nb[3] * 2f;
                    if (nb[0] > viewW + nb[3]) nb[0] -= viewW + nb[3] * 2f;
                    if (nb[1] < -nb[3]) nb[1] += viewH + nb[3] * 2f;
                    if (nb[1] > viewH + nb[3]) nb[1] -= viewH + nb[3] * 2f;
                }
            }
            camera.position.set(camX, camY, 0f);
        }

        // Partikel ledakan
        Iterator<Particle> pit = particles.iterator();
        while (pit.hasNext()) {
            Particle p = pit.next();
            p.life -= delta;
            if (p.life <= 0f) {
                pit.remove();
                continue;
            }
            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.vx *= (1f - 1.6f * delta);
            p.vy *= (1f - 1.6f * delta);
        }

        draw(viewW, viewH);
    }

    private void onPlayerHit(float viewW, float viewH) {
        hitFlash = 0.3f;
        explosion(player.x, player.y, false);
        if (player.dead) {
            explosion(player.x, player.y, true);
            saveHiScore();
            state = STATE_OVER;
            overTimer = 1.8f;
        }
    }

    private void draw(float viewW, float viewH) {
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.06f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // --- Dunia ---
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        // Latar digambar relatif terhadap sudut pandangan kamera (paralaks
        // sudah diterapkan pada posisi simpannya di renderGame).
        float camKiri = camera.position.x - viewW / 2f;
        float camBawah = camera.position.y - viewH / 2f;
        for (float[] nb : nebula) {
            batch.setColor(nb[4], nb[5], nb[6], nb[7]);
            batch.draw(softDotTex, camKiri + nb[0] - nb[3], camBawah + nb[1] - nb[3],
                    nb[3] * 2f, nb[3] * 2f);
        }
        for (float[] s : stars) {
            batch.setColor(1f, 1f, 1f, s[4]);
            batch.draw(whiteTex, camKiri + s[0], camBawah + s[1], s[3], s[3]);
        }
        // Kisi peta samar + garis perbatasan dunia yang menyala, supaya
        // pemain melihat luas & batas peta (tidak terpotong tanpa petunjuk).
        batch.setColor(0.55f, 0.85f, 1f, 0.05f);
        for (float gx = 80f; gx < WORLD_W; gx += 80f) {
            batch.draw(whiteTex, gx, 0f, 0.6f, WORLD_H);
        }
        for (float gy = 80f; gy < WORLD_H; gy += 80f) {
            batch.draw(whiteTex, 0f, gy, WORLD_W, 0.6f);
        }
        batch.setColor(0.45f, 0.85f, 1f, 0.55f);
        batch.draw(whiteTex, 0f, 0f, WORLD_W, 1.4f);
        batch.draw(whiteTex, 0f, WORLD_H - 1.4f, WORLD_W, 1.4f);
        batch.draw(whiteTex, 0f, 0f, 1.4f, WORLD_H);
        batch.draw(whiteTex, WORLD_W - 1.4f, 0f, 1.4f, WORLD_H);
        batch.setColor(1f, 1f, 1f, 1f);
        for (Bullet b : bullets) {
            if (b.friendly) {
                batch.setColor(0.45f, 0.85f, 1f, 0.50f);
                batch.draw(softDotTex, b.x - 8f, b.y - 8f, 16f, 16f);
                batch.setColor(0.85f, 0.98f, 1f, 1f);
                batch.draw(softDotTex, b.x - 4f, b.y - 4f, 8f, 8f);
            } else {
                batch.setColor(1f, 0.40f, 0.85f, 0.55f);
                batch.draw(softDotTex, b.x - 7.5f, b.y - 7.5f, 15f, 15f);
                batch.setColor(1f, 0.65f, 0.95f, 1f);
                batch.draw(whiteTex, b.x - 2f, b.y - 2f, 4f, 4f);
            }
        }
        batch.setColor(1f, 1f, 1f, 1f);
        for (Rocket r : rockets) {
            batch.draw(rocketRegion, r.x - 15f, r.y - 6f, 15f, 6f,
                    30f, 12f, 1f, 1f, r.angle);
        }
        for (Enemy e : enemies) e.draw(batch, font, layout, WORLD_H);
        player.draw(batch);
        for (Particle p : particles) {
            float a = Math.max(0f, p.life / p.maxLife);
            float s = p.size * 2.6f;
            batch.setColor(p.r, p.g, p.b, a * 0.9f);
            batch.draw(softDotTex, p.x - s / 2f, p.y - s / 2f, s, s);
        }
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();

        // --- UI layar ---
        uiBatch.setProjectionMatrix(screenCam.combined);
        uiBatch.begin();
        float density = Gdx.graphics.getDensity();
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        // Bar darah pemain + angka HP di kiri atas (menggantikan ikon hati)
        float hpBarW = 124f * density;
        float hpBarH = 14f * density;
        float hpBarX = 14f * density;
        float hpBarY = screenH - 14f * density - hpBarH;
        float fracHp = Math.max(0f, (float) player.hp / (float) Player.MAX_HP);
        uiBatch.setColor(0f, 0f, 0f, 0.55f);
        uiBatch.draw(whiteTex, hpBarX - 2f, hpBarY - 2f, hpBarW + 4f, hpBarH + 4f);
        uiBatch.setColor(0.10f, 0.13f, 0.20f, 0.9f);
        uiBatch.draw(whiteTex, hpBarX, hpBarY, hpBarW, hpBarH);
        if (fracHp > 0.5f) uiBatch.setColor(0.35f, 0.90f, 0.55f, 0.95f);
        else if (fracHp > 0.25f) uiBatch.setColor(0.95f, 0.80f, 0.30f, 0.95f);
        else uiBatch.setColor(0.95f, 0.35f, 0.30f, 0.95f);
        uiBatch.draw(whiteTex, hpBarX, hpBarY, hpBarW * fracHp, hpBarH);
        uiBatch.setColor(1f, 1f, 1f, 1f);
        font.getData().setScale(0.95f * density);
        font.setColor(1f, 1f, 1f, 1f);
        String hpText = "HP " + player.hp + "/" + Player.MAX_HP;
        layout.setText(font, hpText);
        font.draw(uiBatch, hpText, hpBarX + 6f * density,
                hpBarY + hpBarH / 2f + layout.height / 2f);

        // Total semua yang masih hidup (pemain + seluruh bot musuh)
        font.getData().setScale(1.1f * density);
        font.setColor(0.85f, 0.95f, 1f, 1f);
        int totalHidup = enemies.size() + (player.dead ? 0 : 1);
        String hidupText = "TOTAL HIDUP: " + totalHidup;
        font.draw(uiBatch, hidupText, hpBarX,
                hpBarY - 10f * density);

        // Skor di kanan atas
        font.getData().setScale(1.7f * density);
        font.setColor(1f, 1f, 1f, 1f);
        String scoreText = "SCORE " + String.format(Locale.US, "%06d", score);
        layout.setText(font, scoreText);
        font.draw(uiBatch, scoreText, screenW - 14f * density - layout.width,
                screenH - 26f * density);
        font.getData().setScale(1.1f * density);
        font.setColor(0.75f, 0.85f, 1f, 1f);
        String hiText = "HI " + String.format(Locale.US, "%06d", hiScore);
        layout.setText(font, hiText);
        font.draw(uiBatch, hiText, screenW - 14f * density - layout.width,
                screenH - 54f * density);

        // Kilat merah saat kena serangan
        if (hitFlash > 0f) {
            uiBatch.setColor(0.9f, 0.15f, 0.15f, hitFlash * 0.45f);
            uiBatch.draw(whiteTex, 0f, 0f, screenW, screenH);
            uiBatch.setColor(1f, 1f, 1f, 1f);
        }

        // Joystick + tombol tembak
        uiBatch.setColor(1f, 1f, 1f, joyActive ? 0.95f : 0.75f);
        uiBatch.draw(joyBaseTex, joyCenterX - joyBaseSize / 2f, joyCenterY - joyBaseSize / 2f,
                joyBaseSize, joyBaseSize);
        uiBatch.draw(joyKnobTex, knobX - joyKnobSize / 2f, knobY - joyKnobSize / 2f,
                joyKnobSize, joyKnobSize);
        uiBatch.setColor(1f, 1f, 1f, fireHeld ? 1f : 0.8f);
        uiBatch.draw(fireBtnTex, fireCenterX - fireBtnSize / 2f, fireCenterY - fireBtnSize / 2f,
                fireBtnSize, fireBtnSize);
        uiBatch.setColor(1f, 1f, 1f, 1f);

        // Tombol NOS: menyala terang saat aktif; selama jeda isi ulang
        // tertutup sapuan gelap dari atas + angka detik yang tersisa.
        if (nosTime > 0f) uiBatch.setColor(1f, 0.72f, 0.38f, 1f);
        else uiBatch.setColor(1f, 1f, 1f, nosCooldown > 0f ? 0.6f : 0.9f);
        uiBatch.draw(nosBtnTex, nosCenterX - nosBtnSize / 2f, nosCenterY - nosBtnSize / 2f,
                nosBtnSize, nosBtnSize);
        uiBatch.setColor(1f, 1f, 1f, 1f);
        if (nosCooldown > 0f) {
            float sisa = nosCooldown / NOS_COOLDOWN;
            float tinggiSapuan = nosBtnSize * sisa;
            uiBatch.setColor(0f, 0f, 0f, 0.55f);
            uiBatch.draw(nosBtnTex, nosCenterX - nosBtnSize / 2f,
                    nosCenterY + nosBtnSize / 2f - tinggiSapuan, nosBtnSize, tinggiSapuan,
                    0, 0, 96, Math.max(1, (int) (96f * sisa)), false, false);
            uiBatch.setColor(1f, 1f, 1f, 1f);
            font.getData().setScale(1.05f * density);
            font.setColor(1f, 0.85f, 0.6f, 1f);
            String detik = String.format(Locale.US, "%.1f", nosCooldown);
            layout.setText(font, detik);
            font.draw(uiBatch, detik, nosCenterX - layout.width / 2f,
                    nosCenterY + layout.height / 2f);
        } else {
            font.getData().setScale(0.85f * density);
            font.setColor(1f, 0.82f, 0.55f, 1f);
            layout.setText(font, "NOS");
            font.draw(uiBatch, "NOS", nosCenterX - layout.width / 2f,
                    nosCenterY - nosBtnSize / 2f + 13f * density);
        }

        // Tombol roket di kiri tombol tembak: redup + sapuan gelap +
        // angka detik selama jeda isi ulang, label RKT bila siap.
        uiBatch.setColor(1f, 1f, 1f, rocketCooldown > 0f ? 0.6f : 0.9f);
        uiBatch.draw(rocketBtnTex, rocketCenterX - rocketBtnSize / 2f,
                rocketCenterY - rocketBtnSize / 2f, rocketBtnSize, rocketBtnSize);
        uiBatch.setColor(1f, 1f, 1f, 1f);
        if (rocketCooldown > 0f) {
            float sisa = rocketCooldown / ROCKET_COOLDOWN;
            float tinggiSapuan = rocketBtnSize * sisa;
            uiBatch.setColor(0f, 0f, 0f, 0.55f);
            uiBatch.draw(rocketBtnTex, rocketCenterX - rocketBtnSize / 2f,
                    rocketCenterY + rocketBtnSize / 2f - tinggiSapuan,
                    rocketBtnSize, tinggiSapuan,
                    0, 0, 96, Math.max(1, (int) (96f * sisa)), false, false);
            uiBatch.setColor(1f, 1f, 1f, 1f);
            font.getData().setScale(1.05f * density);
            font.setColor(1f, 0.72f, 0.62f, 1f);
            String detik = String.format(Locale.US, "%.1f", rocketCooldown);
            layout.setText(font, detik);
            font.draw(uiBatch, detik, rocketCenterX - layout.width / 2f,
                    rocketCenterY + layout.height / 2f);
        } else {
            font.getData().setScale(0.85f * density);
            font.setColor(1f, 0.75f, 0.62f, 1f);
            layout.setText(font, "RKT");
            font.draw(uiBatch, "RKT", rocketCenterX - layout.width / 2f,
                    rocketCenterY - rocketBtnSize / 2f + 13f * density);
        }

        // Peta mini (tengah atas): seluruh dunia, bingkai area pandang
        // kamera, titik musuh (drone merah muda, gunner merah) & pemain.
        float petaW = 104f * density;
        float petaH = petaW * WORLD_H / WORLD_W;
        float petaX = (screenW - petaW) / 2f;
        float petaY = screenH - 10f * density - petaH;
        uiBatch.setColor(0.35f, 0.75f, 1f, 0.5f);
        uiBatch.draw(whiteTex, petaX - 1.5f, petaY - 1.5f, petaW + 3f, petaH + 3f);
        uiBatch.setColor(0.02f, 0.04f, 0.10f, 0.72f);
        uiBatch.draw(whiteTex, petaX, petaY, petaW, petaH);
        float skalaPeta = petaW / WORLD_W;
        uiBatch.setColor(1f, 1f, 1f, 0.45f);
        float vwX = petaX + (camera.position.x - viewW / 2f) * skalaPeta;
        float vwY = petaY + (camera.position.y - viewH / 2f) * skalaPeta;
        float vwW = viewW * skalaPeta;
        float vwH = viewH * skalaPeta;
        uiBatch.draw(whiteTex, vwX, vwY, vwW, 1f);
        uiBatch.draw(whiteTex, vwX, vwY + vwH - 1f, vwW, 1f);
        uiBatch.draw(whiteTex, vwX, vwY, 1f, vwH);
        uiBatch.draw(whiteTex, vwX + vwW - 1f, vwY, 1f, vwH);
        float titikM = 2.4f * density;
        for (Enemy e : enemies) {
            if (e.type == Enemy.GUNNER) uiBatch.setColor(1f, 0.38f, 0.30f, 0.95f);
            else uiBatch.setColor(0.95f, 0.45f, 0.90f, 0.95f);
            uiBatch.draw(whiteTex, petaX + e.x * skalaPeta - titikM / 2f,
                    petaY + e.y * skalaPeta - titikM / 2f, titikM, titikM);
        }
        float titikP = 3.4f * density;
        uiBatch.setColor(0.55f, 0.95f, 1f, 1f);
        uiBatch.draw(whiteTex, petaX + player.x * skalaPeta - titikP / 2f,
                petaY + player.y * skalaPeta - titikP / 2f, titikP, titikP);
        uiBatch.setColor(1f, 1f, 1f, 1f);

        // Tulisan GAME OVER
        if (state == STATE_OVER) {
            font.getData().setScale(3.2f * density);
            font.setColor(1f, 0.45f, 0.40f, 1f);
            layout.setText(font, "GAME OVER");
            font.draw(uiBatch, "GAME OVER",
                    (screenW - layout.width) / 2f, screenH * 0.62f);
            font.getData().setScale(1.6f * density);
            font.setColor(1f, 1f, 1f, 1f);
            String finalScore = "SCORE " + String.format(Locale.US, "%06d", score);
            layout.setText(font, finalScore);
            font.draw(uiBatch, finalScore,
                    (screenW - layout.width) / 2f, screenH * 0.62f - 52f * density);
        }
        uiBatch.end();
    }

    @Override
    public void dispose() {
        try {
            saveHiScore();
        } catch (Throwable abaikan) {
            // Dispose tidak boleh melempar error baru
        }
        if (batch != null) batch.dispose();
        if (uiBatch != null) uiBatch.dispose();
        if (font != null) font.dispose();
        if (joyBaseTex != null) joyBaseTex.dispose();
        if (joyKnobTex != null) joyKnobTex.dispose();
        if (fireBtnTex != null) fireBtnTex.dispose();
        if (nosBtnTex != null) nosBtnTex.dispose();
        if (rocketBtnTex != null) rocketBtnTex.dispose();
        if (rocketTex != null) rocketTex.dispose();
        if (whiteTex != null) whiteTex.dispose();
        if (softDotTex != null) softDotTex.dispose();
        if (player != null) player.dispose();
        Enemy.disposeShared();
    }
}
