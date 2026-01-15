package com.boneshardhelper;

import java.util.Set;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Value;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;

@Value
@AllArgsConstructor
class PrayerObject
{
    public static final Set<Integer> PRAYER_OBJECT_IDS = Set.of(
        52799, // EXPOSED_ALTAR
        52405, // SHRINE_OF_RALOS
        53018 // LIBATION_BOWL
    );

    private final Tile tile;
    @Nullable
    private final TileObject tileObject;
    private final int objectId;
    private final String name;

    public static PrayerObject fromTileObject(Tile tile, @Nullable TileObject tileObject)
    {
        if (tileObject == null)
        {
            return null;
        }

        int objectId = tileObject.getId();
        if (!PRAYER_OBJECT_IDS.contains(objectId))
        {
            return null;
        }

        String name = getObjectName(objectId);
        return new PrayerObject(tile, tileObject, objectId, name);
    }

    public static String getObjectName(int objectId)
    {
        switch (objectId)
        {
            case 52799:
                return "Exposed Altar";
            case 52405:
                return "Shrine of Ralos";
            case 53018:
                return "Libation Bowl";
            default:
                return "Unknown Prayer Object";
        }
    }

    public boolean shouldHighlight()
    {
        return tileObject != null && PRAYER_OBJECT_IDS.contains(objectId);
    }

    public java.awt.Color getHighlightColor(BoneShardHelperConfig config, BoneShardTrainingState trainingState)
    // Gets highlight color from config based on current training state.
    {
        BoneShardTrainingState.TrainingState currentState = trainingState.getCurrentTrainingState();
        
        switch (objectId)
        {
            case 52799: // EXPOSED_ALTAR
                // Active in BLESS_WINES state, inactive otherwise
                if (currentState == BoneShardTrainingState.TrainingState.BLESS_WINES) {
                    return config.activeObjectColor();
                } else {
                    return config.inactiveObjectColor();
                }
                
            case 52405: // SHRINE_OF_RALOS
                // Active in RECHARGE_PRAYER state, inactive otherwise
                if (currentState == BoneShardTrainingState.TrainingState.RECHARGE_PRAYER) {
                    return config.activeObjectColor();
                } else {
                    return config.inactiveObjectColor();
                }
            case 53018: // LIBATION_BOWL
                // Active in SACRIFICE_SHARDS state, inactive otherwise
                if (currentState == BoneShardTrainingState.TrainingState.SACRIFICE_SHARDS) {
                    return config.activeObjectColor();
                } else {
                    return config.inactiveObjectColor();
                }
                
            default:
                return null; // Don't highlight unknown objects
        }
    }
}