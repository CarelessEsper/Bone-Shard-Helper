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

    @ConfigItem(keyName = "highlightStyle", name = "Highlight Style", description = "Choose the style for highlighting prayer objects", section = overlaySection, position = 1)
    default HighlightStyle highlightStyle() {
        return HighlightStyle.HIGHLIGHT_CLICKBOX;
    }

    @Alpha
    @ConfigItem(keyName = "activeObjectColor", name = "Active object color", description = "Highlight the color of the active object for training", section = overlaySection, position = 2)
    default Color activeObjectColor() {
        return Color.GREEN;
    }

    @Alpha
    @ConfigItem(keyName = "inactiveObjectColor", name = "Inactive object color", description = "Highlight color for non-active training objects", section = overlaySection, position = 3)
    default Color inactiveObjectColor() {
        return Color.DARK_GRAY;
    }

    @ConfigItem(keyName = "exposedAltarPath", name = "Draw line to Exposed altar", description = "Draw a line to the Exposed altar when you have blessed bone shards and unblessed wines", section = overlaySection, position = 4)
    default boolean exposedAltarPath() {
        return true;
    }

    @Alpha
    @ConfigItem(keyName = "exposedAltarPathColor", name = "Path color", description = "Color of ground path to exposed altar", section = overlaySection, position = 5)
    default Color exposedAltarPathColor() {
        return Color.CYAN;
    }

    // Text overlay
    @ConfigSection(name = "Libation Bowl Text", description = "Display information about the training progress as an overlay on the Libation Bowl", position = 1)
    String infoOverlaySection = "infoOverlaySection";

    @ConfigItem(keyName = "overlayActionsLeft", name = "Actions Left Overlay", description = "Display the number of actions left until the next stage near the Libation Bowl at Ralos' Rise", section = infoOverlaySection, position = 0)
    default boolean toggleOverlayActionsLeft() {
        return true;
    }

    @ConfigItem(keyName = "overlayStageName", name = "Training Stage Overlay", description = "When not in the \"Sacrifice Shards\" training stage, display the current stage name.", section = infoOverlaySection, position = 1)
    default boolean toggleOverlayStageName() {
        return true;
    }

    @ConfigItem(keyName = "textOutline", name = "Text Outline", description = "Use an outline around text instead of a shadow.", section = infoOverlaySection, position = 2)
    default boolean textOutline() {
        return false;
    }

    // Training Stage Colors
    @ConfigSection(name = "Training Stage Colors", description = "Configure colors for each training stage", position = 2)
    String stageColorsSection = "stageColorsSection";

    @Alpha
    @ConfigItem(keyName = "blessWinesColor", name = "Bless Wines", description = "Color for the Bless Wines training stage", section = stageColorsSection, position = 0)
    default Color blessWinesColor() {
        return Color.ORANGE;
    }

    @Alpha
    @ConfigItem(keyName = "sacrificeShardsColor", name = "Sacrifice Shards", description = "Color for the Sacrifice Shards training stage", section = stageColorsSection, position = 1)
    default Color sacrificeShardsColor() {
        return Color.GREEN;
    }

    @Alpha
    @ConfigItem(keyName = "rechargePrayerColor", name = "Recharge Prayer", description = "Color for the Recharge Prayer training stage", section = stageColorsSection, position = 2)
    default Color rechargePrayerColor() {
        return Color.CYAN;
    }

    @Alpha
    @ConfigItem(keyName = "resupplyColor", name = "Resupply", description = "Color for the Resupply training stage", section = stageColorsSection, position = 3)
    default Color resupplyColor() {
        return Color.YELLOW;
    }

    // Infobox Settings
    @ConfigSection(name = "Infobox", description = "Configure training infobox", position = 3)
    String infoboxSection = "infoboxSection";

    @ConfigItem(keyName = "toggleInfobox", name = "Enable infobox at Ralos' Rise", description = "Toggle for \"Bone Shard Helper\" infobox (only shows while in Ralos' Rise map region)", section = infoboxSection, position = 0)
    default boolean toggleInfobox() {
        return true;
    }

    @ConfigItem(keyName = "infoboxTitle", name = "Title", description = "Show title for infobox", section = infoboxSection, position = 1)
    default boolean infoboxTitle() {
        return true;
    }

    @ConfigItem(keyName = "infoboxCurrentState", name = "Display training stage", description = "Show current training stage (bless wines, recharge prayer, sacrifice shards, or resupply)", section = "infoboxSection", position = 2)
    default boolean infoboxCurrentState() {
        return true;
    }

    @ConfigItem(keyName = "infoboxActionsLeft", name = "Remaining actions", description = "Show number of actions left in current training stage", section = infoboxSection, position = 3)
    default boolean infoboxActionsLeft() {
        return true;
    }

    @ConfigItem(keyName = "infoboxInventoriesLeft", name = "Remaining inventories", description = "Show number of inventories remaining until goal. Calculated from number of wine jugs held in inventory.", section = infoboxSection, position = 4)
    default boolean infoboxInventoriesLeft() {
        return true;
    }

    @ConfigItem(keyName = "infoboxRegularWineWarning", name = "Warn if using regular wines", description = "Show a warning if you bring regular jugs of wine to Ralos' Rise", section = infoboxSection, position = 5)
    default boolean infoboxRegularWineWarning() {
        return true;
    }

    // Debug Settings
    @ConfigSection(name = "Debug Settings", description = "Configure debug and development options", position = 4)
    String debugSection = "debugSection";

    @ConfigItem(keyName = "debugMode", name = "Debug Mode", description = "Enable debug mode to show additional debugging information and tables", section = debugSection, position = 0)
    default boolean debugMode() {
        return false;
    }
}