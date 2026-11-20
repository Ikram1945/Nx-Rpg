package com.ikram.nexus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

/**
 * Musuh slime (digambar dari kode, seperti pemain).
 * Perilaku: diam di titik asal; kalau pemain masuk radius deteksi ia
 * mengejar, dan kalau sudah dekat ia menyerang pemain secara berkala.
 * Kena ayunan pedang: darah berkurang, terpental, berkedip merah;
 * darah habis = mati, lalu muncul lagi di titik asal setelah jeda.
 * Posisi (x, y) = titik tengah-bawah sprite (jejak kaki).
 */
public class Enemy {
    private static final int MAX_HP = 3;
    private static final float SPEED = 40f;        // lebih lambat dari pemain
    private static final float RETURN_SPEED = 28f;
    private static final float DETECT_R = 72f;     // radius deteksi pemain
    private static final float LEASH_R = 130f;     // lepas kejar di jarak ini
    private static final float ATTACK_R = 15f;     // mulai menyerang di jarak ini
    private static final float STRIKE_R = 23f;     // serangan kena di jarak ini
    private static final float ATTACK_CD = 0.95f;  // jeda antar serangan
    private static final float RESPAWN_T = 6f;
    private static final float HALF_W = 6f;
    private static final float BOX_H = 7f;

    // Peta piksel slime 16x12, dua frame (normal / memampat).
    private static final String[][] FRAMES = {
            {
                    "................",
                    ".....gggggg.....",
                    "...gggggggggg...",
                    "..ggGGGGGGGGgg..",
                    ".gGGGGGGGGGGGGg.",
                    ".gGGKKGGGGKKGGg.",
                    ".gGGKKGGGGKKGGg.",
                    ".gGGGGGGGGGGGGg.",
                    "..gGGGGGGGGGGg..",
                    "..gggggggggggg..",
                    "...dddddddddd...",
                    "................",
            },
            {
                    "................",
                    "................",
                    "................",
                    "...gggggggggg...",
                    ".gggggggggggggg.",
                    ".gGGKKGGGGKKGGg.",
                    ".gGGKKGGGGKKGGg.",
                    "gGGGGGGGGGGGGGGg",
                    "gGGGGGGGGGGGGGGg",
                    ".gggggggggggggg.",
                    "..dddddddddddd..",
                    "................",
            },
    };

    private final Texture[] textures = new Texture[2];
    private final TextureRegion[] regions = new TextureRegion[2];
    private final TiledMapTileLayer collision;
    private final float mapPixelW, mapPixelH;
    private final float spawnX, spawnY;

    public float x, y;
    public boolean alive = true;
    private int hp = MAX_HP;
    private boolean chasing;
    private float attackCd;
    private float flashTime;
    private float deathTime;
    private float respawnTime;
    private float animTime;
    private float kbx, kby;   // kecepatan terpental

    public Enemy(TiledMapTileLayer collisionLayer, float spawnX, float spawnY,
                 float mapPixelW, float mapPixelH) {
        this.collision = collisionLayer;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.mapPixelW = mapPixelW;
        this.mapPixelH = mapPixelH;
        x = spawnX;
        y = spawnY;

        for (int f = 0; f < 2; f++) {
            Pixmap pm = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
            String[] rows = FRAMES[f];
            for (int row = 0; row < 12; row++) {
                String line = rows[row];
                for (int col = 0; col < 16; col++) {
                    char ch = line.charAt(col);
                    if (ch == '.') continue;
                    pm.setColor(colorOf(ch));
                    pm.drawPixel(8 + col, 20 + row);
                }
            }
            textures[f] = new Texture(pm);
            textures[f].setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            pm.dispose();
            regions[f] = new TextureRegion(textures[f]);
        }
    }

    private static Color colorOf(char ch) {
        switch (ch) {
            case 'G': return new Color(96 / 255f, 208 / 255f, 96 / 255f, 1f);
            case 'g': return new Color(164 / 255f, 236 / 255f, 150 / 255f, 1f);
            case 'd': return new Color(52 / 255f, 148 / 255f, 62 / 255f, 1f);
            case 'K': return new Color(24 / 255f, 28 / 255f, 38 / 255f, 1f);
            default:  return new Color(1f, 0f, 1f, 1f);
        }
    }

