package com.aithor.factorycore.models;

import java.util.List;

/**
 * Represents a distribution request event configuration loaded from event.yml.
 */
public class DistributionEvent {
    private final String id;
    private final String name;
    private final List<String> lore;
    private final double chance;
    private final int time;
    private final String companyId;
    private final List<String> demand;
    private final double priceOffer;
    private final double bonus;
    private final int bonusCondition;
    private final List<String> commandOnAppear;
    private final List<String> commandOnComplete;

    public DistributionEvent(String id, String name, List<String> lore, double chance, int time,
                             String companyId, List<String> demand, double priceOffer,
                             double bonus, int bonusCondition,
                             List<String> commandOnAppear, List<String> commandOnComplete) {
        this.id = id;
        this.name = name;
        this.lore = lore;
        this.chance = chance;
        this.time = time;
        this.companyId = companyId;
        this.demand = demand;
        this.priceOffer = priceOffer;
        this.bonus = bonus;
        this.bonusCondition = bonusCondition;
        this.commandOnAppear = commandOnAppear;
        this.commandOnComplete = commandOnComplete;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<String> getLore() { return lore; }
    public double getChance() { return chance; }
    public int getTime() { return time; }
    public String getCompanyId() { return companyId; }
    public List<String> getDemand() { return demand; }
    public double getPriceOffer() { return priceOffer; }
    public double getBonus() { return bonus; }
    public int getBonusCondition() { return bonusCondition; }
    public List<String> getCommandOnAppear() { return commandOnAppear; }
    public List<String> getCommandOnComplete() { return commandOnComplete; }
}
