package com.aithor.factorycore.models;

import java.util.*;

/**
 * Represents an active (in-progress) distribution request that a player can fulfill.
 * Tracks which resources have been delivered and timing information.
 */
public class ActiveDistributionRequest {
    private final String requestId;
    private final String eventId;
    private final long startTime;
    private final long expireTime;
    private final Map<String, Boolean> deliveredResources;

    public ActiveDistributionRequest(String requestId, String eventId, long startTime, long expireTime,
                                     List<String> demandedResources) {
        this.requestId = requestId;
        this.eventId = eventId;
        this.startTime = startTime;
        this.expireTime = expireTime;
        this.deliveredResources = new LinkedHashMap<>();
        for (String resourceId : demandedResources) {
            deliveredResources.put(resourceId, false);
        }
    }

    public ActiveDistributionRequest(String requestId, String eventId, long startTime, long expireTime,
                                     Map<String, Boolean> deliveredResources) {
        this.requestId = requestId;
        this.eventId = eventId;
        this.startTime = startTime;
        this.expireTime = expireTime;
        this.deliveredResources = new LinkedHashMap<>(deliveredResources);
    }

    public String getRequestId() { return requestId; }
    public String getEventId() { return eventId; }
    public long getStartTime() { return startTime; }
    public long getExpireTime() { return expireTime; }
    public Map<String, Boolean> getDeliveredResources() { return deliveredResources; }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireTime;
    }

    public boolean isCompleted() {
        return deliveredResources.values().stream().allMatch(Boolean::booleanValue);
    }

    public void markDelivered(String resourceId) {
        if (deliveredResources.containsKey(resourceId)) {
            deliveredResources.put(resourceId, true);
        }
    }

    public boolean isResourceDelivered(String resourceId) {
        return deliveredResources.getOrDefault(resourceId, false);
    }

    public int getDeliveredCount() {
        return (int) deliveredResources.values().stream().filter(Boolean::booleanValue).count();
    }

    public int getTotalDemand() {
        return deliveredResources.size();
    }

    /**
     * Returns the percentage of time remaining (0-100).
     */
    public double getTimeRemainingPercent() {
        long totalDuration = expireTime - startTime;
        long remaining = expireTime - System.currentTimeMillis();
        if (remaining <= 0) return 0;
        if (totalDuration <= 0) return 0;
        return (double) remaining / totalDuration * 100.0;
    }

    public long getRemainingTimeMillis() {
        return Math.max(0, expireTime - System.currentTimeMillis());
    }
}
