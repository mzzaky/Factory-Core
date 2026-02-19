package com.aithor.factorycore.models;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a Factory NPC entity with advanced behavior state.
 * An NPC can be:
 * - A "world NPC" spawned by admin at a factory location (legacy)
 * - A "purchased NPC" bought by a player from the Employee Shop and assigned to
 * a factory
 */
public class FactoryNPC {

    private String id;
    private String factoryId;
    private String name;
    private Location location;
    private UUID entityUUID;

    // Template used when this NPC was spawned (e.g. "default",
    // "steel_mill_employee")
    private String template;

    // NPC type ID from the shop (e.g. "skilled_craftsman"). Null for legacy
    // admin-spawned NPCs.
    private String npcTypeId;

    // UUID of the player who owns/purchased this NPC. Null for legacy admin-spawned
    // NPCs.
    private UUID ownerId;

    // Production time reduction buff (%) provided by this NPC type
    private double productionTimeReduction;

    // Hologram entity UUIDs (ArmorStand per line)
    private final List<UUID> hologramUUIDs = new ArrayList<>();

    // Idle animation state: current interpolation target yaw/pitch offsets
    private float targetYawOffset = 0f;
    private float targetPitchOffset = 0f;
    private int idleStep = 0;

    public FactoryNPC(String id, String factoryId, String name, Location location) {
        this.id = id;
        this.factoryId = factoryId;
        this.name = name;
        this.location = location;
        this.template = "default";
        this.productionTimeReduction = 0.0;
    }

    // ─── Basic Getters / Setters ───────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public String getFactoryId() {
        return factoryId;
    }

    public void setFactoryId(String factoryId) {
        this.factoryId = factoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public UUID getEntityUUID() {
        return entityUUID;
    }

    public void setEntityUUID(UUID entityUUID) {
        this.entityUUID = entityUUID;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    // ─── Shop / Ownership ─────────────────────────────────────────────────────

    /**
     * Returns the NPC type ID from the shop config (e.g. "skilled_craftsman").
     * Returns null for legacy admin-spawned NPCs.
     */
    public String getNpcTypeId() {
        return npcTypeId;
    }

    public void setNpcTypeId(String npcTypeId) {
        this.npcTypeId = npcTypeId;
    }

    /**
     * Returns the UUID of the player who purchased this NPC.
     * Returns null for legacy admin-spawned NPCs.
     */
    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    /**
     * Whether this NPC was purchased from the Employee Shop (vs admin-spawned).
     */
    public boolean isPurchased() {
        return ownerId != null && npcTypeId != null;
    }

    // ─── Production Buff ──────────────────────────────────────────────────────

    /**
     * Returns the production time reduction percentage (0-100) this NPC provides.
     * E.g. 15.0 means production takes 15% less time.
     */
    public double getProductionTimeReduction() {
        return productionTimeReduction;
    }

    public void setProductionTimeReduction(double productionTimeReduction) {
        this.productionTimeReduction = productionTimeReduction;
    }

    // ─── Hologram ─────────────────────────────────────────────────────────────

    public List<UUID> getHologramUUIDs() {
        return hologramUUIDs;
    }

    public void addHologramUUID(UUID uuid) {
        hologramUUIDs.add(uuid);
    }

    public void clearHologramUUIDs() {
        hologramUUIDs.clear();
    }

    // ─── Idle Animation State ─────────────────────────────────────────────────

    public float getTargetYawOffset() {
        return targetYawOffset;
    }

    public float getTargetPitchOffset() {
        return targetPitchOffset;
    }

    public int getIdleStep() {
        return idleStep;
    }

    public void setTargetYawOffset(float v) {
        this.targetYawOffset = v;
    }

    public void setTargetPitchOffset(float v) {
        this.targetPitchOffset = v;
    }

    public void setIdleStep(int v) {
        this.idleStep = v;
    }
}
