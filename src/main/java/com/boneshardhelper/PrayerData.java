package com.boneshardhelper;

import java.util.HashMap;
import java.util.Map;

public class PrayerData {
    private int currentXP;
    private int currentLevel;
    private int targetLevel;
    private int targetXP;
    private boolean useSunfireWine;
    private boolean useZealotRobes;
    private Map<BoneType, Integer> availableBones;
    private boolean manualLevelOverride;

    public PrayerData() {
        this.currentXP = 0;
        this.currentLevel = 1;
        this.targetLevel = 2;
        this.targetXP = 0;
        this.useSunfireWine = false;
        this.useZealotRobes = false;
        this.availableBones = new HashMap<>();
        this.manualLevelOverride = false;
    }

    public PrayerData(int currentXP, int currentLevel, int targetLevel, boolean useSunfireWine) {
        this.currentXP = currentXP;
        this.currentLevel = currentLevel;
        this.targetLevel = targetLevel;
        this.targetXP = 0; // Will be calculated from target level if not set
        this.useSunfireWine = useSunfireWine;
        this.useZealotRobes = false;
        this.availableBones = new HashMap<>();
        this.manualLevelOverride = false;
    }

    // Getters and setters
    public int getCurrentXP() {
        return currentXP;
    }

    public void setCurrentXP(int currentXP) {
        this.currentXP = currentXP;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }

    public int getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(int targetLevel) {
        this.targetLevel = targetLevel;
    }

    public int getTargetXP() {
        return targetXP;
    }

    public void setTargetXP(int targetXP) {
        this.targetXP = targetXP;
    }

    public boolean isUseSunfireWine() {
        return useSunfireWine;
    }

    public void setUseSunfireWine(boolean useSunfireWine) {
        this.useSunfireWine = useSunfireWine;
    }

    public boolean isUseZealotRobes() {
        return useZealotRobes;
    }

    public void setUseZealotRobes(boolean useZealotRobes) {
        this.useZealotRobes = useZealotRobes;
    }

    public Map<BoneType, Integer> getAvailableBones() {
        return new HashMap<>(availableBones);
    }

    public void setAvailableBones(Map<BoneType, Integer> availableBones) {
        this.availableBones = new HashMap<>(availableBones);
    }

    public boolean isManualLevelOverride() {
        return manualLevelOverride;
    }

    public void setManualLevelOverride(boolean manualLevelOverride) {
        this.manualLevelOverride = manualLevelOverride;
    }

    public double getXPPerShard() {
        // Gets the XP per shard, accounting for wine type and zealot robes
        double baseXP = useSunfireWine ? 6.0 : 5.0;
        return useZealotRobes ? baseXP * 1.05 : baseXP;
    }

    public int getTotalAvailableShards() {
        // Calculates total available bone shards from all bone types.
        return availableBones.entrySet().stream()
                .mapToInt(entry -> entry.getKey().getShardValue() * entry.getValue())
                .sum();
    }

    public boolean isValidTargetLevel() {
        return targetLevel > currentLevel && targetLevel <= 126;
    }

    @Override
    public String toString() {
        return "PrayerData{" +
                "currentXP=" + currentXP +
                ", currentLevel=" + currentLevel +
                ", targetLevel=" + targetLevel +
                ", targetXP=" + targetXP +
                ", useSunfireWine=" + useSunfireWine +
                ", useZealotRobes=" + useZealotRobes +
                ", availableBones=" + availableBones.size() + " types" +
                ", manualLevelOverride=" + manualLevelOverride +
                '}';
    }
}