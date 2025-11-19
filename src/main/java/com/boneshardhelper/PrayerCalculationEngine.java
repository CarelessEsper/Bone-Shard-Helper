package com.boneshardhelper;

import java.util.Map;

public class PrayerCalculationEngine {

    public CalculationResult calculateForTarget(PrayerData prayerData) {
        if (prayerData == null) {
            throw new IllegalArgumentException("Prayer data cannot be null");
        }

        validatePrayerData(prayerData);

        int currentLevel = prayerData.getCurrentLevel();
        int targetLevel = prayerData.getTargetLevel();
        double xpPerShard = prayerData.getXPPerShard(); // Changed to double for zealot robes support

        // Calculate required XP for target (either from target level or target XP)
        int currentXP = prayerData.getCurrentXP();
        int targetXP;

        // Use target XP if set, otherwise calculate from target level
        if (prayerData.getTargetXP() > 0) {
            targetXP = prayerData.getTargetXP();
        } else {
            targetXP = PrayerConstants.getXPForLevel(targetLevel);
        }

        int requiredXP = targetXP - currentXP;

        // Check if goal is already achieved
        boolean goalAlreadyAchieved;
        if (prayerData.getTargetXP() > 0) {
            goalAlreadyAchieved = currentXP >= targetXP;
        } else {
            goalAlreadyAchieved = currentLevel >= targetLevel;
        }

        // Calculate required shards
        int requiredShards = goalAlreadyAchieved ? 0 : (int) Math.ceil((double) requiredXP / xpPerShard);

        // Get available resources
        int totalAvailableShards = prayerData.getTotalAvailableShards();

        // Create result with XP per shard tracking (convert double to int for display)
        CalculationResult result = new CalculationResult(requiredShards, currentLevel, totalAvailableShards,
                (int) Math.round(xpPerShard));
        result.setRemainingXP(goalAlreadyAchieved ? 0 : requiredXP);
        result.setTotalXPGain(goalAlreadyAchieved ? 0 : requiredXP);
        result.setBoneBreakdown(prayerData.getAvailableBones());
        result.setGoalAlreadyAchieved(goalAlreadyAchieved);

        // Calculate achievable level with available resources
        int achievableLevel = calculateAchievableLevelFromShardsWithXP(prayerData.getCurrentXP(), totalAvailableShards,
                xpPerShard);
        result.setAchievableLevel(achievableLevel);

        // Calculate wine information using the corrected 400 shards per wine formula
        int winesNeeded = calculateWinesNeeded(requiredShards);

        // Populate wine information in result
        result.setWinesNeeded(winesNeeded);

        return result;
    }

    public CalculationResult calculateFromResources(PrayerData prayerData) {
        if (prayerData == null) {
            throw new IllegalArgumentException("Prayer data cannot be null");
        }

        validatePrayerData(prayerData);

        int currentLevel = prayerData.getCurrentLevel();
        double xpPerShard = prayerData.getXPPerShard(); // Changed to double for zealot robes support
        int totalAvailableShards = prayerData.getTotalAvailableShards();

        // Calculate achievable level
        int achievableLevel = calculateAchievableLevelFromShardsWithXP(prayerData.getCurrentXP(), totalAvailableShards,
                xpPerShard);

        // Calculate XP gain
        int currentXP = prayerData.getCurrentXP();
        int achievableXP = PrayerConstants.getXPForLevel(achievableLevel);
        int totalXPGain = achievableXP - currentXP;

        // Create result with XP per shard tracking (convert double to int for display)
        CalculationResult result = new CalculationResult(0, achievableLevel, totalAvailableShards,
                (int) Math.round(xpPerShard));
        result.setTotalXPGain(totalXPGain);
        result.setBoneBreakdown(prayerData.getAvailableBones());
        result.setGoalAlreadyAchieved(false); // Resource mode doesn't have a specific goal

        // If we can't reach the next level, calculate remaining XP needed
        if (achievableLevel == currentLevel) {
            int nextLevelXP = PrayerConstants.getXPForLevel(currentLevel + 1);
            double availableXP = totalAvailableShards * xpPerShard;
            int remainingXP = (int) Math.max(0, nextLevelXP - currentXP - availableXP);
            result.setRemainingXP(remainingXP);
        }

        int winesNeeded = calculateWinesNeeded(totalAvailableShards);

        // Populate wine information in result
        result.setWinesNeeded(winesNeeded);

        return result;
    }

    public int calculateRequiredShards(int requiredXP, double xpPerShard) {
        if (requiredXP <= 0) {
            return 0;
        }
        if (xpPerShard <= 0) {
            throw new IllegalArgumentException("XP per shard must be positive");
        }

        return (int) Math.ceil((double) requiredXP / xpPerShard);
    }

