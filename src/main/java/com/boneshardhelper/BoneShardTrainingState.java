package com.boneshardhelper;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BoneShardTrainingState {
    private static final int REGION_ID = 5681;
    private static final int WINE_ID = 1993;
    private static final int SUNFIRE_WINE_ID = 29382;
    private static final int BLESSED_SUNFIRE_WINE_ID = 29384;
    private static final int BLESSED_WINE_ID = 29386;
    private static final int BLESSED_BONE_SHARDS_ID = 29381;

    @Inject
    private Client client;

    @Setter
    @Getter
    private boolean enabled;

    // Tracking variables
    private int cachedBoneShardCount = -1;
    private int totalActionsPerformed = 0;
    private int sessionStartWineActions = 0;
    private TrainingState lastState = null;
    private int wineActionsInBowl = 0; // Track wine actions in bowl separately

    public enum TrainingState {
        NO_STATE(-1, ""),
        BLESS_WINES(1, "BLESS WINES"),
        SACRIFICE_SHARDS(2, "SACRIFICE SHARDS"),
        RECHARGE_PRAYER(3, "RECHARGE PRAYER");

        private final int id;
        private final String displayName;

        TrainingState(int id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public int getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Core inventory checks
    private ItemContainer getInventory() {
        return client.getItemContainer(93);
    }

    private boolean hasItem(int... itemIds) {
        ItemContainer inv = getInventory();
        if (inv == null)
            return false;
        for (int id : itemIds) {
            if (inv.contains(id))
                return true;
        }
        return false;
    }

    private int getItemCount(int... itemIds) {
        ItemContainer inv = getInventory();
        if (inv == null)
            return 0;
        int total = 0;
        for (int id : itemIds) {
            total += inv.count(id);
        }
        return total;
    }

    // Simple state checks
    public boolean inTrainingRegion() {
        return client.getLocalPlayer().getWorldLocation().getRegionID() == REGION_ID;
    }

    public boolean hasUnblessedWines() {
        return hasItem(WINE_ID, SUNFIRE_WINE_ID);
    }

    public boolean hasBlessedWines() {
        return hasItem(BLESSED_SUNFIRE_WINE_ID, BLESSED_WINE_ID);
    }

    public boolean hasShards() {
        return hasItem(BLESSED_BONE_SHARDS_ID);
    }

    public boolean hasSufficientPrayer() {
        return client.getBoostedSkillLevel(Skill.PRAYER) >= 2;
    }

    public boolean hasWineInBowl() {
        return wineActionsInBowl > 0;
    }

    // Get the number of actions remaining from wine in the bowl
    public int getWineActionsInBowl() {
        updateTrackingState(); // Ensure state is current
        return wineActionsInBowl;
    }

    // Event-driven method called when player checks wine remaining in bowl
    public void onBowlMessageReceived(String message) {
        int wineActions = parseWineActionsFromText(message);
        if (wineActions > 0) {
            syncWineActionsWithMessage(wineActions);
        }
    }

    // Sync our tracking with the chat message value
    private void syncWineActionsWithMessage(int messageActions) {
        // Simply trust the message and update bowl actions directly
        wineActionsInBowl = messageActions;
    }

    // Parse wine actions from inspect bowl text
    private int parseWineActionsFromText(String text) {
        // Look for the specific values we expect: 100, 200, 300, or 400 shards
        if (text.contains("400")) {
            return 4; // 400 shards = 4 actions
        } else if (text.contains("300")) {
            return 3; // 300 shards = 3 actions
        } else if (text.contains("200")) {
            return 2; // 200 shards = 2 actions
        } else if (text.contains("100")) {
            return 1; // 100 shards = 1 action
        }
        return 0; // No valid shard count found; should never happen
    }

    // Action calculations
    public int getBlessedWineCount() {
        return getItemCount(BLESSED_SUNFIRE_WINE_ID, BLESSED_WINE_ID);
    }

    public int getBlessedBoneShardCount() {
        return getItemCount(BLESSED_BONE_SHARDS_ID);
    }

    public int getCurrentPrayerPoints() {
        return client.getBoostedSkillLevel(Skill.PRAYER);
    }

    public int getActionsPerformed() {
        updateTrackingState(); // Process any changes first
        return totalActionsPerformed;
    }

    // Manage wine charges after a sacrifice action
    private int updateWineCharges(int currentCharges) {
        if (currentCharges == 1) {
            // Bowl would be empty after this action - check if we can refill
            if (hasBlessedWines() && hasShards()) {
                return 4; // New wine poured into bowl
            } else {
                return 0; // No wines or shards left, bowl goes empty
            }
        } else if (currentCharges > 1) {
            // Normal consumption - reduce by 1
            return currentCharges - 1;
        } else {
            // Bowl was already empty or invalid state
            return 0;
        }
    }

    // Separate method to handle all state updates
    private void updateTrackingState() {
        int currentShards = getBlessedBoneShardCount();

        if (cachedBoneShardCount == -1) {
            cachedBoneShardCount = currentShards;
            return;
        }

        // Handle shard consumption (actions performed)
        if (currentShards < cachedBoneShardCount) {
            int shardsUsed = cachedBoneShardCount - currentShards;
            int actionsPerformed = shardsUsed / 100; // 100 shards per sacrifice action
            totalActionsPerformed += actionsPerformed;

            // Update wine charges for each action performed
            for (int i = 0; i < actionsPerformed; i++) {
                wineActionsInBowl = updateWineCharges(wineActionsInBowl);
            }

            cachedBoneShardCount = currentShards;
        } else if (currentShards > cachedBoneShardCount) {
            cachedBoneShardCount = currentShards;
        }
    }

    // Main state logic
    public TrainingState getCurrentTrainingState() {
        boolean hasShards = hasShards();
        boolean hasUnblessed = hasUnblessedWines();
        boolean hasBlessed = hasBlessedWines();
        boolean hasWineInBowl = hasWineInBowl();
        boolean hasPrayer = hasSufficientPrayer();

        TrainingState currentState;

        if (!hasShards || (!hasUnblessed && !hasBlessed && !hasWineInBowl)) {
            currentState = TrainingState.NO_STATE;
        } else if (hasUnblessed) {
            currentState = TrainingState.BLESS_WINES;
        } else if ((hasBlessed || hasWineInBowl) && hasPrayer) {
            currentState = TrainingState.SACRIFICE_SHARDS;
        } else if ((hasBlessed || hasWineInBowl) && !hasPrayer) {
            currentState = TrainingState.RECHARGE_PRAYER;
        } else {
            currentState = TrainingState.NO_STATE;
        }

        // Handle state transitions
        if (lastState != currentState) {
            if (currentState == TrainingState.SACRIFICE_SHARDS && lastState == TrainingState.BLESS_WINES) {
                sessionStartWineActions = getBlessedWineCount() * 4;
                resetActionCounter();
            }
            lastState = currentState;
        }

        return currentState;
    }

    // Counter methods
    public int getRemainingInventoryActions() {
        return Math.max(0, sessionStartWineActions - getActionsPerformed());
    }

    public int getActionsUntilPrayerRestore() {
        int prayerActions = getCurrentPrayerPoints() / 2;
        int wineActions = getBlessedWineCount() * 4 + (getWineActionsInBowl());
        int shardActions = (int) Math.ceil(getBlessedBoneShardCount() / 100.0);

        int availableActions = Math.min(wineActions, shardActions);
        return Math.min(prayerActions, availableActions);
    }

    // Consolidated tracker: Actions remaining until prayer restore OR inventory
    // depletion (whichever is smaller)
    public int getActionsRemaining() {
        int inventoryActions = getRemainingInventoryActions();
        int prayerActions = getActionsUntilPrayerRestore();

        // If no session snapshot exists, use prayer actions as fallback
        if (sessionStartWineActions == 0) {
            return prayerActions;
        }

        // Return the smaller of the two (the limiting factor)
        return Math.min(inventoryActions, prayerActions);
    }

    public int getSessionStartWineActions() {
        return sessionStartWineActions;
    }

    public void resetActionCounter() {
        totalActionsPerformed = 0;
        wineActionsInBowl = 0;
        cachedBoneShardCount = getBlessedBoneShardCount();
    }

    // TO DO: 
    //  Handle case where the player logs in with wine still in bowl. 
    //  Actions remaining will stay out of sync after new wine is poured in, unexpected!
}
