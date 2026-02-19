package com.aithor.factorycore.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.FactoryNPC;

public class NPCManager {
    
    private final FactoryCore plugin;
    private final Map<String, FactoryNPC> npcs;
    private final Map<UUID, String> entityToFactory; // Entity UUID -> Factory ID
    private final File dataFile;
    private final File configFile;
    private FileConfiguration npcConfig;
    private FileConfiguration npcSettings;
    
    public NPCManager(FactoryCore plugin) {
        this.plugin = plugin;
        this.npcs = new HashMap<>();
        this.entityToFactory = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "npcs.yml");
        this.configFile = new File(plugin.getDataFolder(), "npc.yml");

        loadNPCSettings();
        loadNPCs();
    }

    private void loadNPCSettings() {
        if (!configFile.exists()) {
            plugin.saveResource("npc.yml", false);
        }

        npcSettings = YamlConfiguration.loadConfiguration(configFile);
    }

    private void loadNPCs() {
        if (!dataFile.exists()) {
            return;
        }
        
        npcConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        for (String key : npcConfig.getKeys(false)) {
            try {
                String factoryId = npcConfig.getString(key + ".factory-id");
                String name = npcConfig.getString(key + ".name");
                
                String worldName = npcConfig.getString(key + ".location.world");
                double x = npcConfig.getDouble(key + ".location.x");
                double y = npcConfig.getDouble(key + ".location.y");
                double z = npcConfig.getDouble(key + ".location.z");
                float yaw = (float) npcConfig.getDouble(key + ".location.yaw");
                float pitch = (float) npcConfig.getDouble(key + ".location.pitch");
                
                Location location = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                
                FactoryNPC npc = new FactoryNPC(key, factoryId, name, location);
                
                if (npcConfig.contains(key + ".entity-uuid")) {
                    UUID entityUUID = UUID.fromString(npcConfig.getString(key + ".entity-uuid"));
                    npc.setEntityUUID(entityUUID);
                    entityToFactory.put(entityUUID, factoryId);
                }
                
                npcs.put(key, npc);

                // NOTE: NPCs are not automatically respawned on server startup
                // They should only be spawned when explicitly requested via spawnNPC() methods
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load NPC: " + key);
                e.printStackTrace();
            }
        }
        
        plugin.getLogger().info("Loaded " + npcs.size() + " NPCs!");
    }
    
    public void saveAll() {
        FileConfiguration config = new YamlConfiguration();
        
        for (FactoryNPC npc : npcs.values()) {
            String path = npc.getId();
            config.set(path + ".factory-id", npc.getFactoryId());
            config.set(path + ".name", npc.getName());
            
            Location loc = npc.getLocation();
            config.set(path + ".location.world", loc.getWorld().getName());
            config.set(path + ".location.x", loc.getX());
            config.set(path + ".location.y", loc.getY());
            config.set(path + ".location.z", loc.getZ());
            config.set(path + ".location.yaw", loc.getYaw());
            config.set(path + ".location.pitch", loc.getPitch());
            
            if (npc.getEntityUUID() != null) {
                config.set(path + ".entity-uuid", npc.getEntityUUID().toString());
            }
        }
        
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save NPCs!");
            e.printStackTrace();
        }
    }
    
    public boolean spawnNPC(String factoryId, String npcId, Location location) {
        if (npcs.containsKey(npcId)) {
            return false;
        }

        // Check if factory exists
        if (plugin.getFactoryManager().getFactory(factoryId) == null) {
            plugin.getLogger().warning("Factory not found: " + factoryId + ". Cannot spawn NPC.");
            return false;
        }

        String name = npcSettings.getString("default.name", "§6Factory Employee");

        FactoryNPC npc = new FactoryNPC(npcId, factoryId, name, location);
        npcs.put(npcId, npc);

        respawnNPC(npc, "default");
        saveAll();

        return true;
    }

    public boolean spawnNPCWithTemplate(String factoryId, String npcId, Location location, String template) {
        if (npcs.containsKey(npcId)) {
            return false;
        }

        // Check if factory exists
        if (plugin.getFactoryManager().getFactory(factoryId) == null) {
            plugin.getLogger().warning("Factory not found: " + factoryId + ". Cannot spawn NPC.");
            return false;
        }

        // Check if template exists
        if (!npcSettings.contains("templates." + template)) {
            plugin.getLogger().warning("NPC template not found: " + template + ". Using default.");
            template = "default";
        }

        String name = npcSettings.getString("templates." + template + ".name", "§6Factory Employee");

        FactoryNPC npc = new FactoryNPC(npcId, factoryId, name, location);
        npcs.put(npcId, npc);

        respawnNPC(npc, "templates." + template);
        saveAll();

        return true;
    }

    private void respawnNPC(FactoryNPC npc, String npcType) {
        Location loc = npc.getLocation();

        // Spawn villager as NPC
        Villager villager = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);

        // Configure villager using npc.yml settings
        villager.setCustomName(npc.getName());
        villager.setCustomNameVisible(npcSettings.getBoolean(npcType + ".custom-name-visible", true));
        villager.setAI(false); // Always disable AI to prevent movement
        villager.setInvulnerable(npcSettings.getBoolean(npcType + ".invulnerable", true));
        villager.setSilent(true);
        villager.setGravity(false); // Prevent falling

        // Set profession and type from npc.yml
        try {
            Villager.Profession profession = Villager.Profession.valueOf(npcSettings.getString(npcType + ".profession", "TOOLSMITH"));
            villager.setProfession(profession);
        } catch (Exception e) {
            villager.setProfession(Villager.Profession.TOOLSMITH);
        }

        try {
            Villager.Type type = Villager.Type.valueOf(npcSettings.getString(npcType + ".type", "PLAINS"));
            villager.setVillagerType(type);
        } catch (Exception e) {
            villager.setVillagerType(Villager.Type.PLAINS);
        }

        // Add glow effect if enabled
        if (npcSettings.getBoolean(npcType + ".glow", true)) {
            villager.setGlowing(true);
        }


        npc.setEntityUUID(villager.getUniqueId());
        entityToFactory.put(villager.getUniqueId(), npc.getFactoryId());

        saveAll();
    }


    public boolean removeNPC(String id) {
        FactoryNPC npc = npcs.remove(id);
        if (npc == null) return false;
        
        // Remove entity from world
        if (npc.getEntityUUID() != null) {
            Location loc = npc.getLocation();
            if (loc.getWorld() != null) {
                loc.getWorld().getNearbyEntities(loc, 1, 1, 1).forEach(entity -> {
                    if (entity.getUniqueId().equals(npc.getEntityUUID())) {
                        entity.remove();
                    }
                });
            }
            entityToFactory.remove(npc.getEntityUUID());
        }
        
        saveAll();
        return true;
    }
    
    public FactoryNPC getNPC(String id) {
        return npcs.get(id);
    }
    
    public String getFactoryIdByEntity(UUID entityUUID) {
        return entityToFactory.get(entityUUID);
    }
    
    public boolean isNPC(UUID entityUUID) {
        return entityToFactory.containsKey(entityUUID);
    }
    
    public List<FactoryNPC> getAllNPCs() {
        return new ArrayList<>(npcs.values());
    }

    public void reload() {
        // Clear existing NPCs and entity mappings
        for (FactoryNPC npc : npcs.values()) {
            if (npc.getEntityUUID() != null) {
                // Remove entity from world if it exists
                Location loc = npc.getLocation();
                if (loc.getWorld() != null) {
                    loc.getWorld().getNearbyEntities(loc, 1, 1, 1).forEach(entity -> {
                        if (entity.getUniqueId().equals(npc.getEntityUUID())) {
                            entity.remove();
                        }
                    });
                }
            }
        }

        npcs.clear();
        entityToFactory.clear();

        // Reload NPC settings from npc.yml
        loadNPCSettings();

        // Reload NPCs from npcs.yml
        loadNPCs();

        // Respawn all NPCs after reload to ensure they exist
        respawnAllNPCsIfNotExists();
    }

    /**
     * Respawn a specific NPC if it's not already spawned
     * @param npcId The ID of the NPC to respawn
     * @return true if NPC was respawned, false if NPC not found or already spawned
     */
    public boolean respawnNPCIfNotExists(String npcId) {
        FactoryNPC npc = npcs.get(npcId);
        if (npc == null) {
            return false;
        }

        // Check if NPC entity already exists in the world
        if (npc.getEntityUUID() != null) {
            Location loc = npc.getLocation();
            if (loc.getWorld() != null) {
                boolean entityExists = loc.getWorld().getNearbyEntities(loc, 1, 1, 1)
                    .stream()
                    .anyMatch(entity -> entity.getUniqueId().equals(npc.getEntityUUID()));

                if (entityExists) {
                    return false; // NPC already exists
                }
            }
        }

        // Respawn the NPC
        respawnNPC(npc, "default");
        return true;
    }

    /**
     * Respawn all NPCs that don't have entities in the world
     * This is useful for restoring NPCs after server restarts
     */
    public void respawnAllNPCsIfNotExists() {
        for (FactoryNPC npc : npcs.values()) {
            respawnNPCIfNotExists(npc.getId());
        }
    }
}