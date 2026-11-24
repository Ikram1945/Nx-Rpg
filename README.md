# Space War

Game **pertempuran luar angkasa** 2D berbasis **libGDX 1.14.2**, dibangun
sebagai proyek ApkBuild/CodeAssist satu modul (paket `com.ikram.nexus`).
Proyek ini awalnya bernama Nx Rpg (dulu RPG desa top-down); pada
2026-11-23 dirombak total menjadi penembak luar angkasa, lalu berganti
nama menjadi **Space War**. Berkas peta desa lama tidak lagi dipakai,
tapi masih tersimpan di `assets/`.

## Menu utama, koin, toko & upgrade

Game **tidak lagi langsung bermain** saat dibuka — yang tampil
lebih dulu adalah **menu utama**: judul SPACE WAR, gambar besar
pesawat pemain, saldo **koin**, dan tombol **MULAI PERMAINAN**,
**TOKO**, **UPGRADE PLAYER**, **UPGRADE PARTNER**. Game over pun
sekarang kembali ke menu utama (tidak lagi mulai sendiri).

- **Koin** didapat dari setiap musuh yang kamu hancurkan: drone
  **10**, gunner **25**, kapal induk **500**. Saldo koin tampil di
  menu dan di HUD selama bermain (di bawah TOTAL HIDUP), dan
  tersimpan permanen bersama level upgrade & stok barang.
- **Upgrade Player** (permanen, maks Lv.5, harga naik tiap level):
  Badan Kuat (darah maks +50), Pemicu Cepat (tembakan 12% lebih
  cepat), Mesin Kencang (terbang 8% lebih cepat).
- **Upgrade Partner** (permanen, maks Lv.5): Badan Partner (darah
  +25), Senjata Partner (damage peluru +5), Pemicu Partner
  (tembakan 12% lebih cepat).
- **Toko** (barang sekali pakai, dipakai otomatis di awal
  permainan berikutnya): Perisai Awal 150 (kebal 8 detik),
  Nyawa Cadangan 300 (hidup lagi sekali dengan setengah darah),
  Booster Roket 100 (jeda roket separuh).

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
- **Alien Roket** (hijau): HP 35, menjaga jarak dan menembakkan
  **mini roket auto-aim** — roket pengejar yang berbelok memburu
  sasarannya (damage **15**) setiap ±3 detik, maks 2 roket aktif
  per penembak. 175 poin, 18 koin.
- **Alien Roket ELITE** (hijau gelap, besar, kubah emas): HP 120,
  jarang muncul (5%). Menembakkan **ROKET BESAR**: damage **35**,
  meledak beradius (percikan **15**), lebih cepat dan lebih lincah
  memburu daripada mini roket, dengan jeda serangan singkat
  (1,6-2,2 detik, maks 3 roket aktif) — tekanannya terasa terus.
  400 poin, 45 koin.
- **Kapal induk** (boss raksasa): hanya **SATU dalam setiap
  permainan**, muncul sekali setelah ±45 detik dengan peringatan
  "KAPAL INDUK MENDEKAT!". HP **500**, 2000 poin. Senjatanya:
  **laser** berkala (ada garis bidik ancang-ancang 0,8 detik —
  hindari garisnya!), **bom tekanan tinggi** beradius ledak besar,
  dan **NOS super** untuk menerjang lurus ke arahmu. Damage-nya
  tetap angka tercatat: laser **20**, bom **20**.
- **Partner** (pesawat teman berwarna hijau toska): AI yang
  mengikutimu dengan bebas — berputar mengelilingimu saat tenang,
  dan **menerjang** musuh yang masuk jangkauan sergapnya, mengorbit
  dekat sambil **menembak otomatis**. Membasmi musuh bersamamu, dan
  bunuhannya ikut jadi skormu. HP **50**; bila hancur, ia muncul
  lagi di sisimu setelah 6 detik. (Rencana masa depan: partner bisa
  dipasang/dilepas; sekarang ia selalu ikut.)
- Semua musuh adalah **bot AI**: mereka memilih lawan terdekat — bisa
  kamu, bisa alien lain (sesama alien saling serang!) — lalu menjaga
  jarak tempur sambil menyamping dan menembak sasarannya. Musuh yang
  terluka menampilkan **bar darah + angka sisa HP** di atas pesawatnya.
  Skor hanya didapat dari musuh yang kamu hancurkan sendiri.
- **Semua HP memakai sistem angka** (tidak ada lagi ikon hati), dan
  semua damage adalah angka TETAP yang tercatat — tidak pernah acak:
  peluru (pemain & musuh) **10**, tabrakan badan **20**, roket **20**
  (+ percikan **10**), mini roket alien **15**, roket besar alien
  elite **35** (+ percikan **15**). Darahmu **200**,
  tampil sebagai bar + tulisan
  "HP 200/200" di kiri atas (bar berubah hijau → kuning → merah).
  Kena serangan = kebal sesaat (berkedip). HP habis = game over, lalu
  kembali ke menu utama dengan skor tertinggi tersimpan.
- Gelombang makin rapat seiring waktu. Komposisi musuh tetap:
  alien **ungu (drone) 50%**, alien **kuning (gunner) 30%**, alien
  **hijau (roket) 15%**, alien **elite 5%**, dan bot
  yang hidup bersamaan **dibatasi 30** — puluhan musuh di arena
  sekaligus. Mereka datang bergerombol (3-5 per kedatangan, makin
  sering seiring waktu) dan 14 langsung menyambut di awal
  permainan, jadi arena ramai sejak detik pertama. Di bawah bar HP ada tulisan
  **"TOTAL HIDUP: N"** — jumlah semua yang masih hidup, termasuk
  pesawatmu. Font HUD dihaluskan (filter Linear) supaya tidak lagi
  tampil berpiksel.

## Kendali

- **Joystick** tetap di kiri bawah (sentuh + geser) untuk bergerak bebas;
  pesawat berbelok mengikuti arah gerakmu.
- **Tombol tembak** di kanan bawah: tahan untuk menembak terus;
  tembakanmu **auto-aim** — peluru mengarah sendiri ke musuh hidup
  terdekat dalam jangkauan (bila tidak ada musuh, peluru meluncur
  lurus ke arah hadap pesawat).
- **Tombol NOS** (oranye, ikon petir) tepat di atas tombol tembak:
  ledakan kecepatan **2,1x selama 1,6 detik** dengan jejak cahaya di
  ekor pesawat. Setelah dipakai ada **cooldown 4 detik** — tombol
  tertutup sapuan gelap dan menampilkan angka detik yang tersisa.
- **Tombol roket / RKT** (merah, ikon roket) di kiri tombol tembak:
  meluncurkan **roket pemburu auto-aim** — mengunci musuh terdekat dan
  mengejarnya sambil berbelok, dengan jejak asap. Ledakannya besar:
  sasaran utama kena 20 damage (drone langsung hancur) dan musuh dalam
  radius percikan kena 10 damage. Bila sasarannya gugur di tengah jalan,
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
