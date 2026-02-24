package com.aithor.factorycore.listeners;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.gui.OutputStorageGUI;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityDamageListener implements Listener {

    private final FactoryCore plugin;

    // Cooldown to prevent double-fire per swing (UUID → last swing time ms)
    private final Map<UUID, Long> swingCooldown = new HashMap<>();
    private static final long SWING_COOLDOWN_MS = 300;

    // Ignore swing events that are caused by a right-click
    private static final Map<UUID, Long> recentRightClicks = new HashMap<>();

    public static void registerRightClick(UUID uuid) {
        recentRightClicks.put(uuid, System.currentTimeMillis());
    }

    public EntityDamageListener(FactoryCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        // Protect factory NPCs from all damage
        if (entity instanceof Villager) {
            if (plugin.getNPCManager().isNPC(entity.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Detects left-click on NPC via arm swing animation + raycast.
     * This works even when the NPC is invulnerable (setInvulnerable = true),
     * because EntityDamageByEntityEvent is never fired for invulnerable entities.
     */
    @EventHandler
    public void onPlayerSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING)
            return;

        Player player = event.getPlayer();

        // Cooldown check — prevent multiple GUI opens per swing
        long now = System.currentTimeMillis();
        Long lastSwing = swingCooldown.get(player.getUniqueId());
        if (lastSwing != null && now - lastSwing < SWING_COOLDOWN_MS)
            return;

        // Ignore if player just right-clicked (prevent right-click opening output
        // storage)
        Long lastRightClick = recentRightClicks.get(player.getUniqueId());
        if (lastRightClick != null && now - lastRightClick < 200)
            return;

        // Raycast up to 5 blocks in the direction the player is looking
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                5.0,
                entity -> entity instanceof Villager
                        && plugin.getNPCManager().isNPC(entity.getUniqueId()));

        if (result == null || result.getHitEntity() == null)
            return;

        Entity hitEntity = result.getHitEntity();
        String factoryId = plugin.getNPCManager().getFactoryIdByEntity(hitEntity.getUniqueId());
        if (factoryId == null)
            return;

        // Register cooldown
        swingCooldown.put(player.getUniqueId(), now);

        // Left-click → Open output storage GUI
        OutputStorageGUI gui = new OutputStorageGUI(plugin, player, factoryId);
        gui.openOutputStorage();
    }
}
