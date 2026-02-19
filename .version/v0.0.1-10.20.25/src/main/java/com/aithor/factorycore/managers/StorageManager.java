package com.aithor.factorycore.managers;

import com.aithor.factorycore.FactoryCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class StorageManager {
    private final FactoryCore plugin;
    private final Map<String, Map<String, Integer>> inputStorage; // factoryId -> resourceId -> amount (for input materials)
    private final Map<String, Map<String, Integer>> outputStorage; // factoryId -> resourceId -> amount (for output products)
    private final File inputDataFile;
    private final File outputDataFile;
    
    public StorageManager(FactoryCore plugin) {
        this.plugin = plugin;
        this.inputStorage = new HashMap<>();
        this.outputStorage = new HashMap<>();
        this.inputDataFile = new File(plugin.getDataFolder(), "input-storage.yml");
        this.outputDataFile = new File(plugin.getDataFolder(), "output-storage.yml");
        loadStorage();
    }
    
    private void loadStorage() {
        // Load input storage
        if (inputDataFile.exists()) {
            FileConfiguration inputConfig = YamlConfiguration.loadConfiguration(inputDataFile);
            for (String factoryId : inputConfig.getKeys(false)) {
                Map<String, Integer> items = new HashMap<>();
                if (inputConfig.contains(factoryId)) {
                    for (String resourceId : inputConfig.getConfigurationSection(factoryId).getKeys(false)) {
                        int amount = inputConfig.getInt(factoryId + "." + resourceId);
                        items.put(resourceId, amount);
                    }
                }
                inputStorage.put(factoryId, items);
            }
        }

        // Load output storage
        if (outputDataFile.exists()) {
            FileConfiguration outputConfig = YamlConfiguration.loadConfiguration(outputDataFile);
            for (String factoryId : outputConfig.getKeys(false)) {
                Map<String, Integer> items = new HashMap<>();
                if (outputConfig.contains(factoryId)) {
                    for (String resourceId : outputConfig.getConfigurationSection(factoryId).getKeys(false)) {
                        int amount = outputConfig.getInt(factoryId + "." + resourceId);
                        items.put(resourceId, amount);
                    }
                }
                outputStorage.put(factoryId, items);
            }
        }

        // Migrate old storage.yml to new format if it exists
        File oldDataFile = new File(plugin.getDataFolder(), "storage.yml");
        if (oldDataFile.exists()) {
            FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(oldDataFile);
            for (String factoryId : oldConfig.getKeys(false)) {
                Map<String, Integer> items = new HashMap<>();
                if (oldConfig.contains(factoryId)) {
                    for (String resourceId : oldConfig.getConfigurationSection(factoryId).getKeys(false)) {
                        int amount = oldConfig.getInt(factoryId + "." + resourceId);
                        items.put(resourceId, amount);
                    }
                }
                // Assume all old data is input storage for migration
                inputStorage.put(factoryId, items);
            }
            // Save migrated data and remove old file
            saveAll();
            oldDataFile.delete();
        }
    }
    
    public void saveAll() {
        // Save input storage
        FileConfiguration inputConfig = new YamlConfiguration();
        for (Map.Entry<String, Map<String, Integer>> entry : inputStorage.entrySet()) {
            for (Map.Entry<String, Integer> item : entry.getValue().entrySet()) {
                inputConfig.set(entry.getKey() + "." + item.getKey(), item.getValue());
            }
        }
        try {
            inputConfig.save(inputDataFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save input storage!");
            e.printStackTrace();
        }

        // Save output storage
        FileConfiguration outputConfig = new YamlConfiguration();
        for (Map.Entry<String, Map<String, Integer>> entry : outputStorage.entrySet()) {
            for (Map.Entry<String, Integer> item : entry.getValue().entrySet()) {
                outputConfig.set(entry.getKey() + "." + item.getKey(), item.getValue());
            }
        }
        try {
            outputConfig.save(outputDataFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save output storage!");
            e.printStackTrace();
        }
    }
    
    public void addInputItem(String factoryId, String resourceId, int amount) {
        inputStorage.putIfAbsent(factoryId, new HashMap<>());
        Map<String, Integer> items = inputStorage.get(factoryId);
        items.put(resourceId, items.getOrDefault(resourceId, 0) + amount);
        saveAll();
    }

    public void addOutputItem(String factoryId, String resourceId, int amount) {
        outputStorage.putIfAbsent(factoryId, new HashMap<>());
        Map<String, Integer> items = outputStorage.get(factoryId);
        items.put(resourceId, items.getOrDefault(resourceId, 0) + amount);
        saveAll();
    }

    // Legacy method for backward compatibility - assumes input storage
    public void addItem(String factoryId, String resourceId, int amount) {
        addInputItem(factoryId, resourceId, amount);
    }
    
    public boolean removeInputItem(String factoryId, String resourceId, int amount) {
        if (!inputStorage.containsKey(factoryId)) return false;

        Map<String, Integer> items = inputStorage.get(factoryId);
        int current = items.getOrDefault(resourceId, 0);

        if (current < amount) return false;

        items.put(resourceId, current - amount);
        if (items.get(resourceId) <= 0) {
            items.remove(resourceId);
        }

        saveAll();
        return true;
    }

    public boolean removeOutputItem(String factoryId, String resourceId, int amount) {
        if (!outputStorage.containsKey(factoryId)) return false;

        Map<String, Integer> items = outputStorage.get(factoryId);
        int current = items.getOrDefault(resourceId, 0);

        if (current < amount) return false;

        items.put(resourceId, current - amount);
        if (items.get(resourceId) <= 0) {
            items.remove(resourceId);
        }

        saveAll();
        return true;
    }

    // Legacy method for backward compatibility - assumes input storage
    public boolean removeItem(String factoryId, String resourceId, int amount) {
        return removeInputItem(factoryId, resourceId, amount);
    }
    
    public int getInputAmount(String factoryId, String resourceId) {
        if (!inputStorage.containsKey(factoryId)) return 0;
        return inputStorage.get(factoryId).getOrDefault(resourceId, 0);
    }

    public int getOutputAmount(String factoryId, String resourceId) {
        if (!outputStorage.containsKey(factoryId)) return 0;
        return outputStorage.get(factoryId).getOrDefault(resourceId, 0);
    }

    // Legacy method for backward compatibility - assumes input storage
    public int getAmount(String factoryId, String resourceId) {
        return getInputAmount(factoryId, resourceId);
    }

    public Map<String, Integer> getInputStorage(String factoryId) {
        return inputStorage.getOrDefault(factoryId, new HashMap<>());
    }

    public Map<String, Integer> getOutputStorage(String factoryId) {
        return outputStorage.getOrDefault(factoryId, new HashMap<>());
    }

    // Legacy method for backward compatibility - assumes input storage
    public Map<String, Integer> getStorage(String factoryId) {
        return getInputStorage(factoryId);
    }

    public void clearInputStorage(String factoryId) {
        inputStorage.remove(factoryId);
        saveAll();
    }

    public void clearOutputStorage(String factoryId) {
        outputStorage.remove(factoryId);
        saveAll();
    }

    // Legacy method for backward compatibility - clears both storages
    public void clearStorage(String factoryId) {
        clearInputStorage(factoryId);
        clearOutputStorage(factoryId);
    }
}