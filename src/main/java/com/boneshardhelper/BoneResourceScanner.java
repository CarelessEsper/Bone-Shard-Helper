package com.boneshardhelper;

import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class BoneResourceScanner {

    private final Client client;
    private final ClientThread clientThread;
    private final ItemManager itemManager;

    // Mapping from item IDs to BoneType enum values
    // Built dynamically from the BoneType enum data
    private static final Map<Integer, BoneType> ITEM_TO_BONE_TYPE_MAPPING = new HashMap<>();

    // Mapping for consolidating bone types (unblessed -> blessed)
    private static final Map<BoneType, BoneType> BONE_TYPE_CONSOLIDATION = new HashMap<>();

    static {
        // Build mappings from the BoneType enum data
        for (BoneType boneType : BoneType.values()) {
            int itemId = boneType.getBaseItemId();
            if (itemId > 0) {
                ITEM_TO_BONE_TYPE_MAPPING.put(itemId, boneType);

                // Map noted versions (base + 1) for unblessed bones only
                if (!boneType.name().startsWith("BLESSED_") && !boneType.name().equals("SUN_KISSED_BONES")) {
                    ITEM_TO_BONE_TYPE_MAPPING.put(itemId + 1, boneType);
                }
            }
        }

        // Build consolidation mapping (unblessed -> blessed)
        BONE_TYPE_CONSOLIDATION.put(BoneType.BONES, BoneType.BLESSED_BONES);
        BONE_TYPE_CONSOLIDATION.put(BoneType.BAT_BONES, BoneType.BLESSED_BAT_BONES);
        BONE_TYPE_CONSOLIDATION.put(BoneType.BIG_BONES, BoneType.BLESSED_BIG_BONES);
        BONE_TYPE_CONSOLIDATION.put(BoneType.BABYDRAGON_BONES, BoneType.BLESSED_BABYDRAGON_BONES);
        BONE_TYPE_CONSOLIDATION.put(BoneType.DRAGON_BONES, BoneType.BLESSED_DRAGON_BONES);
        BONE_TYPE_CONSOLIDATION.put(BoneType.WYVERN_BONES, BoneType.BLESSED_WYVERN_BONES);
        BONE_TYPE_CONSOLIDATION.put(BoneType.DRAKE_BONES, BoneType.BLESSED_DRAKE_BONES);
        BONE_TYPE_CONSOLIDATION.put(BoneType.FAYRG_BONES, BoneType.BLESSED_FAYRG_BONES);
        BONE_TYPE_CONSOLIDATION.put(BoneType.LAVA_DRAGON_BONES, BoneType.BLESSED_LAVA_DRAGON_BONES);
        BONE_TYPE_CONSOLIDATION.put(BoneType.RAURG_BONES, BoneType.BLESSED_RAURG_BONES);
        BONE_TYPE_CONSOLIDATION.put(BoneType.DAGANNOTH_BONES, BoneType.BLESSED_DAGANNOTH_BONES);
        BONE_TYPE_CONSOLIDATION.put(BoneType.OURG_BONES, BoneType.BLESSED_OURG_BONES);
        BONE_TYPE_CONSOLIDATION.put(BoneType.SUPERIOR_DRAGON_BONES, BoneType.BLESSED_SUPERIOR_DRAGON_BONES);
        BONE_TYPE_CONSOLIDATION.put(BoneType.BABYWYRM_BONES, BoneType.BLESSED_BABYWYRM_BONES);
        BONE_TYPE_CONSOLIDATION.put(BoneType.WYRMLING_BONES, BoneType.BLESSED_BABYWYRM_BONES);
        BONE_TYPE_CONSOLIDATION.put(BoneType.WYRM_BONES, BoneType.BLESSED_WYRM_BONES);
        BONE_TYPE_CONSOLIDATION.put(BoneType.HYDRA_BONES, BoneType.BLESSED_HYDRA_BONES);
        BONE_TYPE_CONSOLIDATION.put(BoneType.ZOGRE_BONES, BoneType.BLESSED_ZOGRE_BONES);
        BONE_TYPE_CONSOLIDATION.put(BoneType.FROST_DRAGON_BONES, BoneType.BLESSED_FROST_DRAGON_BONES);
        BONE_TYPE_CONSOLIDATION.put(BoneType.STRYKEWYRM_BONES, BoneType.BLESSED_STRYKEWYRM_BONES);

        // Consolidate blessed bone statuette variations
        BONE_TYPE_CONSOLIDATION.put(BoneType.BLESSED_BONE_STATUETTE1, BoneType.BLESSED_BONE_STATUETTE0);
        BONE_TYPE_CONSOLIDATION.put(BoneType.BLESSED_BONE_STATUETTE2, BoneType.BLESSED_BONE_STATUETTE0);
        BONE_TYPE_CONSOLIDATION.put(BoneType.BLESSED_BONE_STATUETTE3, BoneType.BLESSED_BONE_STATUETTE0);
        BONE_TYPE_CONSOLIDATION.put(BoneType.BLESSED_BONE_STATUETTE4, BoneType.BLESSED_BONE_STATUETTE0);
    }

    @Inject
    public BoneResourceScanner(Client client, ClientThread clientThread, ItemManager itemManager) {
        this.client = client;
        this.clientThread = clientThread;
        this.itemManager = itemManager;
    }

    public ClientThread getClientThread() {
        return clientThread;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public Map<BoneType, Integer> scanInventory() {
        Map<BoneType, Integer> bones = new HashMap<>();

        ItemContainer inventory = client.getItemContainer(93); // Inventory container ID
        if (inventory == null) {
            return bones;
        }

        Item[] items = inventory.getItems();
        if (items == null) {
            return bones;
        }

        for (Item item : items) {
            if (item == null || item.getId() <= 0) {
                continue;
            }

            BoneType boneType = getBoneTypeForItem(item.getId());
            if (boneType != null) {
                BoneType consolidatedType = consolidateBoneType(boneType);
                bones.merge(consolidatedType, item.getQuantity(), Integer::sum);
            }
        }

        return bones;
    }

    public int calculateTotalShards(Map<BoneType, Integer> boneResources) {
        return boneResources.entrySet().stream()
                .mapToInt(entry -> entry.getKey().getShardValue() * entry.getValue())
                .sum();
    }

    public int scanAndCalculateTotalShards() {
        return calculateTotalShards(scanInventory());
    }

    public static BoneType getBoneTypeForItem(int itemId) {
        return ITEM_TO_BONE_TYPE_MAPPING.get(itemId);
    }

    public static boolean isBoneItem(int itemId) {
        return ITEM_TO_BONE_TYPE_MAPPING.containsKey(itemId);
    }

    private BoneType consolidateBoneType(BoneType boneType) {
        // Consolidates bone types (unblessed -> blessed, statuette variations -> base).
        return BONE_TYPE_CONSOLIDATION.getOrDefault(boneType, boneType);
    }
}