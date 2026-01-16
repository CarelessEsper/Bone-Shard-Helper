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
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.util.ColorUtil;
import net.runelite.api.Perspective;

class PrayerObjectOverlay extends Overlay {
    // Overlay for highlighting relevant objects in Ralos' rise (implemented similar to agility plugin)
    private static final int MAX_DISTANCE = 2350;
    private static final int EXPOSED_ALTAR_ID = 52799;
    private static final int LIBATION_BOWL_ID = 53018;
    private static final int RALOS_REGION_ID = 5681;

    private final Client client;
    private final BoneShardHelperConfig config;
    private final ModelOutlineRenderer modelOutlineRenderer;
    private BoneShardHelperPlugin plugin;
    private BoneShardTrainingState trainingState;

    // Path caching to improve performance
    private List<WorldPoint> cachedPath = null;
    private WorldPoint lastPlayerPosition = null;
    private WorldPoint lastAltarPosition = null;
    private BoneShardTrainingState.TrainingState lastTrainingState = null;

    @Inject
    private PrayerObjectOverlay(Client client, BoneShardHelperConfig config, BoneShardTrainingState trainingState, 
                                ModelOutlineRenderer modelOutlineRenderer) {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        this.client = client;
        this.config = config;
        this.trainingState = trainingState;
        this.modelOutlineRenderer = modelOutlineRenderer;
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
                    Color configColor = prayerObject.getHighlightColor(config, trainingState);

                    // Render based on selected highlight style
                    if (config.highlightStyle() == HighlightStyle.HIGHLIGHT_CLICKBOX) {
                        // Clickbox style: draw outline and fill
                        Shape objectClickbox = object.getClickbox();
                        if (objectClickbox != null) {
                            Color renderColor = configColor;
                            if (objectClickbox.contains(mousePosition.getX(), mousePosition.getY())) {
                                renderColor = configColor.darker();
                            }

                            graphics.setColor(renderColor);
                            graphics.draw(objectClickbox);

                            graphics.setColor(ColorUtil.colorWithAlpha(configColor, configColor.getAlpha() / 5));
                            graphics.fill(objectClickbox);
                        }
                    } else if (config.highlightStyle() == HighlightStyle.HIGHLIGHT_OUTLINE) {
                        modelOutlineRenderer.drawOutline(object, 2, configColor, 4);
                    }

                    // Draw text overlay for libation bowl
                    if ((config.toggleOverlayActionsLeft() || config.toggleOverlayStageName()) 
                            && prayerObject.getObjectId() == LIBATION_BOWL_ID) {
                        drawLibationBowlText(graphics, object, prayerObject);
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

    private void drawLibationBowlText(Graphics2D graphics, TileObject object, PrayerObject prayerObject) {
        BoneShardTrainingState.TrainingState currentState = trainingState.getCurrentTrainingState();
        String text = null;
        Color textColor;

        // Determine text and color based on training state
        switch (currentState) {
            case BLESS_WINES:
                if (config.toggleOverlayStageName()) {
                    text = currentState.getDisplayName();
                    textColor = config.blessWinesColor();
                } else {
                    return;
                }
                break;
            case RECHARGE_PRAYER:
                if (config.toggleOverlayStageName()) {
                    text = currentState.getDisplayName();
                    textColor = config.rechargePrayerColor();
                } else {
                    return;
                }
                break;
            case SACRIFICE_SHARDS:
                if (config.toggleOverlayActionsLeft()) {
                    int actionsRemaining = trainingState.getActionsRemaining();
                    text = "Actions: " + actionsRemaining;
                    textColor = config.sacrificeShardsColor();
                } else if (config.toggleOverlayStageName()) {
                    text = currentState.getDisplayName();
                    textColor = config.sacrificeShardsColor();
                } else {
                    return;
                }
                break;
            case RESUPPLY:
                if (config.toggleOverlayStageName()) {
                    text = currentState.getDisplayName();
                    textColor = config.resupplyColor();
                } else {
                    return;
                }
                break;
            default:
                if (config.toggleOverlayStageName()) {
                    text = currentState.getDisplayName();
                    textColor = Color.WHITE;
                } else {
                    return;
                }
                break;
        }

        // Get the canvas text location for the object with vertical offset
        int verticalOffset = 100;
        Point pos = Perspective.getCanvasTextLocation(client, graphics, object.getLocalLocation(), text, verticalOffset);
        if (pos == null) {
            return;
        }

        // Render text with custom outline/shadow
        if (config.textOutline()) {
            // Draw outline in 4 directions
            graphics.setColor(Color.BLACK);
            graphics.drawString(text, pos.getX(), pos.getY() + 1);
            graphics.drawString(text, pos.getX(), pos.getY() - 1);
            graphics.drawString(text, pos.getX() + 1, pos.getY());
            graphics.drawString(text, pos.getX() - 1, pos.getY());
        }
        
        // Render main text (with shadow if outline is disabled)
        OverlayUtil.renderTextLocation(graphics, pos, text, textColor);
    }
}