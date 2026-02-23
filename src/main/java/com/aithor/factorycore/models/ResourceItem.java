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

    // MMOItems integration fields
    private boolean mmoItem;
    private String mmoItemsType;
    private String mmoItemsId;

    // ExecutableItems integration fields
    private boolean executableItem;
    private String executableItemsId;

    public ResourceItem(String id, ResourceType type, String material) {
        this.id = id;
        this.type = type;
        this.material = material;
        this.lore = new ArrayList<>();
        this.customModelData = 0;
        this.glow = false;
        this.sellPrice = 0.0;
        this.mmoItem = false;
        this.mmoItemsType = null;
        this.mmoItemsId = null;
        this.executableItem = false;
        this.executableItemsId = null;
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

    // MMOItems getters & setters
    public boolean isMMOItem() { return mmoItem; }
    public void setMMOItem(boolean mmoItem) { this.mmoItem = mmoItem; }
    public String getMMOItemsType() { return mmoItemsType; }
    public void setMMOItemsType(String mmoItemsType) { this.mmoItemsType = mmoItemsType; }
    public String getMMOItemsId() { return mmoItemsId; }
    public void setMMOItemsId(String mmoItemsId) { this.mmoItemsId = mmoItemsId; }

    // ExecutableItems getters & setters
    public boolean isExecutableItem() { return executableItem; }
    public void setExecutableItem(boolean executableItem) { this.executableItem = executableItem; }
    public String getExecutableItemsId() { return executableItemsId; }
    public void setExecutableItemsId(String executableItemsId) { this.executableItemsId = executableItemsId; }
}
