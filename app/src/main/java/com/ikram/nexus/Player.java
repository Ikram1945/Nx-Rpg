package com.ikram.nexus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

/**
 * Karakter pemain: sprite 4 arah dari sprites/player.png.
 * Susunan baris sheet: 0 = bawah, 1 = kiri, 2 = atas, 3 = kanan.
 * Tiap arah 3 frame jalan; urutan putar 0-1-2-1 supaya langkahnya halus.
 * Posisi (x, y) = titik tengah-bawah sprite (jejak kaki).
 */
public class Player {

    public static final int DOWN = 0, LEFT = 1, UP = 2, RIGHT = 3;

    private static final int FRAME_W = 35;
    private static final int FRAME_H = 40;
    private static final float SPEED = 75f;      // piksel per detik
    private static final float HALF_W = 6f;      // setengah lebar kotak tabrak (di kaki)
    private static final float BOX_H = 10f;      // tinggi kotak tabrak dari telapak

    private final Texture sheet;
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

        sheet = new Texture(Gdx.files.internal("sprites/player.png"));
        sheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        for (int dir = 0; dir < 4; dir++) {
            TextureRegion[] frames = new TextureRegion[4];
            for (int i = 0; i < 4; i++) {
                int col = new int[]{0, 1, 2, 1}[i];
                frames[i] = new TextureRegion(sheet, col * FRAME_W, dir * FRAME_H, FRAME_W, FRAME_H);
            }
            walk[dir] = new Animation<>(0.13f, frames);
            walk[dir].setPlayMode(Animation.PlayMode.LOOP);
            idle[dir] = new TextureRegion(sheet, FRAME_W, dir * FRAME_H, FRAME_W, FRAME_H);
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
        sheet.dispose();
    }
}
