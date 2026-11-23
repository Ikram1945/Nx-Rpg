package com.ikram.nexus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Pesawat alien (game pertempuran luar angkasa).
 * Dua jenis: DRONE (cepat, 1 darah, tidak menembak) dan GUNNER (lambat,
 * 3 darah, menembak ke arah pemain). Terbang dari kanan ke kiri dengan
 * gelombang sinus ringan. Digambar prosedural dari peta piksel; tekstur
 * dipakai bersama oleh semua musuh (dibuat sekali secara statis).
 * Posisi x/y adalah titik TENGAH pesawat.
 */
public class Enemy {

    public static final int DRONE = 0;
    public static final int GUNNER = 1;

    // Peta piksel 32x32 per jenis: [0]=drone, [1]=gunner.
    private static final String[][] PIXELS = {
        {  // alien
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "..............KKKKK.............",
                ".............KMMMMMK............",
                "............KMMMMMMMK...........",
                ".........KKKKMMMMMMMKKKK........",
                "........KPKKKKMMMMMKPPPPK.......",
                ".......KPKRRRKKKKKKPPPPPPK......",
                ".......KPKRRRKPPPPPPPPPPPK......",
                "........KPKKKPPPPPPPPPPPK.......",
                ".........KKKPPPpppppppppp.......",
                "............KKKKKKKKKppp........",
                "......................p.........",
                "................................",
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
        {  // alien
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "................................",
                "............K...................",
                "....K.......KKKKKKK.............",
                "....KKKK.....RKKKKKKKKKKK.......",
                "....KKrKKKK..RRRRRRRRRRRKK......",
                "....KKrrrrKKKKRRRRKKKRRRKK......",
                "....KKrrrrrrrKRRRKOOOKRRRKK.....",
                "....KKKKKKKKrKKRKOOYOOKRRKK.....",
                ".............KKRKOYYYOKRRRKK....",
                "....KKKKKKKKrKKRKOOYOOKRRKK.....",
                "....KKrrrrrrrKRRRKOOOKRRRKK.....",
                "....KKrrrrKKKKRRRRKKKRRRKK......",
                "....KKrKKKK..RRRRRRRRRRRKK......",
                "....KKKK.....RKKKKKKKKKKK.......",
                "....K.......KKKKKKK.............",
                "............K...................",
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

    private static Texture[] sharedTextures;
    private static TextureRegion[] sharedRegions;

    private static void ensureTextures() {
        if (sharedTextures != null) return;
        sharedTextures = new Texture[2];
        sharedRegions = new TextureRegion[2];
        for (int t = 0; t < 2; t++) {
            Pixmap pm = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
            for (int row = 0; row < 32; row++) {
                String line = PIXELS[t][row];
                for (int col = 0; col < 32; col++) {
                    char ch = line.charAt(col);
                    if (ch == '.') continue;
                    pm.setColor(colorOf(ch));
                    pm.drawPixel(col, row);
                }
            }
            sharedTextures[t] = new Texture(pm);
            sharedTextures[t].setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            pm.dispose();
            sharedRegions[t] = new TextureRegion(sharedTextures[t]);
        }
    }

    public static void disposeShared() {
        if (sharedTextures != null) {
            for (Texture t : sharedTextures) t.dispose();
            sharedTextures = null;
            sharedRegions = null;
        }
    }

    private static Color colorOf(char ch) {
        switch (ch) {
            case 'K': return new Color(38f / 255f, 40f / 255f, 58f / 255f, 1f);
            case 'P': return new Color(176f / 255f, 84f / 255f, 224f / 255f, 1f);
            case 'p': return new Color(108f / 255f, 40f / 255f, 158f / 255f, 1f);
            case 'M': return new Color(244f / 255f, 92f / 255f, 204f / 255f, 1f);
            case 'R': return new Color(232f / 255f, 72f / 255f, 60f / 255f, 1f);
            case 'r': return new Color(166f / 255f, 40f / 255f, 40f / 255f, 1f);
            case 'O': return new Color(255f / 255f, 150f / 255f, 40f / 255f, 1f);
            case 'Y': return new Color(255f / 255f, 220f / 255f, 90f / 255f, 1f);
            default:  return Color.WHITE;
        }
    }

    public final int type;
    public float x, y;
    public int hp;
    public final float radius;
    public final int scoreValue;
    private final float baseY;
    private final float speed;
    private final float phase;
    private final float waveAmp;
    private float time;
    private float fireTimer;
    private float flashTime;

    public Enemy(int type, float startX, float startY) {
        ensureTextures();
        this.type = type;
        x = startX;
        y = startY;
        baseY = startY;
        phase = (float) (Math.random() * Math.PI * 2.0);
        if (type == GUNNER) {
            hp = 3;
            radius = 9.5f;
            scoreValue = 250;
            speed = 26f + (float) Math.random() * 8f;
            waveAmp = 5f;
            fireTimer = 1.2f + (float) Math.random() * 1.4f;
        } else {
            hp = 1;
            radius = 7.5f;
            scoreValue = 100;
            speed = 42f + (float) Math.random() * 18f;
            waveAmp = 10f;
            fireTimer = Float.MAX_VALUE;
        }
    }

    public void update(float delta) {
        time += delta;
        if (flashTime > 0f) flashTime -= delta;
        x -= speed * delta;
        y = baseY + (float) Math.sin(time * 2.2f + phase) * waveAmp;
        if (fireTimer != Float.MAX_VALUE) fireTimer -= delta;
    }

    /** True sekali tiap jeda tembak gunner tercapai; timer diatur ulang di sini. */
    public boolean wantsFire() {
        if (type != GUNNER || fireTimer > 0f) return false;
        fireTimer = 2.3f + (float) Math.random() * 0.7f;
        return true;
    }

    /** Kena peluru pemain. Return true bila musuh hancur. */
    public boolean hit() {
        hp--;
        flashTime = 0.12f;
        return hp <= 0;
    }

    public boolean isGone() {
        return x < -24f;
    }

    public void draw(SpriteBatch batch) {
        if (flashTime > 0f) {
            batch.setColor(1f, 0.45f, 0.45f, 1f);
        }
        batch.draw(sharedRegions[type], x - 16f, y - 16f, 32f, 32f);
        batch.setColor(1f, 1f, 1f, 1f);
    }
}
