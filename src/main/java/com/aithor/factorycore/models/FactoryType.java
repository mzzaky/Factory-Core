package com.aithor.factorycore.models;
import java.util.*;

public enum FactoryType {
    STEEL_MILL("steel_mill", "§6Steel Mill"),
    REFINERY("refinery", "§eRefinery"),
    WORKSHOP("workshop", "§bWorkshop"),
    ADVANCED_FACTORY("advanced_factory", "§5Advanced Factory");
    
    private final String id;
    private final String displayName;
    
    FactoryType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    
    public static FactoryType fromId(String id) {
        for (FactoryType type : values()) {
            if (type.getId().equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}
