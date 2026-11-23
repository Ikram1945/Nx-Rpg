package com.ikram.nexus;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * Layar penampil error: menunjukkan laporan error (jejak tumpukan)
 * yang disimpan MainActivity saat game mengalami error fatal, lengkap
 * dengan tombol menyalin laporan dan tombol mencoba main lagi.
 * Tampilan dibangun dari kode (tanpa berkas layout XML).
 */
public class ErrorActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String teks = bacaLaporan();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

        TextView judul = new TextView(this);
        judul.setText("Space War - Terjadi Error");
        judul.setTextSize(20);
        root.addView(judul);

        TextView petunjuk = new TextView(this);
        petunjuk.setText("Salin laporan di bawah ini lalu kirim ke pengembang:");
        petunjuk.setPadding(0, 12, 0, 12);
        root.addView(petunjuk);

        TextView isi = new TextView(this);
        isi.setText(teks);
        isi.setTextIsSelectable(true);
        isi.setTypeface(Typeface.MONOSPACE);
        isi.setTextSize(12);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(isi);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout baris = new LinearLayout(this);
        baris.setOrientation(LinearLayout.HORIZONTAL);
        baris.setPadding(0, 16, 0, 0);

        Button salin = new Button(this);
        salin.setText("Salin Error");
        salin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("Space War error", teks));
                }
                Toast.makeText(ErrorActivity.this,
                        "Error tersalin - tempel & kirim ke pengembang",
                        Toast.LENGTH_LONG).show();
            }
        });
        baris.addView(salin);

        Button lagi = new Button(this);
        lagi.setText("Hapus & Coba Lagi");
        lagi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new File(getFilesDir(), MainActivity.CRASH_FILE).delete();
                startActivity(new Intent(ErrorActivity.this, MainActivity.class));
                finish();
            }
        });
        baris.addView(lagi);

        root.addView(baris);
        setContentView(root);
    }

    private String bacaLaporan() {
        File f = new File(getFilesDir(), MainActivity.CRASH_FILE);
        if (!f.exists()) {
            return "(Tidak ada laporan error tersimpan.)";
        }
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(f), "UTF-8"));
            String baris;
            while ((baris = br.readLine()) != null) {
                sb.append(baris).append('\n');
            }
        } catch (Exception e) {
            return "(Laporan error tidak bisa dibaca: " + e + ")";
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception abaikan) {
                    // Abaikan
                }
            }
        }
        return sb.toString();
    }
}
