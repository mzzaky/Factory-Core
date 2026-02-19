package com.aithor.factorycore.managers;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.models.FactoryNPC;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Advanced NPCManager — manages Factory Employee NPCs with:
 * - Look-at-player head tracking
 * - Smooth idle head animation
 * - Particle effects
 * - Hologram (floating text via ArmorStand)
 * - Ambient sounds
 * - Interact sounds
 * - Auto-respawn scheduler
 */
public class NPCManager {

    private final FactoryCore plugin;

    // Core data
    private final Map<String, FactoryNPC> npcs = new HashMap<>();
    private final Map<UUID, String> entityToFactory = new HashMap<>(); // villager UUID → factory ID
    private final Map<UUID, String> entityToNpcId = new HashMap<>(); // villager UUID → npc ID

    // Files
    private final File dataFile;
    private final File configFile;
    private FileConfiguration npcData;
    private FileConfiguration npcSettings;

    // Scheduler tasks
    private BukkitTask behaviorTask;
    private BukkitTask respawnTask;

    // Manual tick counter (replacement for non-existent Server#getCurrentTick)
    private final AtomicLong tickCounter = new AtomicLong(0);

    // ─── Constructor ──────────────────────────────────────────────────────────

    public NPCManager(FactoryCore plugin) {
        this.plugin = plugin;

        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists())
            dataFolder.mkdirs();

        this.dataFile = new File(dataFolder, "npcs.yml");
        this.configFile = new File(plugin.getDataFolder(), "npc.yml");

