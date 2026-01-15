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
    private int cachedVarbitValue = -1; // Cache the varbit value

    @Setter
    private int wineActionsInBowl; // Track wine actions in bowl separately

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
        int currentRegion = client.getLocalPlayer().getWorldLocation().getRegionID();

        // Check if in the main training region (5681)
        if (currentRegion == REGION_ID) {
            return true;
        }

        // Check if in the additional training area in region 5680
        if (currentRegion == 5680) {
            // Check if within the rectangular area from (39,63) to (52,57)
            int regionX = client.getLocalPlayer().getWorldLocation().getRegionX();
            int regionY = client.getLocalPlayer().getWorldLocation().getRegionY();

            // The area is bounded by (39,63) and (52,57) - these are opposite corners
            // So we need: 39 <= regionX <= 52 AND 57 <= regionY <= 63
            return regionX >= 39 && regionX <= 52 && regionY >= 57 && regionY <= 63;
        }

        return false;
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

    // Get the number of wine actions remaining in the bowl directly from varbit
    public int getWineActionsInBowl() {
        updateVarbitCache();
        return wineActionsInBowl;
    }

    public boolean hasWineInBowl() {
        return getWineActionsInBowl() > 0;
    }

    // Handle varbit changes from the plugin
    public void onVarbitChanged(int newValue) {
        cachedVarbitValue = newValue;
        wineActionsInBowl = newValue / 100; // Convert varbit value to actions (100 shards per action)
    }

    // Update varbit cache when needed
    private void updateVarbitCache() {
        if (client != null) {
            int currentVarbitValue = client.getVarbitValue(9945);
            if (currentVarbitValue != cachedVarbitValue) {
                cachedVarbitValue = currentVarbitValue;
                wineActionsInBowl = currentVarbitValue / 100;
            }
        }
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

    // Debug method to get raw varbit value
    public int getRawVarbitValue() {
        updateVarbitCache();
        return cachedVarbitValue;
    }

    // Main state logic
    public TrainingState getCurrentTrainingState() {
        boolean hasShards = hasShards();
        boolean hasUnblessed = hasUnblessedWines();
        boolean hasBlessed = hasBlessedWines();
        boolean hasWineInBowl = getWineActionsInBowl() > 0;
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

        return currentState;
    }

    // Counter methods
    public int getRemainingInventoryActions() {
        // Calculate wine actions from inventory wines + bowl wine actions
        return (getBlessedWineCount() * 4) + getWineActionsInBowl();
    }

    public int getActionsUntilPrayerRestore() {
        int prayerActions = getCurrentPrayerPoints() / 2;
        int wineActions = getRemainingInventoryActions();
        int shardActions = (int) Math.ceil(getBlessedBoneShardCount() / 100.0);

        int availableActions = Math.min(wineActions, shardActions);
        return Math.min(prayerActions, availableActions);
    }

    // Consolidated tracker: Actions remaining until prayer restore OR inventory
    // depletion (whichever is smaller)
    public int getActionsRemaining() {
        int inventoryActions = getRemainingInventoryActions();
        int prayerActions = getActionsUntilPrayerRestore();

        // Return the smaller of the two (the limiting factor)
        return Math.min(inventoryActions, prayerActions);
    }

    // Wine actions tracked via VARLAMORE_PRAYER_WINEQUANT varbit (ID: 9945)
    // All calculations are done in real-time using current inventory + varbit state
    // No historical tracking needed - everything is calculated from current game
    // state
}
