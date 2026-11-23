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
- **Drone** (ungu, si keroco): cepat & lincah, HP 20, menembak
  beruntun 3x, 100 poin.
- **Gunner** (merah): lambat & tebal, HP 50, tembakan tunggal
  terarah, 250 poin.
- Semua musuh adalah **bot AI**: mereka memilih lawan terdekat — bisa
  kamu, bisa alien lain (sesama alien saling serang!) — lalu menjaga
  jarak tempur sambil menyamping dan menembak sasarannya. Musuh yang
  terluka menampilkan **bar darah + angka sisa HP** di atas pesawatnya.
  Skor hanya didapat dari musuh yang kamu hancurkan sendiri.
- **Semua HP memakai sistem angka** (tidak ada lagi ikon hati), dan
  semua damage adalah angka TETAP yang tercatat — tidak pernah acak:
  peluru (pemain & musuh) **10**, tabrakan badan **20**, roket **20**
  (+ percikan **10**). Darahmu **100**, tampil sebagai bar + tulisan
  "HP 100/100" di kiri atas (bar berubah hijau → kuning → merah).
  Kena serangan = kebal sesaat (berkedip). HP habis = game over, lalu
  mulai lagi sendiri dengan skor tertinggi tersimpan.
- Gelombang makin rapat seiring waktu. Komposisi musuh tetap:
  alien **ungu (drone) 70%**, alien **kuning (gunner) 30%**, dan bot
  yang hidup bersamaan **dibatasi 7**. Di bawah bar HP ada tulisan
  **"TOTAL HIDUP: N"** — jumlah semua yang masih hidup, termasuk
  pesawatmu. Font HUD dihaluskan (filter Linear) supaya tidak lagi
  tampil berpiksel.

## Kendali

- **Joystick** tetap di kiri bawah (sentuh + geser) untuk bergerak bebas;
  pesawat berbelok mengikuti arah gerakmu.
- **Tombol tembak** di kanan bawah: tahan untuk menembak terus; tembakan
  keluar ke **arah hadap pesawat**.
- **Tombol NOS** (oranye, ikon petir) tepat di atas tombol tembak:
  ledakan kecepatan **2,1x selama 1,6 detik** dengan jejak cahaya di
  ekor pesawat. Setelah dipakai ada **cooldown 4 detik** — tombol
  tertutup sapuan gelap dan menampilkan angka detik yang tersisa.
- **Tombol roket / RKT** (merah, ikon roket) di kiri tombol tembak:
  meluncurkan **roket pemburu auto-aim** — mengunci musuh terdekat dan
  mengejarnya sambil berbelok, dengan jejak asap. Ledakannya besar:
  sasaran utama kena 2 damage (drone langsung hancur) dan musuh dalam
  radius percikan kena 1 damage. Bila sasarannya gugur di tengah jalan,
  roket mengunci musuh terdekat lain. **Cooldown 5 detik** dengan
  indikator sapuan + angka detik seperti tombol NOS.
- Di desktop: WASD / tombol panah untuk bergerak, SPASI untuk menembak,
  SHIFT untuk NOS, E untuk roket.

## Penampil error (anti force close diam-diam)

- Error di dalam game (create/render) ditangkap dan **ditampilkan di
  layar game** sebagai teks merah berisi jejak error lengkap — game
  berhenti, tapi aplikasi tidak mati. Foto layar itu untuk diagnosis.
- Error fatal di luar game ditangkap `MainActivity`, disimpan ke
  berkas, lalu ditampilkan `ErrorActivity` (bisa disalin; tombol
  "Hapus & Coba Lagi" untuk mencoba main lagi).

## Catatan teknis

- Semua sprite (pesawat, drone, gunner) digambar **prosedural dari
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
