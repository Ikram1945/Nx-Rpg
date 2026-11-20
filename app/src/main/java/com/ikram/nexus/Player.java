package com.ikram.nexus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

/**
<<<<<<< HEAD
 * Karakter pemain: sprite 4 arah dari sprites/player.png.
 * Susunan baris sheet: 0 = bawah, 1 = kiri, 2 = atas, 3 = kanan.
 * Tiap arah 3 frame jalan; urutan putar 0-1-2-1 supaya langkahnya halus.
=======
 * Karakter pemain: kesatria berpedang 16x20.
 * Sprite DIGAMBAR DARI KODE: peta piksel di bawah diubah menjadi
 * Pixmap lalu Texture saat game mulai — tanpa berkas gambar sama
 * sekali, jadi tampilannya tidak tergantung dekode PNG di HP.
 * Tiap frame ditaruh di tengah-bawah kanvas 32x32 (pangkat dua).
 * Urutan arah: 0=bawah, 1=kiri, 2=atas, 3=kanan.
 * Frame per arah: 0=langkah A, 1=diam/tengah, 2=langkah B.
>>>>>>> branch 'master' of https://github.com/Ikram1945/Nx-Rpg.git
 * Posisi (x, y) = titik tengah-bawah sprite (jejak kaki).
 */
