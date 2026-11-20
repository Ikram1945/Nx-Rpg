package com.ikram.nexus;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.HashSet;

/**
 * Nx RPG — tahap awal.
 * Peta desa dari Tiled (.tmx), pemain bisa jalan 4 arah dengan animasi,
 * kamera mengikuti, kendali: joystick sentuh (tahan + geser di mana saja)
 * atau WASD / tombol panah bila ada keyboard.
 */
public class NxRpgGame extends ApplicationAdapter {

    private static final float VIEW_W = 320f;   // lebar dunia yang terlihat (piksel peta)
    private static final float VIEW_H = 180f;

    private OrthographicCamera camera;
    private Viewport viewport;
    private OrthographicCamera screenCam;       // khusus UI layar (joystick)
    private ShapeRenderer shapes;

    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private float mapPixelW, mapPixelH;

    private Player player;

    // Joystick sentuh
    private boolean joyActive = false;
    private float joyOriginX, joyOriginY;       // koordinat layar (y ke atas)
    private float joyDirX, joyDirY;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_W, VIEW_H, camera);
        screenCam = new OrthographicCamera();
        shapes = new ShapeRenderer();

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

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        screenCam.setToOrtho(false, width, height);
    }

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);

        readInput();
        player.update(delta, joyDirX, joyDirY);
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
        float cx = player.x;
        float cy = player.y + 16f;
        // Jepit kamera supaya tidak keluar peta
        cx = Math.max(VIEW_W / 2f, Math.min(cx, mapPixelW - VIEW_W / 2f));
        cy = Math.max(VIEW_H / 2f, Math.min(cy, mapPixelH - VIEW_H / 2f));
        camera.position.set(cx, cy, 0f);
        camera.update();
    }

    /** Baca keyboard + joystick sentuh, hasil akhir di joyDirX/joyDirY (ternormalisasi). */
    private void readInput() {
        float dx = 0f, dy = 0f;

        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) dx -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dx += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) dy += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) dy -= 1f;

        float density = Gdx.graphics.getDensity();
        float radius = 55f * density;
        float deadzone = 9f * density;

        if (Gdx.input.justTouched()) {
            joyActive = true;
            joyOriginX = Gdx.input.getX();
            joyOriginY = Gdx.graphics.getHeight() - Gdx.input.getY();
        }
        if (Gdx.input.isTouched() && joyActive) {
            float tx = Gdx.input.getX();
            float ty = Gdx.graphics.getHeight() - Gdx.input.getY();
            float vx = tx - joyOriginX;
            float vy = ty - joyOriginY;
            float len = (float) Math.sqrt(vx * vx + vy * vy);
            if (len > deadzone) {
                dx = vx / len;
                dy = vy / len;
            }
        } else if (!Gdx.input.isTouched()) {
            joyActive = false;
        }

        // Normalisasi diagonal keyboard
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 1f) {
            dx /= len;
            dy /= len;
        }
        joyDirX = dx;
        joyDirY = dy;

        // Simpan posisi knob untuk digambar
        if (joyActive && Gdx.input.isTouched()) {
            float tx = Gdx.input.getX();
            float ty = Gdx.graphics.getHeight() - Gdx.input.getY();
            float vx = tx - joyOriginX;
            float vy = ty - joyOriginY;
            float l = (float) Math.sqrt(vx * vx + vy * vy);
            if (l > radius) {
                vx = vx / l * radius;
                vy = vy / l * radius;
            }
            knobX = joyOriginX + vx;
            knobY = joyOriginY + vy;
        }
    }

    private float knobX, knobY;

    private void drawJoystick() {
        if (!joyActive) return;
        float density = Gdx.graphics.getDensity();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(screenCam.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, 1f, 1f, 0.15f);
        shapes.circle(joyOriginX, joyOriginY, 55f * density);
        shapes.setColor(1f, 1f, 1f, 0.35f);
        shapes.circle(knobX, knobY, 24f * density);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void dispose() {
        player.dispose();
        mapRenderer.dispose();
        map.dispose();
        shapes.dispose();
    }
}
