# Nx Space (sebelumnya Nx RPG)

Game **pertempuran luar angkasa** 2D berbasis **libGDX 1.14.2**, dibangun
sebagai proyek ApkBuild/CodeAssist satu modul (paket `com.ikram.nexus`).
Proyek ini awalnya RPG desa top-down; pada 2026-11-23 dirombak total
menjadi penembak luar angkasa (berkas peta desa lama tidak lagi dipakai,
tapi masih tersimpan di `assets/`).

## Cara bermain

- Pesawatmu di sisi kiri layar; alien datang dari kanan tanpa henti.
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

- **Joystick** tetap di kiri bawah (sentuh + geser) untuk bergerak bebas.
- **Tombol tembak** di kanan bawah: tahan untuk menembak terus.
- Di desktop: WASD / tombol panah untuk bergerak, SPASI untuk menembak.

## Catatan teknis

- Semua sprite (pesawat, drone, gunner, hati) digambar **prosedural dari
  kode** (Pixmap -> Texture) sebagai gambar vektor halus — poligon + elips
  di kanvas besar lalu diperkecil bilinear — tanpa berkas gambar, karena
  rantai build di HP terbukti merusak tekstur PNG karakter.
- Latar bintang paralaks 3 lapis; ledakan partikel; kilat merah saat kena.
- Tiga berkas sumber utama: `NxRpgGame.java` (lingkup game + HUD),
  `Player.java` (pesawat pemain), `Enemy.java` (alien).
- Aset yang masih dipakai: tekstur joystick `assets/ui/joy_base.png` dan
  `assets/ui/joy_knob.png`.
