package com.aithor.factorycore.models;

import java.util.*;

public class ResourceItem {
    private String id;
    private ResourceType type;
    private String material;
    private String name;
    private List<String> lore;
    private int customModelData;
    private boolean glow;
    private double sellPrice;
    
    public ResourceItem(String id, ResourceType type, String material) {
        this.id = id;
        this.type = type;
        this.material = material;
        this.lore = new ArrayList<>();
        this.customModelData = 0;
        this.glow = false;
        this.sellPrice = 0.0;
    }
    
    public String getId() { return id; }
    public ResourceType getType() { return type; }
    public String getMaterial() { return material; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getLore() { return lore; }
    public void setLore(List<String> lore) { this.lore = lore; }
    public int getCustomModelData() { return customModelData; }
    public void setCustomModelData(int customModelData) { this.customModelData = customModelData; }
    public boolean isGlow() { return glow; }
    public void setGlow(boolean glow) { this.glow = glow; }
    public double getSellPrice() { return sellPrice; }
    public void setSellPrice(double sellPrice) { this.sellPrice = sellPrice; }
}
