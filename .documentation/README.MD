# FactoryCore Plugin

**Version:** 1.21.4  
**Author:** aithor  
**API Version:** 1.21

Plugin ekonomi berbasis pabrik dengan sistem produksi dan perdagangan yang kompleks untuk Minecraft server.

## 📋 Deskripsi

FactoryCore adalah plugin Spigot/Paper yang menciptakan ekonomi berbasis manufaktur di mana pemain harus mengumpulkan sumber daya mentah, memproses melalui berbagai pabrik, dan menjual produk jadi. Sistem Machine Parts menciptakan siklus permintaan konstan yang mendorong spesialisasi dan perdagangan antar pemain.

## 🔧 Dependencies

### Required (Wajib):
- Spigot/Paper API 1.21.4
- Vault
- WorldEdit
- WorldGuard

### Optional (Opsional):
- PlaceholderAPI (untuk placeholder support)

## 📦 Instalasi

1. Download plugin FactoryCore.jar
2. Letakkan file .jar di folder `plugins/` server Anda
3. Pastikan dependencies sudah terinstall (Vault, WorldEdit, WorldGuard)
4. Restart server
5. Konfigurasi plugin di folder `plugins/FactoryCore/`

## 🏭 Jenis Pabrik

### 1. Steel Mill (Pabrik Baja)
- **Input:** Raw Resources, Fuel, Machine Parts
- **Output:** Advanced Resources
- **Fungsi:** Mengubah raw resources menjadi advanced resources

### 2. Refinery (Kilang)
- **Input:** Raw Resources, Machine Parts
- **Output:** Fuel
- **Fungsi:** Memproduksi bahan bakar untuk semua pabrik

### 3. Workshop (Pabrik Machine Parts)
- **Input:** Raw Material, Redstone, Fuel, Machine Parts
- **Output:** Machine Parts
- **Fungsi:** Memproduksi suku cadang mesin yang dikonsumsi SEMUA pabrik

### 4. Advanced Factory
- **Input:** Advanced Material, Fuel, Machine Parts
- **Output:** Final Resources
- **Fungsi:** Memproduksi produk bernilai tinggi untuk dijual

## 📝 Commands

### User Commands:
```
/factorycore help - Menampilkan bantuan
/factorycore version - Menampilkan versi plugin
/factorycore buy <factory_id> - Membeli factory
/factorycore sell <factory_id> - Menjual factory (50% harga beli)
/factorycore info <factory_id> - Melihat info factory
```

### Admin Commands:
```
/factorycore admin create <region> <type> <id> <price> - Membuat factory baru
/factorycore admin remove <factory_id> - Menghapus factory
/factorycore admin list - Melihat semua factory
/factorycore admin info <factory_id> - Melihat detail factory
/factorycore admin reload - Reload plugin configuration
/factorycore admin setowner <factory_id> <player> - Set pemilik factory
/factorycore admin teleport <factory_id> - Teleport ke factory
/factorycore admin give <resource_id> <amount> <player> - Memberi resource
/factorycore admin npc spawn <npc_id> - Spawn NPC employee
/factorycore admin npc remove <npc_id> - Remove NPC employee
```

**Alias:** `/fc` dapat digunakan sebagai pengganti `/factorycore`

## 🎯 Permissions

### Admin Permissions:
- `factorycore.admin` - Akses semua admin commands
- `factorycore.admin.create` - Membuat factory
- `factorycore.admin.remove` - Menghapus factory
- `factorycore.admin.list` - Melihat daftar factory
- `factorycore.admin.info` - Melihat info factory
- `factorycore.admin.reload` - Reload plugin
- `factorycore.admin.setowner` - Set pemilik factory
- `factorycore.admin.teleport` - Teleport ke factory
- `factorycore.admin.give` - Memberi resource
- `factorycore.admin.npc` - Manage NPCs

### User Permissions:
- `factorycore.use` - Akses command user (default: true)
- `factorycore.buy` - Membeli factory (default: true)
- `factorycore.sell` - Menjual factory (default: true)
- `factorycore.info` - Melihat info factory (default: true)
- `factorycore.help` - Melihat help (default: true)
- `factorycore.version` - Melihat versi (default: true)

## 🎮 Cara Menggunakan

### Untuk Admin:

1. **Buat Region dengan WorldGuard:**
   ```
   //pos1
   //pos2
   /region define factory_area_1
   ```

2. **Buat Factory:**
   ```
   /fc admin create factory_area_1 steel_mill factory1 100000
   ```

3. **Spawn NPC Employee (opsional):**
   ```
   /fc admin npc spawn factory1
   ```

### Untuk Player:

1. **Beli Factory:**
   ```
   /fc buy factory1
   ```

2. **Interaksi dengan NPC:**
   - Klik kanan pada NPC employee untuk membuka Factory Panel

3. **Gunakan Factory Panel:**
   - **Start Production:** Pilih resep dan mulai produksi
   - **Invoice Management:** Bayar pajak dan gaji pegawai
   - **Factory Storage:** Akses storage factory
   - **Upgrade Factory:** Tingkatkan level factory
   - **Fast Travel:** Teleport ke lokasi factory

