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

public class EntityDamageListener implements Listener {
    
    private final FactoryCore plugin;
    
    public EntityDamageListener(FactoryCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        
        // Protect factory NPCs from damage
        if (entity instanceof Villager) {
            if (plugin.getNPCManager().isNPC(entity.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        
        // Protect factory NPCs from damage
        if (entity instanceof Villager) {
            if (plugin.getNPCManager().isNPC(entity.getUniqueId())) {
                event.setCancelled(true);
                
                // If player right-clicked, open GUI
                if (event.getDamager() instanceof Player) {
                    Player player = (Player) event.getDamager();
                    String factoryId = plugin.getNPCManager().getFactoryIdByEntity(entity.getUniqueId());
                    
                    if (factoryId != null) {
                        FactoryGUI gui = new FactoryGUI(plugin, player, factoryId);
                        gui.openMainMenu();
                    }
                }
            }
        }
    }
}