public class Player {
    public static final int DOWN = 0, LEFT = 1, UP = 2, RIGHT = 3;
<<<<<<< HEAD
=======
    private static final int FRAME_W = 16, FRAME_H = 20;
    private static final float SPEED = 72f;    // piksel dunia per detik
    private static final float HALF_W = 5f;    // setengah lebar kotak tabrakan
    private static final float BOX_H = 8f;     // tinggi kotak tabrakan (area kaki)
>>>>>>> branch 'master' of https://github.com/Ikram1945/Nx-Rpg.git

<<<<<<< HEAD
    private static final int FRAME_W = 35;
    private static final int FRAME_H = 40;
    private static final float SPEED = 75f;      // piksel per detik
    private static final float HALF_W = 6f;      // setengah lebar kotak tabrak (di kaki)
    private static final float BOX_H = 10f;      // tinggi kotak tabrak dari telapak
=======
    // Peta piksel kesatria: [arah][frame] = 20 baris x 16 karakter.
    // '.' = transparan; huruf lain = warna (lihat colorOf).
    private static final String[][][] PIXELS = {
        // bawah
        { { "......RRR.......", ".....RRRRR......", ".....SSSSSS.....", "....SSSSSSSS....", "....SSSSSSSS....", "....SKKKKKKS....", "....SSSSSSSS....", ".....SSSSSS.....", "...SSSSSSSSSS...", "..SSSSSSSSSSSS..", "..sSSSSSSSSSSs..", "..sSSGGGGGGSSs..", ".HHsSSBBBBSSs...", ".WG.sSBBBBSS....", ".WG..BBBBBB.....", ".WG..BBBBBB.....", "....BB....BB....", "....BB.....BB...", "....EE......EE..", "...EEE.......EE." }, { "......RRR.......", ".....RRRRR......", ".....SSSSSS.....", "....SSSSSSSS....", "....SSSSSSSS....", "....SKKKKKKS....", "....SSSSSSSS....", ".....SSSSSS.....", "...SSSSSSSSSS...", "..SSSSSSSSSSSS..", "..sSSSSSSSSSSs..", "..sSSGGGGGGSSs..", ".HHsSSBBBBSSs...", ".WG.sSBBBBSS....", ".WG..BBBBBB.....", ".WG..BBBBBB.....", ".....BB..BB.....", ".....BB..BB.....", ".....EE..EE.....", "....EEE..EEE...." }, { "......RRR.......", ".....RRRRR......", ".....SSSSSS.....", "....SSSSSSSS....", "....SSSSSSSS....", "....SKKKKKKS....", "....SSSSSSSS....", ".....SSSSSS.....", "...SSSSSSSSSS...", "..SSSSSSSSSSSS..", "..sSSSSSSSSSSs..", "..sSSGGGGGGSSs..", ".HHsSSBBBBSSs...", ".WG.sSBBBBSS....", ".WG..BBBBBB.....", ".WG..BBBBBB.....", "....BB....BB....", "...BB......BB...", "..EE........EE..", ".EE..........EEE" } },
        // kiri
        { { ".......RRRR.....", "......RRRRRR....", ".....SSSSSS.....", "....SSSSSSSS....", "...SSSSSSSS.....", "..KSSSSSSS......", "..KSSSSSSS......", "....SSSSSS......", "...SSSSSSSS.....", "...SSSSSSSSS....", "...SSSSSSS......", "...SGGGGGG......", ".HHSSBBBB.......", ".WG.SBBBB.......", ".WG..BBBB.......", ".WG..BBBB.......", ".W..BB....BB....", ".W..BB.....BB...", "....EE.......EE.", "...EEE.......EEE" }, { ".......RRRR.....", "......RRRRRR....", ".....SSSSSS.....", "....SSSSSSSS....", "...SSSSSSSS.....", "..KSSSSSSS......", "..KSSSSSSS......", "....SSSSSS......", "...SSSSSSSS.....", "...SSSSSSSSS....", "...SSSSSSS......", "...SGGGGGG......", ".HHSSBBBB.......", ".WG.SBBBB.......", ".WG..BBBB.......", ".WG..BBBB.......", ".W...BB.BB......", ".W...BB.BB......", ".....EE.EE......", "....EEE.EEE....." }, { ".......RRRR.....", "......RRRRRR....", ".....SSSSSS.....", "....SSSSSSSS....", "...SSSSSSSS.....", "..KSSSSSSS......", "..KSSSSSSS......", "....SSSSSS......", "...SSSSSSSS.....", "...SSSSSSSSS....", "...SSSSSSS......", "...SGGGGGG......", ".HHSSBBBB.......", ".WG.SBBBB.......", ".WG..BBBB.......", ".WG..BBBB.......", ".W...BB.BB......", ".W....BBBB......", "...EE....EE.....", "..EEE.....EEE..." } },
        // atas
        { { "......RRR.......", ".....RRRRR......", ".....SSSSSS.....", "....SSSSSSSS....", "....SSSdSSSS....", "....SSSdSSSS....", "....SSSSSSSS....", ".....SSSSSS.....", "...SSSSSSSSSS...", "..SSSSSSSSSSSS..", "..sSBBBBBBBBSs..", "..sSBBBBBBBBSs..", ".HHsBBBBBBBBs...", ".WG.BBBBBBBB....", ".WG.BBBBBBBB....", ".WG.BBBBBBBB....", "....BB....BB....", "....BB.....BB...", "....EE......EE..", "...EEE.......EE." }, { "......RRR.......", ".....RRRRR......", ".....SSSSSS.....", "....SSSSSSSS....", "....SSSdSSSS....", "....SSSdSSSS....", "....SSSSSSSS....", ".....SSSSSS.....", "...SSSSSSSSSS...", "..SSSSSSSSSSSS..", "..sSBBBBBBBBSs..", "..sSBBBBBBBBSs..", ".HHsBBBBBBBBs...", ".WG.BBBBBBBB....", ".WG.BBBBBBBB....", ".WG.BBBBBBBB....", ".....BB..BB.....", ".....BB..BB.....", ".....EE..EE.....", "....EEE..EEE...." }, { "......RRR.......", ".....RRRRR......", ".....SSSSSS.....", "....SSSSSSSS....", "....SSSdSSSS....", "....SSSdSSSS....", "....SSSSSSSS....", ".....SSSSSS.....", "...SSSSSSSSSS...", "..SSSSSSSSSSSS..", "..sSBBBBBBBBSs..", "..sSBBBBBBBBSs..", ".HHsBBBBBBBBs...", ".WG.BBBBBBBB....", ".WG.BBBBBBBB....", ".WG.BBBBBBBB....", "....BB....BB....", "...BB......BB...", "..EE........EE..", ".EE..........EEE" } },
        // kanan
        { { ".....RRRR.......", "....RRRRRR......", ".....SSSSSS.....", "....SSSSSSSS....", ".....SSSSSSSS...", "......SSSSSSSK..", "......SSSSSSSK..", "......SSSSSS....", ".....SSSSSSSS...", "....SSSSSSSSS...", "......SSSSSSS...", "......GGGGGGS...", ".......BBBBSSHH.", ".......BBBBS.GW.", ".......BBBB..GW.", ".......BBBB..GW.", "....BB....BB..W.", "...BB.....BB..W.", ".EE.......EE....", "EEE.......EEE..." }, { ".....RRRR.......", "....RRRRRR......", ".....SSSSSS.....", "....SSSSSSSS....", ".....SSSSSSSS...", "......SSSSSSSK..", "......SSSSSSSK..", "......SSSSSS....", ".....SSSSSSSS...", "....SSSSSSSSS...", "......SSSSSSS...", "......GGGGGGS...", ".......BBBBSSHH.", ".......BBBBS.GW.", ".......BBBB..GW.", ".......BBBB..GW.", "......BB.BB...W.", "......BB.BB...W.", "......EE.EE.....", ".....EEE.EEE...." }, { ".....RRRR.......", "....RRRRRR......", ".....SSSSSS.....", "....SSSSSSSS....", ".....SSSSSSSS...", "......SSSSSSSK..", "......SSSSSSSK..", "......SSSSSS....", ".....SSSSSSSS...", "....SSSSSSSSS...", "......SSSSSSS...", "......GGGGGGS...", ".......BBBBSSHH.", ".......BBBBS.GW.", ".......BBBB..GW.", ".......BBBB..GW.", "......BB.BB...W.", "......BBBB....W.", ".....EE....EE...", "...EEE.....EEE.." } }
    };
>>>>>>> branch 'master' of https://github.com/Ikram1945/Nx-Rpg.git

<<<<<<< HEAD
    private final Texture sheet;
    private final Animation<TextureRegion>[] walk = new Animation[4];
=======
    private final Texture[][] textures = new Texture[4][3];
    private final Animation<TextureRegion>[] walk;
>>>>>>> branch 'master' of https://github.com/Ikram1945/Nx-Rpg.git
    private final TextureRegion[] idle = new TextureRegion[4];
    private final TiledMapTileLayer collision;
    private final float mapPixelW, mapPixelH;

