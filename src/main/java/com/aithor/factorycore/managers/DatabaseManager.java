package com.aithor.factorycore.managers;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.utils.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.StringReader;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * DatabaseManager - Handles MySQL database connections and data persistence.
 * Supports both YAML (default) and MySQL storage types as defined in config.yml.
 * When MySQL is selected, all manager data is stored in MySQL tables instead of YAML files.
 */
public class DatabaseManager {

    private final FactoryCore plugin;
    private Connection connection;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean useMySQL;

    // Table name constants
    public static final String TABLE_FACTORIES = "fc_factories";
    public static final String TABLE_PLAYER_FACTORIES = "fc_player_factories";
    public static final String TABLE_INPUT_STORAGE = "fc_input_storage";
    public static final String TABLE_OUTPUT_STORAGE = "fc_output_storage";
    public static final String TABLE_INVOICES = "fc_invoices";
    public static final String TABLE_NPCS = "fc_npcs";
    public static final String TABLE_MARKETPLACE = "fc_marketplace";
    public static final String TABLE_TAXES = "fc_taxes";
    public static final String TABLE_ACHIEVEMENTS = "fc_achievements";
    public static final String TABLE_DAILY_QUESTS = "fc_daily_quests";
    public static final String TABLE_RESEARCH = "fc_research";

    public DatabaseManager(FactoryCore plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();

        String type = config.getString("database.type", "YAML").toUpperCase();
        this.useMySQL = type.equals("MYSQL");

        this.host = config.getString("database.mysql.host", "localhost");
        this.port = config.getInt("database.mysql.port", 3306);
        this.database = config.getString("database.mysql.database", "factorycore");
        this.username = config.getString("database.mysql.username", "root");
        this.password = config.getString("database.mysql.password", "password");

        if (useMySQL) {
            if (connect()) {
                createTables();
                plugin.getLogger().info("MySQL database connected successfully!");
                Logger.log("[DATABASE] MySQL connection established to " + host + ":" + port + "/" + database);
            } else {
                plugin.getLogger().severe("Failed to connect to MySQL! Falling back to YAML storage.");
                Logger.log("[DATABASE] MySQL connection FAILED - falling back to YAML");
            }
        } else {
            plugin.getLogger().info("Using YAML file storage.");
        }
    }

    /**
     * Returns true if the plugin is configured to use MySQL storage.
     */
    public boolean isMySQL() {
        return useMySQL && connection != null;
    }

