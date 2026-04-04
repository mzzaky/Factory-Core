package com.aithor.factorycore.models;
import java.util.*;

public class ProductionTask {
    private String recipeId;
    private long startTime;
    private int duration; // in seconds
    private boolean continuous;
    
    public ProductionTask(String recipeId, long startTime, int duration) {
        this(recipeId, startTime, duration, false);
    }
    
    public ProductionTask(String recipeId, long startTime, int duration, boolean continuous) {
        this.recipeId = recipeId;
        this.startTime = startTime;
        this.duration = duration;
        this.continuous = continuous;
    }
    
    public String getRecipeId() { return recipeId; }
    public long getStartTime() { return startTime; }
    public int getDuration() { return duration; }
    public boolean isContinuous() { return continuous; }
    
    public boolean isComplete() {
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        return elapsed >= duration;
    }
    
    public double getProgress() {
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        return Math.min(1.0, (double) elapsed / duration);
    }
    
    public int getRemainingTime() {
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        return Math.max(0, duration - (int) elapsed);
    }
}