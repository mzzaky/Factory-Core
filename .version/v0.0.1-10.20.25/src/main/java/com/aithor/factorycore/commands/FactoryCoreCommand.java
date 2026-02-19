package com.aithor.factorycore.commands;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.gui.FactoryGUI;
import com.aithor.factorycore.models.Factory;
import com.aithor.factorycore.models.FactoryType;
import com.aithor.factorycore.models.Recipe;
import com.aithor.factorycore.utils.Logger;
import com.aithor.factorycore.utils.WorldGuardUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class FactoryCoreCommand implements CommandExecutor, TabCompleter {
    
    private final FactoryCore plugin;
    
    public FactoryCoreCommand(FactoryCore plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "admin":
                return handleAdmin(sender, args);
                
            case "buy":
                return handleBuy(sender, args);
                
            case "sell":
                return handleSell(sender, args);
                
            case "info":
                return handleInfo(sender, args);
                
            case "version":
                return handleVersion(sender);

            case "gui":
                return handleGui(sender, args);

            case "teleport":
            case "tp":
                return handleTeleport(sender, args);

            case "help":
                sendHelp(sender);
                return true;

            default:
                sender.sendMessage(plugin.getLanguageManager().getMessage("invalid-args")
                        .replace("{usage}", "/fc help"));
                return true;
        }
    }
    
    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("factorycore.admin")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /fc admin <create|remove|list|info|reload|checkrecipes|setowner|teleport|give|npc>");
            sender.sendMessage("§cFor create: /fc admin create <region_id> <factory_id> <factory_type> <price_value>");
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "create":
                return adminCreate(sender, args);
                
            case "remove":
                return adminRemove(sender, args);
                
            case "list":
                return adminList(sender);
                
            case "info":
                return adminInfo(sender, args);
                
            case "reload":
                return adminReload(sender);

            case "checkrecipes":
                return adminCheckRecipes(sender);
                
            case "setowner":
                return adminSetOwner(sender, args);
                
            case "teleport":
                return adminTeleport(sender, args);
                
            case "give":
                return adminGive(sender, args);
                
            case "npc":
                return adminNPC(sender, args);
                
            default:
                sender.sendMessage("§cInvalid admin command!");
                return true;
        }
    }
    
    private boolean adminCreate(CommandSender sender, String[] args) {
        // /fc admin create <region_id> <factory_id> <factory_type> <price_value>
        if (args.length < 6) {
            sender.sendMessage("§cUsage: /fc admin create <region_id> <factory_id> <factory_type> <price_value>");
            return true;
        }

        String regionId = args[2];
        String factoryId = args[3];
        String factoryType = args[4];
        double price;

        try {
            price = Double.parseDouble(args[5]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid price value!");
            return true;
        }
        
        FactoryType type = FactoryType.fromId(factoryType);
        if (type == null) {
            sender.sendMessage("§cInvalid factory type! Available: steel_mill, refinery, workshop, advanced_factory");
            return true;
        }

        // Get the admin's location for fast travel
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be executed by a player!");
            return true;
        }

        Player adminPlayer = (Player) sender;
        Factory factory = plugin.getFactoryManager().createFactory(factoryId, regionId, type, price, adminPlayer.getLocation());

        if (factory == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("region-not-found")
                    .replace("{region}", regionId));
            return true;
        }

        sender.sendMessage(plugin.getLanguageManager().getMessage("factory-created")
                .replace("{factory}", type.getDisplayName())
                .replace("{id}", factoryId));

        Logger.logAdminCommand(sender.getName(), "create factory " + factoryId);
        return true;
    }
    
    private boolean adminRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /fc admin remove <id>");
            return true;
        }
        
        String id = args[2];
        if (plugin.getFactoryManager().removeFactory(id)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("factory-removed")
                    .replace("{id}", id));
            Logger.logAdminCommand(sender.getName(), "remove factory " + id);
        } else {
            sender.sendMessage(plugin.getLanguageManager().getMessage("factory-not-found"));
        }
        
        return true;
    }
    
    private boolean adminList(CommandSender sender) {
        List<Factory> factories = plugin.getFactoryManager().getAllFactories();
        sender.sendMessage(plugin.getLanguageManager().getMessage("admin-list")
                .replace("{count}", String.valueOf(factories.size())));
        
        for (Factory factory : factories) {
            String owner = factory.getOwner() == null ? "None" : 
                    Bukkit.getOfflinePlayer(factory.getOwner()).getName();
            sender.sendMessage("§e" + factory.getId() + " §7- " + 
                    factory.getType().getDisplayName() + " §7- Owner: §e" + owner);
        }
        
        return true;
    }
    
    private boolean adminInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /fc admin info <id>");
            return true;
        }
        
        Factory factory = plugin.getFactoryManager().getFactory(args[2]);
        if (factory == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("factory-not-found"));
            return true;
        }
        
        String owner = factory.getOwner() == null ? "None" : 
                Bukkit.getOfflinePlayer(factory.getOwner()).getName();
        
        sender.sendMessage("§6=== Factory Info ===");
        sender.sendMessage("§7ID: §e" + factory.getId());
        sender.sendMessage("§7Type: §e" + factory.getType().getDisplayName());
        sender.sendMessage("§7Owner: §e" + owner);
        sender.sendMessage("§7Level: §e" + factory.getLevel());
        sender.sendMessage("§7Region: §e" + factory.getRegionName());
        sender.sendMessage("§7Price: §6$" + factory.getPrice());
        sender.sendMessage("§7Status: " + factory.getStatus().getDisplay());
        
        return true;
    }
    
    private boolean adminReload(CommandSender sender) {
        try {
            plugin.getLogger().info("=== ADMIN RELOAD DEBUG ===");
            plugin.getLogger().info("Starting admin reload process...");

            // Reload main plugin configuration
            plugin.getLogger().info("Reloading main config...");
            plugin.reloadConfig();

            // Reload all manager configurations that have reload methods
            plugin.getLogger().info("Reloading language manager...");
            plugin.getLanguageManager().reload();

            plugin.getLogger().info("Reloading resource manager...");
            plugin.getResourceManager().reload();

            plugin.getLogger().info("Reloading recipe manager...");
            plugin.getRecipeManager().reload();

            plugin.getLogger().info("Reloading NPC manager...");
            plugin.getNPCManager().reload();

            // Force refresh storage manager if it exists
            if (plugin.getStorageManager() != null) {
                plugin.getLogger().info("Storage manager refreshed (data loaded on plugin enable)");
            }

            sender.sendMessage("§a§l✅ Reload Successful!");
            sender.sendMessage("§7Reloaded configurations:");
            sender.sendMessage("§7• config.yml (main)");
            sender.sendMessage("§7• language.yml (language)");
            sender.sendMessage("§7• resources.yml (resources)");
            sender.sendMessage("§7• recipes.yml (recipes)");
            sender.sendMessage("§7• npc.yml (NPCs)");
            sender.sendMessage("§7");
            sender.sendMessage("§e§lNote: If you edited recipes.yml, you may need to:");
            sender.sendMessage("§7• Close and reopen any open factory GUIs");
            sender.sendMessage("§7• Restart production if changes aren't visible");
            sender.sendMessage("§7");
            sender.sendMessage("§7Factory, storage, and invoice data");
            sender.sendMessage("§7is only loaded when the plugin is enabled.");

            Logger.logAdminCommand(sender.getName(), "reload all configurations");
            return true;

        } catch (Exception e) {
            sender.sendMessage("§c§l❌ Reload Failed!");
            sender.sendMessage("§7Error: " + e.getMessage());
            plugin.getLogger().severe("Reload failed: " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }

    private boolean adminCheckRecipes(CommandSender sender) {
        sender.sendMessage("§6=== Current Loaded Recipes ===");
        sender.sendMessage("§7Total recipes: §e" + plugin.getRecipeManager().getAllRecipes().size());

        Map<String, Recipe> recipes = plugin.getRecipeManager().getAllRecipes();
        if (recipes.isEmpty()) {
            sender.sendMessage("§cNo recipes are currently loaded!");
            return true;
        }

        for (Recipe recipe : recipes.values()) {
            sender.sendMessage("§7• §e" + recipe.getName() + " §7(Type: " + recipe.getFactoryType() + ", Time: " + recipe.getProductionTime() + "s)");
        }

        sender.sendMessage("§7");
        sender.sendMessage("§7To reload recipes from recipes.yml, use:");
        sender.sendMessage("§7/fc admin reload");

        return true;
    }
    
    private boolean adminSetOwner(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /fc admin setowner <id> <player>");
            return true;
        }
        
        Factory factory = plugin.getFactoryManager().getFactory(args[2]);
        if (factory == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("factory-not-found"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[3]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return true;
        }
        
        factory.setOwner(target.getUniqueId());
        plugin.getFactoryManager().saveAll();
        
        sender.sendMessage(plugin.getLanguageManager().getMessage("admin-setowner")
                .replace("{id}", factory.getId())
                .replace("{player}", target.getName()));
        
        Logger.logAdminCommand(sender.getName(), "setowner " + factory.getId() + " to " + target.getName());
        return true;
    }
    
    private boolean adminTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /fc admin teleport <factory_id>");
            return true;
        }

        Player player = (Player) sender;
        String factoryId = args[2];

        Factory factory = plugin.getFactoryManager().getFactory(factoryId);
        if (factory == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("factory-not-found"));
            return true;
        }

        // Check if fast travel location is set
        if (factory.getFastTravelLocation() == null) {
            sender.sendMessage("§cFast travel location not set for this factory!");
            sender.sendMessage("§7Create a new factory to set the fast travel location.");
            return true;
        }

        // Admin can teleport to any factory
        if (plugin.getFactoryManager().teleportPlayer(player, factoryId)) {
            sender.sendMessage("§aSuccessfully teleported to factory §e" + factoryId + "§a!");
            Logger.logAdminCommand(sender.getName(), "teleport to factory " + factoryId);
        } else {
            sender.sendMessage("§cFailed to teleport to factory!");
        }

        return true;
    }
    
    private boolean adminGive(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage("§cUsage: /fc admin give <resource_id> <amount> <player>");
            return true;
        }
        
        String resourceId = args[2];
        int amount;
        
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid amount!");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[4]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return true;
        }
        
        // Give resource item to player
        if (plugin.getResourceManager().giveResource(target, resourceId, amount)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("admin-give")
                    .replace("{amount}", String.valueOf(amount))
                    .replace("{item}", resourceId)
                    .replace("{player}", target.getName()));
            Logger.logAdminCommand(sender.getName(), "give " + amount + " " + resourceId + " to " + target.getName());
        } else {
            sender.sendMessage("§cResource not found!");
        }
        
        return true;
    }
    
    private boolean adminNPC(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /fc admin npc <spawn|remove> <factory_id> [employee_id]");
            return true;
        }

        String action = args[2].toLowerCase();
        String factoryId = args[3];
        String npcId = args.length > 4 ? args[4] : factoryId; // Use factory_id as npc_id if not provided

        if (action.equals("spawn")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
                return true;
            }

            Player player = (Player) sender;
            if (plugin.getNPCManager().spawnNPC(factoryId, npcId, player.getLocation())) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("npc-spawned")
                        .replace("{id}", npcId));
                Logger.logAdminCommand(sender.getName(), "spawn npc " + npcId + " for factory " + factoryId);
            } else {
                sender.sendMessage("§cFailed to spawn NPC! Factory not found or NPC already exists.");
            }
        } else if (action.equals("remove")) {
            if (plugin.getNPCManager().removeNPC(npcId)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("npc-removed")
                        .replace("{id}", npcId));
                Logger.logAdminCommand(sender.getName(), "remove npc " + npcId);
            } else {
                sender.sendMessage(plugin.getLanguageManager().getMessage("npc-not-found"));
            }
        }

        return true;
    }
    
    private boolean handleBuy(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /fc buy <id>");
            return true;
        }
        
        Player player = (Player) sender;
        String id = args[1];
        
        Factory factory = plugin.getFactoryManager().getFactory(id);
        if (factory == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("factory-not-found"));
            return true;
        }
        
        if (factory.getOwner() != null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("factory-already-owned"));
            return true;
        }
        
        if (plugin.getFactoryManager().buyFactory(player, id)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("factory-bought")
                    .replace("{factory}", factory.getType().getDisplayName())
                    .replace("{price}", String.valueOf(factory.getPrice())));
        } else {
            sender.sendMessage(plugin.getLanguageManager().getMessage("insufficient-funds")
                    .replace("{amount}", String.valueOf(factory.getPrice())));
        }
        
        return true;
    }
    
    private boolean handleSell(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /fc sell <id>");
            return true;
        }
        
        Player player = (Player) sender;
        String id = args[1];
        
        Factory factory = plugin.getFactoryManager().getFactory(id);
        if (factory == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("factory-not-found"));
            return true;
        }
        
        if (!player.getUniqueId().equals(factory.getOwner())) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("factory-not-owned"));
            return true;
        }
        
        double sellPrice = factory.getPrice() * 0.5;
        if (plugin.getFactoryManager().sellFactory(player, id)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("factory-sold")
                    .replace("{factory}", factory.getType().getDisplayName())
                    .replace("{price}", String.valueOf(sellPrice)));
        }
        
        return true;
    }
    
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /fc info <id>");
            return true;
        }
        
        Factory factory = plugin.getFactoryManager().getFactory(args[1]);
        if (factory == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("factory-not-found"));
            return true;
        }
        
        String owner = factory.getOwner() == null ? "Available" : 
                Bukkit.getOfflinePlayer(factory.getOwner()).getName();
        
        sender.sendMessage("§6=== Factory Info ===");
        sender.sendMessage("§7ID: §e" + factory.getId());
        sender.sendMessage("§7Type: " + factory.getType().getDisplayName());
        sender.sendMessage("§7Owner: §e" + owner);
        sender.sendMessage("§7Level: §e" + factory.getLevel());
        sender.sendMessage("§7Price: §6$" + factory.getPrice());
        sender.sendMessage("§7Status: " + factory.getStatus().getDisplay());
        
        return true;
    }
    
    private boolean handleVersion(CommandSender sender) {
        sender.sendMessage(plugin.getLanguageManager().getMessage("plugin-version")
                .replace("{version}", plugin.getDescription().getVersion()));
        return true;
    }

    private boolean handleGui(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
            return true;
        }

        if (!sender.hasPermission("factorycore.gui")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /fc gui <factory_id>");
            return true;
        }

        Player player = (Player) sender;
        String factoryId = args[1];

        Factory factory = plugin.getFactoryManager().getFactory(factoryId);
        if (factory == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("factory-not-found"));
            return true;
        }

        if (!player.getUniqueId().equals(factory.getOwner())) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("factory-not-owned"));
            return true;
        }

        // Open factory GUI
        FactoryGUI gui = new FactoryGUI(plugin, player, factoryId);
        gui.openMainMenu();

        return true;
    }

    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /fc teleport <factory_id>");
            return true;
        }

        Player player = (Player) sender;
        String factoryId = args[1];

        Factory factory = plugin.getFactoryManager().getFactory(factoryId);
        if (factory == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("factory-not-found"));
            return true;
        }

        if (!player.getUniqueId().equals(factory.getOwner())) {
            sender.sendMessage("§cYou are not the owner of this factory!");
            return true;
        }

        // Check if fast travel location is set
        if (factory.getFastTravelLocation() == null) {
            sender.sendMessage("§cFast travel location not set for this factory!");
            sender.sendMessage("§7Contact an admin to set the fast travel location.");
            return true;
        }

        // Teleport player to factory
        if (plugin.getFactoryManager().teleportPlayer(player, factoryId)) {
            sender.sendMessage("§aSuccessfully teleported to factory!");
        } else {
            sender.sendMessage("§cFailed to teleport to factory!");
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getLanguageManager().getMessage("help.header"));
        
        if (sender.hasPermission("factorycore.admin")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("help.admin"));
            sender.sendMessage(plugin.getLanguageManager().getMessage("help.admin-remove"));
            sender.sendMessage(plugin.getLanguageManager().getMessage("help.admin-list"));
            sender.sendMessage(plugin.getLanguageManager().getMessage("help.admin-info"));
            sender.sendMessage(plugin.getLanguageManager().getMessage("help.admin-reload"));
            sender.sendMessage("§6/fc admin checkrecipes §7- Check currently loaded recipes");
            sender.sendMessage(plugin.getLanguageManager().getMessage("help.admin-setowner"));
            sender.sendMessage(plugin.getLanguageManager().getMessage("help.admin-tp"));
            sender.sendMessage(plugin.getLanguageManager().getMessage("help.admin-give"));
            sender.sendMessage(plugin.getLanguageManager().getMessage("help.admin-npc-spawn"));
            sender.sendMessage(plugin.getLanguageManager().getMessage("help.admin-npc-remove"));
        }
        
        sender.sendMessage(plugin.getLanguageManager().getMessage("help.buy"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("help.sell"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("help.info"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("help.gui"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("help.teleport"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("help.version"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("admin");
            completions.add("buy");
            completions.add("sell");
            completions.add("info");
            completions.add("version");
            completions.add("gui");
            completions.add("help");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            completions.add("create");
            completions.add("remove");
            completions.add("list");
            completions.add("info");
            completions.add("reload");
            completions.add("checkrecipes");
            completions.add("setowner");
            completions.add("teleport");
            completions.add("give");
            completions.add("npc");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("npc")) {
            completions.add("spawn");
            completions.add("remove");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("npc")) {
            // Tab completion for factory_id - show all factories
            List<Factory> allFactories = plugin.getFactoryManager().getAllFactories();
            for (Factory factory : allFactories) {
                completions.add(factory.getId());
            }
        } else if (args.length == 5 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("npc") && args[2].equalsIgnoreCase("spawn")) {
            // Tab completion for employee_id - suggest common employee ID patterns
            completions.add("employee_01");
            completions.add("worker_01");
            completions.add("npc_01");
            completions.add("factory_worker");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("create")) {
            // Tab completion for region_id
            completions.addAll(WorldGuardUtils.getAllRegionNames());
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("create")) {
            // Tab completion for factory_id - suggest common factory ID patterns
            completions.add("factory1");
            completions.add("steel_mill_01");
            completions.add("refinery_01");
            completions.add("workshop_01");
            completions.add("advanced_factory_01");
            completions.add("main_factory");
            completions.add("secondary_factory");
        } else if (args.length == 5 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("create")) {
            // Tab completion for factory_type
            completions.add("steel_mill");
            completions.add("refinery");
            completions.add("workshop");
            completions.add("advanced_factory");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("give")) {
            // Tab completion for resource_id - show all available resources
            completions.addAll(plugin.getResourceManager().getAllResources().keySet());
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("give")) {
            // Tab completion for amount - suggest common amounts
            completions.add("1");
            completions.add("10");
            completions.add("16");
            completions.add("32");
            completions.add("64");
            completions.add("100");
            completions.add("1000");
        } else if (args.length == 5 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("give")) {
            // Tab completion for player names - show online players
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                completions.add(onlinePlayer.getName());
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("buy")) {
            // Tab completion for buy command - only show available factories (no owner)
            List<Factory> availableFactories = plugin.getFactoryManager().getAllFactories().stream()
                .filter(factory -> factory.getOwner() == null)
                .collect(Collectors.toList());

            for (Factory factory : availableFactories) {
                completions.add(factory.getId());
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
            // Tab completion for sell command - only show factories owned by the player
            if (sender instanceof Player) {
                Player player = (Player) sender;
                List<Factory> ownedFactories = plugin.getFactoryManager().getAllFactories().stream()
                    .filter(factory -> player.getUniqueId().equals(factory.getOwner()))
                    .collect(Collectors.toList());

                for (Factory factory : ownedFactories) {
                    completions.add(factory.getId());
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            // Tab completion for info command - show all factories (both available and owned)
            List<Factory> allFactories = plugin.getFactoryManager().getAllFactories();
            for (Factory factory : allFactories) {
                completions.add(factory.getId());
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("gui")) {
            // Tab completion for factory IDs - only show factories owned by the player
            if (sender instanceof Player) {
                Player player = (Player) sender;
                List<Factory> playerFactories = plugin.getFactoryManager().getAllFactories().stream()
                    .filter(factory -> player.getUniqueId().equals(factory.getOwner()))
                    .collect(Collectors.toList());

                for (Factory factory : playerFactories) {
                    completions.add(factory.getId());
                }
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("teleport") || args[0].equalsIgnoreCase("tp"))) {
            // Tab completion for teleport command - only show factories owned by the player
            if (sender instanceof Player) {
                Player player = (Player) sender;
                List<Factory> playerFactories = plugin.getFactoryManager().getAllFactories().stream()
                    .filter(factory -> player.getUniqueId().equals(factory.getOwner()))
                    .collect(Collectors.toList());

                for (Factory factory : playerFactories) {
                    completions.add(factory.getId());
                }
            }
        }
        
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}