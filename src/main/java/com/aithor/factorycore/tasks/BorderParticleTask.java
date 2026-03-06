package com.aithor.factorycore.tasks;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.PlayerFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Scheduler task that displays particle effects at the corners and edges
 * of player-created factory regions to help owners visualize their borders.
 * Players can toggle this on/off.
 */
public class BorderParticleTask implements Runnable {

    private final FactoryCore plugin;
    private final Set<UUID> enabledPlayers = new HashSet<>();

    public BorderParticleTask(FactoryCore plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled(UUID playerId) {
        return enabledPlayers.contains(playerId);
    }

    public boolean toggle(UUID playerId) {
        if (enabledPlayers.contains(playerId)) {
            enabledPlayers.remove(playerId);
            return false;
        } else {
            enabledPlayers.add(playerId);
            return true;
        }
    }

    @Override
    public void run() {
        if (enabledPlayers.isEmpty()) return;

        Particle particle;
        try {
            particle = Particle.valueOf(
                    plugin.getConfig().getString("player-factory.border-particle", "FLAME"));
        } catch (IllegalArgumentException e) {
            particle = Particle.FLAME;
        }

        double spacing = plugin.getConfig().getDouble("player-factory.border-particle-spacing", 1.0);

        for (UUID playerId : new ArrayList<>(enabledPlayers)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                enabledPlayers.remove(playerId);
                continue;
            }

            List<PlayerFactory> factories = plugin.getPlayerFactoryManager()
                    .getFactoriesByOwner(playerId);
            if (factories.isEmpty()) continue;

            for (PlayerFactory factory : factories) {
                World world = Bukkit.getWorld(factory.getWorldName());
                if (world == null || !player.getWorld().equals(world)) continue;

                // Only show particles if player is within render distance
                Location center = factory.getCenterLocation();
                if (center == null) continue;
                if (player.getLocation().distanceSquared(center) > 10000) continue; // 100 blocks

                drawRegionBorder(player, world, factory, particle, spacing);
            }
        }
    }

    private void drawRegionBorder(Player player, World world, PlayerFactory factory,
                                  Particle particle, double spacing) {
        int minX = factory.getMinX();
        int minY = factory.getMinY();
        int minZ = factory.getMinZ();
        int maxX = factory.getMaxX() + 1; // +1 to show outer edge of last block
        int maxY = factory.getMaxY() + 1;
        int maxZ = factory.getMaxZ() + 1;

        // Draw vertical edges (4 corners)
        for (double y = minY; y <= maxY; y += spacing) {
            spawnParticle(player, world, particle, minX, y, minZ);
            spawnParticle(player, world, particle, maxX, y, minZ);
            spawnParticle(player, world, particle, minX, y, maxZ);
            spawnParticle(player, world, particle, maxX, y, maxZ);
        }

        // Draw horizontal edges at bottom and top (connecting the corners)
        for (int yLevel : new int[]{minY, maxY}) {
            for (double x = minX; x <= maxX; x += spacing) {
                spawnParticle(player, world, particle, x, yLevel, minZ);
                spawnParticle(player, world, particle, x, yLevel, maxZ);
            }
            for (double z = minZ; z <= maxZ; z += spacing) {
                spawnParticle(player, world, particle, minX, yLevel, z);
                spawnParticle(player, world, particle, maxX, yLevel, z);
            }
        }
    }

    private void spawnParticle(Player player, World world, Particle particle,
                               double x, double y, double z) {
        Location loc = new Location(world, x, y, z);
        player.spawnParticle(particle, loc, 1, 0, 0, 0, 0);
    }
}
