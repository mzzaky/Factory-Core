package com.aithor.factorycore.gui;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.text.SimpleDateFormat;
import java.util.*;

public class InvoiceGUI {

    private final FactoryCore plugin;
    private final Player player;
    private final String factoryId;

    public InvoiceGUI(FactoryCore plugin, Player player, String factoryId) {
        this.plugin = plugin;
        this.player = player;
        this.factoryId = factoryId;
    }

    public void openInvoiceMenu() {
        // Store factory ID in player's persistent data for click handling
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "current_factory_id"),
                PersistentDataType.STRING, factoryId);

        List<Invoice> invoices = plugin.getInvoiceManager()
                .getInvoicesByOwner(player.getUniqueId());

        int size = ((invoices.size() + 8) / 9) * 9;
        if (size > 54)
            size = 54;
        if (size < 27)
            size = 27;

        Inventory inv = Bukkit.createInventory(null, size, "§6§lInvoice Management");

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        int slot = 0;

        for (Invoice invoice : invoices) {
            if (slot >= size - 9)
                break;

            List<String> lore = new ArrayList<>();
            lore.add("§7Type: " + invoice.getType().getDisplay());

            double finalAmount = invoice.getAmount();
            double reduction = 0;

            if (invoice.getType() == InvoiceType.SALARY && plugin.getResearchManager() != null) {
                reduction = plugin.getResearchManager().getSalaryReduction(player.getUniqueId());
            }

            if (reduction > 0) {
                double originalAmount = finalAmount / (1 - (reduction / 100.0));
                lore.add("§7Amount: §c§m$" + String.format("%.2f", originalAmount) + "§r §a$"
                        + String.format("%.2f", finalAmount) + " §d(-" + String.format("%.0f", reduction) + "%)");
            } else {
                lore.add("§7Amount: §6$" + String.format("%.2f", finalAmount));
            }
            lore.add("§7Due date: §c" + sdf.format(new Date(invoice.getDueDate())));
            lore.add("");

            if (invoice.isOverdue()) {
                lore.add("§c§lOVERDUE!");
            }

            lore.add("§eClick to pay");

            Material material = invoice.isOverdue() ? Material.RED_STAINED_GLASS_PANE : Material.PAPER;

            ItemStack item = createItem(material,
                    "§eInvoice #" + invoice.getId().substring(0, 8), lore);

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "invoice_id"),
                        PersistentDataType.STRING, invoice.getId());
                item.setItemMeta(meta);
            }

            inv.setItem(slot++, item);
        }

        // Back button
        inv.setItem(size - 1, createItem(Material.ARROW, "§e§lBack", null));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (name != null)
                meta.setDisplayName(name);
            if (lore != null)
                meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }
}