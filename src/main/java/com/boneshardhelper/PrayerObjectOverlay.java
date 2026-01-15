package com.boneshardhelper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ColorUtil;
import net.runelite.api.Perspective;

class PrayerObjectOverlay extends Overlay {
    // Overlay for highlighting relevant objects in Ralos' rise (implemented similar to agility plugin)
    private static final int MAX_DISTANCE = 2350;
    private static final int EXPOSED_ALTAR_ID = 52799;
    private static final int RALOS_REGION_ID = 5681;

    private final Client client;
    private final BoneShardHelperConfig config;
    private BoneShardHelperPlugin plugin;
    private BoneShardTrainingState trainingState;

    // Path caching to improve performance
    private List<WorldPoint> cachedPath = null;
    private WorldPoint lastPlayerPosition = null;
    private WorldPoint lastAltarPosition = null;
    private BoneShardTrainingState.TrainingState lastTrainingState = null;

    @Inject
    private PrayerObjectOverlay(Client client, BoneShardHelperConfig config, BoneShardTrainingState trainingState) {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        this.client = client;
        this.config = config;
        this.trainingState = trainingState;
    }

    void setPlugin(BoneShardHelperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        LocalPoint playerLocation = client.getLocalPlayer().getLocalLocation();
        Point mousePosition = client.getMouseCanvasPosition();

        // Render object highlighting if enabled and plugin is set
        if (plugin != null && config.highlightPrayerObjects()) {
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
                        // Get the configured color for this prayer object type based on training state
                        Color configColor = prayerObject.getHighlightColor(config, trainingState);

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
        }

        // Draw path to exposed altar when in BLESS_WINES state and enabled in config
        if (plugin != null && config.exposedAltarPath() && trainingState.inTrainingRegion()) {
            BoneShardTrainingState.TrainingState currentState = trainingState.getCurrentTrainingState();

            // Only show path when in BLESS_WINES state
            if (currentState == BoneShardTrainingState.TrainingState.BLESS_WINES) {
                drawPathToExposedAltar(graphics);
            } else {
                // Clear cached path when not in BLESS_WINES state
                clearPathCache();
            }
        } else {
            clearPathCache();
        }

        return null;
    }

    private void drawPathToExposedAltar(Graphics2D graphics) {
        // Find the exposed altar
        TileObject exposedAltar = findExposedAltar();
        if (exposedAltar == null) {
            cachedPath = null;
            return;
        }

        WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
        WorldPoint altarPos = exposedAltar.getWorldLocation();
        BoneShardTrainingState.TrainingState currentState = trainingState.getCurrentTrainingState();

        if (playerPos == null || altarPos == null) {
            cachedPath = null;
            return;
        }

        // Get the closest accessible tile near the altar
        WorldPoint targetPos = getClosestAltarAccessTile(playerPos, altarPos);
        if (targetPos == null) {
            cachedPath = null;
            return;
        }

        // Check if we need to recalculate the path
        boolean needsRecalculation = cachedPath == null ||
                !playerPos.equals(lastPlayerPosition) ||
                !targetPos.equals(lastAltarPosition) ||
                currentState != lastTrainingState;

        if (needsRecalculation) {
            // Find path using pathfinding
            cachedPath = findPath(playerPos, targetPos);
            lastPlayerPosition = playerPos;
            lastAltarPosition = targetPos;
            lastTrainingState = currentState;
        }

        if (cachedPath != null && !cachedPath.isEmpty()) {
            // Draw the cached path
            drawPath(graphics, cachedPath);
        }
    }

    private WorldPoint getClosestAltarAccessTile(WorldPoint playerPos, WorldPoint altarPos) {
        // Define the accessible tiles around the exposed altar in region 5681
        // These are the tiles where players can stand to interact with the altar
        WorldPoint[] accessTiles = {
                convertRegionToWorld(28, 12, altarPos.getPlane()),
                convertRegionToWorld(27, 12, altarPos.getPlane()),
                convertRegionToWorld(31, 9, altarPos.getPlane()),
                convertRegionToWorld(31, 8, altarPos.getPlane())
        };

        WorldPoint closest = null;
        double minDistance = Double.MAX_VALUE;

        for (WorldPoint tile : accessTiles) {
            if (tile != null) {
                double distance = Math.sqrt(
                        Math.pow(playerPos.getX() - tile.getX(), 2) +
                                Math.pow(playerPos.getY() - tile.getY(), 2));

                if (distance < minDistance) {
                    minDistance = distance;
                    closest = tile;
                }
            }
        }

        return closest;
    }

    private WorldPoint convertRegionToWorld(int regionX, int regionY, int plane) {
        // Convert region coordinates to world coordinates
        // Region 5681 base coordinates need to be calculated
        int baseX = (RALOS_REGION_ID >> 8) * 64;
        int baseY = (RALOS_REGION_ID & 0xFF) * 64;

        return new WorldPoint(baseX + regionX, baseY + regionY, plane);
    }

    private TileObject findExposedAltar() {
        for (Map.Entry<TileObject, PrayerObject> entry : plugin.getPrayerObjects().entrySet()) {
            PrayerObject prayerObject = entry.getValue();
            if (prayerObject != null && prayerObject.getObjectId() == EXPOSED_ALTAR_ID) {
                return entry.getKey();
            }
        }
        return null;
    }

    private List<WorldPoint> findPath(WorldPoint start, WorldPoint end) {
        // Simple two-point path for drawing a straight line
        List<WorldPoint> path = new ArrayList<>();
        path.add(start);
        path.add(end);
        return path;
    }

    private void drawPath(Graphics2D graphics, List<WorldPoint> path) {
        if (path.size() < 2) {
            return;
        }

        // Draw a simple geometric line from start to end
        WorldPoint start = path.get(0);
        WorldPoint end = path.get(path.size() - 1);

        LocalPoint startLocal = LocalPoint.fromWorld(client, start);
        LocalPoint endLocal = LocalPoint.fromWorld(client, end);

        if (startLocal != null && endLocal != null) {
            Point startPoint = Perspective.localToCanvas(client, startLocal, start.getPlane());
            Point endPoint = Perspective.localToCanvas(client, endLocal, end.getPlane());

            if (startPoint != null && endPoint != null) {
                // Set up line drawing style
                Color pathColor = config.exposedAltarPathColor();
                graphics.setColor(ColorUtil.colorWithAlpha(pathColor, 200));
                graphics.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Draw the straight line
                graphics.drawLine(startPoint.getX(), startPoint.getY(),
                        endPoint.getX(), endPoint.getY());

                // Draw a small circle at the destination
                graphics.setColor(ColorUtil.colorWithAlpha(pathColor, 255));
                graphics.fillOval(endPoint.getX() - 4, endPoint.getY() - 4, 8, 8);
            }
        }
    }

    private void clearPathCache() {
        cachedPath = null;
        lastPlayerPosition = null;
        lastAltarPosition = null;
        lastTrainingState = null;
    }
}