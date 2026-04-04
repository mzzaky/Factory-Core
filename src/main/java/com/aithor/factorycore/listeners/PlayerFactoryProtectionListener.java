package com.aithor.factorycore.listeners;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.PlayerFactory;
import com.aithor.factorycore.models.ProtectionFlag;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;

/**
 * Protects player-created factory regions using per-factory protection flags.
 * Each flag can be toggled independently by the factory owner via the
 * Factory Protection GUI.
 */
public class PlayerFactoryProtectionListener implements Listener {

    private final FactoryCore plugin;

    public PlayerFactoryProtectionListener(FactoryCore plugin) {
        this.plugin = plugin;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean isProtectionMasterEnabled() {
        return plugin.getConfig().getBoolean("player-factory.protection.enabled", true);
    }

    private PlayerFactory getFactoryAt(Location loc) {
        return plugin.getPlayerFactoryManager().getFactoryAt(loc);
    }

    private boolean isAllowed(PlayerFactory factory, Player player) {
        if (factory.getOwner().equals(player.getUniqueId())) return true;
        return player.hasPermission("factorycore.admin");
    }

    private void denyMessage(Player player) {
        player.sendMessage(plugin.getLanguageManager().getMessage("player-factory-protected"));
    }

    // ── Block Break ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isProtectionMasterEnabled()) return;

        PlayerFactory factory = getFactoryAt(event.getBlock().getLocation());
        if (factory == null) return;
        if (!factory.isProtectionEnabled(ProtectionFlag.BLOCK_BREAK)) return;
        if (isAllowed(factory, event.getPlayer())) return;

        event.setCancelled(true);
        denyMessage(event.getPlayer());
    }

    // ── Block Place ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isProtectionMasterEnabled()) return;

        PlayerFactory factory = getFactoryAt(event.getBlock().getLocation());
        if (factory == null) return;
        if (!factory.isProtectionEnabled(ProtectionFlag.BLOCK_PLACE)) return;
        if (isAllowed(factory, event.getPlayer())) return;

