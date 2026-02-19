# Dokumentasi Sistem Factory Core

Dokumen ini memberikan penjelasan teknis dan komprehensif mengenai **Factory Core Plugin**, sebuah sistem manajemen pabrik yang kompleks untuk server Minecraft.

## 1. Pendahuluan
Factory Core adalah sistem yang memungkinkan pemain memiliki, mengelola, dan mengoperasikan pabrik fisik di dalam permainan. Sistem ini mengintegrasikan mekanisme ekonomi, manajemen sumber daya, dan progresi level untuk menciptakan pengalaman gameplay industri yang mendalam.

## 2. Arsitektur Sistem

### 2.1 Entitas Utama (Factory Model)
Setiap pabrik dalam sistem merepresentasikan sebuah entitas dengan properti berikut:
- **ID & Nama**: Identifikasi unik untuk sistem dan tampilan.
- **Region**: Terikat dengan wilayah **WorldGuard** untuk proteksi area.
- **Tipe (FactoryType)**: Menentukan jenis produk yang dapat dibuat (contoh: *Electronics*, *Food*, *Refinery*).
- **Pemilik (Owner)**: UUID pemain yang memiliki hak akses penuh.
- **Level**: Tingkat kemajuan pabrik yang mempengaruhi efisiensi.
- **Status**: Kondisi operasional saat ini (`STOPPED`, `RUNNING`).

### 2.2 Sistem Penyimpanan (Storage System)
Setiap pabrik memiliki dua kontainer penyimpanan terpisah yang dikelola oleh `StorageManager`:

1.  **Input Storage**:
    - Tempat penyimpanan bahan baku (*raw materials*).
    - Pemain harus menyetorkan (*deposit*) item ke sini sebelum produksi dapat dimulai.
    - Mendukung fitur *Deposit All* untuk kemudahan transfer item.

2.  **Output Storage**:
    - Tempat penampungan hasil produksi (*finished goods*).
    - Produk otomatis masuk ke sini setelah proses produksi selesai.
    - Pemain dapat mengambil (*withdraw*) hasil produksi kapan saja.
    - Kapasitas penyimpanan dapat bertambah seiring kenaikan level pabrik.

### 2.3 Manajemen Karyawan (Employee System)
Sistem ini mewajibkan keberadaan tenaga kerja virtual (NPC):
- **Syarat Wajib**: Produksi **TIDAK** dapat dimulai tanpa adanya karyawan yang ditugaskan pada pabrik tersebut.
- **Buff Produksi**: Karyawan dapat memberikan bonus efisiensi berupa pengurangan durasi produksi (dalam persentase).

## 3. Mekanisme Operasional

### 3.1 Siklus Produksi (Production Cycle)
Proses produksi berjalan melalui tahapan sekuensial yang ketat:

1.  **Inisiasi (Start)**:
    - Pemain memilih **Resep (Recipe)** melalui GUI.
    - Sistem memvalidasi keberadaan **Karyawan**.
    - Sistem memvalidasi ketersediaan bahan baku di **Input Storage**.
    
2.  **Konsumsi & Proses (Processing)**:
    - Bahan baku dikurangi dari *Input Storage*.
    - Status pabrik berubah menjadi `RUNNING`.
    - Durasi produksi dihitung dengan rumus:
      $$ \text{Waktu Akhir} = \text{Waktu Dasar} \times (1 - \text{Bonus Level}) \times (1 - \text{Buff Karyawan}) $$
    - **BossBar** ditampilkan kepada pemilik untuk melacak progres secara real-time.

3.  **Penyelesaian (Completion)**:
    - Saat durasi habis, status kembali menjadi `STOPPED`.
    - Produk ditambahkan ke **Output Storage**.
    - Command console (jika dikonfigurasi pada resep) dieksekusi.
    - Notifikasi suara dan pesan dikirim ke pemilik.

### 3.2 Peningkatan Pabrik (Upgrades)
Pemain dapat meningkatkan level pabrik dengan membayar biaya upgrade.
- **Biaya**: Dihitung secara progresif (contoh: $HargaDasar \times 0.5 \times Level$).
- **Manfaat**:
    - Peningkatan kapasitas penyimpanan.
    - Pengurangan waktu produksi secara permanen.

### 3.3 Fast Travel
Setiap pabrik memiliki titik teleportasi (`Fast Travel Location`) yang dapat diakses oleh pemilik untuk berpindah tempat dengan cepat ke lokasi pabrik.

## 4. Struktur Data & Persistensi
Data disimpan menggunakan format YAML dalam folder `plugins/FactoryCore/data/`:

- **factories.yml**: Menyimpan data utama pabrik (lokasi, level, pemilik, status).
- **input-storage.yml**: Menyimpan data inventaris bahan baku per pabrik.
- **output-storage.yml**: Menyimpan data inventaris hasil produksi per pabrik.

## 5. Ringkasan Fitur Teknis
| Fitur | Deskripsi |
| :--- | :--- |
| **Region Binding** | Terintegrasi dengan WorldGuard untuk keamanan area. |
| **Dual Storage** | Pemisahan logis antara bahan mentah dan produk jadi. |
| **Asynchronous** | Proses produksi berjalan di latar belakang tanpa memblokir server. |
| **Economy** | Terintegrasi dengan Vault untuk transaksi jual/beli dan upgrade. |
| **Smart GUI** | Antarmuka interaktif untuk manajemen resep dan inventaris. |

---
*Dokumentasi ini disusun untuk tujuan teknis dan referensi pengembangan sistem Factory Core.*
