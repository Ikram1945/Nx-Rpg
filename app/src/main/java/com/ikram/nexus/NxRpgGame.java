package com.ikram.nexus;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.HashSet;

/**
 * Nx RPG — tahap awal.
 * Peta desa dari Tiled (.tmx), pemain (kesatria) bisa jalan 4 arah dengan
 * animasi, kamera mengikuti. Kendali: joystick tetap di kiri bawah layar
 * (sentuh + geser di separuh kiri layar) atau WASD / tombol panah;
 * tombol pedang di kanan bawah (atau spasi) untuk mengayunkan pedang.
 */
public class NxRpgGame extends ApplicationAdapter {

    private static final float VIEW_W = 320f;   // lebar dunia minimum (piksel peta);
    private static final float VIEW_H = 180f;   // layar lebih lebar -> peta terlihat lebih luas, tanpa garis hitam

    private OrthographicCamera camera;
    private Viewport viewport;
    private OrthographicCamera screenCam;       // khusus UI layar (joystick)
    private SpriteBatch uiBatch;
    private Texture joyBaseTex, joyKnobTex;
    private Texture atkBtnTex;
    private float atkCenterX, atkCenterY, atkSize;
    private final boolean[] prevTouched = new boolean[10];

    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private float mapPixelW, mapPixelH;

    private Player player;

    // Joystick tetap (posisi dalam piksel layar, y ke atas)
    private boolean joyActive = false;
    private float joyCenterX, joyCenterY;
    private float joyBaseSize, joyKnobSize, joyTravel;
    private float knobX, knobY;                 // posisi tengah knob saat ini
    private float moveX, moveY;                 // arah gerak akhir (ternormalisasi)

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(VIEW_W, VIEW_H, camera);
        screenCam = new OrthographicCamera();
        uiBatch = new SpriteBatch();

        joyBaseTex = new Texture(Gdx.files.internal("ui/joy_base.png"));
        joyKnobTex = new Texture(Gdx.files.internal("ui/joy_knob.png"));
        atkBtnTex = makeAttackButtonTexture();
        layoutJoystick();

        map = new TmxMapLoader().load("maps/village.tmx");
        // Pixel art harus tajam: paksa filter Nearest pada tekstur tileset
        HashSet<Texture> textures = new HashSet<>();
        for (TiledMapTileSet set : map.getTileSets()) {
            for (TiledMapTile tile : set) {
                textures.add(tile.getTextureRegion().getTexture());
            }
        }
        for (Texture t : textures) {
            t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }

