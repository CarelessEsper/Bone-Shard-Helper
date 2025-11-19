package com.boneshardhelper;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("boneshardhelper")
public interface BoneShardHelperConfig extends Config {
    @ConfigSection(name = "Object Highlighting", description = "Configure prayer object highlighting", position = 0)
    String highlightingSection = "highlighting";

    @ConfigSection(name = "Debug Settings", description = "Configure debug and development options", position = 1)
    String debugSection = "debug";

    // Object Highlighting Settings
    @ConfigItem(keyName = "highlightPrayerObjects", name = "Highlight Prayer Objects", description = "Enable highlighting of prayer objects with colored outlines", section = highlightingSection, position = 0)
    default boolean highlightPrayerObjects() {
        return true;
    }

    @Alpha
    @ConfigItem(keyName = "exposedAltarColor", name = "Exposed Altar Color", description = "Color for highlighting Exposed Altar", section = highlightingSection, position = 3)
    default Color getExposedAltarColor() {
        return Color.GREEN;
    }

    @Alpha
    @ConfigItem(keyName = "shrineOfRalosColor", name = "Shrine of Ralos Color", description = "Color for highlighting Shrine of Ralos", section = highlightingSection, position = 2)
    default Color getShrineOfRalosColor() {
        return Color.GREEN;
    }

    @Alpha
    @ConfigItem(keyName = "libationBowlColor", name = "Libation Bowl Color", description = "Color for highlighting Libation Bowl", section = highlightingSection, position = 1)
    default Color getLibationBowlColor() {
        return Color.GREEN;
    }

    // Debug Settings
    @ConfigItem(keyName = "debugMode", name = "Debug Mode", description = "Enable debug mode to show additional debugging information and tables", section = debugSection, position = 0)
    default boolean debugMode() {
        return false;
    }
}