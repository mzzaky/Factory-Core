package com.aithor.factorycore.listeners;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.gui.OutputStorageGUI;
import com.aithor.factorycore.models.FactoryNPC;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class PlayerInteractListener implements Listener {

    private final FactoryCore plugin;

    public PlayerInteractListener(FactoryCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        // Only care about Villager NPCs
        if (!(entity instanceof Villager))
            return;

        if (!plugin.getNPCManager().isNPC(entity.getUniqueId()))
            return;

        event.setCancelled(true);

        String factoryId = plugin.getNPCManager().getFactoryIdByEntity(entity.getUniqueId());
        if (factoryId == null)
            return;

        // Play interact sound for natural feel
        FactoryNPC npc = plugin.getNPCManager().getNPCByEntityUUID(entity.getUniqueId());
        if (npc != null) {
            plugin.getNPCManager().playInteractSound(player, npc);
        }

        // Open output storage GUI
        OutputStorageGUI gui = new OutputStorageGUI(plugin, player, factoryId);
        gui.openOutputStorage();
    }
}