package com.boneshardhelper;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(name = "Bone Shard Helper", description = "A helper plugin for Prayer training using bone shards in Varlamore.", tags = {
		"prayer", "varlamore", "calculator", "planning", "bone", "xp", "training", "wine", "shard", "blessed",
		"skilling","teomat","ralos" })
public class BoneShardHelperPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private Provider<BoneShardHelperPanel> uiPanel;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PrayerObjectOverlay prayerObjectOverlay;

	@Getter
	private final Map<TileObject, PrayerObject> prayerObjects = new HashMap<>();

	private NavigationButton uiNavigationButton;

	@Provides
	BoneShardHelperConfig getConfig(ConfigManager configManager) {
		return configManager.getConfig(BoneShardHelperConfig.class);
	}

	@Override
	protected void startUp() throws Exception {
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/bone_shard_helper_icon.png");

		uiNavigationButton = NavigationButton.builder()
				.tooltip("Bone Shard Helper")
				.icon(icon)
				.priority(6)
				.panel(uiPanel.get())
				.build();

		clientToolbar.addNavigation(uiNavigationButton);

		// Set plugin reference in overlay to avoid circular dependency
		prayerObjectOverlay.setPlugin(this);
		overlayManager.add(prayerObjectOverlay);
	}

	@Override
	protected void shutDown() throws Exception {
		clientToolbar.removeNavigation(uiNavigationButton);
		overlayManager.remove(prayerObjectOverlay);
		prayerObjects.clear();
	}

	@Subscribe
	public void onConfigChanged(net.runelite.client.events.ConfigChanged event) {
		if ("boneshardhelper".equals(event.getGroup())) {
			if ("debugMode".equals(event.getKey())) {
				boolean debugMode = Boolean.parseBoolean(event.getNewValue());
				uiPanel.get().updateDebugMode(debugMode);
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		switch (event.getGameState()) {
			case HOPPING:
			case LOGIN_SCREEN:
			case LOADING:
				prayerObjects.clear();
				break;
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event) {
		onTileObject(event.getTile(), null, event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event) {
		onTileObject(event.getTile(), event.getGameObject(), null);
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event) {
		onTileObject(event.getTile(), null, event.getGroundObject());
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event) {
		onTileObject(event.getTile(), event.getGroundObject(), null);
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event) {
		onTileObject(event.getTile(), null, event.getWallObject());
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned event) {
		onTileObject(event.getTile(), event.getWallObject(), null);
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event) {
		onTileObject(event.getTile(), null, event.getDecorativeObject());
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event) {
		onTileObject(event.getTile(), event.getDecorativeObject(), null);
	}

	private void onTileObject(Tile tile, TileObject oldObject, TileObject newObject) {
		// Remove old object if it exists
		prayerObjects.remove(oldObject);

		if (newObject == null) {
			return;
		}

		// Check if this is a prayer object we should track
		if (PrayerObject.PRAYER_OBJECT_IDS.contains(newObject.getId())) {
			PrayerObject prayerObject = PrayerObject.fromTileObject(tile, newObject);
			if (prayerObject != null) {
				prayerObjects.put(newObject, prayerObject);
			}
		}
	}
}