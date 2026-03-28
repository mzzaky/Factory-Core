package com.aithor.factorycore.models;

import org.bukkit.Material;

/**
 * Represents all available protection flags for a player-created factory region.
 * Each flag controls whether a specific type of event is blocked for non-owners.
 */
public enum ProtectionFlag {

    BLOCK_BREAK("Block Break", Material.IRON_PICKAXE,
            "Prevent non-owners from breaking blocks."),

    BLOCK_PLACE("Block Place", Material.BRICKS,
            "Prevent non-owners from placing blocks."),

    PLAYER_INTERACT("Player Interact", Material.OAK_DOOR,
            "Prevent non-owners from interacting",
            "with blocks (doors, levers, buttons, etc)."),

    CONTAINER_ACCESS("Container Access", Material.CHEST,
            "Prevent non-owners from opening",
            "containers (chests, furnaces, hoppers, etc)."),

    ENTITY_DAMAGE("Entity Damage", Material.DIAMOND_SWORD,
            "Prevent non-owners from damaging",
            "entities inside the factory region."),

    PVP("PvP", Material.IRON_SWORD,
            "Prevent players from attacking",
            "each other inside the factory region."),

    EXPLOSION("Explosion", Material.TNT,
            "Prevent explosions from damaging",
            "blocks inside the factory region."),

    MOB_SPAWNING("Mob Spawning", Material.ZOMBIE_HEAD,
            "Prevent hostile mobs from spawning",
            "inside the factory region."),

    ITEM_PICKUP("Item Pickup", Material.HOPPER,
            "Prevent non-owners from picking up",
            "items inside the factory region."),

    ITEM_DROP("Item Drop", Material.DROPPER,
            "Prevent non-owners from dropping",
            "items inside the factory region."),

    REDSTONE("Redstone", Material.REDSTONE,
            "Prevent non-owners from modifying",
            "redstone components in the region."),

    VEHICLE_PLACE("Vehicle Place", Material.OAK_BOAT,
            "Prevent non-owners from placing",
            "vehicles (boats, minecarts) in the region."),

    FIRE_SPREAD("Fire Spread", Material.FLINT_AND_STEEL,
            "Prevent fire from spreading",
            "inside the factory region."),

    LIQUID_FLOW("Liquid Flow", Material.WATER_BUCKET,
            "Prevent liquid (water/lava) from",
            "flowing into the factory region."),

    PISTON("Piston", Material.PISTON,
            "Prevent pistons from pushing/pulling",
            "blocks in the factory region."),

    CROP_TRAMPLE("Crop Trample", Material.WHEAT,
            "Prevent farmland from being trampled",
            "inside the factory region."),

    HANGING_BREAK("Hanging Break", Material.ITEM_FRAME,
            "Prevent non-owners from breaking",
            "hanging entities (item frames, paintings)."),

    ARMOR_STAND("Armor Stand", Material.ARMOR_STAND,
            "Prevent non-owners from manipulating",
            "armor stands inside the factory region.");

    private final String displayName;
    private final Material icon;
    private final String[] description;

    ProtectionFlag(String displayName, Material icon, String... description) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }
    public String[] getDescription() { return description; }
}
