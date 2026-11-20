# Nx RPG

Game RPG top-down 2D berbasis **libGDX 1.14.2**, dibangun sebagai proyek
ApkBuild/CodeAssist satu modul (paket `com.ikram.nexus`).

## Tahap awal — yang sudah ada

- Peta desa 40×30 tile (`app/src/main/assets/maps/village.tmx`, format Tiled):
  jalan tanah bersilang, 3 rumah, kandang berpagar, pohon, jamur, papan nama.
- Karakter pemain: kesatria berpedang 16×20 dengan animasi jalan 4 arah
  (bawah/kiri/atas/kanan).
- Kamera mengikuti pemain dan terjepit di tepi peta.
- Tabrakan (collision) dari layer `collision` di peta: rumah, pohon, pagar,
  dan air tidak bisa ditembus; pemain meluncur di sepanjang dinding.
- Kendali:
  - **Sentuh**: joystick tetap di kiri bawah layar — sentuh di separuh kiri
    layar lalu geser, knob mengikuti arah geser.
  - **Serang**: tombol pedang di kanan bawah layar — kesatria mengayunkan
    pedang ke arah hadapnya (dengan bayangan ayunan).
  - **Keyboard**: WASD atau tombol panah untuk bergerak, spasi untuk menyerang.

## Struktur kode

| Berkas | Isi |
| --- | --- |
| `app/src/main/java/com/ikram/nexus/MainActivity.java` | Launcher Android libGDX |
| `app/src/main/java/com/ikram/nexus/NxRpgGame.java` | Game utama: peta, kamera, input, joystick |
| `app/src/main/java/com/ikram/nexus/Player.java` | Pemain: animasi, gerak, tabrakan |

Aset ada di `app/src/main/assets/` (`maps/`, `tilesets/`, `sprites/`).

## Kredit aset

- Tileset **“Tiny Town”** oleh Kenney (kenney.nl) — lisensi **CC0**
  (bebas pakai, tanpa atribusi wajib). Lisensi lengkap di
  `app/src/main/assets/tilesets/kenney-license.txt`.
- Sprite kesatria di `app/src/main/assets/sprites/player.png` dan grafis
  joystick di `app/src/main/assets/ui/` digambar khusus untuk proyek ini
  (peta piksel dari kode), bebas dipakai tanpa atribusi.

## Rencana tahap berikutnya

- NPC desa + dialog
- Pintu rumah bisa dimasuki (peta interior)
- Musuh + sistem bertarung sederhana
- Musik latar + efek suara
- Urutan gambar berdasarkan sumbu Y (pemain bisa lewat di belakang pohon)
