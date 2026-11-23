package com.ikram.nexus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Pesawat pemain (game pertempuran luar angkasa).
 * Digambar prosedural dari peta piksel (Pixmap -> Texture) — tanpa berkas
 * gambar, cara yang sama yang terbukti aman di rantai build HP.
 * Posisi x/y adalah titik TENGAH pesawat.
 */
public class Player {

    public static final int MAX_HP = 3;
    public static final float SPEED = 118f;
    public static final float RADIUS = 7f;

    // Peta piksel pesawat 32x32, 2 bingkai (api mesin pendek / panjang).
    private static final String[][] PIXELS = {
        {  // pesawat
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "..........K.....................",
                ".....KKKKKwK....................",
                "......wwwwwwK...................",
                ".......wwwwwK...................",
                ".......wwwKKWKKKKKKKKKKKK.......",
                "....O...KKWWWWWWWKKWCCKKKKK.....",
                "...OY.KWWWWWWWWWWKCCCCCKWWKYK...",
                "..OYYWWWWWWWWWWWWKKCCCKKWWWYYYY.",
                "...OY.KWWWWWWWWWWWWKKKWWWWKYK...",
                "....O...KwwwwwwwwwwwwwWWKKK.....",
                ".......wwwKKWKKKKKKKKKKKK.......",
                ".......wwwwwK...................",
                "......wwwwwwK...................",
                ".....KKKKKwK....................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................"
        },
        {  // pesawat
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "..........K.....................",
                ".....KKKKKwK....................",
                "......wwwwwwK...................",
                ".......wwwwwK...................",
                ".......wwwKKWKKKKKKKKKKKK.......",
                "........KKWWWWWWWKKWCCKKKKK.....",
                "....Y.KWWWWWWWWWWKCCCCCKWWKYK...",
                ".OYYYWWWWWWWWWWWWKKCCCKKWWWYYYY.",
                "....Y.KWWWWWWWWWWWWKKKWWWWKYK...",
                "........KwwwwwwwwwwwwwWWKKK.....",
                ".......wwwKKWKKKKKKKKKKKK.......",
                ".......wwwwwK...................",
                "......wwwwwwK...................",
                ".....KKKKKwK....................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................"
        }
    };

    private final Texture[] textures = new Texture[2];
    private final TextureRegion[] frames = new TextureRegion[2];

    public float x, y;
    public int hp = MAX_HP;
    public boolean dead;
    private float invulnTime;
    private float stateTime;

    public Player(float startX, float startY) {
        for (int f = 0; f < 2; f++) {
            Pixmap pm = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
            for (int row = 0; row < 32; row++) {
                String line = PIXELS[f][row];
                for (int col = 0; col < 32; col++) {
                    char ch = line.charAt(col);
                    if (ch == '.') continue;
                    pm.setColor(colorOf(ch));
                    pm.drawPixel(col, row);
                }
            }
            textures[f] = new Texture(pm);
            textures[f].setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            pm.dispose();
            frames[f] = new TextureRegion(textures[f]);
        }
        x = startX;
        y = startY;
    }

    private static Color colorOf(char ch) {
        switch (ch) {
            case 'K': return new Color(38f / 255f, 40f / 255f, 58f / 255f, 1f);
            case 'W': return new Color(232f / 255f, 236f / 255f, 244f / 255f, 1f);
            case 'w': return new Color(170f / 255f, 180f / 255f, 200f / 255f, 1f);
            case 'C': return new Color(80f / 255f, 220f / 255f, 255f / 255f, 1f);
            case 'O': return new Color(255f / 255f, 150f / 255f, 40f / 255f, 1f);
            case 'Y': return new Color(255f / 255f, 220f / 255f, 90f / 255f, 1f);
            default:  return Color.WHITE;
        }
    }

    public void update(float delta, float mx, float my, float viewW, float viewH) {
        stateTime += delta;
        if (invulnTime > 0f) invulnTime -= delta;
        if (dead) return;
        float len = (float) Math.sqrt(mx * mx + my * my);
        if (len > 0.01f) {
            float nx = mx / len;
            float ny = my / len;
            float mag = Math.min(1f, len);
            x += nx * SPEED * mag * delta;
            y += ny * SPEED * mag * delta;
        }
        if (x < 14f) x = 14f;
        if (x > viewW - 14f) x = viewW - 14f;
        if (y < 10f) y = 10f;
        if (y > viewH - 10f) y = viewH - 10f;
    }

    /** Titik keluar peluru (ujung hidung pesawat). */
    public float gunX() { return x + 14f; }
    public float gunY() { return y; }

    public boolean isInvulnerable() { return invulnTime > 0f; }

    /** Kena serangan: darah berkurang, kebal sesaat. Return true bila serangan masuk. */
    public boolean takeDamage() {
        if (dead || invulnTime > 0f) return false;
        hp--;
        invulnTime = 1.2f;
        if (hp <= 0) {
            hp = 0;
            dead = true;
        }
        return true;
    }

    public void respawn(float rx, float ry) {
        x = rx;
        y = ry;
        hp = MAX_HP;
        dead = false;
        invulnTime = 2f;
    }

    public void draw(SpriteBatch batch) {
        if (dead) return;
        TextureRegion frame = frames[(int) (stateTime * 14f) % 2];
        // Berkedip tembus pandang selagi kebal setelah kena serangan
        if (invulnTime > 0f && ((int) (invulnTime * 12f)) % 2 == 0) {
            batch.setColor(1f, 1f, 1f, 0.45f);
        }
        batch.draw(frame, x - 16f, y - 16f, 32f, 32f);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void dispose() {
        for (Texture t : textures) t.dispose();
    }
}
