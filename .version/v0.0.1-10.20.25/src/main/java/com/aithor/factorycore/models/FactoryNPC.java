package com.aithor.factorycore.models;

import java.util.UUID;

public class FactoryNPC {
    private String id;
    private String factoryId;
    private String name;
    private org.bukkit.Location location;
    private UUID entityUUID;
    
    public FactoryNPC(String id, String factoryId, String name, org.bukkit.Location location) {
        this.id = id;
        this.factoryId = factoryId;
        this.name = name;
        this.location = location;
    }
    
    public String getId() { return id; }
    public String getFactoryId() { return factoryId; }
    public String getName() { return name; }
    public org.bukkit.Location getLocation() { return location; }
    public UUID getEntityUUID() { return entityUUID; }
    public void setEntityUUID(UUID entityUUID) { this.entityUUID = entityUUID; }
}
