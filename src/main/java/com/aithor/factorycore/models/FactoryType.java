package com.aithor.factorycore.models;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class FactoryType {
    private static final Map<String, FactoryType> registry = new LinkedHashMap<>();

    private final String id;
    private final String displayName;

    private FactoryType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String name() {
        return id;
    }

    public static FactoryType fromId(String id) {
        if (id == null) return null;
        for (FactoryType type : values()) {
            if (type.getId().equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }

    public static FactoryType valueOf(String id) {
        return fromId(id);
    }

    public static Collection<FactoryType> values() {
        return registry.values();
    }

    public static void load(FileConfiguration config) {
        registry.clear();
        ConfigurationSection typesConfig = config.getConfigurationSection("factory-types");
        if (typesConfig != null) {
            for (String key : typesConfig.getKeys(false)) {
                String name = typesConfig.getString(key + ".name", "§7" + key);
                registry.put(key.toLowerCase(), new FactoryType(key, name));
            }
        }
    }
}
