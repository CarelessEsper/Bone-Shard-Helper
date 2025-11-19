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
        53018, // EXPOSED_ALTAR
        52405, // SHRINE_OF_RALOS  
        53016,  // 
        52799   // EXPOSED_ALTAR
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
            case 53018:
                return "Exposed Altar";
            case 52405:
                return "Shrine of Ralos";
            case 53016:
                return "Libation Bowl2";
            case 52799:
                return "Libation Bowl";
            default:
                return "Unknown Prayer Object";
        }
    }

    public boolean shouldHighlight()
    {
        return tileObject != null && PRAYER_OBJECT_IDS.contains(objectId);
    }

    public java.awt.Color getHighlightColor(BoneShardHelperConfig config)
    // Gets highlight color from config.
    {
        switch (objectId)
        {
            case 53018: // LIBATION_BOWL
                return config.getLibationBowlColor();
            case 52405: // SHRINE_OF_RALOS
                return config.getShrineOfRalosColor();
            case 52799: // EXPOSED_ALTAR
                return config.getExposedAltarColor();
            default:
                return java.awt.Color.GREEN; // Fallback color
        }
    }
}