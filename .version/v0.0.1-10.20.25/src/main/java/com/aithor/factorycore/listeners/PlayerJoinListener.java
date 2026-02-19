package com.aithor.factorycore.listeners;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.gui.FactoryGUI;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    
    private final FactoryCore plugin;
    
    public PlayerJoinListener(FactoryCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check for overdue invoices
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            int overdueCount = (int) plugin.getInvoiceManager()
                .getInvoicesByOwner(player.getUniqueId())
                .stream()
                .filter(invoice -> invoice.isOverdue())
                .count();
            
            if (overdueCount > 0) {
                player.sendMessage(plugin.getLanguageManager().getMessage("invoice-overdue"));
            }
        }, 40L); // 2 seconds after join
    }
}