    public int calculateAchievableLevelFromShards(int currentLevel, int availableShards, double xpPerShard) {
        if (availableShards <= 0) {
            return currentLevel;
        }

        int currentXP = PrayerConstants.getXPForLevel(currentLevel);
        double availableXP = availableShards * xpPerShard;
        int totalXP = (int) (currentXP + availableXP);

        return PrayerConstants.getLevelForXP(totalXP);
    }

    public int calculateAchievableLevelFromShardsWithXP(int currentXP, int availableShards, double xpPerShard) {
        if (availableShards <= 0) {
            return PrayerConstants.getLevelForXP(currentXP);
        }

        double availableXP = availableShards * xpPerShard;
        int totalXP = (int) (currentXP + availableXP);

        return PrayerConstants.getLevelForXP(totalXP);
    }

    public int calculateXPDifference(int fromLevel, int toLevel) {
        return PrayerConstants.getXPDifference(fromLevel, toLevel);
    }

    public int calculateTotalShards(Map<BoneType, Integer> boneBreakdown) {
        if (boneBreakdown == null || boneBreakdown.isEmpty()) {
            return 0;
        }

        return boneBreakdown.entrySet().stream()
                .mapToInt(entry -> entry.getKey().getShardValue() * entry.getValue())
                .sum();
    }

    public int calculateXPFromShards(int shards, double xpPerShard) {
        if (shards < 0) {
            throw new IllegalArgumentException("Shards cannot be negative");
        }
        if (xpPerShard <= 0) {
            throw new IllegalArgumentException("XP per shard must be positive");
        }

        return (int) (shards * xpPerShard);
    }

    public int calculateShardsForNextLevel(int currentLevel, double xpPerShard) {
        if (currentLevel >= PrayerConstants.MAX_PRAYER_LEVEL) {
            return 0;
        }

        int currentXP = PrayerConstants.getXPForLevel(currentLevel);
        int nextLevelXP = PrayerConstants.getXPForLevel(currentLevel + 1);
        int requiredXP = nextLevelXP - currentXP;

        return calculateRequiredShards(requiredXP, xpPerShard);
    }

    public int calculateShardsForNextLevelFromXP(int currentXP, double xpPerShard) {
        int currentLevel = PrayerConstants.getLevelForXP(currentXP);
        if (currentLevel >= PrayerConstants.MAX_PRAYER_LEVEL) {
            return 0;
        }

        int nextLevelXP = PrayerConstants.getXPForLevel(currentLevel + 1);
        int requiredXP = nextLevelXP - currentXP;

        return calculateRequiredShards(requiredXP, xpPerShard);
    }

    public int calculateWinesNeeded(int totalShards) {
        if (totalShards < 0) {
            throw new IllegalArgumentException("Total shards cannot be negative");
        }

        if (totalShards == 0) {
            return 0;
        }

        return (int) Math.ceil((double) totalShards / PrayerConstants.SHARDS_PER_WINE);
    }

    private void validatePrayerData(PrayerData prayerData) {
        if (prayerData.getCurrentLevel() < PrayerConstants.MIN_PRAYER_LEVEL ||
                prayerData.getCurrentLevel() > PrayerConstants.MAX_PRAYER_LEVEL) {
            throw new IllegalArgumentException("Invalid current prayer level: " + prayerData.getCurrentLevel());
        }

        // Validate target level if no target XP is set
        if (prayerData.getTargetXP() <= 0) {
            if (prayerData.getTargetLevel() < PrayerConstants.MIN_PRAYER_LEVEL ||
                    prayerData.getTargetLevel() > PrayerConstants.MAX_PRAYER_LEVEL) {
                throw new IllegalArgumentException("Invalid target prayer level: " + prayerData.getTargetLevel());
            }

            // Allow target level to equal current level (in case goal is already achieved, or if there's an xp goal)
            if (prayerData.getTargetLevel() < prayerData.getCurrentLevel()) {
                throw new IllegalArgumentException("Target level cannot be lower than current level");
            }
        } else {
            // When target XP is set, allow target level to be higher than normal max for 200M XP goals
            if (prayerData.getTargetLevel() < PrayerConstants.MIN_PRAYER_LEVEL) {
                throw new IllegalArgumentException("Invalid target prayer level: " + prayerData.getTargetLevel());
            }
            // Validate target XP
            if (prayerData.getTargetXP() < prayerData.getCurrentXP()) {
                throw new IllegalArgumentException("Target XP cannot be lower than current XP");
            }

            if (prayerData.getTargetXP() > 200_000_000) {
                throw new IllegalArgumentException("Target XP exceeds maximum prayer XP (200,000,000)");
            }
        }
    }
}