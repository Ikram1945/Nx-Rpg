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
 * (sentuh + geser di separuh kiri layar) atau WASD / tombol panah.
 */
public class NxRpgGame extends ApplicationAdapter {

    private static final float VIEW_W = 320f;   // lebar dunia minimum (piksel peta);
    private static final float VIEW_H = 180f;   // layar lebih lebar -> peta terlihat lebih luas, tanpa garis hitam

    private OrthographicCamera camera;
    private Viewport viewport;
    private OrthographicCamera screenCam;       // khusus UI layar (joystick)
    private SpriteBatch uiBatch;
    private Texture joyBaseTex, joyKnobTex;

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
        uiBatch.dispose();
    }
}
