package com.aithor.factorycore.listeners;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.gui.OutputStorageGUI;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

// ==================== PlayerInteractListener.java ====================
public class PlayerInteractListener implements Listener {
    
    private final FactoryCore plugin;
    
    public PlayerInteractListener(FactoryCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();
        
        // Check if entity is a factory NPC
        if (!(entity instanceof Villager)) return;
        
        if (plugin.getNPCManager().isNPC(entity.getUniqueId())) {
            event.setCancelled(true);

            String factoryId = plugin.getNPCManager().getFactoryIdByEntity(entity.getUniqueId());
            if (factoryId == null) return;

            // Open output storage GUI instead of main GUI
            OutputStorageGUI gui = new OutputStorageGUI(plugin, player, factoryId);
            gui.openOutputStorage();
        }
    }
}