        mapPixelW = map.getProperties().get("width", Integer.class) * 16f;
        mapPixelH = map.getProperties().get("height", Integer.class) * 16f;
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        TiledMapTileLayer collision = (TiledMapTileLayer) map.getLayers().get("collision");
        // Muncul di tengah persimpangan jalan desa
        player = new Player(collision, mapPixelW / 2f, mapPixelH / 2f, mapPixelW, mapPixelH);
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
        // Tombol pedang di kanan bawah, simetris dengan joystick
        atkSize = 84f * density;
        atkCenterX = Gdx.graphics.getWidth() - margin - atkSize / 2f;
        atkCenterY = margin + atkSize / 2f;
    }

    /** Tekstur tombol pedang digambar dari kode (Pixmap): lingkaran baja
     *  berisi ikon pedang diagonal. */
    private Texture makeAttackButtonTexture() {
        int s = 96;
        com.badlogic.gdx.graphics.Pixmap pm =
                new com.badlogic.gdx.graphics.Pixmap(s, s, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pm.setColor(0.55f, 0.62f, 0.72f, 0.95f);
        pm.fillCircle(s / 2, s / 2, s / 2 - 1);
        pm.setColor(0.07f, 0.08f, 0.12f, 0.78f);
        pm.fillCircle(s / 2, s / 2, s / 2 - 7);
        // Bilah: garis tebal dari gagang (kiri bawah) ke ujung (kanan atas)
        pm.setColor(0.91f, 0.93f, 0.96f, 1f);
        for (int i = -1; i <= 1; i++) pm.drawLine(37 + i, 59, 62 + i, 34);
        // Pelindung emas, tegak lurus bilah di pangkalnya
        pm.setColor(0.88f, 0.65f, 0.15f, 1f);
        for (int i = 0; i <= 1; i++) pm.drawLine(32 + i, 54 + i, 41 + i, 63 + i);
        // Gagang cokelat + bandul emas
        pm.setColor(0.48f, 0.29f, 0.13f, 1f);
        for (int i = -1; i <= 1; i++) pm.drawLine(35 + i, 61, 29 + i, 67);
        pm.setColor(0.88f, 0.65f, 0.15f, 1f);
        pm.fillCircle(27, 69, 3);
        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return tex;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        screenCam.setToOrtho(false, width, height);
        layoutJoystick();
    }

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);

        readInput();
        player.update(delta, moveX, moveY);
        updateCamera();

        Gdx.gl.glClearColor(0.08f, 0.12f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        mapRenderer.setView(camera);
        mapRenderer.render();

        SpriteBatch batch = (SpriteBatch) mapRenderer.getBatch();
        batch.begin();
        player.draw(batch);
        batch.end();

        drawJoystick();
    }

    private void updateCamera() {
        float viewW = viewport.getWorldWidth();
        float viewH = viewport.getWorldHeight();
        float cx = player.x;
        float cy = player.y + 16f;
        // Jepit kamera supaya tidak keluar peta; kalau pandangan lebih besar
        // dari peta di satu sumbu, kamera diam di tengah sumbu itu
        cx = viewW >= mapPixelW ? mapPixelW / 2f
                : Math.max(viewW / 2f, Math.min(cx, mapPixelW - viewW / 2f));
        cy = viewH >= mapPixelH ? mapPixelH / 2f
                : Math.max(viewH / 2f, Math.min(cy, mapPixelH - viewH / 2f));
        camera.position.set(cx, cy, 0f);
        camera.update();
    }

    /** Baca keyboard + joystick sentuh, hasil akhir di moveX/moveY (ternormalisasi). */
    private void readInput() {
        // Tombol pedang: sentuhan baru (pointer mana pun) di dalam tombol
        float atkR = atkSize * 0.6f;
        for (int p = 0; p < prevTouched.length; p++) {
            boolean t = Gdx.input.isTouched(p);
            if (t && !prevTouched[p]) {
                float tx = Gdx.input.getX(p);
                float ty = Gdx.graphics.getHeight() - Gdx.input.getY(p);
                float ddx = tx - atkCenterX;
                float ddy = ty - atkCenterY;
                if (ddx * ddx + ddy * ddy <= atkR * atkR) player.attack();
            }
            prevTouched[p] = t;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) player.attack();

        float dx = 0f, dy = 0f;

        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) dx -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dx += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) dy += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) dy -= 1f;

        float density = Gdx.graphics.getDensity();
        float deadzone = 8f * density;

        // Joystick: sentuhan yang mulai di separuh kiri layar mengendalikan knob
        if (Gdx.input.justTouched() && Gdx.input.getX() < Gdx.graphics.getWidth() * 0.55f) {
            joyActive = true;
        }
        if (joyActive && Gdx.input.isTouched()) {
            float tx = Gdx.input.getX();
            float ty = Gdx.graphics.getHeight() - Gdx.input.getY();
            float vx = tx - joyCenterX;
            float vy = ty - joyCenterY;
            float len = (float) Math.sqrt(vx * vx + vy * vy);
            float clamped = Math.min(len, joyTravel);
            if (len > 0.01f) {
                knobX = joyCenterX + vx / len * clamped;
                knobY = joyCenterY + vy / len * clamped;
            }
            if (len > deadzone) {
                dx = vx / len;
                dy = vy / len;
            }
        } else {
            joyActive = false;
            knobX = joyCenterX;
            knobY = joyCenterY;
        }

        // Normalisasi diagonal keyboard
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 1f) {
            dx /= len;
            dy /= len;
        }
        moveX = dx;
        moveY = dy;
    }

    private void drawJoystick() {
        uiBatch.setProjectionMatrix(screenCam.combined);
        uiBatch.begin();
        // Basis sedikit redup saat tidak dipakai, terang saat aktif
        uiBatch.setColor(1f, 1f, 1f, joyActive ? 0.95f : 0.6f);
        uiBatch.draw(joyBaseTex,
                joyCenterX - joyBaseSize / 2f, joyCenterY - joyBaseSize / 2f,
                joyBaseSize, joyBaseSize);
        uiBatch.setColor(1f, 1f, 1f, joyActive ? 1f : 0.75f);
        uiBatch.draw(joyKnobTex,
                knobX - joyKnobSize / 2f, knobY - joyKnobSize / 2f,
                joyKnobSize, joyKnobSize);
        // Tombol pedang: menyala penuh selagi ayunan berjalan
        uiBatch.setColor(1f, 1f, 1f, player.isAttacking() ? 1f : 0.8f);
        uiBatch.draw(atkBtnTex,
                atkCenterX - atkSize / 2f, atkCenterY - atkSize / 2f,
                atkSize, atkSize);
        uiBatch.setColor(1f, 1f, 1f, 1f);
        uiBatch.end();
    }

    @Override
    public void dispose() {
        player.dispose();
        mapRenderer.dispose();
        map.dispose();
        joyBaseTex.dispose();
        joyKnobTex.dispose();
        atkBtnTex.dispose();
        uiBatch.dispose();
    }
}