## 💰 Sistem Ekonomi

### Pajak (Tax):
- Rate: 5% dari harga factory (default)
- Interval: Setiap 3 hari Minecraft
- Bertambah 2.5% setiap level factory

### Gaji Pegawai (Salary):
- Rate: 1% dari harga factory (default)
- Interval: Setiap 24 jam Minecraft

### Harga Jual Factory:
- 50% dari harga beli (default)

## 📊 Sistem Level

Factory dapat di-upgrade hingga level 5 (default):

**Bonus per Level:**
- Pengurangan biaya produksi: 5%
- Pengurangan waktu produksi: 10%
- Tambahan slot storage: 9 slots
- Pajak tambahan: 2.5%

**Biaya Upgrade:**
- Level 2: 50% harga factory
- Level 3: 100% harga factory (level 2 × 50%)
- Dan seterusnya...

## 🔨 Jenis Item Resources

### 1. Raw Resources
- Iron Ore
- Coal Ore
- Copper Ore
- Gold Ore
- Diamond Ore
- Redstone Dust

### 2. Advanced Resources
- Steel Ingot
- Refined Copper
- Gold Alloy
- Carbon Fiber

### 3. Final Resources
- Advanced Weapon ($5,000)
- Advanced Armor ($4,500)
- Electronic Component ($3,000)
- Luxury Item ($10,000)

### 4. Fuel
- Basic Fuel
- Refined Fuel
- Premium Fuel

### 5. Machine Parts
- Basic Gear
- Advanced Gear
- Engine Part
- Circuit Board
- Hydraulic System

## 📱 PlaceholderAPI Support

Plugin mendukung PlaceholderAPI dengan placeholder berikut:

### General Placeholders:
- `%factorycore_factories_owned%` - Jumlah factory yang dimiliki
- `%factorycore_factories_running%` - Jumlah factory yang sedang produksi
- `%factorycore_total_value%` - Total nilai semua factory
- `%factorycore_highest_level%` - Level factory tertinggi

### Invoice Placeholders:
- `%factorycore_invoices_pending%` - Jumlah invoice pending
- `%factorycore_invoices_overdue%` - Jumlah invoice jatuh tempo
- `%factorycore_total_invoice_amount%` - Total nilai invoice

### Factory Type Placeholders:
- `%factorycore_steel_mills_owned%` - Jumlah Steel Mill
- `%factorycore_refineries_owned%` - Jumlah Refinery
- `%factorycore_workshops_owned%` - Jumlah Workshop
- `%factorycore_advanced_factories_owned%` - Jumlah Advanced Factory

### Specific Factory Placeholders:
- `%factorycore_factory_<id>_status%` - Status factory
- `%factorycore_factory_<id>_level%` - Level factory
- `%factorycore_factory_<id>_type%` - Tipe factory
- `%factorycore_factory_<id>_production%` - Produksi saat ini
- `%factorycore_factory_<id>_progress%` - Progress produksi (%)

## 📁 File Konfigurasi

### config.yml
Konfigurasi utama plugin (database, tax, salary, factory settings)

### resources.yml
Definisi semua item resources yang digunakan

### recipes.yml
Resep produksi untuk setiap factory type

### language.yml
Pesan dan bahasa yang dapat dikustomisasi

### npc.yml
Konfigurasi NPC employees

## 📝 Logging System

Plugin mencatat aktivitas berikut:
- Admin commands
- Transaksi ekonomi
- Produksi factory
- Invoice generation

Log disimpan di: `plugins/FactoryCore/logs/factorycore.log`

## 🐛 Troubleshooting

### Factory tidak bisa dibeli
- Pastikan Vault dan economy plugin terinstall
- Cek balance player mencukupi
- Pastikan factory belum dimiliki player lain

### NPC tidak spawn
- Pastikan chunk dimana NPC spawn sudah loaded
- Cek console untuk error messages
- Restart server dan coba spawn ulang

### Produksi tidak berjalan
- Cek apakah bahan mencukupi di storage
- Pastikan factory status RUNNING
- Cek console untuk error

### Invoice tidak muncul
- Pastikan scheduler berjalan (cek config interval)
- Cek apakah factory memiliki owner
- Restart server jika diperlukan

## 🔄 Update & Migration

Untuk update plugin:
1. Backup folder `plugins/FactoryCore/`
2. Replace file .jar lama dengan yang baru
3. Restart server
4. Cek console untuk migration messages

## 📞 Support

Jika mengalami masalah atau bug:
1. Cek console error messages
2. Cek log file di `plugins/FactoryCore/logs/`
3. Pastikan dependencies up-to-date
4. Contact developer dengan detail error

## 📄 License

Copyright © 2024 aithor. All rights reserved.

## 🎉 Credits

- Developer: aithor
- API Version: 1.21.4
- Dependencies: Vault, WorldEdit, WorldGuard, PlaceholderAPI

---

**Terima kasih telah menggunakan FactoryCore!** 🏭