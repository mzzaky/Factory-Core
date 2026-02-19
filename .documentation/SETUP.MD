# FactoryCore - Tutorial Setup Lengkap

## 📚 Daftar Isi
1. [Instalasi Plugin](#instalasi-plugin)
2. [Setup Pertama Kali](#setup-pertama-kali)
3. [Membuat Factory Pertama](#membuat-factory-pertama)
4. [Konfigurasi Advanced](#konfigurasi-advanced)
5. [Tips & Tricks](#tips--tricks)
6. [Troubleshooting](#troubleshooting)

---

## 1. Instalasi Plugin

### Persiapan
Pastikan server Anda memenuhi syarat:
- **Server:** Spigot/Paper 1.21.4 atau lebih baru
- **Java:** Java 17 atau lebih baru
- **RAM:** Minimal 2GB (recommended 4GB+)

### Install Dependencies

#### A. Install Vault
```bash
1. Download Vault dari: https://www.spigotmc.org/resources/vault.34315/
2. Letakkan Vault.jar di folder plugins/
3. Restart server
```

#### B. Install Economy Plugin (Pilih salah satu)
- **EssentialsX** (Recommended): https://essentialsx.net/downloads.html
- **CMI**: https://www.spigotmc.org/resources/cmi.3742/

#### C. Install WorldEdit
```bash
1. Download dari: https://dev.bukkit.org/projects/worldedit
2. Letakkan di folder plugins/
3. Restart server
```

#### D. Install WorldGuard
```bash
1. Download dari: https://dev.bukkit.org/projects/worldguard
2. Letakkan di folder plugins/
3. Restart server
```

#### E. Install PlaceholderAPI (Opsional)
```bash
1. Download dari: https://www.spigotmc.org/resources/placeholderapi.6245/
2. Letakkan di folder plugins/
3. Restart server
```

### Install FactoryCore
```bash
1. Download FactoryCore-1.21.4.jar
2. Letakkan di folder plugins/
3. Restart server
4. Cek console, pastikan tidak ada error
```

---

## 2. Setup Pertama Kali

### Verifikasi Instalasi
```bash
# Jalankan command di console atau in-game
/fc version
```
Output: `FactoryCore v1.21.4 by aithor`

### Struktur Folder
Setelah restart pertama, struktur folder:
```
plugins/FactoryCore/
├── config.yml          # Konfigurasi utama
├── resources.yml       # Definisi resources
├── recipes.yml         # Resep produksi
├── language.yml        # Pesan plugin
├── npc.yml            # Konfigurasi NPC
└── logs/              # Folder log (auto-created)
```

### Konfigurasi Dasar

#### A. Edit config.yml
```yaml
# Sesuaikan tax dan salary
tax:
  enabled: true
  rate: 5.0              # 5% dari harga factory
  interval-ticks: 144000 # 3 hari (sesuaikan)

salary:
  enabled: true
  rate: 1.0              # 1% dari harga factory
  interval-ticks: 24000  # 1 hari (sesuaikan)
```

#### B. Tes Konfigurasi
```bash
/fc admin reload
```
Pastikan tidak ada error di console.

---

## 3. Membuat Factory Pertama

### Step 1: Buat Region dengan WorldGuard

#### A. Pilih Area
```bash
# Gunakan WorldEdit untuk select area
//wand                    # Dapat selection tool
# Klik kiri di pojok pertama
# Klik kanan di pojok kedua
```

#### B. Buat Region
```bash
# Buat region dari selection
/region define factory_steel_1

# Verifikasi region
/region info factory_steel_1
```

#### C. Set Region Flags (Opsional)
```bash
# Protect dari grief
/region flag factory_steel_1 block-break deny
/region flag factory_steel_1 block-place deny

# Allow owner untuk interact
/region flag factory_steel_1 interact allow
```

### Step 2: Buat Factory

#### A. Command Dasar
```bash
/fc admin create <region_name> <factory_type> <factory_id> <price>
```

**Factory Types:**
- `steel_mill` - Steel Mill (Pabrik Baja)
- `refinery` - Refinery (Kilang)
- `workshop` - Workshop (Pabrik Machine Parts)
- `advanced_factory` - Advanced Factory

#### B. Contoh Praktis
```bash
# Buat Steel Mill dengan harga $100,000
/fc admin create factory_steel_1 steel_mill steel_mill_1 100000

# Output:
# [FactoryCore] Factory created successfully!
```

### Step 3: Spawn NPC Employee

#### A. Spawn NPC
```bash
# Berdiri di lokasi dimana NPC akan spawn
/fc admin npc spawn steel_mill_1

# Output:
# [FactoryCore] NPC steel_mill_1 spawned successfully!
```

#### B. Customize NPC (Opsional)
Edit `npc.yml`:
```yaml
steel_mill_1:
  name: "§6Steel Mill Worker"
  profession: ARMORER
  equipment:
    helmet: IRON_HELMET
    chestplate: IRON_CHESTPLATE
```
Lalu: `/fc admin reload`

### Step 4: Test Factory

#### A. Beli Factory (Sebagai Player)
```bash
/fc buy steel_mill_1

# Output:
# [FactoryCore] You bought the factory for $100,000!
```

#### B. Cek Info Factory
```bash
/fc info steel_mill_1

# Output:
# === Factory Info ===
# ID: steel_mill_1
# Type: Steel Mill
# Owner: YourName
# Level: 1
# Price: $100,000
# Status: STOPPED
```

#### C. Interaksi dengan NPC
```
1. Klik kanan NPC employee
2. GUI Factory Panel akan terbuka
3. Explore menu yang tersedia
```

---

## 4. Konfigurasi Advanced

### Membuat Factory dengan Ekonomi Seimbang

#### A. Hitung Harga Factory
```
Formula Recommended:
- Steel Mill: $100,000 - $150,000
- Refinery: $150,000 - $200,000
- Workshop: $200,000 - $250,000
- Advanced Factory: $250,000 - $350,000
```

#### B. Setup Multiple Factories
```bash
# Area 1: Steel Mill
/region define factory_steel_1
/fc admin create factory_steel_1 steel_mill steel_mill_1 120000
/fc admin npc spawn steel_mill_1

# Area 2: Refinery
/region define factory_refinery_1
/fc admin create factory_refinery_1 refinery refinery_1 180000
/fc admin npc spawn refinery_1

# Area 3: Workshop
/region define factory_workshop_1
/fc admin create factory_workshop_1 workshop workshop_1 220000
/fc admin npc spawn workshop_1

# Area 4: Advanced Factory
/region define factory_advanced_1
/fc admin create factory_advanced_1 advanced_factory advanced_1 300000
/fc admin npc spawn advanced_1
```

### Customize Resources

#### Edit resources.yml untuk custom items
```yaml
custom_steel:
  type: ADVANCED_RESOURCES
  material: IRON_INGOT
  name: "§b§lCustom Steel"
  lore:
    - "§7Premium quality steel"
    - "§7Crafted with care"
  custom-model-data: 9001  # ResourcePack custom model
  glow: true
```

### Customize Recipes

#### Edit recipes.yml untuk balance
```yaml
custom_steel_production:
  name: "§b§lCustom Steel Production"
  factory-type: steel_mill
  production-time: 600     # 10 minutes
  inputs:
    iron_ore: 10
    coal_ore: 5
    basic_fuel: 2
    basic_gear: 1
  outputs:
    custom_steel: 15
  console-commands:
    - "eco give {player} 200"
    - "give {player} diamond 1"
```

### Setup Permissions dengan LuckPerms

```bash
# Group untuk factory owners
/lp creategroup factory_owner

# Permissions untuk group
/lp group factory_owner permission set factorycore.buy true
/lp group factory_owner permission set factorycore.sell true
/lp group factory_owner permission set factorycore.info true

# Assign ke player
/lp user PlayerName parent add factory_owner
```

---

## 5. Tips & Tricks

### Optimasi Performance

#### A. Adjust Production Update Interval
Edit `FactoryManager.java` atau config:
```yaml
production:
  update-interval: 20  # Setiap 1 detik (default)
  # Naikkan ke 40 atau 60 untuk server dengan banyak factory
```

#### B. Limit Max Factories per Player
Tambahkan custom logic di `FactoryCoreCommand.java`:
```java
// Dalam method handleBuy()
List<Factory> owned = plugin.getFactoryManager()
    .getFactoriesByOwner(player.getUniqueId());
if (owned.size() >= 5) {  // Max 5 factories
    player.sendMessage("§cYou can only own 5 factories!");
    return true;
}
```

### Membuat Economy Challenge

#### A. Requirement System
Tambahkan requirement sebelum bisa beli factory:
```java
// Require player sudah punya factory lain
if (factory.getType() == FactoryType.ADVANCED_FACTORY) {
    boolean hasWorkshop = owned.stream()
        .anyMatch(f -> f.getType() == FactoryType.WORKSHOP);
    if (!hasWorkshop) {
        player.sendMessage("§cYou need a Workshop first!");
        return true;
    }
}
```

### Integration dengan Plugin Lain

#### A. Dengan Jobs Plugin
Beri job experience saat produksi selesai:
```yaml
# recipes.yml
console-commands:
  - "jobs exp {player} Builder 100"
```

#### B. Dengan McMMO
```yaml
console-commands:
  - "mcmmo experience add {player} Mining 500"
```

#### C. Dengan Quests Plugin
Buat quest: "Produce 100 Steel Ingots"

---

## 6. Troubleshooting

### Problem: Plugin tidak load

**Solution:**
```bash
1. Cek Java version: java -version (harus 17+)
2. Cek dependencies installed
3. Cek console untuk error
4. Delete FactoryCore folder dan reinstall
```

### Problem: Factory tidak bisa dibuat

**Symptoms:** Error "Region not found"

**Solution:**
```bash
# Verifikasi region exists
/region info <region_name>

# Pastikan WorldGuard loaded
/plugins

# Cek spelling region name (case-sensitive!)
```

### Problem: NPC tidak spawn

**Solution:**
```bash
1. Pastikan chunk loaded (berdiri di area)
2. Cek console untuk error
3. Cek npc.yml configuration
4. Remove dan spawn ulang:
   /fc admin npc remove <id>
   /fc admin npc spawn <id>
```

### Problem: Produksi tidak berjalan

**Symptoms:** Status STOPPED meskipun sudah start

**Solution:**
```bash
1. Cek materials di storage cukup
2. Cek recipe configuration valid
3. Cek console log untuk error
4. Restart server
```

### Problem: Invoice tidak terbuat

**Solution:**
```bash
1. Cek scheduler interval di config.yml
2. Cek factory punya owner
3. Cek log untuk error:
   plugins/FactoryCore/logs/factorycore.log
4. Manually trigger:
   # Edit source code untuk test command
```

### Problem: Performance lag

**Solution:**
```bash
1. Reduce production update interval (config)
2. Limit max factories per player
3. Disable BossBar if many factories:
   production:
     show-bossbar: false
4. Optimize scheduler timing
```

---

## 🎯 Quick Reference Commands

### Admin Commands Cheat Sheet
```bash
# Factory Management
/fc admin create <region> <type> <id> <price>
/fc admin remove <id>
/fc admin list
/fc admin info <id>
/fc admin setowner <id> <player>
/fc admin teleport <id>

# Resources
/fc admin give <resource_id> <amount> <player>

# NPC Management
/fc admin npc spawn <id>
/fc admin npc remove <id>

# System
/fc admin reload
/fc version
```

### Player Commands Cheat Sheet
```bash
/fc buy <id>           # Beli factory
/fc sell <id>          # Jual factory
/fc info <id>          # Info factory
/fc help               # Bantuan
```

---

## 📊 Recommended Server Setup

### Small Server (10-50 players)
```yaml
tax:
  interval-ticks: 144000  # 3 days
salary:
  interval-ticks: 24000   # 1 day
factory:
  max-level: 5
```
**Factories:** 5-10 total
**Max per player:** 2-3

### Medium Server (50-200 players)
```yaml
tax:
  interval-ticks: 72000   # 1.5 days
salary:
  interval-ticks: 12000   # 12 hours
factory:
  max-level: 7
```
**Factories:** 20-40 total
**Max per player:** 3-5

### Large Server (200+ players)
```yaml
tax:
  interval-ticks: 48000   # 1 day
salary:
  interval-ticks: 6000    # 6 hours
factory:
  max-level: 10
```
**Factories:** 50+ total
**Max per player:** 5-10

---

## ✅ Setup Checklist

- [ ] Install Java 17+
- [ ] Install Spigot/Paper 1.21.4
- [ ] Install Vault + Economy plugin
- [ ] Install WorldEdit
- [ ] Install WorldGuard
- [ ] Install PlaceholderAPI (optional)
- [ ] Install FactoryCore
- [ ] Configure config.yml
- [ ] Test create factory
- [ ] Test spawn NPC
- [ ] Test buy factory as player
- [ ] Test production system
- [ ] Test invoice system
- [ ] Setup permissions
- [ ] Configure backups

---

**Happy Factory Building! 🏭**