package com.boneshardhelper;

// All blessed bone types and their corresponding shard values
public enum BoneType {
    BLESSED_BONE_SHARDS(1, 29381), // No base item for bone shards
    BLESSED_BONE_STATUETTE0(125, 29338), // Variations of blessed bone statuette
    BLESSED_BONE_STATUETTE1(125, 29339),
    BLESSED_BONE_STATUETTE2(125, 29340),
    BLESSED_BONE_STATUETTE3(125, 29342),
    BLESSED_BONE_STATUETTE4(125, 29343),
    SUPERIOR_DRAGON_BONES(121, 22124),
    OURG_BONES(115, 4834),
    DAGANNOTH_BONES(100, 6729),
    HYDRA_BONES(93, 22786),
    // FROST_DRAGON_BONES(84, 31729),
    RAURG_BONES(77, 4832),
    LAVA_DRAGON_BONES(68, 11943),
    DRAKE_BONES(67, 22783),
    FAYRG_BONES(67, 4830),
    DRAGON_BONES(58, 536),
    WYVERN_BONES(58, 6812),
    SUN_KISSED_BONES(45, 29380),
    WYRM_BONES(42, 22780),
    BABYDRAGON_BONES(24, 534),
    BABYWYRM_BONES(21, 28899), // Wyrmling bones
    WYRMLING_BONES(21, 28899),
    ZOGRE_BONES(18, 4812),
    BIG_BONES(12, 532),
    BAT_BONES(5, 530),
    BONES(4, 526),
    BLESSED_BONES(4, 29344),
    BLESSED_BAT_BONES(5, 29346),
    BLESSED_BIG_BONES(12, 29348),
    BLESSED_BABYDRAGON_BONES(24, 29352),
    BLESSED_DRAGON_BONES(58, 29356),
    BLESSED_WYVERN_BONES(58, 29360),
    BLESSED_DRAKE_BONES(67, 29366),
    BLESSED_FAYRG_BONES(67, 29370),
    BLESSED_LAVA_DRAGON_BONES(68, 29358),
    BLESSED_RAURG_BONES(77, 29372),
    BLESSED_DAGANNOTH_BONES(100, 29376),
    BLESSED_OURG_BONES(115, 29374),
    BLESSED_SUPERIOR_DRAGON_BONES(121, 29362),
    BLESSED_BABYWYRM_BONES(21, 29354), // Wyrmling bones
    BLESSED_WYRM_BONES(42, 29364),
    BLESSED_HYDRA_BONES(93, 29368),
    // BLESSED_FROST_DRAGON_BONES(84, -1),
    BLESSED_ZOGRE_BONES(18, 29350);

    private final int shardValue;
    private final int baseItemId;

    BoneType(int shardValue, int baseItemId) {
        this.shardValue = shardValue;
        this.baseItemId = baseItemId;
    }

    public int getShardValue() {
        return shardValue;
    }

    public int getBaseItemId() {
        // Gets the base (unblessed) item ID for this bone type.
        return baseItemId;
    }

    public boolean hasBaseItem() {
        // Some bone types don't have blessed versions so we gotta check it
        return baseItemId > 0;
    }

    public String getDisplayName() {
        // Special case for blessed bone statuette (consolidated display)
        if (name().startsWith("BLESSED_BONE_STATUETTE")) {
            return "Blessed Bone Statuette";
        }

        // Special case for blessed bone shards
        if (name().equals("BLESSED_BONE_SHARDS")) {
            return "Bone Shards";
        }

        // Special case for sun-kissed bones
        if (name().equals("SUN_KISSED_BONES")) {
            return "Sun-kissed Bones";
        }

        String name = name().toLowerCase()
                .replace("_", " ")
                .replace("blessed ", ""); // Remove "blessed" prefix for clean display

        // Capitalize first letter of each word
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : name.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
}