        event.setCancelled(true);
        denyMessage(event.getPlayer());
    }

    // ── Player Interact (doors, levers, buttons, etc.) ──────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isProtectionMasterEnabled()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.PHYSICAL) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        PlayerFactory factory = getFactoryAt(block.getLocation());
        if (factory == null) return;

        // Check for container access separately
        if (isContainer(block.getType())) {
            if (!factory.isProtectionEnabled(ProtectionFlag.CONTAINER_ACCESS)) return;
        }
        // Check for redstone components
        else if (isRedstoneComponent(block.getType())) {
            if (!factory.isProtectionEnabled(ProtectionFlag.REDSTONE)) return;
        }
        // Crop trample (PHYSICAL action on farmland)
        else if (event.getAction() == Action.PHYSICAL && block.getType() == Material.FARMLAND) {
            if (!factory.isProtectionEnabled(ProtectionFlag.CROP_TRAMPLE)) return;
        }
        // General interact
        else {
            if (!factory.isProtectionEnabled(ProtectionFlag.PLAYER_INTERACT)) return;
        }

        if (isAllowed(factory, event.getPlayer())) return;

        event.setCancelled(true);
        denyMessage(event.getPlayer());
    }

    // ── Entity Damage ───────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!isProtectionMasterEnabled()) return;

        Location loc = event.getEntity().getLocation();
        PlayerFactory factory = getFactoryAt(loc);
        if (factory == null) return;

        // PvP check
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            if (!factory.isProtectionEnabled(ProtectionFlag.PVP)) return;

            Player attacker = (Player) event.getDamager();
            // Both owner and admin can still attack — but PvP flag blocks all player-vs-player
            event.setCancelled(true);
            denyMessage(attacker);
            return;
        }

        // General entity damage by player
        if (event.getDamager() instanceof Player) {
            if (!factory.isProtectionEnabled(ProtectionFlag.ENTITY_DAMAGE)) return;
            Player damager = (Player) event.getDamager();
            if (isAllowed(factory, damager)) return;

            event.setCancelled(true);
            denyMessage(damager);
        }
    }

    // ── Explosion ───────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!isProtectionMasterEnabled()) return;

        event.blockList().removeIf(block -> {
            PlayerFactory factory = getFactoryAt(block.getLocation());
            return factory != null && factory.isProtectionEnabled(ProtectionFlag.EXPLOSION);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!isProtectionMasterEnabled()) return;

        event.blockList().removeIf(block -> {
            PlayerFactory factory = getFactoryAt(block.getLocation());
            return factory != null && factory.isProtectionEnabled(ProtectionFlag.EXPLOSION);
        });
    }

    // ── Mob Spawning ────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isProtectionMasterEnabled()) return;
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) return;

        if (!(event.getEntity() instanceof Monster)) return;

        PlayerFactory factory = getFactoryAt(event.getLocation());
        if (factory == null) return;
        if (!factory.isProtectionEnabled(ProtectionFlag.MOB_SPAWNING)) return;

        event.setCancelled(true);
    }

    // ── Item Pickup ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!isProtectionMasterEnabled()) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        PlayerFactory factory = getFactoryAt(event.getItem().getLocation());
        if (factory == null) return;
        if (!factory.isProtectionEnabled(ProtectionFlag.ITEM_PICKUP)) return;
        if (isAllowed(factory, player)) return;

        event.setCancelled(true);
    }

    // ── Item Drop ───────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!isProtectionMasterEnabled()) return;

        Player player = event.getPlayer();
        PlayerFactory factory = getFactoryAt(player.getLocation());
        if (factory == null) return;
        if (!factory.isProtectionEnabled(ProtectionFlag.ITEM_DROP)) return;
        if (isAllowed(factory, player)) return;

        event.setCancelled(true);
        denyMessage(player);
    }

    // ── Vehicle Place ───────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehiclePlace(EntityPlaceEvent event) {
        if (!isProtectionMasterEnabled()) return;
        if (event.getPlayer() == null) return;

        // Ensure we only handle vehicle types (Boats, Minecarts) for this specific flag
        if (!(event.getEntity() instanceof Vehicle)) return;

        PlayerFactory factory = getFactoryAt(event.getEntity().getLocation());
        if (factory == null) return;
        if (!factory.isProtectionEnabled(ProtectionFlag.VEHICLE_PLACE)) return;
        if (isAllowed(factory, event.getPlayer())) return;

        event.setCancelled(true);
        denyMessage(event.getPlayer());
    }

    // ── Fire Spread ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!isProtectionMasterEnabled()) return;

        PlayerFactory factory = getFactoryAt(event.getBlock().getLocation());
        if (factory == null) return;
        if (!factory.isProtectionEnabled(ProtectionFlag.FIRE_SPREAD)) return;

        // Allow owner to light fires manually
        if (event.getPlayer() != null && isAllowed(factory, event.getPlayer())) return;

        // Block all non-player ignition (spread, lava, lightning) and non-owner ignition
        if (event.getCause() != BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL || event.getPlayer() == null) {
            event.setCancelled(true);
        } else {
            // Non-owner with flint and steel
            event.setCancelled(true);
            denyMessage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (!isProtectionMasterEnabled()) return;

        PlayerFactory factory = getFactoryAt(event.getBlock().getLocation());
        if (factory == null) return;
        if (!factory.isProtectionEnabled(ProtectionFlag.FIRE_SPREAD)) return;

        event.setCancelled(true);
    }

    // ── Liquid Flow ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (!isProtectionMasterEnabled()) return;

        Material type = event.getBlock().getType();
        if (type != Material.WATER && type != Material.LAVA) return;

        // Check if liquid is flowing INTO a factory region from outside
        PlayerFactory toFactory = getFactoryAt(event.getToBlock().getLocation());
        if (toFactory == null) return;
        if (!toFactory.isProtectionEnabled(ProtectionFlag.LIQUID_FLOW)) return;

        PlayerFactory fromFactory = getFactoryAt(event.getBlock().getLocation());
        // Only block if liquid source is outside the factory region
        if (fromFactory == null || !fromFactory.getId().equals(toFactory.getId())) {
            event.setCancelled(true);
        }
    }

    // ── Piston ──────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!isProtectionMasterEnabled()) return;

        for (Block block : event.getBlocks()) {
            PlayerFactory factory = getFactoryAt(block.getLocation());
            if (factory != null && factory.isProtectionEnabled(ProtectionFlag.PISTON)) {
                // Block piston if it originates from outside the factory
                PlayerFactory pistonFactory = getFactoryAt(event.getBlock().getLocation());
                if (pistonFactory == null || !pistonFactory.getId().equals(factory.getId())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!isProtectionMasterEnabled()) return;

        for (Block block : event.getBlocks()) {
            PlayerFactory factory = getFactoryAt(block.getLocation());
            if (factory != null && factory.isProtectionEnabled(ProtectionFlag.PISTON)) {
                PlayerFactory pistonFactory = getFactoryAt(event.getBlock().getLocation());
                if (pistonFactory == null || !pistonFactory.getId().equals(factory.getId())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    // ── Hanging Entity Break (Item Frames, Paintings) ───────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (!isProtectionMasterEnabled()) return;

        PlayerFactory factory = getFactoryAt(event.getEntity().getLocation());
        if (factory == null) return;
        if (!factory.isProtectionEnabled(ProtectionFlag.HANGING_BREAK)) return;

        if (event.getRemover() instanceof Player) {
            Player player = (Player) event.getRemover();
            if (isAllowed(factory, player)) return;
            event.setCancelled(true);
            denyMessage(player);
        } else {
            event.setCancelled(true);
        }
    }

    // ── Armor Stand Manipulation ────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (!isProtectionMasterEnabled()) return;

        PlayerFactory factory = getFactoryAt(event.getRightClicked().getLocation());
        if (factory == null) return;
        if (!factory.isProtectionEnabled(ProtectionFlag.ARMOR_STAND)) return;
        if (isAllowed(factory, event.getPlayer())) return;

        event.setCancelled(true);
        denyMessage(event.getPlayer());
    }

    // ── Utility: Container check ────────────────────────────────────────────

    private boolean isContainer(Material material) {
        switch (material) {
            case CHEST:
            case TRAPPED_CHEST:
            case BARREL:
            case SHULKER_BOX:
            case WHITE_SHULKER_BOX:
            case ORANGE_SHULKER_BOX:
            case MAGENTA_SHULKER_BOX:
            case LIGHT_BLUE_SHULKER_BOX:
            case YELLOW_SHULKER_BOX:
            case LIME_SHULKER_BOX:
            case PINK_SHULKER_BOX:
            case GRAY_SHULKER_BOX:
            case LIGHT_GRAY_SHULKER_BOX:
            case CYAN_SHULKER_BOX:
            case PURPLE_SHULKER_BOX:
            case BLUE_SHULKER_BOX:
            case BROWN_SHULKER_BOX:
            case GREEN_SHULKER_BOX:
            case RED_SHULKER_BOX:
            case BLACK_SHULKER_BOX:
            case FURNACE:
            case BLAST_FURNACE:
            case SMOKER:
            case HOPPER:
            case DROPPER:
            case DISPENSER:
            case BREWING_STAND:
                return true;
            default:
                return false;
        }
    }

    // ── Utility: Redstone component check ───────────────────────────────────

    private boolean isRedstoneComponent(Material material) {
        switch (material) {
            case REPEATER:
            case COMPARATOR:
            case DAYLIGHT_DETECTOR:
            case REDSTONE_WIRE:
            case LEVER:
            case STONE_BUTTON:
            case OAK_BUTTON:
            case SPRUCE_BUTTON:
            case BIRCH_BUTTON:
            case JUNGLE_BUTTON:
            case ACACIA_BUTTON:
            case DARK_OAK_BUTTON:
            case WARPED_BUTTON:
            case CRIMSON_BUTTON:
                return true;
            default:
                return false;
        }
    }
}
