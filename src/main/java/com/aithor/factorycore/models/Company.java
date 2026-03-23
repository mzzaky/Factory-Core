package com.aithor.factorycore.models;

import java.util.List;

/**
 * Represents a virtual company entity in the Distribution Center system.
 * Companies act as consumers that request materials from players.
 */
public class Company {
    private final String id;
    private final String name;
    private final String description;
    private final String icon;
    private final String industry;
    private final String ceo;
    private final String headquarters;
    private final int reputation;
    private final List<String> lore;

    public Company(String id, String name, String description, String icon,
                   String industry, String ceo, String headquarters, int reputation, List<String> lore) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.industry = industry;
        this.ceo = ceo;
        this.headquarters = headquarters;
        this.reputation = reputation;
        this.lore = lore;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public String getIndustry() { return industry; }
    public String getCeo() { return ceo; }
    public String getHeadquarters() { return headquarters; }
    public int getReputation() { return reputation; }
    public List<String> getLore() { return lore; }
}
