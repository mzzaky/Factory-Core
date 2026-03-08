package com.aithor.factorycore.managers;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.utils.Logger;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * BackupManager - Comprehensive backup system for the FactoryCore data folder.
 * Supports manual and automatic (scheduled) backups with ZIP compression.
 * Backups can be created, restored, listed, and deleted via admin commands.
 */
public class BackupManager {

    private final FactoryCore plugin;
    private final File backupFolder;
    private int autoBackupTaskId = -1;

    public BackupManager(FactoryCore plugin) {
        this.plugin = plugin;
        this.backupFolder = new File(plugin.getDataFolder(), "backups");
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }

        // Start auto-backup if enabled
        if (isAutoBackupEnabled()) {
            startAutoBackup();
        }
    }

    // ==================== CONFIGURATION ====================

    /**
     * Check if automatic backup is enabled in config.
     */
    public boolean isAutoBackupEnabled() {
        return plugin.getConfig().getBoolean("backup.auto-backup.enabled", false);
    }

    /**
     * Get auto-backup interval in ticks.
     */
    public long getAutoBackupIntervalTicks() {
        return plugin.getConfig().getLong("backup.auto-backup.interval-ticks", 1728000L); // Default 24 hours
    }

    /**
     * Get maximum number of backups to keep.
     */
    public int getMaxBackups() {
        return plugin.getConfig().getInt("backup.max-backups", 10);
    }

    // ==================== BACKUP OPERATIONS ====================

    /**
     * Create a new backup of the entire data folder.
     * Returns the backup file name or null if failed.
     */
    public String createBackup(String label) {
        // Save all manager data first
        saveAllManagers();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String timestamp = sdf.format(new Date());
        String fileName = "backup_" + timestamp;
        if (label != null && !label.isEmpty()) {
            // Sanitize label for filename safety
            String safeLabel = label.replaceAll("[^a-zA-Z0-9_-]", "_");
            fileName += "_" + safeLabel;
        }
        fileName += ".zip";

        File backupFile = new File(backupFolder, fileName);
        File dataFolder = new File(plugin.getDataFolder(), "data");

        if (!dataFolder.exists() || !dataFolder.isDirectory()) {
            plugin.getLogger().warning("No data folder found to backup!");
            return null;
        }

        try {
            zipDirectory(dataFolder, backupFile);
            Logger.log("[BACKUP] Backup created: " + fileName + " (size: " + formatFileSize(backupFile.length()) + ")");
            plugin.getLogger().info("Backup created: " + fileName);

            // Clean old backups if exceeding max
            cleanupOldBackups();

            return fileName;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create backup: " + e.getMessage());
            e.printStackTrace();
            Logger.log("[BACKUP] Backup creation FAILED: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create a quick backup without label.
     */
    public String createBackup() {
        return createBackup(null);
    }

    /**
     * Restore a backup by its file name.
     * Returns true if restoration was successful.
     */
    public boolean restoreBackup(String fileName) {
        File backupFile = new File(backupFolder, fileName);

        if (!backupFile.exists()) {
            plugin.getLogger().warning("Backup file not found: " + fileName);
            return false;
        }

        if (!fileName.endsWith(".zip")) {
            plugin.getLogger().warning("Invalid backup file format (expected .zip): " + fileName);
            return false;
        }

        File dataFolder = new File(plugin.getDataFolder(), "data");

        // Create a safety backup before restoring
        String safetyBackup = createBackup("pre_restore_safety");
        if (safetyBackup != null) {
            plugin.getLogger().info("Safety backup created before restore: " + safetyBackup);
        }

        try {
            // Clear existing data folder
            if (dataFolder.exists()) {
                deleteDirectory(dataFolder);
            }
            dataFolder.mkdirs();

            // Extract backup
            unzipFile(backupFile, dataFolder);

            Logger.log("[BACKUP] Backup restored: " + fileName);
            plugin.getLogger().info("Backup restored successfully: " + fileName);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to restore backup: " + e.getMessage());
            e.printStackTrace();
            Logger.log("[BACKUP] Backup restoration FAILED: " + e.getMessage());
            return false;
        }
    }

    /**
     * List all available backup files sorted by date (newest first).
     */
    public List<BackupInfo> listBackups() {
        List<BackupInfo> backups = new ArrayList<>();

        File[] files = backupFolder.listFiles((dir, name) -> name.endsWith(".zip"));
        if (files == null) return backups;

        for (File file : files) {
            backups.add(new BackupInfo(
                    file.getName(),
                    file.length(),
                    file.lastModified()
            ));
        }

        // Sort newest first
        backups.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        return backups;
    }

    /**
     * Delete a specific backup file.
     */
    public boolean deleteBackup(String fileName) {
        File backupFile = new File(backupFolder, fileName);
        if (!backupFile.exists()) {
            return false;
        }

        boolean deleted = backupFile.delete();
        if (deleted) {
            Logger.log("[BACKUP] Backup deleted: " + fileName);
        }
        return deleted;
    }

    // ==================== AUTO BACKUP ====================

    /**
     * Start the automatic backup scheduler.
     */
    public void startAutoBackup() {
        if (autoBackupTaskId != -1) {
            stopAutoBackup();
        }

        long interval = getAutoBackupIntervalTicks();
        autoBackupTaskId = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            plugin.getLogger().info("Running automatic backup...");

            // Must save on main thread, then backup async
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                String result = createBackup("auto");
                if (result != null) {
                    plugin.getLogger().info("Automatic backup completed: " + result);
                } else {
                    plugin.getLogger().warning("Automatic backup failed!");
                }
            });
        }, interval, interval).getTaskId();

        plugin.getLogger().info("Auto-backup scheduler started (interval: " + (interval / 20) + "s)");
        Logger.log("[BACKUP] Auto-backup scheduler started. Interval: " + (interval / 20) + " seconds.");
    }

    /**
     * Stop the automatic backup scheduler.
     */
    public void stopAutoBackup() {
        if (autoBackupTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(autoBackupTaskId);
            autoBackupTaskId = -1;
            plugin.getLogger().info("Auto-backup scheduler stopped.");
            Logger.log("[BACKUP] Auto-backup scheduler stopped.");
        }
    }

    /**
     * Toggle auto-backup on/off.
     */
    public boolean toggleAutoBackup() {
        if (autoBackupTaskId != -1) {
            stopAutoBackup();
            return false; // now off
        } else {
            startAutoBackup();
            return true; // now on
        }
    }

    /**
     * Check if auto-backup scheduler is currently running.
     */
    public boolean isAutoBackupRunning() {
        return autoBackupTaskId != -1;
    }

    // ==================== CLEANUP ====================

    /**
     * Remove old backups exceeding the max backup count.
     * Keeps the most recent backups and deletes the oldest.
     */
    private void cleanupOldBackups() {
        int max = getMaxBackups();
        if (max <= 0) return;

        List<BackupInfo> backups = listBackups();
        if (backups.size() <= max) return;

        // Delete oldest backups
        for (int i = max; i < backups.size(); i++) {
            BackupInfo old = backups.get(i);
            File oldFile = new File(backupFolder, old.getFileName());
            if (oldFile.delete()) {
                Logger.log("[BACKUP] Old backup cleaned up: " + old.getFileName());
            }
        }
    }

    // ==================== ZIP UTILITIES ====================

    private void zipDirectory(File sourceDir, File zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zipDirectoryRecursive(sourceDir, sourceDir.getName(), zos);
        }
    }

    private void zipDirectoryRecursive(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectoryRecursive(file, parentFolder + "/" + file.getName(), zos);
                continue;
            }

            ZipEntry entry = new ZipEntry(parentFolder + "/" + file.getName());
            zos.putNextEntry(entry);

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
            }
            zos.closeEntry();
        }
    }

    private void unzipFile(File zipFile, File destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // Strip the top-level "data/" prefix from the entry name
                String entryName = entry.getName();
                // Remove leading folder (e.g., "data/factories.yml" -> "factories.yml")
                int slashIdx = entryName.indexOf('/');
                if (slashIdx >= 0) {
                    entryName = entryName.substring(slashIdx + 1);
                }
                if (entryName.isEmpty()) {
                    continue;
                }

                File newFile = new File(destDir, entryName);

                // Security: prevent zip slip
                if (!newFile.getCanonicalPath().startsWith(destDir.getCanonicalPath())) {
                    throw new IOException("Zip entry is outside of the target directory: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    // Ensure parent dirs exist
                    newFile.getParentFile().mkdirs();

                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[4096];
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    // ==================== FILE UTILITIES ====================

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                }
                file.delete();
            }
        }
        dir.delete();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    // ==================== SAVE HELPER ====================

    /**
     * Trigger all managers to save their data to disk.
     */
    private void saveAllManagers() {
        if (plugin.getFactoryManager() != null) plugin.getFactoryManager().saveAll();
        if (plugin.getStorageManager() != null) plugin.getStorageManager().saveAll();
        if (plugin.getInvoiceManager() != null) plugin.getInvoiceManager().saveAll();
        if (plugin.getTaxManager() != null) plugin.getTaxManager().saveAll();
        if (plugin.getMarketplaceManager() != null) plugin.getMarketplaceManager().saveAll();
        if (plugin.getResearchManager() != null) plugin.getResearchManager().saveAll();
        if (plugin.getAchievementManager() != null) plugin.getAchievementManager().saveAll();
        if (plugin.getDailyQuestManager() != null) plugin.getDailyQuestManager().saveAll();
        if (plugin.getPlayerFactoryManager() != null) plugin.getPlayerFactoryManager().saveAll();
    }

    // ==================== SHUTDOWN ====================

    /**
     * Shutdown the backup manager (stop auto-backup).
     */
    public void shutdown() {
        stopAutoBackup();
    }

    // ==================== INNER CLASS ====================

    /**
     * Data class representing backup file information.
     */
    public static class BackupInfo {
        private final String fileName;
        private final long size;
        private final long timestamp;

        public BackupInfo(String fileName, long size, long timestamp) {
            this.fileName = fileName;
            this.size = size;
            this.timestamp = timestamp;
        }

        public String getFileName() {
            return fileName;
        }

        public long getSize() {
            return size;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getFormattedSize() {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }

        public String getFormattedDate() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(new Date(timestamp));
        }
    }
}
