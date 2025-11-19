package com.boneshardhelper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ColorUtil;

class PrayerObjectOverlay extends Overlay {
    // Overlay for highlighting relevant objects in Ralos' rise (implemented similar to agility plugin)
    private static final int MAX_DISTANCE = 2350;

    private final Client client;
    private final BoneShardHelperConfig config;
    private BoneShardHelperPlugin plugin;

    @Inject
    private PrayerObjectOverlay(Client client, BoneShardHelperConfig config) {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        this.client = client;
        this.config = config;
    }

    void setPlugin(BoneShardHelperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        // Only render if object highlighting is enabled and plugin is set
        if (plugin == null || !config.highlightPrayerObjects()) {
            return null;
        }

        LocalPoint playerLocation = client.getLocalPlayer().getLocalLocation();
        Point mousePosition = client.getMouseCanvasPosition();

        // Render each object that should be highlighted
        plugin.getPrayerObjects().forEach((tileObject, prayerObject) -> {
            if (prayerObject == null || !prayerObject.shouldHighlight()) {
                return;
            }

            Tile tile = prayerObject.getTile();
            TileObject object = prayerObject.getTileObject();

            // Only highlight objects on the same plane and within distance
            if (tile.getPlane() == client.getLocalPlayer().getWorldLocation().getPlane()
                    && object.getLocalLocation().distanceTo(playerLocation) < MAX_DISTANCE) {
                Shape objectClickbox = object.getClickbox();
                if (objectClickbox != null) {
                    // Get the configured color for this prayer object type
                    Color configColor = prayerObject.getHighlightColor(config);

                    // Darker color on mouse hover to indicate interaction
                    if (objectClickbox.contains(mousePosition.getX(), mousePosition.getY())) {
                        graphics.setColor(configColor.darker());
                    } else {
                        graphics.setColor(configColor);
                    }

                    // Draw the outline
                    graphics.draw(objectClickbox);

                    // Fill with transparent color (alpha / 5 for subtle fill)
                    graphics.setColor(ColorUtil.colorWithAlpha(configColor, configColor.getAlpha() / 5));
                    graphics.fill(objectClickbox);
                }
            }
        });

        return null;
    }
}