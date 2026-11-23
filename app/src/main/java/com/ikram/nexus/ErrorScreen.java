package com.ikram.nexus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

/**
 * Penampil error di dalam game (Space War).
 * Menangkap error, menyimpan jejak lengkapnya (log + berkas), lalu
 * menggambarnya sebagai layar merah "TERJADI ERROR" — game berhenti,
 * tapi aplikasi TIDAK mati, supaya errornya bisa dibaca, difoto, dan
 * dikirim ke pengembang. Dipakai oleh SpaceWarGame; perlengkapan
 * gambarnya (uiBatch, font, kamera layar, tekstur putih) disiapkan
 * oleh game sebelum bagian game lain dibuat.
 */
public class ErrorScreen {

    private String errorText;
    private final ArrayList<String> lines = new ArrayList<>();

    public boolean isActive() {
        return errorText != null;
    }

    /** Tangkap & siapkan error untuk ditampilkan. */
    public void showError(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        errorText = sw.toString();
        try {
            Gdx.app.error("SpaceWar", "Error tertangkap:\n" + errorText);
        } catch (Throwable abaikan) {
            // Log bisa gagal bila app belum siap; abaikan
        }
        try {
            Gdx.files.local("space-war-error.txt").writeString(errorText, false);
        } catch (Throwable abaikan) {
            // Penyimpanan cadangan gagal pun tak boleh mematikan game
        }
        // Bungkus baris panjang agar muat selebar layar
        lines.clear();
        int perBaris = Math.max(40, (Gdx.graphics.getWidth() - 32) / 8);
        for (String mentah : errorText.split("\n")) {
            String baris = mentah.replace("\t", "    ").replace("\r", "");
            while (baris.length() > perBaris) {
                lines.add(baris.substring(0, perBaris));
                baris = "  " + baris.substring(perBaris);
            }
            lines.add(baris);
        }
    }

    /** Gambar layar error memenuhi layar. */
    public void draw(SpriteBatch uiBatch, BitmapFont font,
                     OrthographicCamera screenCam, Texture whiteTex) {
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
        for (int i = 0; i < lines.size() && i < maxLines; i++) {
            font.draw(uiBatch, lines.get(i), 16, y);
            y -= 15f;
        }
        font.getData().setScale(1f);
        font.setColor(1f, 0.85f, 0.45f, 1f);
        font.draw(uiBatch, "Foto layar ini, lalu kirim ke pengembang.", 16, 26);
        font.setColor(1f, 1f, 1f, 1f);
        uiBatch.end();
    }
}
