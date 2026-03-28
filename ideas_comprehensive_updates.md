# 🏭 FactoryCore — Ide Update & Fitur Baru (Komprehensif)
*Berdasarkan analisis penuh kode v1.21.4 — Maret 2026*

---

## 📑 Daftar Isi
1. [🎮 Gameplay & Ekonomi Baru](#gameplay)
2. [⚙️ Sistem Produksi Lanjutan](#produksi)
3. [🖥️ GUI & UX Overhaul](#gui)
4. [🔌 Integrasi Plugin Eksternal](#integrasi)
5. [👥 Sistem Sosial & Multiplayer](#sosial)
6. [🌍 Dunia & Lingkungan](#dunia)
7. [📊 Analytics & Statistik](#analytics)
8. [🛡️ Admin & Developer Tools](#admin)
9. [🚀 Long-term Vision](#longterm)
10. [📋 Tabel Prioritas Master](#prioritas)

---

## 🎮 Gameplay & Ekonomi Baru {#gameplay}

### 1. 📈 **Dynamic Market Pricing** *(Mega Feature)*
**Konsep:** Harga `suggested_price` item tidak lagi statis — berfluktuasi berdasarkan supply & demand server.

```
📊 [MARKET TICKER]
Steel Ingot:   $2,400 ▲ +12%  (Low Supply)
Carbon Fiber:  $8,100 ▼ -5%   (High Supply)
Engine Part:   $3,600 → =0%   (Balanced)
```

**Mekanisme:**
- Setiap X jam, harga disesuaikan berdasarkan total transaksi di MarketplaceManager.
- Ada **floor price** dan **ceiling price** yang bisa dikonfigurasi.
- Player bisa melihat grafik harga via `/fc market graph <item>`.
- Notifikasi ke player ketika harga item miliknya naik signifikan.

**Dampak gameplay:** Menciptakan strategi waktu jual, mendorong diversifikasi produksi.

---

### 2. 🏦 **Factory Loans & Bank System**
**Konsep:** Tambahkan sistem pinjaman (loan) yang memungkinkan player meminjam uang dari "Bank NPC" untuk beli/upgrade factory, lalu cicil melalui invoice.

```yaml
loan-system:
  enabled: true
  max-loan-multiplier: 3    # maks 3x total asset value
  interest-rate: 0.05       # 5% per periode
  collateral: factory       # aset dijaminkan
  default-penalty: "factory_seized"  # disita jika gagal bayar
```

**Detail:**
- Loan dihitung otomatis dari nilai factory yang dimiliki.
- Cicilan otomatis dipotong dari revenue produksi.
- Jika gagal bayar N kali, factory disita dan bisa dilelang.
- Integrasi ke `InvoiceManager` sebagai `InvoiceType.LOAN_PAYMENT`.

---

### 3. 🎲 **Factory Auction System**
**Konsep:** Factory yang tidak aktif / disita bisa dilelang secara real-time.

**Fitur:**
- Auction berlangsung 24-48 jam.
- Tawaran real-time via GUI atau command `/fc auction bid <factory_id> <amount>`.
- Notifikasi ke semua bidder ketika ada tawaran baru.
- Pemenang otomatis mendapat ownership setelah timer habis.
- Integrasi ke `MarketplaceManager` atau sistem terpisah `AuctionManager`.

---

### 4. 📜 **Factory Insurance System**
**Konsep:** Player bisa membeli asuransi untuk factory mereka.

```yaml
insurance:
  monthly-premium: 1000     # biaya per periode
  coverage-percentage: 80   # 80% nilai factory jika dihancurkan/bug
  covers:
    - griefing
    - server_crash_loss
    - production_failure
```

**Detail:**
- Invoice tipe baru `InvoiceType.INSURANCE_PREMIUM`.
- Jika terjadi kehilangan data tak terduga, admin bisa trigger klaim.

---

### 5. 🏆 **Factory Specialization Tree** *(Mega Feature)*
**Konsep:** Setiap factory bisa memilih jalur spesialisasi yang memberikan bonus khusus.

```
STEEL MILL Specialization:
├── ⚔️  WEAPONS PATH    → +20% output senjata, -10% waktu produksi
├── 🛡️  ARMOR PATH      → +30% durability bonus items
└── ⚙️  INDUSTRIAL PATH → -25% machine parts consumption
```

**Implementasi:**
- Panel baru `SpecializationGUI.java` di FactoryGUI.
- Setiap spesialisasi memerlukan pengorbanan: unlock dengan resources langka.
- Max 1 jalur per factory, bisa direset dengan biaya.
- Menambahkan field `specializationPath` ke `Factory.java`.

---

### 6. 💹 **Stock Market untuk Factory Shares**
**Konsep:** Player bisa menjual "saham" factory mereka ke player lain.

```
FACTORY SHARES — Iron Forge Co.
Current Price:  $150 per share
Shares Issued:  1000
Your Holdings:  200 shares (20%)
Dividends:      $15/share per produksi
```

**Detail:**
- Owner bisa IPO factory dengan memilih berapa % saham dijual.
- Pemegang saham mendapat persentase keuntungan dari setiap produksi.
- Ada `StockManager.java` dan `StockMarketGUI.java`.
- Harga saham fluktuasi berdasarkan performa produksi factory.

---

### 7. 🌐 **Factory Corporation / Conglomerate**
**Konsep:** Beberapa player bisa membentuk korporasi untuk memiliki factory bersama.

```yaml
corporation:
  name: "Iron Brotherhood Corp"
  members:
    - Player1 (CEO, 40%)
    - Player2 (CFO, 30%)
    - Player3 (Worker, 30%)
  factories: [factory1, factory5, factory8]
  shared-treasury: 250000
```

**Detail:**
- Pembagian keuntungan otomatis berdasarkan persentase kepemilikan.
- CEO sebagai decision maker, CFO approval untuk transaksi besar.
- `CorporationManager.java` baru.
- Vote system untuk keputusan besar (upgrade, sell factory, dll).

---

## ⚙️ Sistem Produksi Lanjutan {#produksi}

### 8. ⛓️ **Factory Chaining (Production Pipeline)**
**Konsep:** Output Factory A otomatis menjadi input Factory B — rantai produksi imersi!

```
[Refinery] → Basic Fuel →→→ [Steel Mill] → Steel Ingot →→→ [Advanced Factory]
```

**Config:**
```yaml
factory_chain:
  source_factory: "refinery_1"
  target_factory: "steel_mill_1"
  resource_id: "basic_fuel"
  transfer_every_ticks: 200
  max_transfer_per_cycle: 64
```

**Detail:**
- `ChainManager.java` baru yang berjalan sebagai scheduled task.
- Visualisasi chain di GUI sebagai "pipeline diagram" (ASCII art di lore).
- Validasi: hanya bisa chain factory milik sendiri atau corporation.

---

### 9. 🤖 **Production Queue System**
**Konsep:** Set antrian resep yang akan diproduksi secara otomatis tanpa perlu manual restart.

```
PRODUCTION QUEUE — Steel Mill #1
[1] Steel Ingot x100    ████████░░  80%  (22 min remaining)
[2] Carbon Fiber x50    ░░░░░░░░░░  0%   (queued)
[3] Gold Alloy x50      ░░░░░░░░░░  0%   (queued)
[DRAG TO REORDER] [ADD TO QUEUE] [CLEAR ALL]
```

**Detail:**
- `ProductionTask.java` diperluas dengan antrian (Queue<Recipe>).
- GUI drag-and-drop reorder (emulasi via shift+click swap).
- Notifikasi ketika satu item queue selesai dan berikutnya dimulai.

---

### 10. 🔥 **Fuel Efficiency & Overclocking System**
**Konsep:** Jenis fuel yang berbeda memberikan efek berbeda pada kecepatan produksi.

| Fuel Type | Speed Bonus | Cost Penalty | Risk |
|-----------|-------------|--------------|------|
| Basic Fuel | +0% | none | none |
| Refined Fuel | +25% speed | +10% cost | none |
| Premium Fuel | +50% speed | +30% cost | 5% breakdown risk |
| Experimental Fuel | +100% speed | +80% cost | 20% breakdown risk |

**Detail:**
- Factory "breakdown" dari Experimental Fuel → produksi berhenti, butuh repair.
- Repair membutuhkan Machine Parts dan biaya.
- Mechanic baru yang mendorong value untuk Premium Fuel.

---

### 11. 🛠️ **Factory Maintenance & Durability System**
**Konsep:** Factory memiliki kondisi/durabilitas yang menurun seiring produksi.

```
⚙️  Machine Condition: ████████░░  82% (Good)
    Next Maintenance:  in ~3 production cycles
    Maintenance Cost:  2x Basic Gear, 1x Engine Part + $500
```

**Mechanic:**
- Deterioration semakin cepat jika memakai Experimental Fuel.
- Di bawah 50% kondisi: produksi lambat.
- Di bawah 20% kondisi: risiko breakdown acak.
- Admin bisa set kondisi via command.
- Mendorong konsumsi Machine Parts (core loop plugin terjaga!).

---

### 12. 🎲 **Bonus Output / Lucky Drop System**
**Deskripsi:** Peluang mendapatkan bonus item saat produksi selesai.

```yaml
bonus-output:
  chance: 0.05
  items:
    - resource: "rare_alloy"
      amount: 1
    - resource: "circuit_board"
      amount: 2
  research-multiplier: true
```

---

### 13. ⏰ **Scheduled Production (Timed Start)**
**Konsep:** Player bisa set factory untuk mulai produksi di waktu tertentu.

```
⏰ SCHEDULED START
   Factory:    Iron Forge #2
   Recipe:     Steel Ingot x50
   Start at:   Minecraft Day (06:00)
   Repeat:     Every Minecraft Day
```

---

### 14. 📦 **Batch Production / Bulk Recipe**
**Konsep:** Pilih seberapa banyak batch yang ingin diproduksi sekaligus dengan economies of scale.

```
BATCH PRODUCTION — Carbon Fiber
  Standard:  1x batch = 20 Carbon Fiber (2 hr)
  x3 batch:  60 Carbon Fiber (5.5 hr) [10% time discount]
  x5 batch:  100 Carbon Fiber (8 hr) [20% time discount]
  x10 batch: 200 Carbon Fiber (14 hr) [30% time discount]
```

---

## 🖥️ GUI & UX Overhaul {#gui}

### 15. 📊 **Factory Dashboard (Revamp FactoryGUI)**
**Konsep:** Halaman utama factory diubah menjadi "control room" yang informatif.

- Colored glass border dinamis berdasarkan status (hijau = running, kuning = idle, merah = error).
- Header real-time dengan progress bar di nama display item.
- Semua tombol berbasis ikon yang lebih besar dan menarik.
- Animasi item saat produksi berjalan.

---

### 16. 🗺️ **Factory Map GUI**
**Konsep:** GUI yang menampilkan peta seluruh factory milik player dengan status masing-masing.

```
YOUR FACTORY EMPIRE
[🟢] Iron Forge #1  — RUNNING  (L3)
[🟡] Carbon Works   — IDLE     (L2)
[🔴] Gold Refinery  — FULL     (L1)
[⚫] Steel Corp #3  — OFFLINE  (L4)
```

Klik factory untuk langsung buka panelnya atau teleport.

---

### 17. 📱 **Live ActionBar / Scoreboard HUD**
**Konsep:** Tampilkan informasi real-time di ActionBar atau Scoreboard.

```
ActionBar: 🏭 Iron Forge: ████████░░ 82% | ⏱ 18m | 📦 3/9 Storage
```

Config: Player bisa toggle via `/fc hud toggle`.

---

### 18. 🔔 **Notification Center GUI**
**Konsep:** Centralisasi semua notifikasi plugin dalam satu GUI.

```
📬 NOTIFICATION CENTER (3 unread)
🟢 [NEW] Iron Forge #1 — Production Complete!    5min ago
🟡 [NEW] Invoice DUE — Pay $2,500 before 2h!    1hr ago
🔴 [NEW] Storage Full — Carbon Works 100%!      2hr ago
```

Klik notifikasi untuk langsung action.

---

### 19. 🎨 **Factory Custom Name & Icon**
```
/fc rename <factory_id> "Iron Brotherhood Forge"
/fc seticon <factory_id> FURNACE
```

Nama dan icon custom tampil di semua GUI, NPC name, dan bossbar.

---

### 20. 🖼️ **Recipe Wiki Enhancement**
- Search bar via AnvilGUI.
- Bookmark resep favorit per-player.
- Tampilkan profitability: `💰 Est. Profit: $3,200 per batch`.
- "You can craft this!" indicator jika bahan sudah ada di storage.

---

### 21. 📦 **Output Storage GUI Overhaul**
- Header row: Total Items, Total Value, Storage Capacity bar.
- Setiap item: total value di lore.
- Tombol "Sell All" (Pro) dan "Take All".
- Middle-Click: Custom amount via AnvilGUI.
- Color-coded border: hijau → kuning → merah.
- Sort: By Name, By Amount, By Value, By Type.

---

### 22. 🎬 **Onboarding Tutorial System**
- Tutorial interaktif untuk player baru.
- Highlight NPC dengan particle saat tutorial aktif.
- Reward selesai tutorial: Resources awal (beginner pack).
- `TutorialManager.java` dan `TutorialGUI.java`.

---

## 🔌 Integrasi Plugin Eksternal {#integrasi}

### 23. 🔌 **ItemsAdder Integration Hook**
```yaml
my_item:
  itemsadder-namespace: "mypack"
  itemsadder-id: "magic_ingot"
```
Gunakan `ItemsAdder.getCustomItem(namespace + ":" + id)`.

---

### 24. 🔌 **Nexo (ex-Oraxen v2) Hook**
```yaml
nexo_iron_dust:
  nexo-id: "iron_dust"
```
Gunakan `NexoItems.itemFromId(id)`.

---

### 25. 🔌 **EcoItems & Oraxen v1 Hook**
```yaml
eco_gem:
  ecoitems-id: "ruby_gem"
oraxen_crystal:
  oraxen-id: "magic_crystal"
```

---

### 26. 🔌 **Universal CustomItem Hook Interface** *(Arsitektur)*
```java
public interface CustomItemHook {
    String getPluginName();
    boolean isEnabled();
    ItemStack getItem(String id, int amount);
    boolean isItem(ItemStack stack, String id);
    String getItemId(ItemStack stack);
}
```
Loop di `ResourceManager` menggantikan if-else chain yang panjang dan tidak scalable.

---

### 27. 🗄️ **MySQL / MariaDB Database Support**
```yaml
database:
  type: mysql
  host: localhost
  port: 3306
  name: factorycore_db
  pool-size: 10
```
- `DatabaseManager.java` sudah ada, tinggal expand.
- Migration script otomatis dari YAML → MySQL.
- Async read/write dengan HikariCP.

---

### 28. 💬 **Discord Webhook Integration**
Kirim notifikasi penting ke Discord channel:
- Production complete, storage full, invoice overdue, factory sold/bought, achievement unlocked.

---

### 29. 🗺️ **DynMap / BlueMap Integration**
- Marker factory di web map dengan warna berdasarkan type & status.
- Klik marker → popup info factory.
- Auto-update saat ownership berubah.

---

### 30. 💰 **ShopGUI+ / EssentialsX Shop Integration**
Toggle per-factory: "Auto-list ke shop saat storage penuh."

---

### 31. 🏠 **GriefPrevention / Lands / Towny Integration**
```yaml
region-plugin: towny  # worldguard | griefprevention | lands | towny
require-owned-region: true
```
Mengurangi dependensi berat ke WorldGuard untuk server kecil.

---

## 👥 Sistem Sosial & Multiplayer {#sosial}

### 32. 🤝 **Factory Co-ownership System**
```yaml
factory_owners:
  primary: "Player1"
  co-owners:
    - name: "Player2"
      role: MANAGER    # MANAGER | WORKER | VIEWER
      revenue-share: 30
```

---

### 33. 🏘️ **Factory District / Zone System**
```yaml
industrial_district_1:
  name: "Iron Valley"
  bonus: "raw_resources_production +15%"
  factories: [forge1, forge2]
  monthly-rent: 5000
```
District memberikan bonus kolektif → insentif untuk berkumpul secara geografis.

---

### 34. 🏆 **Leaderboard & Competition System**
```
🏆 TOP FACTORIES — This Week
#1 Iron Brotherhood    $248,000 revenue
#2 Carbon Empire       $187,500 revenue
```
- `/fc leaderboard` command + `LeaderboardGUI.java`.
- Reset weekly/monthly (configurable).
- PlaceholderAPI baru: `%factorycore_leaderboard_top1_name%`.

---

### 35. 📣 **Factory Trade Board (Player-to-Player)**
```
📋 TRADE BOARD
[WTS] Player1: 100x Steel Ingot @ $250/ea  [BUY NOW]
[WTB] Player2: 50x Circuit Board (offer: $500/ea) [SELL NOW]
```
Trade escrow system: item ditaruh di "hold" hingga deal selesai.

---

### 36. 🎌 **Factory Alliance / War System** *(Endgame Content)*
**Economic War:**
- "Dump" market → jual murah untuk memaksa rival rugi.
- "Sabotage" → bayar NPC untuk delay produksi factory musuh.

**Alliance:**
- Shared research tree.
- Joint production: 2 factory bergabung untuk resep eksklusif.

---

## 🌍 Dunia & Lingkungan {#dunia}

### 37. ✨ **Particle & Visual Effects**
- Smoke particles di atas cerobong (CAMPFIRE_COSY_SMOKE).
- Spark particles saat produksi selesai.
- Glow border yang berubah warna berdasarkan status.

---

### 38. 🔊 **Sound Effects System**
```yaml
sounds:
  production-start: BLOCK_FURNACE_FIRE_CRACKLE
  production-complete: ENTITY_EXPERIENCE_ORB_PICKUP
  storage-full: ENTITY_ENDERMAN_TELEPORT
  upgrade-complete: ENTITY_PLAYER_LEVELUP
```

---

### 39. 🗿 **Custom Hologram Display**
*(via DecentHolograms / CMI)*
```
      🏭 IRON FORGE #1
     [Owner: PlayerName]
   ████████░░ 80% Running
    Next Done: 18 minutes
```
Auto-update setiap 10 detik.

---

### 40. 🌙 **Time-based Production Bonuses**
```yaml
time-bonuses:
  minecraft-night:
    production-speed: +10%
  real-weekend:
    output-bonus: +20%
  peak-hours:
    hours: "18:00-21:00"
    efficiency: +15%
```

---

## 📊 Analytics & Statistik {#analytics}

### 41. 📈 **Production History & Analytics Dashboard**
```
📊 FACTORY ANALYTICS — Iron Forge #1
Total Produced:    1,247 cycles
Best Day:          $48,000 (Monday)
Avg Daily Revenue: $22,500
Uptime:            87% (last 30 days)
Most Produced:     Steel Ingot (47%)
```
Akses via tab baru di FactoryGUI atau `/fc analytics <factory_id>`.

---

### 42. 💰 **Revenue Tracking & Profit/Loss Report**
```
📋 WEEKLY REPORT — Week 12, 2026
Revenue:     $168,000
Expenses:
  Tax:        -$8,400
  Salary:     -$2,100
Net Profit:  $157,500  ✅ (+12% vs last week)
```
Kirim via chat + Discord webhook + simpan di `InvoiceManager`.

---

### 43. 🔮 **Production Prediction System**
```
📊 PRODUCTION FORECAST
Expected completion: 6 hours
Expected output value: $72,000
Expected profit (after costs): $61,200
```

---

## 🛡️ Admin & Developer Tools {#admin}

### 44. 🔧 **Admin Debug Panel** `/fc admin panel`
```
🔧 ADMIN CONTROL CENTER
Total Factories: 47 | Running: 23 | Idle: 18 | Error: 6

[🔴 ERROR FACTORIES]
  - factory_12: Storage Full
  - factory_28: No fuel

[⚡ QUICK ACTIONS]
  [Force Start All] [Clear All Errors] [Reload Config] [Export Data]
```

---

### 45. 📤 **Import/Export System**
```
/fc admin export all → factories_backup_2026-03-28.json
/fc admin import-all backup_2026.json
```

---

### 46. 📋 **Factory Template System**
```yaml
template-name: "High-End Steel Mill"
factory-type: STEEL_MILL
initial-level: 2
pre-configured-recipe: steel_ingot_x100
price: 500000
```
```
/fc admin create-from-template factory_area_5 high_end_steelmill
```

---

### 47. 🔒 **Per-Factory Permission System**
```yaml
permissions:
  factory.steel_mill: factorycore.buy.steelmill
  factory.advanced_factory: factorycore.buy.advanced
```

---

### 48. 🧪 **Simulation Mode**
```
/fc admin simulate <factory_id> <recipe_id> <runs>
```
Jalankan factory tanpa mengkonsumsi resource sungguhan — untuk testing config baru.

---

### 49. 🌐 **Web Dashboard (External)**
- Overview server: semua factory, status, total ekonomi.
- Per-player report: factory milik, revenue, invoice.
- Admin panel: CRUD factory, force-start, dll.
- Live production monitor dengan grafik.
- Teknologi: Embedded HTTP (Javalin/NanoHTTPD) atau external service.

---

## 🚀 Long-term Vision {#longterm}

### 50. 📡 **Multi-Server Support (BungeeCord / Velocity)**
- Marketplace dan Leaderboard tersinkronisasi lintas server.
- via Redis Pub/Sub atau SQL shared database.
- Plugin companion `FactoryCore-Bridge` untuk Bungee.

---

### 51. 🤖 **AI-Powered NPC Economics**
- NPC "pedagang AI" yang otomatis beli/jual di marketplace berdasarkan supply-demand.
- Menstabilkan harga ekstrem.
- Configurable aggressiveness dan budget.

---

### 52. 🎮 **Factory Mini-Game Events**
```
⚡ EVENT: IRON RUSH — 1 hour remaining!
Produce the most Steel Ingot to win!
#1 Iron Brotherhood   450 ingots
#2 Carbon Empire      380 ingots
Prize: 3x Production Speed (24hr) + $100,000!
```
Admin jadwalkan event, durasi, syarat, reward.

---

## 📋 Tabel Prioritas Master {#prioritas}

| # | Fitur | Prioritas | Kompleksitas | Impact |
|---|-------|-----------|--------------|--------|
| 26 | Universal CustomItem Hook Interface | 🔴 P1 | Sedang | ⭐⭐⭐⭐⭐ |
| 23-25 | ItemsAdder / Nexo / EcoItems Hooks | 🔴 P1 | Rendah | ⭐⭐⭐⭐⭐ |
| 27 | MySQL Database Support | 🔴 P1 | Tinggi | ⭐⭐⭐⭐⭐ |
| 8 | Factory Chaining (Pipeline) | 🔴 P1 | Tinggi | ⭐⭐⭐⭐⭐ |
| 21 | Output Storage GUI Overhaul | 🔴 P1 | Sedang | ⭐⭐⭐⭐ |
| 9 | Production Queue System | 🔴 P1 | Sedang | ⭐⭐⭐⭐ |
| 15 | Factory Dashboard Revamp | 🟡 P2 | Sedang | ⭐⭐⭐⭐ |
| 41 | Production History & Analytics | 🟡 P2 | Sedang | ⭐⭐⭐⭐ |
| 1 | Dynamic Market Pricing | 🟡 P2 | Tinggi | ⭐⭐⭐⭐ |
| 11 | Maintenance & Durability | 🟡 P2 | Sedang | ⭐⭐⭐⭐ |
| 17 | Live ActionBar HUD | 🟡 P2 | Rendah | ⭐⭐⭐⭐ |
| 37 | Particle & Visual Effects | 🟡 P2 | Rendah | ⭐⭐⭐ |
| 34 | Leaderboard System | 🟡 P2 | Sedang | ⭐⭐⭐ |
| 28 | Discord Webhook | 🟠 P3 | Rendah | ⭐⭐⭐ |
| 19 | Factory Custom Name & Icon | 🟠 P3 | Rendah | ⭐⭐⭐ |
| 10 | Fuel Efficiency & Overclocking | 🟠 P3 | Sedang | ⭐⭐⭐ |
| 32 | Co-ownership System | 🟠 P3 | Tinggi | ⭐⭐⭐ |
| 5 | Specialization Tree | 🟠 P3 | Sangat Tinggi | ⭐⭐⭐⭐ |
| 2 | Loan & Bank System | 🟠 P3 | Tinggi | ⭐⭐⭐ |
| 7 | Corporation System | 🔵 P4 | Sangat Tinggi | ⭐⭐⭐⭐ |
| 6 | Stock Market | 🔵 P4 | Sangat Tinggi | ⭐⭐⭐⭐ |
| 50 | Multi-Server Support | 🔵 P4 | Ekstrem | ⭐⭐⭐⭐⭐ |

---

## 💡 Rekomendasi Roadmap

### 🎯 v1.2.8 (Sekarang)
- ✅ ItemsAdder + Nexo hooks
- ✅ Universal CustomItem Hook Interface
- ✅ Output Storage GUI Overhaul (sort, filter, take-amount)
- ✅ Output Capacity System + notification

### 🎯 v1.3.0
Focus: Production System Upgrade
- Production Queue System
- Factory Chaining (Pipeline)
- Fuel Efficiency System
- Production History & Analytics

### 🎯 v1.4.0
Focus: Economy & Social
- Dynamic Market Pricing
- Leaderboard System
- Co-ownership System
- Discord Webhook Integration

### 🎯 v1.5.0
Focus: Database & Performance
- MySQL Support
- Web Dashboard (basic)
- Factory Templates
- Simulation Mode

### 🎯 v2.0.0 (Major)
Focus: Endgame & Multiplayer
- Corporation System
- Stock Market
- Factory Specialization Tree
- Factory Alliance/War System
- Multi-server Support

---

*Dokumen ini dibuat berdasarkan analisis mendalam kode FactoryCore v1.21.4*
*Total managers: 16 | GUI classes: 23 | Models: 14*
*Dibuat: 2026-03-28*
