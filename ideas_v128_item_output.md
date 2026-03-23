# 💡 Ide Update FactoryCore v1.2.8
**Fokus: Item Output & Integrasi Custom Item Plugin**
*Dibuat berdasarkan analisis kode: ResourceManager, OutputStorageGUI, StorageManager, hooks MMOItems & ExecutableItems*

---

## 🔍 Kondisi Saat Ini

Saat ini plugin sudah memiliki:
- ✅ Integrasi **MMOItems** (via `mmoitems-type` + `mmoitems-id` di [resources.yml](file:///c:/Users/mohza/IdeaProjects/Factory-Core-Plugin/src/main/resources/resources.yml))
- ✅ Integrasi **ExecutableItems** (via `executable-items-id` di [resources.yml](file:///c:/Users/mohza/IdeaProjects/Factory-Core-Plugin/src/main/resources/resources.yml))
- ✅ Output storage system (YAML-based di [StorageManager](file:///c:/Users/mohza/IdeaProjects/Factory-Core-Plugin/src/main/java/com/aithor/factorycore/managers/StorageManager.java#11-223))
- ✅ [OutputStorageGUI](file:///c:/Users/mohza/IdeaProjects/Factory-Core-Plugin/src/main/java/com/aithor/factorycore/gui/OutputStorageGUI.java#17-227) — withdraw all / withdraw 1
- ✅ Fallback ke vanilla item jika plugin tidak tersedia
- ❌ Belum ada hook untuk **ItemsAdder**, **Nexo**, **EcoItems**, **Oraxen**
- ❌ Output item tidak bisa di-**auto-sell** langsung
- ❌ Output storage tidak punya **limit kapasitas nyata** (angka saja, tidak enforce per-slot)
- ❌ Tidak ada **filter / sort** di OutputStorageGUI
- ❌ Tidak ada **output routing** (kirim ke chest / ke pemain / ke marketplace otomatis)

---

## 🚀 IDE UTAMA — PRIORITAS TINGGI

### 1. 🔌 **ItemsAdder Integration Hook**
**Deskripsi:** Tambah `ItemsAdderHook.java` mirip [MMOItemsHook.java](file:///c:/Users/mohza/IdeaProjects/Factory-Core-Plugin/src/main/java/com/aithor/factorycore/hooks/MMOItemsHook.java) dan [ExecutableItemsHook.java](file:///c:/Users/mohza/IdeaProjects/Factory-Core-Plugin/src/main/java/com/aithor/factorycore/hooks/ExecutableItemsHook.java).
**Config baru di [resources.yml](file:///c:/Users/mohza/IdeaProjects/Factory-Core-Plugin/src/main/resources/resources.yml):**
```yaml
my_custom_item:
  type: FINAL_RESOURCES
  material: PAPER              # fallback
  name: "§6Magic Ingot"        # fallback
  itemsadder-namespace: "mypack"
  itemsadder-id: "magic_ingot"
```
**Detail teknis:**
- Gunakan `ItemsAdder.getCustomItem(namespace + ":" + id)` dari ItemsAdder API.
- `isItemsAdderItem(ItemStack)` menggunakan `ItemsAdder.getCustomItemData(item)`.
- Fallback ke vanilla jika ItemsAdder tidak ada.
- Di `ResourceManager.createItemStack()` dan [getResourceId()](file:///c:/Users/mohza/IdeaProjects/Factory-Core-Plugin/src/main/java/com/aithor/factorycore/managers/ResourceManager.java#166-218), tambahkan branch baru setelah EI.

**Mengapa penting:** ItemsAdder adalah salah satu plugin custom item **paling populer** di komunitas. Banyak server menggunakannya sebagai sumber item custom utama.

---

### 2. 🔌 **Nexo Integration Hook** *(sebelumnya Oraxen v2)*
**Deskripsi:** Tambah `NexoHook.java`.
**Config baru di [resources.yml](file:///c:/Users/mohza/IdeaProjects/Factory-Core-Plugin/src/main/resources/resources.yml):**
```yaml
nexo_iron_dust:
  type: RAW_RESOURCES
  material: IRON_INGOT         # fallback
  nexo-id: "iron_dust"
```
**Detail teknis:**
- Gunakan `NexoItems.itemFromId(id)` untuk mendapatkan ItemStack.
- `NexoItems.idFromItem(item)` untuk pengecekan kebalik.
- Struktur sama persis seperti [ExecutableItemsHook](file:///c:/Users/mohza/IdeaProjects/Factory-Core-Plugin/src/main/java/com/aithor/factorycore/hooks/ExecutableItemsHook.java#14-143).

---

### 3. 🔌 **EcoItems Integration Hook**
**Deskripsi:** Tambah `EcoItemsHook.java`.
**Config baru di [resources.yml](file:///c:/Users/mohza/IdeaProjects/Factory-Core-Plugin/src/main/resources/resources.yml):**
```yaml
eco_gem:
  type: ADVANCED_RESOURCES
  material: AMETHYST_SHARD     # fallback
  ecoitems-id: "ruby_gem"
```
**Detail teknis:**
- Gunakan API `EcoItems.getItem(id)` → `EcoItem.getItemStack()`.
- Pengecekan: `EcoItem.isItem(stack)`.

---

### 4. 🔌 **Oraxen (v1 Legacy) Integration Hook**
**Deskripsi:** Beberapa server masih pakai Oraxen lama, bukan Nexo.
```yaml
oraxen_crystal:
  type: MACHINE_PARTS
  material: AMETHYST_SHARD
  oraxen-id: "magic_crystal"
```
**Detail teknis:**
- Gunakan `OraxenItems.getItemById(id)`.
- Pengecekan: `OraxenItems.getIdByItem(stack)`.

---

### 5. ✨ **Universal Custom Item Hook System (Arsitektur)**
**Deskripsi:** Refactor semua hook ke dalam sebuah interface `CustomItemHook` agar mudah ditambah hook baru ke depannya.

```java
public interface CustomItemHook {
    String getPluginName();
    boolean isEnabled();
    ItemStack getItem(String id, int amount);
    boolean isItem(ItemStack stack, String id);
    String getItemId(ItemStack stack); // reverse lookup
}
```
**Keuntungan:**
- [ResourceManager](file:///c:/Users/mohza/IdeaProjects/Factory-Core-Plugin/src/main/java/com/aithor/factorycore/managers/ResourceManager.java#20-384) tidak perlu dimodifikasi untuk setiap plugin baru — cukup daftarkan hook baru.
- Lebih mudah di-maintain dan di-extend.
- Loop di [getResourceId()](file:///c:/Users/mohza/IdeaProjects/Factory-Core-Plugin/src/main/java/com/aithor/factorycore/managers/ResourceManager.java#166-218) dan [createItemStack()](file:///c:/Users/mohza/IdeaProjects/Factory-Core-Plugin/src/main/java/com/aithor/factorycore/managers/ResourceManager.java#91-156) menjadi lebih bersih.

---

## 🏭 IDE OUTPUT SYSTEM — FUNGSIONALITAS BARU

### 6. 📦 **Output Storage Capacity System**
**Deskripsi:** Saat ini `output-storage` tidak memiliki batas kapasitas riil. Tambahkan **output storage limit** berdasarkan level factory.
**Implementasi:**
- Tambah config: `factory.output-storage-multiplier: 2` (tiap level nambah slot).
- [OutputStorageGUI](file:///c:/Users/mohza/IdeaProjects/Factory-Core-Plugin/src/main/java/com/aithor/factorycore/gui/OutputStorageGUI.java#17-227) harus enforce bahwa jika storage penuh, produksi berhenti atau hasilnya drop ke lantai.
- Tampilkan **progress bar** di GUI: `§a██████████ 6/9 slots`.
- Tambah warning di Chat/Title ketika storage 80% penuh.

---

### 7. ⚡ **Auto-Sell Output** *(Pro Feature)*
**Deskripsi:** Tambah opsi di `FactoryGUI` atau [OutputStorageGUI](file:///c:/Users/mohza/IdeaProjects/Factory-Core-Plugin/src/main/java/com/aithor/factorycore/gui/OutputStorageGUI.java#17-227) untuk mengaktifkan **Auto-Sell** — ketika produksi selesai, item output langsung dijual ke market dengan harga `suggested_price` dan uangnya masuk ke balance owner.
**Config:**
```yaml
auto-sell:
  enabled: true
  sell-percentage: 100  # berapa % item yang dijual otomatis
```
**Detail:**
- Toggle di GUI (mirip auto-tax).
- Uang diberikan via Vault.
- Log transaksi masuk ke InvoiceManager sebagai tipe `SALE`.
- Kirim notif ke pemilik: `§aFactory §e{name} §asold §e{amount}x {item} §afor §6${price}!`

---

### 8. 📬 **Output Routing System** *(Pro Feature)*
**Deskripsi:** Owner factory bisa mengkonfigurasi ke mana item output dikirim setelah produksi selesai:
- **Option A:** Simpan di Output Storage (default).
- **Option B:** Kirim ke chest di koordinat tertentu.
- **Option C:** Langsung masuk ke inventory pemilik (jika online).
- **Option D:** Auto-list ke Marketplace dengan harga yang ditentukan.
**Config tambahan di factory data:**
```yaml
factory_id:
  output-routing: STORAGE  # STORAGE | CHEST | PLAYER | MARKETPLACE
  routing-chest-location: "world,100,64,200"
  routing-marketplace-price: 5000.0
```

---

### 9. 🔄 **Output → Input Transfer (Factory Chaining)**
**Deskripsi:** Izinkan output dari Factory A menjadi input otomatis ke Factory B — seperti rantai produksi!
- Contoh: Smeltery menghasilkan `steel_ingot` → Steel Mill langsung menggunakannya sebagai input.
- Konfigurasi di `FactoryGUI` dengan memilih "Factory Chain" → pilih target factory.
- Sistem akan otomatis memindahkan item dari output A ke input B setiap X ticks.

---

### 10. 📊 **Output History & Production Log**
**Deskripsi:** Catat riwayat produksi per factory.
```
[2026-03-23 01:00] iron_smeltery → 10x Steel Ingot (sold: $1000)
[2026-03-23 00:00] iron_smeltery → 10x Steel Ingot (stored)
```
- Accessible via `/fc log <factory_id>` atau di GUI.
- Simpan max N riwayat terakhir (configurable).
- Berguna untuk debugging dan tracking income.

---

### 11. 🎲 **Bonus Output / Lucky Drop System**
**Deskripsi:** Tambahkan peluang untuk mendapatkan **bonus item** saat produksi selesai.
**Config di `recipes.yml`:**
```yaml
bonus-output:
  chance: 0.05  # 5% chance
  item: "diamond_ore"
  amount: 1
```
- Bisa mengacu ke resource ID biasa, vanilla item, atau item dari plugin custom.
- Research buff bisa meningkatkan persentase bonus.

---

## 🔧 QUALITY OF LIFE — OUTPUT GUI

### 12. 🔃 **Sort & Filter di OutputStorageGUI**
**Deskripsi:** Tambahkan tombol sort di pojok GUI:
- **Sort by Name** (A-Z)
- **Sort by Amount** (terbanyak dulu)
- **Sort by Type** (RAW → ADVANCED → FINAL → dll)
- **Filter by Type** (tampilkan hanya FINAL_RESOURCES misalnya)

---

### 13. 📦 **Take Specific Amount (Input Amount Dialog)**
**Deskripsi:** Saat Middle-Click di item output, buka dialog untuk input jumlah yang ingin diambil (menggunakan AnvilGUI / sign input).
- Left Click = Take All
- Right Click = Take 1
- Middle Click = Take Custom Amount

---

### 14. 🖥️ **Output Storage GUI Overhaul**
**Deskripsi:** Redesign [OutputStorageGUI](file:///c:/Users/mohza/IdeaProjects/Factory-Core-Plugin/src/main/java/com/aithor/factorycore/gui/OutputStorageGUI.java#17-227) agar lebih informatif:
- Header row menampilkan stats: Total items, Total value (`suggested_price × amount`), Storage capacity.
- Setiap item menampilkan total value di lore: `§7Total Value: §6$50,000`
- Tombol **"Sell All"** (Pro): Jual semua sekaligus.
- Tombol **"Take All to Inventory"**: Ambil semua sekaligus.
- Color-coded border berdasarkan storage fullness (hijau → kuning → merah).

---

### 15. 🔔 **Output Storage Full Notification**
**Deskripsi:** Ketika output storage mencapai 100%, kirim notifikasi ke pemilik:
- Chat message + Title.
- Opsional: Sound effect.
- Produksi di-pause otomatis (configurable).
- Konfigurasi cooldown notifikasi agar tidak spam.

---

## 📋 RANGKUMAN PRIORITAS

| # | Fitur | Prioritas | Kompleksitas | Tipe |
|---|-------|-----------|--------------|------|
| 1 | ItemsAdder Hook | 🔴 Tinggi | Sedang | Integration |
| 2 | Nexo Hook | 🔴 Tinggi | Sedang | Integration |
| 5 | Universal Hook Interface | 🔴 Tinggi | Tinggi | Arsitektur |
| 6 | Output Capacity System | 🔴 Tinggi | Sedang | Core |
| 7 | Auto-Sell Output | 🟡 Sedang | Sedang | Pro Feature |
| 12 | Sort & Filter GUI | 🟡 Sedang | Rendah | QoL |
| 14 | Output GUI Overhaul | 🟡 Sedang | Sedang | QoL |
| 8 | Output Routing System | 🟠 Rendah | Tinggi | Pro Feature |
| 9 | Factory Chaining | 🟠 Rendah | Tinggi | Pro Feature |
| 3 | EcoItems Hook | 🟡 Sedang | Rendah | Integration |
| 4 | Oraxen Hook | 🟡 Sedang | Rendah | Integration |
| 10 | Production Log | 🟡 Sedang | Sedang | Core |
| 11 | Bonus Output/Lucky Drop | 🟠 Rendah | Rendah | Core |
| 13 | Custom Amount Withdraw | 🟡 Sedang | Rendah | QoL |
| 15 | Storage Full Notification | 🟡 Sedang | Rendah | Core |

---

## 💭 Rekomendasi untuk v1.2.8

Fokus yang paling impactful untuk v1.2.8 adalah:

1. **ItemsAdder + Nexo Hook** — coverage plugin custom item terbesar di komunitas Minecraft
2. **Universal Hook Interface** — future-proofing arsitektur untuk integrasi plugin berikutnya
3. **Output Capacity System** — mechanic yang lebih realistis dan mendorong upgrade factory
4. **Output GUI Overhaul** — QoL yang langsung terasa oleh semua player
5. **Auto-Sell Output** (Pro) — fitur Pro yang sangat dicari owner server ekonomi

