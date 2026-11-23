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
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

/**
 * Nx Space — pertempuran luar angkasa (pengganti total mode RPG desa).
 * Pesawat pemain di kiri, alien datang dari kanan: drone cepat dan gunner
 * penembak. Kendali: joystick tetap di kiri bawah (sentuh + geser) atau
 * WASD / panah untuk bergerak, tombol tembak di kanan bawah (tahan untuk
 * menembak terus) atau SPASI. Semua sprite digambar prosedural.
 */
public class NxRpgGame extends ApplicationAdapter {

    private static final float VIEW_W = 320f;
    private static final float VIEW_H = 180f;
    private static final float FIRE_INTERVAL = 0.14f;
    private static final float PLAYER_BULLET_SPEED = 300f;

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private OrthographicCamera screenCam;
    private SpriteBatch uiBatch;
    private BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();

    private Texture joyBaseTex, joyKnobTex, fireBtnTex, whiteTex, softDotTex;
    private Texture heartFullTex, heartEmptyTex;

    private Player player;
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Bullet> bullets = new ArrayList<>();
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

    // Penampil error: bila terisi, game berhenti & layar error yang digambar
    private String errorText;
    private final ArrayList<String> errorLines = new ArrayList<>();

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
        try {
            whiteTex = makeWhiteTexture();
        } catch (Throwable abaikan) {
            whiteTex = null;
        }
        try {
            createGame();
        } catch (Throwable t) {
            showError(t);
        }
    }

    private void createGame() {
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(VIEW_W, VIEW_H, camera);
        batch = new SpriteBatch();

        joyBaseTex = new Texture(Gdx.files.internal("ui/joy_base.png"));
        joyKnobTex = new Texture(Gdx.files.internal("ui/joy_knob.png"));
        fireBtnTex = makeFireButtonTexture();
        softDotTex = makeSoftDotTexture();
        heartFullTex = makeHeartTexture(true);
        heartEmptyTex = makeHeartTexture(false);

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

    /** Hati halus dari kurva implisit hati (bukan piksel kotak-kotak lagi). */
    private Texture makeHeartTexture(boolean full) {
        Pixmap pm = new Pixmap(48, 40, Pixmap.Format.RGBA8888);
        for (int y = 0; y < 40; y++) {
            for (int x = 0; x < 48; x++) {
                double nx = (x - 23.5) / 15.5;
                double ny = (19.5 - y) / 15.5 + 0.25;
                double f = Math.pow(nx * nx + ny * ny - 1.0, 3) - nx * nx * Math.pow(ny, 3);
                if (f < 0.35) {
                    float a = (float) Math.min(1.0, Math.max(0.0, (0.35 - f) / 0.7));
                    if (full) pm.setColor(0.93f, 0.26f, 0.31f, a);
                    else pm.setColor(0.32f, 0.34f, 0.44f, a);
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
            player = new Player(viewport.getWorldWidth() * 0.22f, viewport.getWorldHeight() / 2f);
        }
    }

    private void readInput() {
        moveX = 0f;
        moveY = 0f;
        fireHeld = Gdx.input.isKeyPressed(Input.Keys.SPACE);

        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) moveX -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) moveX += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) moveY += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) moveY -= 1f;

        int screenH = Gdx.graphics.getHeight();
        for (int i = 0; i < 5; i++) {
            if (!Gdx.input.isTouched(i)) {
                if (joyPointer == i) { joyPointer = -1; joyActive = false; }
                if (firePointer == i) { firePointer = -1; }
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
            if (firePointer == -1 && joyPointer != i) {
                float fdx = sx - fireCenterX;
                float fdy = sy - fireCenterY;
                if (Math.sqrt(fdx * fdx + fdy * fdy) <= fireBtnSize * 0.85f) {
                    firePointer = i;
                }
            }
            if (joyPointer == -1 && firePointer != i && sx < Gdx.graphics.getWidth() * 0.55f) {
                joyPointer = i;
                joyActive = true;
            }
            if (firePointer == i) fireHeld = true;
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

    private void spawnEnemy(float viewW, float viewH) {
        float y = 18f + (float) Math.random() * (viewH - 36f);
        int type = (elapsed > 18f && Math.random() < 0.22)
                ? Enemy.GUNNER : Enemy.DRONE;
        enemies.add(new Enemy(type, viewW + 24f, y));
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
        player.respawn(viewW * 0.22f, viewH / 2f);
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
        if (errorText != null) {
            drawErrorScreen();
            return;
        }
        try {
            renderGame();
        } catch (Throwable t) {
            showError(t);
            drawErrorScreen();
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
            player.update(delta, moveX, moveY, viewW, viewH);

            // Menembak (tahan tombol / spasi)
            fireTimer -= delta;
            if (fireHeld && fireTimer <= 0f && !player.dead) {
                bullets.add(new Bullet(player.gunX(), player.gunY(),
                        PLAYER_BULLET_SPEED, 0f, true, null));
                fireTimer = FIRE_INTERVAL;
            }

            // Gelombang musuh (maksimal 7 bot hidup bersamaan)
            spawnTimer -= delta;
            if (spawnTimer <= 0f) {
                if (enemies.size() < 7) {
                    spawnEnemy(viewW, viewH);
                    float interval = Math.max(0.42f, 1.05f - elapsed * 0.008f);
                    spawnTimer = interval * (0.7f + (float) Math.random() * 0.6f);
                } else {
                    spawnTimer = 0.35f;
                }
            }

            // Musuh: AI memilih sasaran & mengemudi, lalu bergerak & menembak
            for (Enemy e : enemies) {
                updateEnemyAi(e, delta, viewW, viewH);
                e.update(delta, viewW, viewH);
                boolean engaged = e.hasTarget && e.targetDist <= 205f
                        && e.x < viewW - 4f && e.x > 4f;
                if (e.updateFire(delta, engaged)) {
                    spawnEnemyBullet(e);
                }
            }

            // Peluru bergerak
            for (Bullet b : bullets) {
                b.x += b.vx * delta;
                b.y += b.vy * delta;
                if (b.x < -12f || b.x > viewW + 12f || b.y < -12f || b.y > viewH + 12f) {
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
                        if (e.hit()) e.killedByPlayer = true;
                        break;
                    }
                }
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
                if (e.isGone()) {
                    eit.remove();
                    continue;
                }
                // Tabrakan badan musuh vs pesawat: musuh rusak berat (2),
                // pemain -1 hati. Gunner yang tebal bisa selamat dari tabrakan.
                if (!player.dead && !player.isInvulnerable()) {
                    float dx = e.x - player.x;
                    float dy = e.y - player.y;
                    float rr = e.radius + Player.RADIUS;
                    if (dx * dx + dy * dy < rr * rr) {
                        if (e.damage(2)) {
                            e.killedByPlayer = true;
                            explosion(e.x, e.y, e.type == Enemy.GUNNER);
                            score += e.scoreValue;
                            eit.remove();
                        }
                        if (player.takeDamage()) onPlayerHit(viewW, viewH);
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
                        if (player.takeDamage()) onPlayerHit(viewW, viewH);
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
                        e.damage(1); // hancurnya disapu pada iterasi musuh berikutnya
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
                e.update(delta, viewW, viewH);
            }
            overTimer -= delta;
            if (overTimer <= 0f) resetRun(viewW, viewH);
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
        for (float[] nb : nebula) {
            batch.setColor(nb[4], nb[5], nb[6], nb[7]);
            batch.draw(softDotTex, nb[0] - nb[3], nb[1] - nb[3], nb[3] * 2f, nb[3] * 2f);
        }
        for (float[] s : stars) {
            batch.setColor(1f, 1f, 1f, s[4]);
            batch.draw(whiteTex, s[0], s[1], s[3], s[3]);
        }
        batch.setColor(1f, 1f, 1f, 1f);
        for (Bullet b : bullets) {
            if (b.friendly) {
                batch.setColor(0.45f, 0.85f, 1f, 0.50f);
                batch.draw(softDotTex, b.x - 8f, b.y - 8f, 16f, 16f);
                batch.setColor(0.85f, 0.98f, 1f, 1f);
                batch.draw(whiteTex, b.x - 3.5f, b.y - 1f, 7f, 2f);
            } else {
                batch.setColor(1f, 0.40f, 0.85f, 0.55f);
                batch.draw(softDotTex, b.x - 7.5f, b.y - 7.5f, 15f, 15f);
                batch.setColor(1f, 0.65f, 0.95f, 1f);
                batch.draw(whiteTex, b.x - 2f, b.y - 2f, 4f, 4f);
            }
        }
        batch.setColor(1f, 1f, 1f, 1f);
        for (Enemy e : enemies) e.draw(batch);
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

        // Hati di kiri atas
        float heartW = 22f * density;
        float heartH = 18f * density;
        for (int i = 0; i < Player.MAX_HP; i++) {
            Texture t = i < player.hp ? heartFullTex : heartEmptyTex;
            uiBatch.draw(t, 14f * density + i * (heartW + 5f * density),
                    screenH - 14f * density - heartH, heartW, heartH);
        }

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

    // ---------- Penampil error di dalam game ----------

    /**
     * Tangkap error: simpan jejak lengkapnya, catat ke log, dan siapkan
     * baris-barisnya untuk digambar oleh drawErrorScreen(). Game berhenti
     * berjalan, tapi aplikasi TIDAK mati — errornya tampil di layar
     * supaya bisa dibaca / difoto / dikirim ke pengembang.
     */
    private void showError(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        errorText = sw.toString();
        try {
            Gdx.app.error("NxSpace", "Error tertangkap:\n" + errorText);
        } catch (Throwable abaikan) {
            // Log bisa gagal bila app belum siap; abaikan
        }
        try {
            Gdx.files.local("nx-space-error.txt").writeString(errorText, false);
        } catch (Throwable abaikan) {
            // Penyimpanan cadangan gagal pun tak boleh mematikan game
        }
        // Bungkus baris panjang agar muat selebar layar
        errorLines.clear();
        int perBaris = Math.max(40, (Gdx.graphics.getWidth() - 32) / 8);
        for (String mentah : errorText.split("\n")) {
            String baris = mentah.replace("\t", "    ").replace("\r", "");
            while (baris.length() > perBaris) {
                errorLines.add(baris.substring(0, perBaris));
                baris = "  " + baris.substring(perBaris);
            }
            errorLines.add(baris);
        }
    }

    private void drawErrorScreen() {
        if (uiBatch == null || font == null || screenCam == null) {
            // Tak ada alat gambar: biarkan penangkap di MainActivity bekerja
            return;
        }
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        screenCam.setToOrtho(false, w, h);
        screenCam.update();
        Gdx.gl.glClearColor(0.10f, 0.02f, 0.03f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        uiBatch.setProjectionMatrix(screenCam.combined);
        uiBatch.begin();
        if (whiteTex != null) {
            uiBatch.setColor(0.50f, 0.09f, 0.11f, 1f);
            uiBatch.draw(whiteTex, 0, h - 52, w, 52);
            uiBatch.setColor(1f, 1f, 1f, 1f);
        }
        font.getData().setScale(1.5f);
        font.setColor(1f, 0.92f, 0.92f, 1f);
        font.draw(uiBatch, "TERJADI ERROR", 16, h - 16);
        font.getData().setScale(0.9f);
        font.setColor(1f, 0.78f, 0.78f, 1f);
        float y = h - 76;
        int maxLines = (int) ((h - 110) / 15f);
        for (int i = 0; i < errorLines.size() && i < maxLines; i++) {
            font.draw(uiBatch, errorLines.get(i), 16, y);
            y -= 15f;
        }
        font.getData().setScale(1f);
        font.setColor(1f, 0.85f, 0.45f, 1f);
        font.draw(uiBatch, "Foto layar ini, lalu kirim ke pengembang.", 16, 26);
        font.setColor(1f, 1f, 1f, 1f);
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
        if (whiteTex != null) whiteTex.dispose();
        if (softDotTex != null) softDotTex.dispose();
        if (heartFullTex != null) heartFullTex.dispose();
        if (heartEmptyTex != null) heartEmptyTex.dispose();
        if (player != null) player.dispose();
        Enemy.disposeShared();
    }
}