    /** Kembali ke keadaan awal di titik asal. */
    public void reset() {
        x = spawnX;
        y = spawnY;
        hp = MAX_HP;
        alive = true;
        chasing = false;
        attackCd = 0f;
        flashTime = 0f;
        deathTime = 0f;
        respawnTime = 0f;
        kbx = kby = 0f;
    }

    /**
     * Coba kenai slime ini dengan ayunan pedang pemain di (px, py)
     * yang mengayun ke arah (fx, fy). Dipanggil sekali per ayunan.
     */
    public void tryHitBySword(float px, float py, float fx, float fy) {
        if (!alive || deathTime > 0f) return;
        float dx = x - px;
        float dy = y - py;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > 32f || dist < 0.01f) return;
        float dot = (dx * fx + dy * fy) / dist;
        if (dot < 0.15f) return;   // di luar busur ayunan
        hp--;
        flashTime = 0.15f;
        kbx = dx / dist * 130f;
        kby = dy / dist * 130f;
        chasing = true;            // kena pukul = langsung sadar & mengejar
        if (hp <= 0) deathTime = 0.3f;
    }

    public void update(float delta, Player player) {
        if (!alive) {
            respawnTime -= delta;
            if (respawnTime <= 0f) reset();
            return;
        }
        if (deathTime > 0f) {
            deathTime -= delta;
            if (deathTime <= 0f) {
                alive = false;
                respawnTime = RESPAWN_T;
            }
            return;
        }
        if (flashTime > 0f) flashTime -= delta;
        animTime += delta;

        // Terpental (dari pukulan pedang)
        if (kbx != 0f || kby != 0f) {
            if (!blocked(x + kbx * delta, y)) x += kbx * delta;
            if (!blocked(x, y + kby * delta)) y += kby * delta;
            float decay = Math.max(0f, 1f - 7f * delta);
            kbx *= decay;
            kby *= decay;
            if (Math.abs(kbx) < 1f) kbx = 0f;
            if (Math.abs(kby) < 1f) kby = 0f;
        }

        float dx = player.x - x;
        float dy = player.y - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (!chasing && dist < DETECT_R) chasing = true;
        if (chasing && dist > LEASH_R) chasing = false;

        if (attackCd > 0f) attackCd -= delta;

        if (chasing) {
            if (dist > ATTACK_R && dist > 0.01f) {
                moveToward(dx / dist, dy / dist, SPEED, delta);
            } else if (attackCd <= 0f) {
                // Saatnya menyerang: kena kalau pemain masih cukup dekat
                attackCd = ATTACK_CD;
                if (dist < STRIKE_R) player.takeDamage(x, y);
            }
        } else {
            // Pulang ke titik asal, lalu diam
            float hx = spawnX - x;
            float hy = spawnY - y;
            float hdist = (float) Math.sqrt(hx * hx + hy * hy);
            if (hdist > 2f) moveToward(hx / hdist, hy / hdist, RETURN_SPEED, delta);
        }

        x = Math.max(HALF_W, Math.min(x, mapPixelW - HALF_W));
        y = Math.max(0f, Math.min(y, mapPixelH - BOX_H));
    }

    private void moveToward(float nx, float ny, float speed, float delta) {
        float mx = nx * speed * delta;
        float my = ny * speed * delta;
        if (!blocked(x + mx, y)) x += mx;
        if (!blocked(x, y + my)) y += my;
    }

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
        if (!alive) return;
        // Lompat-lompat kecil: frame berganti cepat saat bergerak/mengejar
        float interval = chasing ? 0.16f : 0.45f;
        int f = ((int) (animTime / interval)) % 2;
        if (flashTime > 0f) {
            batch.setColor(1f, 0.35f, 0.35f, 1f);
        }
        if (deathTime > 0f) {
            // Menyusut sambil memudar
            float s = Math.max(0.2f, deathTime / 0.3f);
            batch.setColor(1f, 1f, 1f, s);
            float w = 32f * s;
            batch.draw(regions[f], x - w / 2f, y, w, 32f * s);
        } else {
            batch.draw(regions[f], x - 16f, y, 32f, 32f);
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void dispose() {
        for (Texture t : textures) t.dispose();
    }
}
