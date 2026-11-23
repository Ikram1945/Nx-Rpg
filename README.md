# Space War

Game **pertempuran luar angkasa** 2D berbasis **libGDX 1.14.2**, dibangun
sebagai proyek ApkBuild/CodeAssist satu modul (paket `com.ikram.nexus`).
Proyek ini awalnya bernama Nx Rpg (dulu RPG desa top-down); pada
2026-11-23 dirombak total menjadi penembak luar angkasa, lalu berganti
nama menjadi **Space War**. Berkas peta desa lama tidak lagi dipakai,
tapi masih tersimpan di `assets/`.

## Cara bermain

- Arena pertempuran adalah **peta besar 1280x720** (16x luas layar) —
  tidak terpotong: **kamera mengikuti pesawatmu** ke segala arah, peta
  dibatasi garis perbatasan biru menyala + kisi samar, dan ada **peta
  mini** di tengah atas yang menunjukkan seluruh peta, area pandang
  kamera, posisimu (biru muda) dan posisi musuh (merah muda/merah).
- Pesawatmu melawan gelombang alien yang datang tanpa henti; musuh
  muncul tepat di luar pandangan kameramu dari tepi mana pun.
- Semua pesawat — pemain maupun alien — **bisa berbelok ke segala
  arah**: badannya berputar mengikuti arah terbangnya, tidak terkunci
  menghadap satu sisi.
- **Drone** (ungu, si keroco): cepat & lincah, 2 darah, menembak
  beruntun 3x, 100 poin.
- **Gunner** (merah): lambat & tebal, 5 darah, tembakan tunggal
  terarah, 250 poin.
- Semua musuh adalah **bot AI**: mereka memilih lawan terdekat — bisa
  kamu, bisa alien lain (sesama alien saling serang!) — lalu menjaga
  jarak tempur sambil menyamping dan menembak sasarannya. Musuh yang
  terluka menampilkan bar darah di atas pesawatnya. Skor hanya didapat
  dari musuh yang kamu hancurkan sendiri.
- Darahmu 3 hati. Kena peluru/tabrakan = -1 hati + kebal sesaat (berkedip).
  Hati habis = game over, lalu mulai lagi sendiri dengan skor tertinggi
  tersimpan.
- Gelombang makin rapat seiring waktu; gunner mulai muncul setelah
  ±18 detik.

## Kendali

- **Joystick** tetap di kiri bawah (sentuh + geser) untuk bergerak bebas;
  pesawat berbelok mengikuti arah gerakmu.
- **Tombol tembak** di kanan bawah: tahan untuk menembak terus; tembakan
  keluar ke **arah hadap pesawat**.
- **Tombol NOS** (oranye, ikon petir) tepat di atas tombol tembak:
  ledakan kecepatan **2,1x selama 1,6 detik** dengan jejak cahaya di
  ekor pesawat. Setelah dipakai ada **cooldown 4 detik** — tombol
  tertutup sapuan gelap dan menampilkan angka detik yang tersisa.
- Di desktop: WASD / tombol panah untuk bergerak, SPASI untuk menembak,
  SHIFT untuk NOS.

## Penampil error (anti force close diam-diam)

- Error di dalam game (create/render) ditangkap dan **ditampilkan di
  layar game** sebagai teks merah berisi jejak error lengkap — game
  berhenti, tapi aplikasi tidak mati. Foto layar itu untuk diagnosis.
- Error fatal di luar game ditangkap `MainActivity`, disimpan ke
  berkas, lalu ditampilkan `ErrorActivity` (bisa disalin; tombol
  "Hapus & Coba Lagi" untuk mencoba main lagi).

## Catatan teknis

- Semua sprite (pesawat, drone, gunner, hati) digambar **prosedural dari
  kode** (Pixmap -> Texture) sebagai gambar vektor halus — poligon + elips
  di kanvas besar lalu diperkecil bilinear — tanpa berkas gambar, karena
  rantai build di HP terbukti merusak tekstur PNG karakter.
- Latar bintang paralaks 3 lapis + nebula lembut yang bergeser mengikuti
  gerak kamera; ledakan partikel; kilat merah saat kena.
- Berkas sumber (paket `com.ikram.nexus`):
  - `SpaceWarGame.java` — lingkup game, AI bot, tabrakan, HUD.
  - `Player.java` — pesawat pemain. `Enemy.java` — alien + mesin tembak.
  - `SpriteArt.java` — perkakas gambar vektor bersama + putaran sudut.
  - `ErrorScreen.java` — penampil error di dalam game.
  - `MainActivity.java` / `ErrorActivity.java` — pintu masuk Android +
    penangkap & penampil error fatal.
  - `NxRpgGame.java` — berkas kosong sisa ganti nama (boleh dihapus).
- Aset yang masih dipakai: tekstur joystick `assets/ui/joy_base.png` dan
  `assets/ui/joy_knob.png`.