    public float x, y;
    public int facing = DOWN;
    private float stateTime;
    private boolean moving;

    @SuppressWarnings("unchecked")
    public Player(TiledMapTileLayer collisionLayer, float startX, float startY,
                  float mapPixelW, float mapPixelH) {
        this.collision = collisionLayer;
        this.mapPixelW = mapPixelW;
        this.mapPixelH = mapPixelH;
        x = startX;
        y = startY;

<<<<<<< HEAD
        sheet = new Texture(Gdx.files.internal("sprites/player.png"));
        sheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

=======
        TextureRegion[][] regions = new TextureRegion[4][3];
>>>>>>> branch 'master' of https://github.com/Ikram1945/Nx-Rpg.git
        for (int dir = 0; dir < 4; dir++) {
<<<<<<< HEAD
            TextureRegion[] frames = new TextureRegion[4];
            for (int i = 0; i < 4; i++) {
                int col = new int[]{0, 1, 2, 1}[i];
                frames[i] = new TextureRegion(sheet, col * FRAME_W, dir * FRAME_H, FRAME_W, FRAME_H);
=======
            for (int f = 0; f < 3; f++) {
                Pixmap pm = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
                String[] rows = PIXELS[dir][f];
                for (int row = 0; row < FRAME_H; row++) {
                    String line = rows[row];
                    for (int col = 0; col < FRAME_W; col++) {
                        char ch = line.charAt(col);
                        if (ch == '.') continue;
                        pm.setColor(colorOf(ch));
                        pm.drawPixel(8 + col, 12 + row);
                    }
                }
                Texture tex = new Texture(pm);
                tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                pm.dispose();
                textures[dir][f] = tex;
                regions[dir][f] = new TextureRegion(tex);
>>>>>>> branch 'master' of https://github.com/Ikram1945/Nx-Rpg.git
            }
<<<<<<< HEAD
            walk[dir] = new Animation<>(0.13f, frames);
=======
        }
        walk = new Animation[4];
        for (int dir = 0; dir < 4; dir++) {
            walk[dir] = new Animation<>(0.13f,
                    regions[dir][0], regions[dir][1], regions[dir][2], regions[dir][1]);
>>>>>>> branch 'master' of https://github.com/Ikram1945/Nx-Rpg.git
            walk[dir].setPlayMode(Animation.PlayMode.LOOP);
<<<<<<< HEAD
            idle[dir] = new TextureRegion(sheet, FRAME_W, dir * FRAME_H, FRAME_W, FRAME_H);
=======
            idle[dir] = regions[dir][1];
>>>>>>> branch 'master' of https://github.com/Ikram1945/Nx-Rpg.git
        }
    }

    private static Color colorOf(char ch) {
        switch (ch) {
            case 'K': return new Color(38 / 255f, 40 / 255f, 58 / 255f, 1f);
            case 'S': return new Color(198 / 255f, 208 / 255f, 226 / 255f, 1f);
            case 's': return new Color(141 / 255f, 153 / 255f, 174 / 255f, 1f);
            case 'd': return new Color(90 / 255f, 106 / 255f, 132 / 255f, 1f);
            case 'G': return new Color(224 / 255f, 165 / 255f, 38 / 255f, 1f);
            case 'B': return new Color(58 / 255f, 107 / 255f, 216 / 255f, 1f);
            case 'b': return new Color(43 / 255f, 76 / 255f, 155 / 255f, 1f);
            case 'R': return new Color(216 / 255f, 64 / 255f, 64 / 255f, 1f);
            case 'r': return new Color(166 / 255f, 40 / 255f, 40 / 255f, 1f);
            case 'W': return new Color(232 / 255f, 236 / 255f, 244 / 255f, 1f);
            case 'w': return new Color(170 / 255f, 180 / 255f, 200 / 255f, 1f);
            case 'H': return new Color(122 / 255f, 74 / 255f, 33 / 255f, 1f);
            case 'F': return new Color(240 / 255f, 192 / 255f, 138 / 255f, 1f);
            case 'E': return new Color(107 / 255f, 68 / 255f, 35 / 255f, 1f);
            case 'e': return new Color(74 / 255f, 46 / 255f, 21 / 255f, 1f);
            default:  return new Color(1f, 0f, 1f, 1f);
        }
    }

    /** Gerakkan pemain; moveX/moveY dalam rentang -1..1. */
    public void update(float delta, float moveX, float moveY) {
        moving = moveX != 0 || moveY != 0;
        if (!moving) {
            stateTime = 0f;
            return;
        }
        stateTime += delta;

        if (Math.abs(moveX) > Math.abs(moveY)) {
            facing = moveX < 0 ? LEFT : RIGHT;
        } else {
            facing = moveY < 0 ? DOWN : UP;
        }

        float dx = moveX * SPEED * delta;
        float dy = moveY * SPEED * delta;
        if (!blocked(x + dx, y)) x += dx;
        if (!blocked(x, y + dy)) y += dy;

        // Tetap di dalam peta
        x = Math.max(HALF_W, Math.min(x, mapPixelW - HALF_W));
        y = Math.max(0f, Math.min(y, mapPixelH - BOX_H));
    }

    /** Cek kotak kaki di posisi (px, py) menabrak tile padat atau tidak. */
    private boolean blocked(float px, float py) {
        return solid(px - HALF_W, py) || solid(px + HALF_W, py)
                || solid(px - HALF_W, py + BOX_H) || solid(px + HALF_W, py + BOX_H);
    }

    private boolean solid(float wx, float wy) {
        int tx = (int) (wx / collision.getTileWidth());
        int ty = (int) (wy / collision.getTileHeight());
        if (tx < 0 || ty < 0 || tx >= collision.getWidth() || ty >= collision.getHeight()) {
            return true;
        }
        TiledMapTileLayer.Cell cell = collision.getCell(tx, ty);
        return cell != null && cell.getTile() != null;
    }

    public void draw(SpriteBatch batch) {
        TextureRegion frame = moving
                ? walk[facing].getKeyFrame(stateTime, true)
                : idle[facing];
        // Kanvas gambar 32x32; kesatria 16x20 ada di tengah-bawahnya,
        // jadi gambar digeser 16 ke kiri dari titik kaki.
        batch.draw(frame, x - 16f, y, 32f, 32f);
    }

    public void dispose() {
<<<<<<< HEAD
        sheet.dispose();
=======
        for (Texture[] row : textures) {
            for (Texture t : row) t.dispose();
        }
>>>>>>> branch 'master' of https://github.com/Ikram1945/Nx-Rpg.git
    }
}
