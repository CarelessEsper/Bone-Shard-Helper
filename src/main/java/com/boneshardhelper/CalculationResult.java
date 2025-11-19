package com.boneshardhelper;

import java.util.HashMap;
import java.util.Map;

public class CalculationResult {
    private int requiredShards;
    private int achievableLevel;
    private int totalAvailableShards;
    private Map<BoneType, Integer> boneBreakdown;
    private int remainingXP;
    private int totalXPGain;
    private boolean hasEnoughResources;
    private int xpPerShard;
    private boolean goalAlreadyAchieved;
    private int winesNeeded;

    public CalculationResult() {
        this.requiredShards = 0;
        this.achievableLevel = 1;
        this.totalAvailableShards = 0;
        this.boneBreakdown = new HashMap<>();
        this.remainingXP = 0;
        this.totalXPGain = 0;
        this.hasEnoughResources = false;
        this.xpPerShard = 5; // Default to regular wine
        this.goalAlreadyAchieved = false;
        this.winesNeeded = 0;
    }

    public CalculationResult(int requiredShards, int achievableLevel, int totalAvailableShards) {
        this.requiredShards = requiredShards;
        this.achievableLevel = achievableLevel;
        this.totalAvailableShards = totalAvailableShards;
        this.boneBreakdown = new HashMap<>();
        this.remainingXP = 0;
        this.totalXPGain = 0;
        this.hasEnoughResources = totalAvailableShards >= requiredShards;
        this.xpPerShard = 5; // Default to regular wine
        this.goalAlreadyAchieved = false;
        this.winesNeeded = 0;
    }

    public CalculationResult(int requiredShards, int achievableLevel, int totalAvailableShards, int xpPerShard) {
        this.requiredShards = requiredShards;
        this.achievableLevel = achievableLevel;
        this.totalAvailableShards = totalAvailableShards;
        this.boneBreakdown = new HashMap<>();
        this.remainingXP = 0;
        this.totalXPGain = 0;
        this.hasEnoughResources = totalAvailableShards >= requiredShards;
        this.xpPerShard = xpPerShard;
        this.goalAlreadyAchieved = false;
        this.winesNeeded = 0;
    }

    // Getters and setters
    public int getRequiredShards() {
        return requiredShards;
    }

    public void setRequiredShards(int requiredShards) {
        this.requiredShards = requiredShards;
        updateHasEnoughResources();
    }

    public int getAchievableLevel() {
        return achievableLevel;
    }

    public void setAchievableLevel(int achievableLevel) {
        this.achievableLevel = achievableLevel;
    }

    public int getTotalAvailableShards() {
        return totalAvailableShards;
    }

    public void setTotalAvailableShards(int totalAvailableShards) {
        this.totalAvailableShards = totalAvailableShards;
        updateHasEnoughResources();
    }

    public Map<BoneType, Integer> getBoneBreakdown() {
        return new HashMap<>(boneBreakdown);
    }

    public void setBoneBreakdown(Map<BoneType, Integer> boneBreakdown) {
        this.boneBreakdown = new HashMap<>(boneBreakdown);
    }

    public int getRemainingXP() {
        return remainingXP;
    }

    public void setRemainingXP(int remainingXP) {
        this.remainingXP = remainingXP;
    }

    public int getTotalXPGain() {
        return totalXPGain;
    }

    public void setTotalXPGain(int totalXPGain) {
        this.totalXPGain = totalXPGain;
    }

    public boolean hasEnoughResources() {
        return hasEnoughResources;
    }

    public void setHasEnoughResources(boolean hasEnoughResources) {
        this.hasEnoughResources = hasEnoughResources;
    }

    public int getXpPerShard() {
        return xpPerShard;
    }

    public void setXpPerShard(int xpPerShard) {
        this.xpPerShard = xpPerShard;
    }

    public boolean isGoalAlreadyAchieved() {
        return goalAlreadyAchieved;
    }

    public void setGoalAlreadyAchieved(boolean goalAlreadyAchieved) {
        this.goalAlreadyAchieved = goalAlreadyAchieved;
    }

    public int getWinesNeeded() {
        return winesNeeded;
    }

    public void setWinesNeeded(int winesNeeded) {
        this.winesNeeded = winesNeeded;
    }

    private void updateHasEnoughResources() {
        // Update the hasEnoughResources flag based on current values.
        this.hasEnoughResources = totalAvailableShards >= requiredShards;
    }

    public int getShardShortage() {
        // Gets the shortage of shards if resources are insufficient.
        return Math.max(0, requiredShards - totalAvailableShards);
    }

    public int getExcessShards() {
        // Gets the excess shards if resources exceed requirements.
        return Math.max(0, totalAvailableShards - requiredShards);
    }

    public boolean isValid() {
        // Checks if this result represents a valid calculation.
        return achievableLevel > 0 && (requiredShards > 0 || totalAvailableShards > 0);
    }

    @Override
    public String toString() {
        return "CalculationResult{" +
                "requiredShards=" + requiredShards +
                ", achievableLevel=" + achievableLevel +
                ", totalAvailableShards=" + totalAvailableShards +
                ", boneTypes=" + boneBreakdown.size() +
                ", remainingXP=" + remainingXP +
                ", totalXPGain=" + totalXPGain +
                ", hasEnoughResources=" + hasEnoughResources +
                ", xpPerShard=" + xpPerShard +
                ", goalAlreadyAchieved=" + goalAlreadyAchieved +
                ", winesNeeded=" + winesNeeded +
                '}';
    }
}