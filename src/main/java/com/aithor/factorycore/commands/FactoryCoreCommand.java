package com.aithor.factorycore.commands;

import com.aithor.factorycore.FactoryCore;
import com.aithor.factorycore.gui.*;
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
            // Default action: open hub if player, show help if console
            if (sender instanceof Player) {
                return handleHub(sender);
            }
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "hub":
            case "menu":
            case "main":
                return handleHub(sender);

            case "browse":
                return handleBrowse(sender);

            case "myfactories":
            case "my":
                return handleMyFactories(sender);

            case "invoices":
                return handleInvoices(sender);

            case "taxes":
            case "tax":
                return handleTaxes(sender);

            case "employees":
            case "npcs":
                return handleEmployees(sender);

            case "market":
            case "marketplace":
                return handleMarketplace(sender);

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

            case "dailyquest":
            case "dq":
            case "quest":
                return handleDailyQuest(sender);

            case "help":
                sendHelp(sender);
                return true;

            default:
                sender.sendMessage(plugin.getLanguageManager().getMessage("invalid-args")
                        .replace("{usage}", "/fc help"));
                return true;
        }
    }

    // ==================== HUB COMMANDS ====================

    private boolean handleHub(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        plugin.openHub(player);
        return true;
    }

    private boolean handleBrowse(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        FactoryBrowseGUI gui = new FactoryBrowseGUI(plugin, player);
        gui.openBrowseMenu();
        return true;
    }

    private boolean handleMyFactories(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        MyFactoriesGUI gui = new MyFactoriesGUI(plugin, player);
        gui.openMyFactoriesMenu();
        return true;
    }

    private boolean handleInvoices(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        InvoiceCenterGUI gui = new InvoiceCenterGUI(plugin, player);
        gui.openInvoiceCenterMenu();
        return true;
    }

    private boolean handleTaxes(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        TaxCenterGUI gui = new TaxCenterGUI(plugin, player);
        gui.openTaxCenterMenu();
        return true;
    }

    private boolean handleEmployees(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        EmployeesCenterGUI gui = new EmployeesCenterGUI(plugin, player);
        gui.openEmployeesCenterMenu();
        return true;
    }

    private boolean handleMarketplace(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        MarketplaceGUI gui = new MarketplaceGUI(plugin, player);
        gui.openMarketplaceMenu();
        return true;
    }

    private boolean handleDailyQuest(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        DailyQuestGUI gui = new DailyQuestGUI(plugin, player);
        gui.openDailyQuestMenu();
        return true;
    }

    // ==================== ADMIN COMMANDS ====================

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("factorycore.admin")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(
                    "§cUsage: /fc admin <create|remove|list|info|reload|checkrecipes|setowner|teleport|give|npc|tax|market|research>");
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

            case "tax":
                return adminTax(sender, args);

            case "market":
                return adminMarket(sender, args);

            case "research":
                return adminResearch(sender, args);

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
        Factory factory = plugin.getFactoryManager().createFactory(factoryId, regionId, type, price,
                adminPlayer.getLocation());

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
            String owner = factory.getOwner() == null ? "None" : Bukkit.getOfflinePlayer(factory.getOwner()).getName();
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

        String owner = factory.getOwner() == null ? "None" : Bukkit.getOfflinePlayer(factory.getOwner()).getName();

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

            plugin.reloadConfig();
            plugin.getLanguageManager().reload();
            plugin.getResourceManager().reload();
            plugin.getRecipeManager().reload();
            plugin.getNPCManager().reload();

            sender.sendMessage("§a§l✅ Reload Successful!");
            sender.sendMessage("§7Reloaded all configurations.");

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
            sender.sendMessage("§7• §e" + recipe.getName() + " §7(Type: " + recipe.getFactoryType() + ", Time: "
                    + recipe.getProductionTime() + "s)");
        }

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

        if (factory.getFastTravelLocation() == null) {
            sender.sendMessage("§cFast travel location not set for this factory!");
            return true;
        }

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
        // /fc admin npc <spawn|remove|list|respawn> [args...]
        if (args.length < 3) {
            sender.sendMessage("§6=== NPC Admin Commands ===");
            sender.sendMessage("§e/fc admin npc spawn §7<factory_id> [npc_id] [template]");
            sender.sendMessage("§e/fc admin npc remove §7<npc_id>");
            sender.sendMessage("§e/fc admin npc list §7- List all NPCs");
            sender.sendMessage("§e/fc admin npc respawn §7[npc_id] - Respawn missing NPC(s)");
            sender.sendMessage(
                    "§7Templates: default, steel_mill_employee, refinery_employee, workshop_employee, advanced_factory_employee");
            return true;
        }

        String action = args[2].toLowerCase();

        switch (action) {

            // ── SPAWN ──────────────────────────────────────────────────────────
            case "spawn": {
                // /fc admin npc spawn <factory_id> [npc_id] [template]
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /fc admin npc spawn <factory_id> [npc_id] [template]");
                    return true;
                }

                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
                    return true;
                }

                Player player = (Player) sender;
                String factoryId = args[3];

                // Validate factory exists first
                if (plugin.getFactoryManager().getFactory(factoryId) == null) {
                    sender.sendMessage("§cFactory §e" + factoryId + " §cnot found!");
                    return true;
                }

                // npc_id: use provided or auto-generate a unique one
                String npcId;
                if (args.length >= 5) {
                    npcId = args[4];
                } else {
                    // Auto-generate: factoryId + "_npc_" + short UUID
                    npcId = factoryId + "_npc_" + java.util.UUID.randomUUID().toString().substring(0, 6);
                }

                // Check NPC ID collision
                if (plugin.getNPCManager().getNPC(npcId) != null) {
                    sender.sendMessage("§cNPC with ID §e" + npcId + " §calready exists! Use a different ID.");
                    return true;
                }

                // template: optional 6th arg
                String template = args.length >= 6 ? args[5] : "default";

                boolean success = plugin.getNPCManager().spawnNPCWithTemplate(
                        factoryId, npcId, player.getLocation(), template);

                if (success) {
                    sender.sendMessage("§a✔ NPC §e" + npcId + " §aspawned for factory §e" + factoryId
                            + " §awith template §e" + template + "§a.");
                    Logger.logAdminCommand(sender.getName(),
                            "spawn npc " + npcId + " for factory " + factoryId + " template=" + template);
                } else {
                    sender.sendMessage("§c✘ Failed to spawn NPC. Factory not found or NPC ID already exists.");
                }
                return true;
            }

            // ── REMOVE ────────────────────────────────────────────────────────
            case "remove": {
                // /fc admin npc remove <npc_id>
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /fc admin npc remove <npc_id>");
                    return true;
                }

                String npcId = args[3];

                if (plugin.getNPCManager().getNPC(npcId) == null) {
                    sender.sendMessage("§cNPC §e" + npcId + " §cnot found!");
                    return true;
                }

                if (plugin.getNPCManager().removeNPC(npcId)) {
                    sender.sendMessage("§a✔ NPC §e" + npcId + " §ahas been removed.");
                    Logger.logAdminCommand(sender.getName(), "remove npc " + npcId);
                } else {
                    sender.sendMessage("§c✘ Failed to remove NPC §e" + npcId + "§c.");
                }
                return true;
            }

            // ── LIST ──────────────────────────────────────────────────────────
            case "list": {
                java.util.List<com.aithor.factorycore.models.FactoryNPC> allNpcs = plugin.getNPCManager().getAllNPCs();

                if (allNpcs.isEmpty()) {
                    sender.sendMessage("§7No NPCs are currently registered.");
                    return true;
                }

                sender.sendMessage("§6=== Registered NPCs (" + allNpcs.size() + ") ===");
                for (com.aithor.factorycore.models.FactoryNPC npc : allNpcs) {
                    org.bukkit.Location loc = npc.getLocation();
                    String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";
                    String coords = String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
                    boolean alive = npc.getEntityUUID() != null
                            && org.bukkit.Bukkit.getEntity(npc.getEntityUUID()) != null;

                    sender.sendMessage("§e" + npc.getId()
                            + " §7→ factory: §e" + npc.getFactoryId()
                            + " §7template: §e" + npc.getTemplate()
                            + " §7@ §b" + worldName + " " + coords
                            + (alive ? " §a[ALIVE]" : " §c[MISSING]"));
                }
                return true;
            }

            // ── RESPAWN ───────────────────────────────────────────────────────
            case "respawn": {
                // /fc admin npc respawn [npc_id] — omit npc_id to respawn ALL missing
                if (args.length >= 4) {
                    String npcId = args[3];
                    if (plugin.getNPCManager().getNPC(npcId) == null) {
                        sender.sendMessage("§cNPC §e" + npcId + " §cnot found!");
                        return true;
                    }
                    boolean respawned = plugin.getNPCManager().respawnNPCIfNotExists(npcId);
                    if (respawned) {
                        sender.sendMessage("§a✔ NPC §e" + npcId + " §ahas been respawned.");
                    } else {
                        sender.sendMessage("§7NPC §e" + npcId + " §7is already alive — no respawn needed.");
                    }
                } else {
                    plugin.getNPCManager().respawnAllNPCsIfNotExists();
                    sender.sendMessage("§a✔ Respawn check completed for all NPCs.");
                }
                Logger.logAdminCommand(sender.getName(), "respawn npc " + (args.length >= 4 ? args[3] : "all"));
                return true;
            }

            default:
                sender.sendMessage("§cUnknown NPC action: §e" + action);
                sender.sendMessage("§7Valid actions: §espawn§7, §eremove§7, §elist§7, §erespawn");
                return true;
        }
    }

    // New admin commands for tax and marketplace
    private boolean adminTax(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /fc admin tax <assess|check>");
            return true;
        }

        String action = args[2].toLowerCase();

        if (action.equals("assess")) {
            if (plugin.getTaxManager() != null) {
                plugin.getTaxManager().assessTaxes();
                sender.sendMessage("§aTaxes assessed for all factories!");
            }
        } else if (action.equals("check")) {
            if (plugin.getTaxManager() != null) {
                plugin.getTaxManager().checkOverdueTaxes();
                sender.sendMessage("§aOverdue taxes checked!");
            }
        }

        return true;
    }

    private boolean adminMarket(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /fc admin market <cleanup|stats>");
            return true;
        }

        String action = args[2].toLowerCase();

        if (action.equals("cleanup")) {
            if (plugin.getMarketplaceManager() != null) {
                plugin.getMarketplaceManager().cleanupExpiredListings();
                sender.sendMessage("§aExpired listings cleaned up!");
            }
        } else if (action.equals("stats")) {
            if (plugin.getMarketplaceManager() != null) {
                int listings = plugin.getMarketplaceManager().getAllListings().size();
                sender.sendMessage("§6=== Marketplace Stats ===");
                sender.sendMessage("§7Active Listings: §e" + listings);
            }
        }

        return true;
    }

    private boolean adminResearch(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage("§cUsage:");
            sender.sendMessage("§c/fc admin research upgrade <factory_id> <factory_research_id>");
            sender.sendMessage("§c/fc admin research set <factory_id> <factory_research_id> <level>");
            return true;
        }

        String action = args[2].toLowerCase();
        String factoryId = args[3];
        String researchId = args[4];

        Factory factory = plugin.getFactoryManager().getFactory(factoryId);
        if (factory == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("factory-not-found"));
            return true;
        }

        UUID ownerId = factory.getOwner();
        if (ownerId == null) {
            sender.sendMessage("§cThis factory has no owner to apply research to!");
            return true;
        }

        if (plugin.getResearchManager() == null) {
            sender.sendMessage("§cResearch system is currently disabled or unavailable!");
            return true;
        }

        if (!plugin.getResearchManager().getResearchIds().contains(researchId)) {
            sender.sendMessage("§cInvalid research ID: §e" + researchId);
            return true;
        }

        if (action.equals("upgrade")) {
            if (plugin.getResearchManager().forceUpgradeResearch(ownerId, researchId)) {
                sender.sendMessage(
                        "§a✔ Force upgraded §e" + researchId + " §afor factory owner §e" + factoryId + "§a!");
                Logger.logAdminCommand(sender.getName(), "research upgrade " + factoryId + " " + researchId);
            } else {
                sender.sendMessage("§cFailed to upgrade research! Typically this means it is already at max level.");
            }
        } else if (action.equals("set")) {
            if (args.length < 6) {
                sender.sendMessage("§cUsage: /fc admin research set <factory_id> <factory_research_id> <level>");
                return true;
            }
            int level;
            try {
                level = Integer.parseInt(args[5]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid level. Must be a number.");
                return true;
            }

            if (plugin.getResearchManager().forceSetResearchLevel(ownerId, researchId, level)) {
                sender.sendMessage("§a✔ Force set §e" + researchId + " §ato level §e" + level
                        + " §afor factory owner §e" + factoryId + "§a!");
                Logger.logAdminCommand(sender.getName(), "research set " + factoryId + " " + researchId + " " + level);
            } else {
                sender.sendMessage("§cFailed to set research level! Make sure the level is valid (0 - max levels).");
            }
        } else {
            sender.sendMessage("§cUnknown action: §e" + action);
        }

        return true;
    }

    // ==================== PLAYER COMMANDS ====================

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

        String owner = factory.getOwner() == null ? "Available" : Bukkit.getOfflinePlayer(factory.getOwner()).getName();

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

        if (factory.getFastTravelLocation() == null) {
            sender.sendMessage("§cFast travel location not set for this factory!");
            return true;
        }

        if (plugin.getFactoryManager().teleportPlayer(player, factoryId)) {
            sender.sendMessage("§aSuccessfully teleported to factory!");
        } else {
            sender.sendMessage("§cFailed to teleport to factory!");
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== FactoryCore Commands ===");
        sender.sendMessage("");
        sender.sendMessage("§e§lMain Commands:");
        sender.sendMessage("§6/fc hub §7- Open the main hub menu");
        sender.sendMessage("§6/fc browse §7- Browse available factories");
        sender.sendMessage("§6/fc my §7- View your factories");
        sender.sendMessage("§6/fc invoices §7- View invoice center");
        sender.sendMessage("§6/fc taxes §7- View tax center");
        sender.sendMessage("§6/fc employees §7- View employees center");
        sender.sendMessage("§6/fc market §7- Open marketplace");
        sender.sendMessage("§6/fc dailyquest §7- View daily quests");
        sender.sendMessage("");
        sender.sendMessage("§e§lFactory Commands:");
        sender.sendMessage("§6/fc buy <id> §7- Purchase a factory");
        sender.sendMessage("§6/fc sell <id> §7- Sell your factory");
        sender.sendMessage("§6/fc info <id> §7- View factory info");
        sender.sendMessage("§6/fc gui <id> §7- Open factory GUI");
        sender.sendMessage("§6/fc tp <id> §7- Teleport to factory");

        if (sender.hasPermission("factorycore.admin")) {
            sender.sendMessage("");
            sender.sendMessage("§c§lAdmin Commands:");
            sender.sendMessage("§6/fc admin create §7- Create a factory");
            sender.sendMessage("§6/fc admin remove <id> §7- Remove a factory");
            sender.sendMessage("§6/fc admin list §7- List all factories");
            sender.sendMessage("§6/fc admin reload §7- Reload config");
            sender.sendMessage("§6/fc admin npc §7- Manage NPCs");
            sender.sendMessage("§6/fc admin tax §7- Manage taxes");
            sender.sendMessage("§6/fc admin market §7- Manage marketplace");
            sender.sendMessage("§6/fc admin research §7- Manage factory research");
        }

        sender.sendMessage("");
        sender.sendMessage("§6/fc version §7- Show plugin version");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("hub", "browse", "my", "invoices", "taxes", "employees", "market",
                    "dailyquest", "buy", "sell", "info", "gui", "tp", "version", "help"));
            if (sender.hasPermission("factorycore.admin")) {
                completions.add("admin");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            completions.addAll(Arrays.asList("create", "remove", "list", "info", "reload", "checkrecipes",
                    "setowner", "teleport", "give", "npc", "tax", "market", "research"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            if (args[1].equalsIgnoreCase("npc")) {
                completions.addAll(Arrays.asList("spawn", "remove", "list", "respawn"));
            } else if (args[1].equalsIgnoreCase("tax")) {
                completions.addAll(Arrays.asList("assess", "check"));
            } else if (args[1].equalsIgnoreCase("market")) {
                completions.addAll(Arrays.asList("cleanup", "stats"));
            } else if (args[1].equalsIgnoreCase("research")) {
                completions.addAll(Arrays.asList("upgrade", "set"));
            } else if (args[1].equalsIgnoreCase("create")) {
                completions.addAll(WorldGuardUtils.getAllRegionNames());
            } else if (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("info")) {
                for (Factory factory : plugin.getFactoryManager().getAllFactories()) {
                    completions.add(factory.getId());
                }
            } else if (args[1].equalsIgnoreCase("give")) {
                completions.addAll(plugin.getResourceManager().getAllResources().keySet());
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("give")) {
            completions.addAll(Arrays.asList("1", "10", "32", "64"));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("npc")) {
            // /fc admin npc <spawn|remove|respawn> <arg>
            String npcAction = args[2].toLowerCase();
            if (npcAction.equals("spawn")) {
                // Suggest factory IDs
                plugin.getFactoryManager().getAllFactories()
                        .forEach(f -> completions.add(f.getId()));
            } else if (npcAction.equals("remove") || npcAction.equals("respawn")) {
                // Suggest existing NPC IDs
                plugin.getNPCManager().getAllNPCs()
                        .forEach(n -> completions.add(n.getId()));
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("research")) {
            // /fc admin research <upgrade|set> <factory_id>
            String researchAction = args[2].toLowerCase();
            if (researchAction.equals("upgrade") || researchAction.equals("set")) {
                plugin.getFactoryManager().getAllFactories().stream()
                        .filter(f -> f.getOwner() != null)
                        .forEach(f -> completions.add(f.getId()));
            }
        } else if (args.length == 5 && args[0].equalsIgnoreCase("admin")) {
            if (args[1].equalsIgnoreCase("npc") && args[2].equalsIgnoreCase("spawn")) {
                // Suggest npc_id — hint with factory_id prefix
                String factoryId = args[3];
                completions.add(factoryId + "_npc_1");
                completions.add(factoryId + "_npc_2");
            } else if (args[1].equalsIgnoreCase("create")) {
                for (FactoryType type : FactoryType.values()) {
                    completions.add(type.getId());
                }
            } else if (args[1].equalsIgnoreCase("research")) {
                // /fc admin research <upgrade|set> <factory_id> <research_id>
                if (plugin.getResearchManager() != null) {
                    completions.addAll(plugin.getResearchManager().getResearchIds());
                }
            } else if (args[1].equalsIgnoreCase("give")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 6 && args[0].equalsIgnoreCase("admin")) {
            if (args[1].equalsIgnoreCase("npc") && args[2].equalsIgnoreCase("spawn")) {
                // Suggest template names
                completions.addAll(Arrays.asList(
                        "default",
                        "steel_mill_employee",
                        "refinery_employee",
                        "workshop_employee",
                        "advanced_factory_employee"));
            } else if (args[1].equalsIgnoreCase("create")) {
                completions.addAll(Arrays.asList("1000", "5000", "10000", "50000", "100000"));
            } else if (args[1].equalsIgnoreCase("research") && args[2].equalsIgnoreCase("set")) {
                // /fc admin research set <factory_id> <research_id> <level>
                completions.addAll(Arrays.asList("0", "1", "2", "3", "4", "5"));
            }
        } else if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("buy")) {
                plugin.getFactoryManager().getAllFactories().stream()
                        .filter(f -> f.getOwner() == null)
                        .forEach(f -> completions.add(f.getId()));
            } else if (subCmd.equals("sell") || subCmd.equals("gui") || subCmd.equals("tp")
                    || subCmd.equals("teleport")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    plugin.getFactoryManager().getFactoriesByOwner(player.getUniqueId())
                            .forEach(f -> completions.add(f.getId()));
                }
            } else if (subCmd.equals("info")) {
                plugin.getFactoryManager().getAllFactories()
                        .forEach(f -> completions.add(f.getId()));
            }
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
