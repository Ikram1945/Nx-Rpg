package com.ikram.nexus;

import android.content.Intent;
import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Pintu masuk Android: menjalankan game libGDX (NxRpgGame).
 * Dilengkapi penangkap error menyeluruh: error apa pun yang lolos dari
 * penampil error di dalam game akan disimpan ke berkas lalu ditampilkan
 * oleh ErrorActivity — bukan force close diam-diam.
 */
public class MainActivity extends AndroidApplication {

    /** Nama berkas laporan error terakhir (di penyimpanan internal aplikasi). */
    public static final String CRASH_FILE = "nx-space-crash.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pasangPenangkapError();

        // Ada laporan error dari kejadian sebelumnya yang belum ditampilkan?
        File sisa = getFileStreamPath(CRASH_FILE);
        if (sisa != null && sisa.exists()) {
            startActivity(new Intent(this, ErrorActivity.class));
            finish();
            return;
        }

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true;
        config.useAccelerometer = false;
        config.useCompass = false;
        initialize(new NxRpgGame(), config);
    }

    private void pasangPenangkapError() {
        final Thread.UncaughtExceptionHandler bawaan =
                Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable error) {
                try {
                    StringWriter sw = new StringWriter();
                    error.printStackTrace(new PrintWriter(sw));
                    String teks = "Nx Space - laporan error\n\n" + sw.toString();
                    FileOutputStream fos = openFileOutput(CRASH_FILE, MODE_PRIVATE);
                    try {
                        fos.write(teks.getBytes("UTF-8"));
                    } finally {
                        fos.close();
                    }
                    Intent i = new Intent(getApplicationContext(), ErrorActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                } catch (Throwable abaikan) {
                    if (bawaan != null) bawaan.uncaughtException(thread, error);
                }
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
        });
    }
}
