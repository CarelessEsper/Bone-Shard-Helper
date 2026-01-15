package com.boneshardhelper;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("boneshardhelper")
public interface BoneShardHelperConfig extends Config {
    @ConfigSection(name = "Overlay", description = "Configure prayer object overlay", position = 0)
    String overlaySection = "overlaySection";

    // Object Highlighting Settings
    @ConfigItem(keyName = "highlightPrayerObjects", name = "Highlight Prayer Objects", description = "Enable highlighting of prayer objects with colored outlines", section = overlaySection, position = 0)
    default boolean highlightPrayerObjects() {
        return true;
    }

    @ConfigItem(keyName="showActionsRemaining", name="Display actions remaining", description = "Show overlay text near libation bowl with number of remaining actions", section = overlaySection, position = 1)
    default boolean showActionsRemaining() {
        return true;
    }

    @Alpha
    @ConfigItem(keyName="activeObjectColor", name="Active object color", description = "Highlight the color of the active object for training", section = overlaySection, position = 2)
    default Color activeObjectColor() {
        return Color.GREEN;
    }

    @Alpha
    @ConfigItem(keyName="cautionObjectColor", name="Caution object color", description = "Highlight color when there are few actions remaining", section = overlaySection, position = 3)
    default Color cautionObjectColor() {
        return Color.ORANGE;
    }

    @Alpha
    @ConfigItem(keyName="inactiveObjectColor", name="Inactive object color", description = "Highlight the color of non-active training objects", section = overlaySection, position = 4)
    default Color inactiveObjectColor() {
        return Color.DARK_GRAY;
    }

    @ConfigItem(keyName="exposedAltarPath", name="Draw path to exposed altar", description = "Draw ground path to the exposed altar when you have unblessed wines in training area", section = overlaySection, position = 5)
    default boolean exposedAltarPath() {
        return true;
    }

    @Alpha
    @ConfigItem(keyName="exposedAltarPathColor", name="Path color", description = "Color of ground path to exposed altar", section = overlaySection, position = 6)
    default Color exposedAltarPathColor() {
        return Color.CYAN;
    }

    // Infobox Settings
    @ConfigSection(name="Infobox", description = "Configure training infobox", position=1)
    String infoboxSection = "infobox";

    @ConfigItem(keyName="toggleInfobox", name="Enable infobox at Ralos' Rise", description="Toggle for \"Bone Shard Helper\" infobox (only shows while in Ralos' Rise map region)", section="infobox",position=0)
    default boolean toggleInfobox() {
        return true;
    }

    @ConfigItem(keyName="infoboxTitle", name="Title", description="Show title for infobox", section="infobox",position=1)
    default boolean infoboxTitle() {
        return true;
    }

    @ConfigItem(keyName="infoboxCurrentState", name = "Current Step", description="Show current training action", section="infobox",position=2)
    default boolean infoboxCurrentState() {
        return true;
    }

    @ConfigItem(keyName="infoboxActionsLeft", name="Remaining actions", description="Show number of actions remaining", section="infobox",position=3)
    default boolean infoboxActionsLeft() {
        return true;
    }

    @ConfigItem(keyName="infoboxInventoriesLeft", name="Remaining inventories", description="Show number of inventories remaining", section="infobox",position=4)
    default boolean infoboxInventoriesLeft() {
        return true;
    }

    // Debug Settings
    @ConfigSection(name = "Debug Settings", description = "Configure debug and development options", position = 2)
    String debugSection = "debug";

    @ConfigItem(keyName = "debugMode", name = "Debug Mode", description = "Enable debug mode to show additional debugging information and tables", section = debugSection, position = 0)
    default boolean debugMode() {
        return false;
    }
}