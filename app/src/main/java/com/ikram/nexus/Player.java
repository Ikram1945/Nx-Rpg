package com.ikram.nexus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

/**
 * Karakter pemain: kesatria berpedang 16x20.
 * Tiap frame animasi adalah berkas PNG sendiri di sprites/frames/
 * (down_0..2, left_0..2, up_0..2, right_0..2) — sengaja tidak memakai
 * potongan spritesheet supaya tidak ada risiko salah potong tekstur.
 * Kolom frame: 0 = langkah A, 1 = diam/tengah, 2 = langkah B.
 * Posisi (x, y) = titik tengah-bawah sprite (jejak kaki).
 */
public class Player {

    public static final int DOWN = 0, LEFT = 1, UP = 2, RIGHT = 3;
    private static final String[] DIR_NAME = {"down", "left", "up", "right"};

    private static final int FRAME_W = 16;
    private static final int FRAME_H = 20;
    private static final float SPEED = 72f;      // piksel per detik
    private static final float HALF_W = 5f;      // setengah lebar kotak tabrak (di kaki)
    private static final float BOX_H = 8f;       // tinggi kotak tabrak dari telapak

    private final Texture[][] textures = new Texture[4][3];
    private final Animation<TextureRegion>[] walk = new Animation[4];
    private final TextureRegion[] idle = new TextureRegion[4];
    private final TiledMapTileLayer collision;
    private final float mapPixelW, mapPixelH;

    public float x, y;
    public int facing = DOWN;
    private float stateTime = 0f;
    private boolean moving = false;

    @SuppressWarnings("unchecked")
    public Player(TiledMapTileLayer collision, float startX, float startY,
                  float mapPixelW, float mapPixelH) {
        this.collision = collision;
        this.mapPixelW = mapPixelW;
        this.mapPixelH = mapPixelH;
        this.x = startX;
        this.y = startY;

        for (int dir = 0; dir < 4; dir++) {
            TextureRegion[] regions = new TextureRegion[3];
            for (int col = 0; col < 3; col++) {
                Texture t = new Texture(
                        Gdx.files.internal("sprites/frames/" + DIR_NAME[dir] + "_" + col + ".png"));
                t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                textures[dir][col] = t;
                regions[col] = new TextureRegion(t);
            }
            // Urutan langkah 0-1-2-1 supaya halus
            walk[dir] = new Animation<>(0.13f,
                    regions[0], regions[1], regions[2], regions[1]);
            walk[dir].setPlayMode(Animation.PlayMode.LOOP);
            idle[dir] = regions[1];
        }
    }

    /** Gerak mengikuti vektor arah (sudah ternormalisasi atau nol). */
    public void update(float delta, float dirX, float dirY) {
        moving = dirX != 0f || dirY != 0f;
        if (!moving) {
            stateTime = 0f;
            return;
        }
        stateTime += delta;

        // Arah hadap: ikuti sumbu yang dominan
        if (Math.abs(dirX) > Math.abs(dirY)) {
            facing = dirX > 0 ? RIGHT : LEFT;
        } else {
            facing = dirY > 0 ? UP : DOWN;
        }

        // Geser per sumbu supaya bisa meluncur di sepanjang dinding
        float newX = x + dirX * SPEED * delta;
        if (!blocked(newX, y)) x = newX;
        float newY = y + dirY * SPEED * delta;
        if (!blocked(x, newY)) y = newY;

        // Tetap di dalam peta
        if (x < HALF_W) x = HALF_W;
        if (x > mapPixelW - HALF_W) x = mapPixelW - HALF_W;
        if (y < 0) y = 0;
        if (y > mapPixelH - BOX_H) y = mapPixelH - BOX_H;
    }

    /** Cek kotak tabrak di kaki terhadap layer "collision" peta. */
    private boolean blocked(float px, float py) {
        int tile = 16;
        int x0 = (int) ((px - HALF_W) / tile);
        int x1 = (int) ((px + HALF_W - 0.01f) / tile);
        int y0 = (int) (py / tile);
        int y1 = (int) ((py + BOX_H - 0.01f) / tile);
        for (int ty = y0; ty <= y1; ty++) {
            for (int tx = x0; tx <= x1; tx++) {
                if (tx < 0 || ty < 0 || tx >= collision.getWidth() || ty >= collision.getHeight()) {
                    return true;
                }
                if (collision.getCell(tx, ty) != null) return true;
            }
        }
        return false;
    }

    public void draw(SpriteBatch batch) {
        TextureRegion frame = moving
                ? walk[facing].getKeyFrame(stateTime, true)
                : idle[facing];
        batch.draw(frame, x - FRAME_W / 2f, y, FRAME_W, FRAME_H);
    }

    public void dispose() {
        for (int dir = 0; dir < 4; dir++) {
            for (int col = 0; col < 3; col++) {
                textures[dir][col].dispose();
            }
        }
    }
}