    /**
     * Establish a connection to the MySQL database.
     */
    public boolean connect() {
        try {
            if (connection != null && !connection.isClosed()) {
                return true;
            }

            String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true"
                    + "&useUnicode=true&characterEncoding=UTF-8";

            connection = DriverManager.getConnection(url, username, password);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("MySQL connection error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Reconnect if the connection was lost.
     */
    private boolean ensureConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                return connect();
            }
            // Test with a lightweight query
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SELECT 1");
            }
            return true;
        } catch (SQLException e) {
            return connect();
        }
    }

    /**
     * Get the active database connection (reconnects if necessary).
     */
    public Connection getConnection() {
        ensureConnection();
        return connection;
    }

    /**
     * Close the database connection gracefully.
     */
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("MySQL connection closed.");
                Logger.log("[DATABASE] MySQL connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error closing MySQL connection: " + e.getMessage());
        }
    }

    /**
     * Create all required tables if they don't exist.
     * Each table stores its data as a YAML-serialized blob for maximum compatibility
     * with the existing manager save/load logic.
     */
    private void createTables() {
        String[] tables = {
                TABLE_FACTORIES, TABLE_PLAYER_FACTORIES,
                TABLE_INPUT_STORAGE, TABLE_OUTPUT_STORAGE,
                TABLE_INVOICES, TABLE_NPCS,
                TABLE_MARKETPLACE, TABLE_TAXES,
                TABLE_ACHIEVEMENTS, TABLE_DAILY_QUESTS,
                TABLE_RESEARCH
        };

        try (Statement stmt = connection.createStatement()) {
            for (String table : tables) {
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS `" + table + "` ("
                                + "`data_key` VARCHAR(255) NOT NULL, "
                                + "`data_value` MEDIUMTEXT NOT NULL, "
                                + "`updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                                + "PRIMARY KEY (`data_key`)"
                                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
                );
            }
            Logger.log("[DATABASE] All MySQL tables verified/created successfully.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create MySQL tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== YAML-COMPATIBLE SAVE/LOAD ====================

    /**
     * Save a YamlConfiguration to a MySQL table.
     * Each top-level key in the YAML becomes a row in the table.
     * The value is the YAML-serialized content of that section.
     */
    public void saveYamlToTable(String tableName, FileConfiguration config) {
        if (!isMySQL()) return;
        if (!ensureConnection()) return;

        try {
            // Clear existing data
            try (PreparedStatement clear = connection.prepareStatement(
                    "DELETE FROM `" + tableName + "`")) {
                clear.executeUpdate();
            }

            // Insert each top-level key as a row
            String insertSql = "INSERT INTO `" + tableName + "` (`data_key`, `data_value`) VALUES (?, ?) "
                    + "ON DUPLICATE KEY UPDATE `data_value` = VALUES(`data_value`)";

            try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                for (String key : config.getKeys(false)) {
                    // Serialize each section as standalone YAML
                    YamlConfiguration section = new YamlConfiguration();
                    if (config.isConfigurationSection(key)) {
                        for (String subKey : config.getConfigurationSection(key).getKeys(true)) {
                            if (!config.getConfigurationSection(key).isConfigurationSection(subKey)) {
                                section.set(subKey, config.getConfigurationSection(key).get(subKey));
                            }
                        }
                    } else {
                        section.set("value", config.get(key));
                    }

                    insert.setString(1, key);
                    insert.setString(2, section.saveToString());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save data to MySQL table " + tableName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load data from a MySQL table into a YamlConfiguration.
     * Reconstructs the same YAML structure that would be loaded from a file.
     */
    public FileConfiguration loadYamlFromTable(String tableName) {
        YamlConfiguration config = new YamlConfiguration();
        if (!isMySQL()) return config;
        if (!ensureConnection()) return config;

        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT `data_key`, `data_value` FROM `" + tableName + "`");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String key = rs.getString("data_key");
                String yamlValue = rs.getString("data_value");

                YamlConfiguration section = new YamlConfiguration();
                section.loadFromString(yamlValue);

                // If it was a simple value (not a section)
                if (section.contains("value") && section.getKeys(false).size() == 1) {
                    config.set(key, section.get("value"));
                } else {
                    // Rebuild the section under the key
                    for (String subKey : section.getKeys(true)) {
                        if (!section.isConfigurationSection(subKey)) {
                            config.set(key + "." + subKey, section.get(subKey));
                        }
                    }
                }
            }
        } catch (SQLException | org.bukkit.configuration.InvalidConfigurationException e) {
            plugin.getLogger().severe("Failed to load data from MySQL table " + tableName + ": " + e.getMessage());
            e.printStackTrace();
        }

        return config;
    }

    /**
     * Save raw YAML content (full file content) to a single-row table entry.
     * This is a simpler approach where the entire file is stored as one blob.
     */
    public void saveRawYaml(String tableName, String yamlContent) {
        if (!isMySQL()) return;
        if (!ensureConnection()) return;

        String sql = "INSERT INTO `" + tableName + "` (`data_key`, `data_value`) VALUES ('_full_data', ?) "
                + "ON DUPLICATE KEY UPDATE `data_value` = VALUES(`data_value`)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, yamlContent);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save raw YAML to table " + tableName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load raw YAML content from a single-row table entry.
     */
    public FileConfiguration loadRawYaml(String tableName) {
        YamlConfiguration config = new YamlConfiguration();
        if (!isMySQL()) return config;
        if (!ensureConnection()) return config;

        String sql = "SELECT `data_value` FROM `" + tableName + "` WHERE `data_key` = '_full_data'";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                String yamlContent = rs.getString("data_value");
                config.loadFromString(yamlContent);
            }
        } catch (SQLException | org.bukkit.configuration.InvalidConfigurationException e) {
            plugin.getLogger().severe("Failed to load raw YAML from table " + tableName + ": " + e.getMessage());
            e.printStackTrace();
        }

        return config;
    }

    // ==================== MIGRATION UTILITIES ====================

    /**
     * Migrate all existing YAML data files to MySQL.
     * This reads each YAML data file and stores its content in the corresponding MySQL table.
     */
    public void migrateYamlToMySQL() {
        if (!isMySQL()) {
            plugin.getLogger().warning("Cannot migrate: MySQL is not enabled or connected.");
            return;
        }

        plugin.getLogger().info("Starting YAML to MySQL migration...");
        Logger.log("[DATABASE] Starting YAML -> MySQL migration...");

        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            plugin.getLogger().info("No data folder found. Nothing to migrate.");
            return;
        }

        Map<String, String> fileToTable = new HashMap<>();
        fileToTable.put("factories.yml", TABLE_FACTORIES);
        fileToTable.put("player_factories.yml", TABLE_PLAYER_FACTORIES);
        fileToTable.put("input-storage.yml", TABLE_INPUT_STORAGE);
        fileToTable.put("output-storage.yml", TABLE_OUTPUT_STORAGE);
        fileToTable.put("invoices.yml", TABLE_INVOICES);
        fileToTable.put("npcs.yml", TABLE_NPCS);
        fileToTable.put("marketplace.yml", TABLE_MARKETPLACE);
        fileToTable.put("taxes.yml", TABLE_TAXES);
        fileToTable.put("achievements.yml", TABLE_ACHIEVEMENTS);
        fileToTable.put("daily-quests.yml", TABLE_DAILY_QUESTS);
        fileToTable.put("research.yml", TABLE_RESEARCH);

        int migrated = 0;
        for (Map.Entry<String, String> entry : fileToTable.entrySet()) {
            File file = new File(dataFolder, entry.getKey());
            if (!file.exists()) continue;

            try {
                FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                saveRawYaml(entry.getValue(), yaml.saveToString());
                migrated++;
                plugin.getLogger().info("Migrated: " + entry.getKey() + " -> " + entry.getValue());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to migrate " + entry.getKey() + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info("Migration complete! " + migrated + " files migrated to MySQL.");
        Logger.log("[DATABASE] Migration complete. " + migrated + " files migrated to MySQL.");
    }

    /**
     * Migrate all data from MySQL back to YAML files.
     */
    public void migrateMySQLToYaml() {
        if (!isMySQL()) {
            plugin.getLogger().warning("Cannot migrate: MySQL is not connected.");
            return;
        }

        plugin.getLogger().info("Starting MySQL to YAML migration...");
        Logger.log("[DATABASE] Starting MySQL -> YAML migration...");

        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        Map<String, String> tableToFile = new HashMap<>();
        tableToFile.put(TABLE_FACTORIES, "factories.yml");
        tableToFile.put(TABLE_PLAYER_FACTORIES, "player_factories.yml");
        tableToFile.put(TABLE_INPUT_STORAGE, "input-storage.yml");
        tableToFile.put(TABLE_OUTPUT_STORAGE, "output-storage.yml");
        tableToFile.put(TABLE_INVOICES, "invoices.yml");
        tableToFile.put(TABLE_NPCS, "npcs.yml");
        tableToFile.put(TABLE_MARKETPLACE, "marketplace.yml");
        tableToFile.put(TABLE_TAXES, "taxes.yml");
        tableToFile.put(TABLE_ACHIEVEMENTS, "achievements.yml");
        tableToFile.put(TABLE_DAILY_QUESTS, "daily-quests.yml");
        tableToFile.put(TABLE_RESEARCH, "research.yml");

        int migrated = 0;
        for (Map.Entry<String, String> entry : tableToFile.entrySet()) {
            try {
                FileConfiguration yaml = loadRawYaml(entry.getKey());
                if (yaml.getKeys(false).isEmpty()) continue;

                File file = new File(dataFolder, entry.getValue());
                yaml.save(file);
                migrated++;
                plugin.getLogger().info("Migrated: " + entry.getKey() + " -> " + entry.getValue());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to migrate " + entry.getKey() + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info("Migration complete! " + migrated + " tables migrated to YAML files.");
        Logger.log("[DATABASE] Migration complete. " + migrated + " tables migrated to YAML.");
    }

    // ==================== STATUS & INFO ====================

    /**
     * Get database connection status information.
     */
    public String getStatusInfo() {
        if (!useMySQL) {
            return "§7Storage Type: §eYAML (File-based)";
        }

        try {
            if (connection != null && !connection.isClosed()) {
                return "§7Storage Type: §aMySQL §7(Connected to §e" + host + ":" + port + "/" + database + "§7)";
            }
        } catch (SQLException ignored) {
        }

        return "§7Storage Type: §cMySQL (Disconnected!)";
    }

    /**
     * Get the number of rows in a table.
     */
    public int getTableRowCount(String tableName) {
        if (!isMySQL()) return -1;
        if (!ensureConnection()) return -1;

        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM `" + tableName + "`");
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            // Table may not exist
        }
        return 0;
    }
}