        loadNPCSettings();
        loadNPCs();
        startBehaviorScheduler();
        startRespawnScheduler();
    }

    // ─── Config Loading ───────────────────────────────────────────────────────

    private void loadNPCSettings() {
        if (!configFile.exists()) {
            plugin.saveResource("npc.yml", false);
        }
        npcSettings = YamlConfiguration.loadConfiguration(configFile);
    }

    private void loadNPCs() {
        if (!dataFile.exists())
            return;

        npcData = YamlConfiguration.loadConfiguration(dataFile);

        for (String key : npcData.getKeys(false)) {
            try {
                String factoryId = npcData.getString(key + ".factory-id");
                String name = npcData.getString(key + ".name");
                String template = npcData.getString(key + ".template", "default");

                // Purchased NPC fields
                String npcTypeId = npcData.getString(key + ".npc-type-id", null);
                String ownerStr = npcData.getString(key + ".owner-id", null);
                UUID ownerId = null;
                if (ownerStr != null) {
                    try {
                        ownerId = UUID.fromString(ownerStr);
                    } catch (Exception ignored) {
                    }
                }
                double productionTimeReduction = npcData.getDouble(key + ".production-time-reduction", 0.0);

                // Location (may be null for unassigned purchased NPCs)
                Location location = null;
                if (npcData.contains(key + ".location.world")) {
                    String worldName = npcData.getString(key + ".location.world");
                    double x = npcData.getDouble(key + ".location.x");
                    double y = npcData.getDouble(key + ".location.y");
                    double z = npcData.getDouble(key + ".location.z");
                    float yaw = (float) npcData.getDouble(key + ".location.yaw");
                    float pit = (float) npcData.getDouble(key + ".location.pitch");
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        location = new Location(world, x, y, z, yaw, pit);
                    } else {
                        plugin.getLogger().warning("World '" + worldName + "' not found for NPC: " + key);
                    }
                }

                // Purchased NPCs with no factory (unassigned) get a null location — that's OK
                if (location == null && factoryId != null && !factoryId.isEmpty()) {
                    plugin.getLogger()
                            .warning("NPC " + key + " has factory-id but no valid location — skipping spawn.");
                }

                FactoryNPC npc = new FactoryNPC(key, factoryId, name, location);
                npc.setTemplate(template);
                npc.setNpcTypeId(npcTypeId);
                npc.setOwnerId(ownerId);
                npc.setProductionTimeReduction(productionTimeReduction);

                if (npcData.contains(key + ".entity-uuid")) {
                    UUID uuid = UUID.fromString(npcData.getString(key + ".entity-uuid"));
                    npc.setEntityUUID(uuid);
                    if (factoryId != null)
                        entityToFactory.put(uuid, factoryId);
                    entityToNpcId.put(uuid, key);
                }

                npcs.put(key, npc);

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load NPC: " + key);
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded " + npcs.size() + " NPC(s).");
    }

    // ─── Persistence ──────────────────────────────────────────────────────────

    public void saveAll() {
        FileConfiguration config = new YamlConfiguration();

        for (FactoryNPC npc : npcs.values()) {
            String path = npc.getId();
            config.set(path + ".factory-id", npc.getFactoryId());
            config.set(path + ".name", npc.getName());
            config.set(path + ".template", npc.getTemplate());

            // Purchased NPC fields
            if (npc.getNpcTypeId() != null) {
                config.set(path + ".npc-type-id", npc.getNpcTypeId());
            }
            if (npc.getOwnerId() != null) {
                config.set(path + ".owner-id", npc.getOwnerId().toString());
            }
            config.set(path + ".production-time-reduction", npc.getProductionTimeReduction());

            // Location (may be null for unassigned purchased NPCs)
            Location loc = npc.getLocation();
            if (loc != null && loc.getWorld() != null) {
                config.set(path + ".location.world", loc.getWorld().getName());
                config.set(path + ".location.x", loc.getX());
                config.set(path + ".location.y", loc.getY());
                config.set(path + ".location.z", loc.getZ());
                config.set(path + ".location.yaw", (double) loc.getYaw());
                config.set(path + ".location.pitch", (double) loc.getPitch());
            }

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

    // ─── Spawn / Remove ───────────────────────────────────────────────────────

    /**
     * Spawn an NPC with the default template.
     */
    public boolean spawnNPC(String factoryId, String npcId, Location location) {
        return spawnNPCWithTemplate(factoryId, npcId, location, "default");
    }

    /**
     * Spawn an NPC with a specific template defined in npc.yml.
     */
    public boolean spawnNPCWithTemplate(String factoryId, String npcId, Location location, String template) {
        if (npcs.containsKey(npcId))
            return false;

        if (plugin.getFactoryManager().getFactory(factoryId) == null) {
            plugin.getLogger().warning("Factory not found: " + factoryId + ". Cannot spawn NPC.");
            return false;
        }

        // Resolve template config path
        String configPath = resolveConfigPath(template);

        String name = npcSettings.getString(configPath + ".name",
                npcSettings.getString("default.name", "§6Factory Employee"));

        FactoryNPC npc = new FactoryNPC(npcId, factoryId, name, location);
        npc.setTemplate(template);
        npcs.put(npcId, npc);

        spawnVillager(npc, configPath);
        saveAll();
        return true;
    }

    /**
     * Remove an NPC and its holograms from the world.
     */
    public boolean removeNPC(String id) {
        FactoryNPC npc = npcs.remove(id);
        if (npc == null)
            return false;

        removeVillagerEntity(npc);
        removeHolograms(npc);
        entityToFactory.remove(npc.getEntityUUID());
        entityToNpcId.remove(npc.getEntityUUID());

        saveAll();
        return true;
    }

    // ─── Internal Spawn Logic ─────────────────────────────────────────────────

    /**
     * Physically spawn the Villager entity and its holograms.
     */
    private void spawnVillager(FactoryNPC npc, String configPath) {
        Location loc = npc.getLocation();
        if (loc.getWorld() == null)
            return;

        // Remove any stale entity first
        removeVillagerEntity(npc);
        removeHolograms(npc);

        Villager villager = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);

        // ── Basic properties ──────────────────────────────────────────────────
        villager.setCustomName(npc.getName());
        villager.setCustomNameVisible(npcSettings.getBoolean(configPath + ".custom-name-visible", true));
        villager.setAI(false);
        villager.setInvulnerable(npcSettings.getBoolean(configPath + ".invulnerable", true));
        villager.setSilent(true);
        villager.setGravity(false);
        villager.setCollidable(false);

        // ── Profession & Type ─────────────────────────────────────────────────
        String professionName = npcSettings.getString(configPath + ".profession", "TOOLSMITH");
        Villager.Profession profession = parseProfession(professionName);
        villager.setProfession(profession);

        String typeName = npcSettings.getString(configPath + ".type", "PLAINS");
        Villager.Type villagerType = parseVillagerType(typeName);
        villager.setVillagerType(villagerType);

        // ── Glow ──────────────────────────────────────────────────────────────
        if (npcSettings.getBoolean(configPath + ".glow", true)) {
            villager.setGlowing(true);
        }

        // ── Register ──────────────────────────────────────────────────────────
        npc.setEntityUUID(villager.getUniqueId());
        if (npc.getFactoryId() != null) {
            entityToFactory.put(villager.getUniqueId(), npc.getFactoryId());
        }
        entityToNpcId.put(villager.getUniqueId(), npc.getId());

        // ── Hologram ──────────────────────────────────────────────────────────
        spawnHolograms(npc, configPath, loc);

        saveAll();
    }

    // ─── Hologram ─────────────────────────────────────────────────────────────

    private void spawnHolograms(FactoryNPC npc, String configPath, Location baseLoc) {
        if (!npcSettings.getBoolean(configPath + ".hologram.enabled",
                npcSettings.getBoolean("default.hologram.enabled", true)))
            return;

        List<String> lines = npcSettings.getStringList(configPath + ".hologram.lines");
        if (lines.isEmpty()) {
            lines = npcSettings.getStringList("default.hologram.lines");
        }

        double heightOffset = npcSettings.getDouble(configPath + ".hologram.height-offset",
                npcSettings.getDouble("default.hologram.height-offset", 2.3));
        double lineSpacing = npcSettings.getDouble(configPath + ".hologram.line-spacing",
                npcSettings.getDouble("default.hologram.line-spacing", 0.3));

        npc.clearHologramUUIDs();

        // Lines are listed bottom-to-top in config; render from top down
        for (int i = lines.size() - 1; i >= 0; i--) {
            int lineIndex = lines.size() - 1 - i;
            double yOffset = heightOffset + lineIndex * lineSpacing;

            Location holoLoc = baseLoc.clone().add(0, yOffset, 0);
            ArmorStand stand = (ArmorStand) baseLoc.getWorld().spawnEntity(holoLoc, EntityType.ARMOR_STAND);

            // npc.yml uses § directly; also support & for convenience
            String holoLine = ChatColor.translateAlternateColorCodes('&', lines.get(i));
            stand.setCustomName(holoLine);
            stand.setCustomNameVisible(true);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setCanPickupItems(false);
            stand.setArms(false);
            stand.setBasePlate(false);
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.setCollidable(false);
            stand.setSmall(true);
            stand.setHeadPose(new EulerAngle(0, 0, 0));

            npc.addHologramUUID(stand.getUniqueId());
        }
    }

    private void removeHolograms(FactoryNPC npc) {
        for (UUID uuid : npc.getHologramUUIDs()) {
            Entity e = Bukkit.getEntity(uuid);
            if (e != null)
                e.remove();
        }
        npc.clearHologramUUIDs();
    }

    // ─── Entity Removal ───────────────────────────────────────────────────────

    private void removeVillagerEntity(FactoryNPC npc) {
        if (npc.getEntityUUID() == null)
            return;

        Entity entity = Bukkit.getEntity(npc.getEntityUUID());
        if (entity != null) {
            entity.remove();
        } else {
            // Fallback: search nearby
            Location loc = npc.getLocation();
            if (loc.getWorld() != null) {
                loc.getWorld().getNearbyEntities(loc, 2, 2, 2).forEach(e -> {
                    if (e.getUniqueId().equals(npc.getEntityUUID()))
                        e.remove();
                });
            }
        }
    }

    // ─── Behavior Scheduler ───────────────────────────────────────────────────

    /**
     * Main behavior tick — runs every tick or at configured intervals.
     * Handles: look-at-player, idle animation, particles, ambient sounds.
     */
    private void startBehaviorScheduler() {
        if (behaviorTask != null)
            behaviorTask.cancel();

        // Run every tick (1 tick = 50ms) for smooth head tracking
        behaviorTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long tick = tickCounter.getAndIncrement();

            boolean lookEnabled = npcSettings.getBoolean("global.enable-look-at-player", true);
            boolean idleEnabled = npcSettings.getBoolean("global.enable-idle-animation", true);
            boolean particleEnabled = npcSettings.getBoolean("global.enable-particles", true);
            boolean ambientEnabled = npcSettings.getBoolean("global.ambient-sound-enabled", true);

            double lookRadius = npcSettings.getDouble("global.look-at-player-radius", 6.0);
            int idleInterval = npcSettings.getInt("global.idle-animation-interval", 60);
            int particleInterval = npcSettings.getInt("global.particle-interval", 40);
            int ambientInterval = npcSettings.getInt("global.ambient-sound-interval", 300);

            for (FactoryNPC npc : npcs.values()) {
                if (npc.getEntityUUID() == null)
                    continue;

                Entity rawEntity = Bukkit.getEntity(npc.getEntityUUID());
                if (!(rawEntity instanceof Villager))
                    continue;
                Villager villager = (Villager) rawEntity;

                Location npcLoc = villager.getLocation();

                // ── 1. Look-at-player ─────────────────────────────────────────
                Player nearest = getNearestPlayer(npcLoc, lookRadius);

                if (lookEnabled && nearest != null) {
                    lookAt(villager, nearest.getLocation()); // feet pos; lookAt() applies eye/chest offsets internally
                    npc.setIdleStep(0); // Reset idle when tracking a player
                } else if (idleEnabled && tick % idleInterval == 0) {
                    // ── 2. Idle animation ─────────────────────────────────────
                    tickIdleAnimation(npc, villager);
                }

                // ── 3. Particles ──────────────────────────────────────────────
                if (particleEnabled && tick % particleInterval == 0) {
                    spawnParticles(npc, villager.getLocation());
                }

                // ── 4. Ambient sound ──────────────────────────────────────────
                if (ambientEnabled && tick % ambientInterval == 0) {
                    playAmbientSound(npc, villager.getLocation());
                }
            }
        }, 1L, 1L);
    }

    // ─── Look-at Logic ────────────────────────────────────────────────────────

    /**
     * Rotate the villager to face a player naturally.
     *
     * Root cause of the "looking too high" bug:
     * - villager.getLocation() returns the feet position (Y = ground).
     * - player.getEyeLocation() returns ~1.62 blocks above the player's feet.
     * - The angle is computed from NPC feet → player eyes, which is a steep
     * upward angle even when standing right next to each other.
     *
     * Fix: compute the angle from the NPC's eye level (~1.62 blocks above feet)
     * toward the player's chest/torso (~1.1 blocks above feet) so the NPC
     * appears to look naturally at the player's face area.
     */
    private void lookAt(Villager villager, Location playerFeet) {
        Location npcFeet = villager.getLocation();

        // NPC eye height for a Villager entity (~1.62 blocks)
        final double NPC_EYE_HEIGHT = 1.62;
        // Target: player chest/torso — feels more natural than aiming at eyes
        final double PLAYER_CHEST_HEIGHT = 1.1;

        double fromX = npcFeet.getX();
        double fromY = npcFeet.getY() + NPC_EYE_HEIGHT;
        double fromZ = npcFeet.getZ();

        double toX = playerFeet.getX();
        double toY = playerFeet.getY() + PLAYER_CHEST_HEIGHT;
        double toZ = playerFeet.getZ();

        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;

        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) -Math.toDegrees(Math.atan2(dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, distXZ));

        // Clamp pitch to a natural range (avoid extreme up/down)
        pitch = Math.max(-25f, Math.min(25f, pitch));

        Location newLoc = npcFeet.clone();
        newLoc.setYaw(yaw);
        newLoc.setPitch(pitch);

        villager.teleport(newLoc);
    }

    // ─── Idle Animation ───────────────────────────────────────────────────────

    /**
     * Smooth idle head movement using linear interpolation toward a random target.
     */
    private void tickIdleAnimation(FactoryNPC npc, Villager villager) {
        String configPath = resolveConfigPath(npc.getTemplate());

        float yawRange = (float) npcSettings.getDouble(configPath + ".idle-animation.yaw-range",
                npcSettings.getDouble("default.idle-animation.yaw-range", 30.0));
        float pitchRange = (float) npcSettings.getDouble(configPath + ".idle-animation.pitch-range",
                npcSettings.getDouble("default.idle-animation.pitch-range", 15.0));
        int steps = npcSettings.getInt(configPath + ".idle-animation.smooth-steps",
                npcSettings.getInt("default.idle-animation.smooth-steps", 5));

        // Pick a new random target when step resets
        if (npc.getIdleStep() <= 0) {
            Random rng = new Random();
            npc.setTargetYawOffset((rng.nextFloat() * 2 - 1) * yawRange);
            npc.setTargetPitchOffset((rng.nextFloat() * 2 - 1) * pitchRange);
            npc.setIdleStep(steps);
        }

        // Interpolate current rotation toward target
        Location loc = villager.getLocation();
        float baseYaw = npc.getLocation().getYaw();
        float basePitch = npc.getLocation().getPitch();

        float progress = 1f - ((float) npc.getIdleStep() / steps);
        float newYaw = baseYaw + npc.getTargetYawOffset() * progress;
        float newPitch = basePitch + npc.getTargetPitchOffset() * progress;

        loc.setYaw(newYaw);
        loc.setPitch(newPitch);
        villager.teleport(loc);

        npc.setIdleStep(npc.getIdleStep() - 1);
    }

    // ─── Particles ────────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private void spawnParticles(FactoryNPC npc, Location loc) {
        String configPath = resolveConfigPath(npc.getTemplate());

        String particleName = npcSettings.getString(configPath + ".particle.type",
                npcSettings.getString("default.particle.type", "VILLAGER_HAPPY"));
        int count = npcSettings.getInt(configPath + ".particle.count",
                npcSettings.getInt("default.particle.count", 3));
        double offX = npcSettings.getDouble(configPath + ".particle.offset-x",
                npcSettings.getDouble("default.particle.offset-x", 0.3));
        double offY = npcSettings.getDouble(configPath + ".particle.offset-y",
                npcSettings.getDouble("default.particle.offset-y", 0.5));
        double offZ = npcSettings.getDouble(configPath + ".particle.offset-z",
                npcSettings.getDouble("default.particle.offset-z", 0.3));
        double speed = npcSettings.getDouble(configPath + ".particle.speed",
                npcSettings.getDouble("default.particle.speed", 0.05));

        try {
            Particle particle = Particle.valueOf(particleName);
            loc.getWorld().spawnParticle(particle, loc.clone().add(0, 1.5, 0),
                    count, offX, offY, offZ, speed);
        } catch (Exception e) {
            // Silently ignore unknown particle types
        }
    }

    // ─── Sounds ───────────────────────────────────────────────────────────────

    /**
     * Play the interact sound to a specific player when they click the NPC.
     */
    public void playInteractSound(Player player, FactoryNPC npc) {
        String soundName = npcSettings.getString("global.interact-sound", "ENTITY_VILLAGER_YES");
        float volume = (float) npcSettings.getDouble("global.interact-sound-volume", 0.8);
        float pitch = (float) npcSettings.getDouble("global.interact-sound-pitch", 1.0);

        Sound sound = parseSound(soundName);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    /**
     * Play ambient sound to nearby players.
     */
    private void playAmbientSound(FactoryNPC npc, Location loc) {
        String soundName = npcSettings.getString("global.ambient-sound", "ENTITY_VILLAGER_AMBIENT");
        float volume = (float) npcSettings.getDouble("global.ambient-sound-volume", 0.3);
        float pitch = (float) npcSettings.getDouble("global.ambient-sound-pitch", 1.0);
        double radius = npcSettings.getDouble("global.ambient-sound-radius", 8.0);

        Sound sound = parseSound(soundName);
        if (sound == null)
            return;

        loc.getWorld().getNearbyEntities(loc, radius, radius, radius).forEach(e -> {
            if (e instanceof Player) {
                ((Player) e).playSound(loc, sound, volume, pitch);
            }
        });
    }

    // ─── Respawn Scheduler ────────────────────────────────────────────────────

    private void startRespawnScheduler() {
        if (respawnTask != null)
            respawnTask.cancel();

        int interval = npcSettings.getInt("global.respawn-check-interval", 200);

        respawnTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (FactoryNPC npc : npcs.values()) {
                // Skip unassigned purchased NPCs (no location = not in world)
                if (npc.getLocation() == null)
                    continue;
                if (!isEntityAlive(npc)) {
                    plugin.getLogger().info("Respawning missing NPC: " + npc.getId());
                    String configPath = resolveConfigPath(npc.getTemplate());
                    spawnVillager(npc, configPath);
                }
            }
        }, interval, interval);
    }

    // ─── Utility ──────────────────────────────────────────────────────────────

    /**
     * Resolve the config section path for a given template name.
     * Falls back to "default" if the template doesn't exist.
     */
    private String resolveConfigPath(String template) {
        if (template == null || template.equals("default") || !npcSettings.contains("templates." + template)) {
            return "default";
        }
        return "templates." + template;
    }

    /**
     * Check whether the villager entity for an NPC is alive in the world.
     */
    private boolean isEntityAlive(FactoryNPC npc) {
        if (npc.getEntityUUID() == null)
            return false;

        Entity entity = Bukkit.getEntity(npc.getEntityUUID());
        if (entity != null && !entity.isDead())
            return true;

        // Fallback: search nearby
        Location loc = npc.getLocation();
        if (loc == null || loc.getWorld() == null)
            return false;

        return loc.getWorld().getNearbyEntities(loc, 2, 2, 2)
                .stream()
                .anyMatch(e -> e.getUniqueId().equals(npc.getEntityUUID()) && !e.isDead());
    }

    /**
     * Find the nearest player within a given radius of a location.
     */
    private Player getNearestPlayer(Location loc, double radius) {
        Player nearest = null;
        double minDist = Double.MAX_VALUE;

        for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (!(e instanceof Player))
                continue;
            double dist = e.getLocation().distanceSquared(loc);
            if (dist < minDist) {
                minDist = dist;
                nearest = (Player) e;
            }
        }
        return nearest;
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public FactoryNPC getNPC(String id) {
        return npcs.get(id);
    }

    public FactoryNPC getNPCByEntityUUID(UUID entityUUID) {
        String npcId = entityToNpcId.get(entityUUID);
        return npcId != null ? npcs.get(npcId) : null;
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

    /**
     * Expose the npc.yml settings for use by GUIs.
     */
    public FileConfiguration getNpcSettings() {
        return npcSettings;
    }

    // ─── Shop / Ownership API ─────────────────────────────────────────────────

    /**
     * Purchase an NPC from the Employee Shop.
     * Deducts the buy price from the player's balance and creates an unassigned NPC
     * record.
     * Returns the new NPC ID on success, or null on failure.
     */
    public String purchaseNPC(org.bukkit.entity.Player player, String npcTypeId) {
        ConfigurationSection npcSection = npcSettings.getConfigurationSection("shop.npcs." + npcTypeId);
        if (npcSection == null) {
            player.sendMessage("§cEmployee type not found!");
            return null;
        }

        double buyPrice = npcSection.getDouble("buy-price", 0.0);
        if (!plugin.getEconomy().has(player, buyPrice)) {
            player.sendMessage(plugin.getLanguageManager().getMessage("insufficient-funds")
                    .replace("{amount}", String.format("%.2f", buyPrice)));
            return null;
        }

        // Deduct cost
        plugin.getEconomy().withdrawPlayer(player, buyPrice);

        // Create NPC record (unassigned — no factory, no location)
        String npcId = "npc_" + player.getUniqueId().toString().replace("-", "").substring(0, 8)
                + "_" + System.currentTimeMillis();

        String displayName = npcSection.getString("display-name", "§fEmployee");
        String template = npcSection.getString("template", "default");
        double reduction = npcSection.getDouble("production-time-reduction", 0.0);

        FactoryNPC npc = new FactoryNPC(npcId, null, displayName, null);
        npc.setTemplate(template);
        npc.setNpcTypeId(npcTypeId);
        npc.setOwnerId(player.getUniqueId());
        npc.setProductionTimeReduction(reduction);

        npcs.put(npcId, npc);
        saveAll();

        plugin.getLogger()
                .info("Player " + player.getName() + " purchased NPC type '" + npcTypeId + "' (id: " + npcId + ")");
        return npcId;
    }

    /**
     * Returns all NPCs owned by a player that are NOT assigned to any factory.
     */
    public List<FactoryNPC> getUnassignedNPCsByOwner(UUID ownerId) {
        return npcs.values().stream()
                .filter(npc -> ownerId.equals(npc.getOwnerId()))
                .filter(npc -> npc.getFactoryId() == null || npc.getFactoryId().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Returns all NPCs owned by a player (assigned and unassigned).
     */
    public List<FactoryNPC> getOwnedNPCsByOwner(UUID ownerId) {
        return npcs.values().stream()
                .filter(npc -> ownerId.equals(npc.getOwnerId()))
                .collect(Collectors.toList());
    }

    /**
     * Returns the purchased NPC assigned to a specific factory, or null if none.
     * Only returns purchased NPCs (with ownerId set).
     */
    public FactoryNPC getAssignedNPCForFactory(String factoryId) {
        for (FactoryNPC npc : npcs.values()) {
            if (factoryId.equals(npc.getFactoryId()) && npc.isPurchased()) {
                return npc;
            }
        }
        return null;
    }

    /**
     * Returns any NPC (admin-spawned or purchased) for a factory.
     */
    public FactoryNPC getAnyNPCForFactory(String factoryId) {
        for (FactoryNPC npc : npcs.values()) {
            if (factoryId.equals(npc.getFactoryId())) {
                return npc;
            }
        }
        return null;
    }

    /**
     * Assign a purchased NPC to a factory.
     * Spawns the NPC at the factory's fast-travel location (or a default location).
     * Returns false if the factory already has an assigned NPC, or if the NPC
     * doesn't belong to the player.
     */
    public boolean assignNPCToFactory(org.bukkit.entity.Player player, String npcId, String factoryId) {
        FactoryNPC npc = npcs.get(npcId);
        if (npc == null) {
            player.sendMessage("§cEmployee not found!");
            return false;
        }
        if (!player.getUniqueId().equals(npc.getOwnerId())) {
            player.sendMessage("§cThis employee doesn't belong to you!");
            return false;
        }

        // Check factory exists and player owns it
        com.aithor.factorycore.models.Factory factory = plugin.getFactoryManager().getFactory(factoryId);
        if (factory == null) {
            player.sendMessage("§cFactory not found!");
            return false;
        }
        if (!player.getUniqueId().equals(factory.getOwner())) {
            player.sendMessage("§cYou don't own this factory!");
            return false;
        }

        // Check factory doesn't already have an assigned NPC
        FactoryNPC existing = getAssignedNPCForFactory(factoryId);
        if (existing != null) {
            player.sendMessage("§cThis factory already has an employee assigned!");
            return false;
        }

        // Determine spawn location: use fast-travel location if available
        Location spawnLoc = factory.getFastTravelLocation();
        if (spawnLoc == null) {
            player.sendMessage("§cThis factory has no fast-travel location set. Ask an admin to set one first.");
            return false;
        }

        // Assign
        npc.setFactoryId(factoryId);
        npc.setLocation(spawnLoc);

        // Spawn the villager entity
        String configPath = resolveConfigPath(npc.getTemplate());
        spawnVillager(npc, configPath);

        saveAll();
        plugin.getLogger().info("NPC " + npcId + " assigned to factory " + factoryId + " by " + player.getName());
        return true;
    }

    /**
     * Unassign a purchased NPC from its factory (removes the villager entity but
     * keeps the NPC record).
     */
    public boolean unassignNPC(org.bukkit.entity.Player player, String npcId) {
        FactoryNPC npc = npcs.get(npcId);
        if (npc == null) {
            player.sendMessage("§cEmployee not found!");
            return false;
        }
        if (!player.getUniqueId().equals(npc.getOwnerId())) {
            player.sendMessage("§cThis employee doesn't belong to you!");
            return false;
        }

        // Remove entity from world
        removeVillagerEntity(npc);
        removeHolograms(npc);
        if (npc.getEntityUUID() != null) {
            entityToFactory.remove(npc.getEntityUUID());
            entityToNpcId.remove(npc.getEntityUUID());
        }

        // Clear assignment
        npc.setFactoryId(null);
        npc.setLocation(null);
        npc.setEntityUUID(null);

        saveAll();
        return true;
    }

    /**
     * Dismiss (permanently remove) a purchased NPC — removes entity and deletes the
     * record.
     */
    public boolean dismissNPC(org.bukkit.entity.Player player, String npcId) {
        FactoryNPC npc = npcs.get(npcId);
        if (npc == null)
            return false;
        if (!player.getUniqueId().equals(npc.getOwnerId())) {
            player.sendMessage("§cThis employee doesn't belong to you!");
            return false;
        }
        return removeNPC(npcId);
    }

    /**
     * Get the total production time reduction for a factory based on its assigned
     * NPC.
     * Returns 0.0 if no NPC is assigned.
     */
    public double getProductionTimeReductionForFactory(String factoryId) {
        FactoryNPC npc = getAssignedNPCForFactory(factoryId);
        if (npc != null)
            return npc.getProductionTimeReduction();
        // Also check admin-spawned NPCs (they have 0 reduction by default)
        FactoryNPC adminNpc = getAnyNPCForFactory(factoryId);
        return adminNpc != null ? adminNpc.getProductionTimeReduction() : 0.0;
    }

    /**
     * Check if a factory has any employee (admin-spawned or purchased).
     */
    public boolean factoryHasEmployee(String factoryId) {
        return getAnyNPCForFactory(factoryId) != null;
    }

    /**
     * Respawn a specific NPC if it's missing from the world.
     */
    public boolean respawnNPCIfNotExists(String npcId) {
        FactoryNPC npc = npcs.get(npcId);
        if (npc == null)
            return false;

        if (isEntityAlive(npc))
            return false;

        String configPath = resolveConfigPath(npc.getTemplate());
        spawnVillager(npc, configPath);
        return true;
    }

    /**
     * Respawn all NPCs that are missing from the world.
     */
    public void respawnAllNPCsIfNotExists() {
        for (FactoryNPC npc : npcs.values()) {
            respawnNPCIfNotExists(npc.getId());
        }
    }

    /**
     * Full reload: cancel tasks, remove all entities, reload config, respawn
     * everything.
     */
    public void reload() {
        // Cancel running tasks
        if (behaviorTask != null) {
            behaviorTask.cancel();
            behaviorTask = null;
        }
        if (respawnTask != null) {
            respawnTask.cancel();
            respawnTask = null;
        }

        // Remove all entities and holograms
        for (FactoryNPC npc : npcs.values()) {
            removeVillagerEntity(npc);
            removeHolograms(npc);
        }

        npcs.clear();
        entityToFactory.clear();
        entityToNpcId.clear();

        // Reload
        loadNPCSettings();
        loadNPCs();

        // Respawn all
        respawnAllNPCsIfNotExists();

        // Restart schedulers
        startBehaviorScheduler();
        startRespawnScheduler();

        plugin.getLogger().info("NPCManager reloaded successfully.");
    }

    /**
     * Clean shutdown — cancel tasks and remove all NPC entities from the world.
     */
    public void shutdown() {
        if (behaviorTask != null) {
            behaviorTask.cancel();
            behaviorTask = null;
        }
        if (respawnTask != null) {
            respawnTask.cancel();
            respawnTask = null;
        }

        for (FactoryNPC npc : npcs.values()) {
            removeVillagerEntity(npc);
            removeHolograms(npc);
        }
    }

    // ─── Registry Helpers (1.21+ compatible) ─────────────────────────────────

    /**
     * Parse a Villager.Profession by name using the Registry API (1.21+).
     * Falls back to TOOLSMITH if the name is invalid.
     */
    @SuppressWarnings("deprecation")
    private Villager.Profession parseProfession(String name) {
        try {
            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
            Villager.Profession p = Registry.VILLAGER_PROFESSION.get(key);
            return p != null ? p : Villager.Profession.TOOLSMITH;
        } catch (Exception e) {
            return Villager.Profession.TOOLSMITH;
        }
    }

    /**
     * Parse a Villager.Type by name using the Registry API (1.21+).
     * Falls back to PLAINS if the name is invalid.
     */
    @SuppressWarnings("deprecation")
    private Villager.Type parseVillagerType(String name) {
        try {
            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
            Villager.Type t = Registry.VILLAGER_TYPE.get(key);
            return t != null ? t : Villager.Type.PLAINS;
        } catch (Exception e) {
            return Villager.Type.PLAINS;
        }
    }

    /**
     * Parse a Sound by name. Tries Registry first (1.21.3+), falls back to
     * the legacy enum valueOf() which is still functional even if deprecated.
     */
    @SuppressWarnings("deprecation")
    private Sound parseSound(String name) {
        // Try Registry lookup first (1.21.3+)
        try {
            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
            Sound s = Registry.SOUNDS.get(key);
            if (s != null)
                return s;
        } catch (Exception ignored) {
            // Registry.SOUNDS may not exist on older versions — fall through
        }
        // Fallback: legacy enum (still works, just deprecated)
        try {
            return Sound.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}
