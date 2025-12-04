package com.boneshardhelper;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

@Getter
@Singleton
@Slf4j
class BoneShardHelperPanel extends PluginPanel {
	private static final Pattern NON_NUMERIC = Pattern.compile("\\D");

	// Panel that holds the content of the currently selected tab
	private final JPanel display = new JPanel();

	// Tab group and individual tabs
	private final MaterialTabGroup tabGroup = new MaterialTabGroup(display);
	private final MaterialTab goalModeTab;
	private final MaterialTab resourceModeTab;

	// Individual tab panels
	private final GoalModePanel goalModePanel;
	private final ResourceModePanel resourceModePanel;

	// Refresh button panel
	private final JPanel refreshPanel = new JPanel();
	private final JButton refreshButton = new JButton("Refresh Current Stats");

	// Client for getting current player stats
	private final Client client;

	// Resource scanner for inventory scanning
	private final BoneResourceScanner resourceScanner;

	// Config for plugin settings
	private final BoneShardHelperConfig config;

	// Session tracking for first-time initialization
	private boolean hasBeenInitializedThisSession = false;

	// Sample text label that changes based on active tab
	private JLabel sampleTextLabel;

	@Inject
	BoneShardHelperPanel(Client client, BoneResourceScanner resourceScanner, BoneShardHelperConfig config,
			ItemManager itemManager, net.runelite.client.hiscore.HiscoreClient hiscoreClient) {
		super();
		this.client = client;
		this.resourceScanner = resourceScanner;
		this.config = config;

		// Initialize tab panels (each with their own UI components)
		goalModePanel = new GoalModePanel();
		resourceModePanel = new ResourceModePanel(config);

		// Set up synchronized event handling for checkboxes and scan buttons
		setupSynchronizedEventHandlers();

		// Inject dependencies into Goal Mode panel
		goalModePanel.setResourceScanner(resourceScanner);
		goalModePanel.setItemManager(itemManager);
		goalModePanel.setHiscoreClient(hiscoreClient);

		// Set up debug mode in Goal Mode panel
		goalModePanel.setDebugMode(config.debugMode(), this);

		// Set dependencies for Resource Mode panel
		resourceModePanel.setParentPanel(this);
		resourceModePanel.setItemManager(itemManager);

		// Set up automatic calculation listeners
		setupCalculationListeners();

		// Set up resource scanning functionality
		setupResourceScanning();

		// Set up main panel layout
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Create tabs
		goalModeTab = new MaterialTab("Goal Mode", tabGroup, goalModePanel);
		resourceModeTab = new MaterialTab("Resource Mode", tabGroup, resourceModePanel);

		// Configure tab group
		tabGroup.setBorder(new EmptyBorder(5, 0, 0, 0));
		tabGroup.addTab(goalModeTab);
		tabGroup.addTab(resourceModeTab);
		tabGroup.select(goalModeTab); // Default to Goal Mode

		// Add tab switching logic to preserve shared state
		goalModeTab.setOnSelectEvent(this::onGoalModeSelected);
		resourceModeTab.setOnSelectEvent(this::onResourceModeSelected);

		// Create sample text panel that changes based on active tab
		JPanel sampleTextPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		sampleTextPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JLabel sampleTextLabel = new JLabel("Calculate what you need to reach your goal");
		sampleTextLabel.setForeground(java.awt.Color.LIGHT_GRAY);
		sampleTextLabel.setFont(FontManager.getRunescapeSmallFont());
		sampleTextPanel.add(sampleTextLabel);

		// Set up refresh button panel
		refreshPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		refreshPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		refreshButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		refreshButton.setForeground(java.awt.Color.WHITE);
		refreshButton.setFont(FontManager.getRunescapeSmallFont());
		refreshButton.setBorder(new EmptyBorder(5, 10, 5, 10));
		refreshButton.addActionListener(e -> refreshCurrentStats());
		refreshPanel.add(refreshButton);

		// Create a container for tabs, sample text, and refresh button
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		topPanel.add(tabGroup, BorderLayout.NORTH);

		// Create middle panel for sample text and refresh button
		JPanel middlePanel = new JPanel(new BorderLayout());
		middlePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		middlePanel.add(sampleTextPanel, BorderLayout.NORTH);
		middlePanel.add(refreshPanel, BorderLayout.SOUTH);

		topPanel.add(middlePanel, BorderLayout.SOUTH);

		// Store reference to sample text label for updating when tabs change
		this.sampleTextLabel = sampleTextLabel;

		// Add components to main panel
		add(topPanel, BorderLayout.NORTH);
		add(display, BorderLayout.CENTER);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		// Initialize fields with current player stats the first time the panel is
		// opened this session
		if (visible && !hasBeenInitializedThisSession) {
			hasBeenInitializedThisSession = true;
			try {
				// Refresh current stats to populate fields with player's current prayer data
				refreshCurrentStats();
			} catch (Exception e) {
				log.error("Error initializing prayer calculator on first panel open", e);
			}
		}
	}

	// Tab switching event handlers that preserve shared state
	private boolean onGoalModeSelected() {
		// Update sample text for Goal Mode
		if (sampleTextLabel != null) {
			sampleTextLabel.setText("Calculate what you need to reach your goal");
		}

		// Sync shared state from Resource Mode to Goal Mode
		syncSharedState(resourceModePanel, goalModePanel);

		// Update zealot robes warning to reflect current checkbox state
		goalModePanel.updateZealotRobesWarningDisplay();

		// Update calculations to reflect any changes made in Resource Mode
		// Call directly since tab selection might not be complete yet
		try {
			goalModePanel.updateBoneShardsRequired();
		} catch (Exception e) {
			handleCalculationError("Error updating calculations on mode switch", e);
		}

		// Trigger resource planning recalculation (same as XP field listener)
		triggerResourcePlanningRecalculation();

		return true;
	}

	private boolean onResourceModeSelected() {
		// Update sample text for Resource Mode
		if (sampleTextLabel != null) {
			sampleTextLabel.setText("Calculate the shard value of your inventory");
		}

		// Sync shared state from Goal Mode to Resource Mode
		syncSharedState(goalModePanel, resourceModePanel);

		// Update zealot robes warning to reflect current checkbox state
		resourceModePanel.updateZealotRobesWarningDisplay();

		// Update Resource Mode calculations if we have scanned data
		// This ensures checkbox changes made in Goal Mode are reflected in Resource
		// Mode calculations
		if (resourceModePanel.getResourceBreakdownTable().getRowCount() > 0) {
			// Re-scan to update achievable level with current checkbox settings
			performResourceScan();
		}

		// Trigger resource planning recalculation (same as XP field listener)
		triggerResourcePlanningRecalculation();

		return true;
	}

	// Synchronize shared state between panels
	private void syncSharedState(GoalModePanel source, ResourceModePanel target) {
		target.setCurrentLevelInput(source.getCurrentLevelInput());
		// Apply formatting when syncing XP values (setCurrentXPInput updates both
		// panels)
		int currentXP = source.getCurrentXPInput();
		setCurrentXPInput(String.format("%,d", currentXP));
		target.setSunfireWineSelected(source.isSunfireWineSelected());
		target.setZealotRobesSelected(source.isZealotRobesSelected());
	}

	private void syncSharedState(ResourceModePanel source, GoalModePanel target) {
		target.setCurrentLevelInput(source.getCurrentLevelInput());
		// Apply formatting when syncing XP values (setCurrentXPInput updates both
		// panels)
		int currentXP = source.getCurrentXPInput();
		setCurrentXPInput(String.format("%,d", currentXP));
		target.setSunfireWineSelected(source.isSunfireWineSelected());
		target.setZealotRobesSelected(source.isZealotRobesSelected());
	}

	// Accessor methods for current level and XP (shared between both modes)
	int getCurrentLevelInput() {
		// Get from currently active tab
		if (goalModeTab.isSelected()) {
			return getNumericInput(goalModePanel.getUiFieldCurrentLevel().getText());
		} else {
			return getNumericInput(resourceModePanel.getUiFieldCurrentLevel().getText());
		}
	}

	void setCurrentLevelInput(int value) {
		// Set on both panels to keep them in sync
		goalModePanel.setCurrentLevelInput(value);
		resourceModePanel.setCurrentLevelInput(value);
	}

	int getCurrentXPInput() {
		// Get from currently active tab
		if (goalModeTab.isSelected()) {
			return getNumericInput(goalModePanel.getUiFieldCurrentXP().getText());
		} else {
			return getNumericInput(resourceModePanel.getUiFieldCurrentXP().getText());
		}
	}

	void setCurrentXPInput(Object value) {
		// Set on both panels to keep them in sync
		goalModePanel.setCurrentXPInput(value);
		resourceModePanel.setCurrentXPInput(value);
	}

	// Goal Mode specific methods
	int getTargetLevelInput() {
		return getNumericInput(goalModePanel.getUiFieldTargetLevel().getText());
	}

	void setTargetLevelInput(Object value) {
		goalModePanel.setTargetLevelInput(value);
	}

	int getTargetXPInput() {
		return getNumericInput(goalModePanel.getUiFieldTargetXP().getText());
	}

	void setTargetXPInput(Object value) {
		goalModePanel.setTargetXPInput(value);
	}

	// Checkbox state methods (shared between both modes)
	boolean isSunfireWineSelected() {
		// Get from currently active tab
		if (goalModeTab.isSelected()) {
			return goalModePanel.isSunfireWineSelected();
		} else {
			return resourceModePanel.isSunfireWineSelected();
		}
	}

	void setSunfireWineSelected(boolean selected) {
		// Set on both panels to keep them in sync
		goalModePanel.setSunfireWineSelected(selected);
		resourceModePanel.setSunfireWineSelected(selected);
	}

	boolean isZealotRobesSelected() {
		// Get from currently active tab
		if (goalModeTab.isSelected()) {
			return goalModePanel.isZealotRobesSelected();
		} else {
			return resourceModePanel.isZealotRobesSelected();
		}
	}

	void setZealotRobesSelected(boolean selected) {
		// Set on both panels to keep them in sync
		goalModePanel.setZealotRobesSelected(selected);
		resourceModePanel.setZealotRobesSelected(selected);
	}

	// Method to show Goal Mode tab
	void showGoalMode() {
		if (goalModePanel.isShowing()) {
			return;
		}
		tabGroup.select(goalModeTab);
		revalidate();
	}

	// Method to show Resource Mode tab
	void showResourceMode() {
		if (resourceModePanel.isShowing()) {
			return;
		}
		tabGroup.select(resourceModeTab);
		revalidate();
	}

	// Method to refresh current player stats
	public void refreshCurrentStats() {
		refreshCurrentStats(false);
	}

	public void refreshCurrentStatsFromPlugin() {
		refreshCurrentStats(true);
	}

	private void refreshCurrentStats(boolean fromPlugin) {
		Integer currentXP = null;
		Integer currentLevel = null;
		boolean dataAvailable = false;

		if (client != null) {
			try {
				// Test that reading of current prayer XP from game client works correctly
				currentXP = client.getSkillExperience(Skill.PRAYER);
				currentLevel = Experience.getLevelForXp(currentXP);
				dataAvailable = true;

				// Validate that XP and level are consistent
				int expectedLevel = Experience.getLevelForXp(currentXP);
				if (currentLevel != expectedLevel) {
					log.warn("Prayer XP/Level inconsistency detected. XP: {}, Level: {}, Expected: {}",
							currentXP, currentLevel, expectedLevel);
				}

				// Validate XP bounds
				if (currentXP < 0 || currentXP > Experience.MAX_SKILL_XP) {
					log.warn("Prayer XP out of bounds: {}", currentXP);
					dataAvailable = false;
					currentXP = null;
					currentLevel = null;
				}

				// Validate level bounds
				if (currentLevel != null && (currentLevel < 1 || currentLevel > Experience.MAX_VIRT_LEVEL)) {
					log.warn("Prayer level out of bounds: {}", currentLevel);
					dataAvailable = false;
					currentXP = null;
					currentLevel = null;
				}

				if (fromPlugin && dataAvailable) {
					log.debug("Prayer XP detection working: Level {} ({} XP)", currentLevel,
							String.format("%,d", currentXP));
				}
			} catch (Exception e) {
				log.error("Error retrieving prayer data from client", e);
				dataAvailable = false;
				currentXP = null;
				currentLevel = null;
			}
		}

		// Update fields only when manually refreshing and data is available
		if (!fromPlugin) {
			if (dataAvailable && currentXP != null && currentLevel != null) {
				try {
					// Clear any previous error messages when data becomes available
					clearValidationError();

					// Calculate target: next level for most players, or 200M XP if level 99+ Prayer
					int targetLevel;
					int targetXP;

					if (currentLevel >= 99) {
						// For level 99+ players, default to 200M XP goal
						targetXP = Experience.MAX_SKILL_XP;
						targetLevel = Experience.getLevelForXp(targetXP);
					} else {
						// For lower level players, target next level
						targetLevel = Math.min(currentLevel + 1, Experience.MAX_VIRT_LEVEL);
						targetXP = Experience.getXpForLevel(targetLevel);
					}

					// Manual refresh - update all fields with actual data
					setCurrentLevelInput(currentLevel);
					setCurrentXPInput(String.format("%,d", currentXP));
					setTargetLevelInput(targetLevel);
					setTargetXPInput(String.format("%,d", targetXP));

					// Update Goal Mode calculations after refresh
					updateGoalModeCalculations();
				} catch (Exception e) {
					handleCalculationError("Error updating fields after refresh", e);
				}
			} else {
				// No data available - show error but don't populate fields with defaults
				showValidationError(
						"Game data unavailable. Please log in and try again, or manually enter your current stats.");
			}
		} else if (fromPlugin && !dataAvailable) {
			log.debug("Prayer data unavailable during validation");
		}
	}

	// Set up automatic calculation listeners for level/XP fields
	private void setupCalculationListeners() {
		// Goal Mode listeners
		goalModePanel.getUiFieldCurrentLevel().addActionListener(e -> onCurrentLevelUpdated());
		goalModePanel.getUiFieldCurrentLevel().addFocusListener(buildFocusAdapter(e -> onCurrentLevelUpdated()));

		goalModePanel.getUiFieldCurrentXP().addActionListener(e -> onCurrentXPUpdated());
		goalModePanel.getUiFieldCurrentXP().addFocusListener(buildFocusAdapter(e -> onCurrentXPUpdated()));

		goalModePanel.getUiFieldTargetLevel().addActionListener(e -> onTargetLevelUpdated());
		goalModePanel.getUiFieldTargetLevel().addFocusListener(buildFocusAdapter(e -> onTargetLevelUpdated()));

		goalModePanel.getUiFieldTargetXP().addActionListener(e -> onTargetXPUpdated());
		goalModePanel.getUiFieldTargetXP().addFocusListener(buildFocusAdapter(e -> onTargetXPUpdated()));

		// Resource Mode listeners (only for current level/XP since it doesn't have
		// target fields)
		resourceModePanel.getUiFieldCurrentLevel().addActionListener(e -> onCurrentLevelUpdated());
		resourceModePanel.getUiFieldCurrentLevel().addFocusListener(buildFocusAdapter(e -> onCurrentLevelUpdated()));

		resourceModePanel.getUiFieldCurrentXP().addActionListener(e -> onCurrentXPUpdated());
		resourceModePanel.getUiFieldCurrentXP().addFocusListener(buildFocusAdapter(e -> onCurrentXPUpdated()));
	}

	// Event handlers for automatic calculations with validation
	private void onCurrentLevelUpdated() {
		try {
			clearValidationError();
			int currentLevel = enforceSkillBounds(getCurrentLevelInput());
			int currentXP = Experience.getXpForLevel(currentLevel);
			setCurrentXPInput(String.format("%,d", currentXP));

			// Update Goal Mode calculations
			updateGoalModeCalculations();
		} catch (Exception e) {
			handleCalculationError("Error updating current level", e);
		}
	}

	private void onCurrentXPUpdated() {
		try {
			clearValidationError();
			int currentXP = enforceXPBounds(getCurrentXPInput());
			int currentLevel = Experience.getLevelForXp(currentXP);
			setCurrentLevelInput(currentLevel);

			// Format the XP value with commas
			setCurrentXPInput(String.format("%,d", currentXP));

			// Update Goal Mode calculations
			updateGoalModeCalculations();
		} catch (Exception e) {
			handleCalculationError("Error updating current XP", e);
		}
	}

	private void onTargetLevelUpdated() {
		try {
			clearValidationError();
			int targetLevel = getTargetLevelInput();

			// This implementation sucks but my brain is fried. Maybe fixing QOL in later
			// update.
			int targetXP = getTargetXPInput();
			if (targetLevel > Experience.MAX_VIRT_LEVEL && targetXP < Experience.MAX_SKILL_XP) {
				showValidationError("Target level cannot exceed " + Experience.MAX_VIRT_LEVEL
						+ ". Use 'Target Experience' input box if pursuing 200M XP.");
				return;
			}

			// Calculate target XP, but cap at 200M if level would exceed that
			int maxPossibleXP = Experience.MAX_SKILL_XP;
			int calculatedTargetXP = Math.min(Experience.getXpForLevel(targetLevel), maxPossibleXP);
			setTargetXPInput(String.format("%,d", calculatedTargetXP));

			// Update Goal Mode calculations
			updateGoalModeCalculations();
		} catch (Exception e) {
			handleCalculationError("Error updating target level", e);
		}
	}

	private void onTargetXPUpdated() {
		try {
			clearValidationError();
			int targetXP = enforceXPBounds(getTargetXPInput());
			int targetLevel = Experience.getLevelForXp(targetXP);
			setTargetLevelInput(targetLevel);

			// Format the XP value with commas
			setTargetXPInput(String.format("%,d", targetXP));

			// Update Goal Mode calculations
			updateGoalModeCalculations();
		} catch (Exception e) {
			handleCalculationError("Error updating target XP", e);
		}
	}

	private void updateGoalModeCalculations() {
		// Update Goal Mode calculation results when inputs change.
		if (goalModeTab.isSelected()) {
			try {
				goalModePanel.updateBoneShardsRequired();
			} catch (Exception e) {
				handleCalculationError("Error updating calculations", e);
			}
		}
	}

	public void performResourceModeResourceScan() {
		if (resourceScanner == null) {
			resourceModePanel.updateDebugError("Resource scanner not available");
			return;
		}

		try {
			resourceModePanel.updateDebugStatus("Scanning inventory...");

			// Use ClientThread to safely access game data
			resourceScanner.getClientThread().invoke(() -> {
				try {
					// Scan inventory for bones
					Map<BoneType, Integer> inventoryBones = resourceScanner.scanInventory();

					// Calculate current total shards from inventory
					int currentShards = resourceScanner.calculateTotalShards(inventoryBones);

					// Update UI on EDT
					javax.swing.SwingUtilities.invokeLater(() -> {
						try {
							// Update resource breakdown table
							resourceModePanel.updateResourceBreakdown(inventoryBones);

							// Update achievable level calculation (use debug override if available)
							int currentXP = resourceModePanel.getCurrentXPInput();
							boolean useSunfireWine = resourceModePanel.isSunfireWineSelected();
							int effectiveShards = resourceModePanel.getEffectiveTotalShards(currentShards);
							resourceModePanel.updateAchievableLevel(effectiveShards, currentXP, useSunfireWine);

							// Show success message
							resourceModePanel.updateDebugSuccess("Resource display updated");
						} catch (Exception e) {
							resourceModePanel.updateDebugError("Error updating display: " + e.getMessage());
						}
					});
				} catch (Exception e) {
					javax.swing.SwingUtilities.invokeLater(() -> {
						resourceModePanel.updateDebugError("Error scanning inventory: " + e.getMessage());
					});
				}
			});
		} catch (Exception e) {
			resourceModePanel.updateDebugError("Error: " + e.getMessage());
		}
	}

	private void handleCalculationError(String context, Exception e) {
		log.error("Prayer Calculator: {}", context, e);
		showValidationError("Calculation error: Please check your input values");
	}

	// Helper methods for input validation with error handling
	private static int enforceSkillBounds(int input) {
		return Math.min(Experience.MAX_VIRT_LEVEL, Math.max(1, input));
	}

	private static int enforceXPBounds(int input) {
		return Math.min(Experience.MAX_SKILL_XP, Math.max(0, input));
	}

	private void showValidationError(String message) {
		if (goalModeTab.isSelected()) {
			goalModePanel.showValidationError(message);
		}
	}

	private void clearValidationError() {
		goalModePanel.clearValidationError();
	}

	// Helper method to parse numeric input from text fields
	private static int getNumericInput(String text) {
		try {
			return Integer.parseInt(NON_NUMERIC.matcher(text).replaceAll(""));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	// Helper method to build focus adapters
	private static FocusAdapter buildFocusAdapter(Consumer<FocusEvent> focusLostConsumer) {
		return new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				focusLostConsumer.accept(e);
			}
		};
	}

	// Set up resource scanning functionality
	private void setupResourceScanning() {
		try {
			// Add action listener to the scan resources button
			resourceModePanel.updateDebugStatus("Setting up button listener...");
			resourceModePanel.getUiButtonScanResources().addActionListener(e -> {
				resourceModePanel.updateDebugStatus("Main button clicked!");
				performResourceScan();
			});

			// Note: Synchronized event handlers are set up in
			// setupSynchronizedEventHandlers()

			resourceModePanel.updateDebugSuccess("Setup complete");
		} catch (Exception ex) {
			resourceModePanel.updateDebugError("Setup failed: " + ex.getMessage());
		}
	}

	private void performResourceScan() {
		// Perform resource scanning and update the breakdown table
		resourceModePanel.updateDebugStatus("Button clicked - starting inventory scan...");

		if (client == null) {
			resourceModePanel.updateDebugError("Game client unavailable - cannot scan inventory");
			return;
		}

		if (resourceScanner == null) {
			resourceModePanel.updateDebugError("Resource scanner unavailable");
			return;
		}

		ClientThread clientThread = resourceScanner.getClientThread();
		if (clientThread == null) {
			resourceModePanel.updateDebugError("Client thread unavailable");
			return;
		}

		// Execute inventory access on ClientThread with error handling
		clientThread.invoke(() -> {
			try {
				resourceModePanel.updateDebugStatus("Accessing inventory...");

				// Get inventory container with validation
				net.runelite.api.ItemContainer inventory = client.getItemContainer(93);
				if (inventory == null) {
					resourceModePanel.updateDebugError("Inventory not accessible - make sure you're logged in");
					return;
				}

				// Get items array with validation
				net.runelite.api.Item[] items = inventory.getItems();
				if (items == null) {
					resourceModePanel.updateDebugError("Inventory items not accessible");
					return;
				}

				// Process inventory items with error handling
				Map<Integer, String[]> debugInfo = new HashMap<>();
				Map<BoneType, Integer> boneResources = new HashMap<>();
				int totalItems = 0;
				int errorCount = 0;

				for (int i = 0; i < items.length; i++) {
					try {
						net.runelite.api.Item item = items[i];
						if (item == null || item.getId() <= 0) {
							continue;
						}

						totalItems++;
						int itemId = item.getId();
						int quantity = item.getQuantity();

						// Validate quantity bounds
						if (quantity < 0 || quantity > Integer.MAX_VALUE / 1000) {
							errorCount++;
							continue;
						}

						// Get proper item name using ItemManager with error handling
						String itemName = "Item " + itemId;
						try {
							if (resourceScanner.getItemManager() != null) {
								itemName = resourceScanner.getItemManager().getItemComposition(itemId).getMembersName();
							}
						} catch (Exception e) {
							itemName = "Item " + itemId + " (name error)";
							errorCount++;
						}

						// Check if it's a bone item with error handling
						BoneType boneType = null;
						try {
							boneType = BoneResourceScanner.getBoneTypeForItem(itemId);
							if (boneType != null) {
								boneResources.merge(boneType, quantity, Integer::sum);
								itemName += " [BONE: " + boneType.name() + "]";
							}
						} catch (Exception e) {
							errorCount++;
							// Continue processing other items
						}

						// Add to debug table
						debugInfo.put(i, new String[] {
								String.valueOf(itemId),
								itemName,
								String.valueOf(quantity)
						});

					} catch (Exception e) {
						errorCount++;
						// Continue processing other items
					}
				}

				// Debug inventory table removed - no longer needed

				if (boneResources.isEmpty()) {
					String message = "Found " + totalItems + " items, no bones detected";
					if (errorCount > 0) {
						message += " (" + errorCount + " items had errors)";
					}
					resourceModePanel.updateDebugStatus(message);

					// Update with empty bone resources to show 0 values with icons
					resourceModePanel.updateResourceBreakdown(boneResources);

					// Update achievable level with 0 shards to show proper 0 values and icons (use
					// debug override if available)
					int currentXP = getCurrentXPInput();
					boolean useSunfireWine = isSunfireWineSelected();
					int effectiveShards = resourceModePanel.getEffectiveTotalShards(0);
					resourceModePanel.updateAchievableLevel(effectiveShards, currentXP, useSunfireWine);
				} else {
					try {
						// Update bone breakdown with error handling
						resourceModePanel.updateResourceBreakdown(boneResources);

						// Calculate achievable level with bounds checking
						int totalShards = 0;
						for (Map.Entry<BoneType, Integer> entry : boneResources.entrySet()) {
							int shardValue = entry.getKey().getShardValue() * entry.getValue();
							// Prevent integer overflow
							if (totalShards > Integer.MAX_VALUE - shardValue) {
								resourceModePanel.updateDebugError("Too many shards - calculation overflow");
								return;
							}
							totalShards += shardValue;
						}

						int currentXP = getCurrentXPInput();
						boolean useSunfireWine = isSunfireWineSelected();
						int effectiveShards = resourceModePanel.getEffectiveTotalShards(totalShards);
						resourceModePanel.updateAchievableLevel(effectiveShards, currentXP, useSunfireWine);

						String message = "Resource display updated";
						if (errorCount > 0) {
							message += " (" + errorCount + " items had errors)";
						}
						resourceModePanel.updateDebugSuccess(message);

					} catch (Exception e) {
						resourceModePanel.updateDebugError("Error calculating results: " + e.getMessage());
					}
				}

			} catch (Exception ex) {
				// Requirement 10.5: Log errors without crashing RuneLite
				log.error("Prayer Calculator: Resource scan failed", ex);
				resourceModePanel.updateDebugError("Scan failed: " + ex.getMessage());
			}
		});
	}

	// Handle wine type changes to recalculate levels
	private void onWineTypeChanged() {
		// Update Goal Mode calculations - call directly to ensure it runs regardless of
		// tab selection
		try {
			goalModePanel.updateBoneShardsRequired();
		} catch (Exception e) {
			handleCalculationError("Error updating calculations on wine type change", e);
		}

		// Update Resource Mode calculations if we have scanned data (regardless of tab
		// selection)
		try {
			// Recalculate using existing data with new wine type (no inventory rescan
			// needed)
			resourceModePanel.recalculateWithCurrentSettings();
		} catch (Exception e) {
			handleCalculationError("Error updating Resource Mode calculations on wine type change", e);
		}
	}

	// Handle zealot robes checkbox changes to update calculations
	private void onZealotRobesChanged() {
		// Update Goal Mode calculations - call directly to ensure it runs regardless of
		// tab selection
		try {
			goalModePanel.updateBoneShardsRequired();
		} catch (Exception e) {
			handleCalculationError("Error updating calculations on zealot robes change", e);
		}

		// Update Resource Mode calculations if we have scanned data (regardless of tab
		// selection)
		try {
			// Recalculate using existing data with new zealot robes setting (no inventory
			// rescan needed)
			resourceModePanel.recalculateWithCurrentSettings();
		} catch (Exception e) {
			handleCalculationError("Error updating Resource Mode calculations on zealot robes change", e);
		}

		// Update zealot robes warning display for both modes
		goalModePanel.updateZealotRobesWarningDisplay();
		resourceModePanel.updateZealotRobesWarningDisplay();
	}

	public void updateDebugMode(boolean debugMode) {
		// Update debug mode in Resource Mode panel
		if (resourceModePanel != null) {
			resourceModePanel.updateDebugSectionVisibility();
		}

		// Update debug mode in Goal Mode panel
		if (goalModePanel != null) {
			goalModePanel.setDebugMode(debugMode, this);
		}
	}

	private void triggerResourcePlanningRecalculation() {
		// Delegate to Goal Mode panel's resource planning recalculation
		goalModePanel.triggerResourcePlanningRecalculation();
	}

	private void setupSynchronizedEventHandlers() {
		// Set up synchronized checkbox event handlers
		goalModePanel.getUiCheckboxSunfireWine().addActionListener(e -> {
			// Sync state to Resource Mode
			resourceModePanel.setSunfireWineSelected(goalModePanel.isSunfireWineSelected());
			onWineTypeChanged();
		});

		resourceModePanel.getUiCheckboxSunfireWine().addActionListener(e -> {
			// Sync state to Goal Mode
			goalModePanel.setSunfireWineSelected(resourceModePanel.isSunfireWineSelected());
			onWineTypeChanged();
		});

		goalModePanel.getUiCheckboxZealotRobes().addActionListener(e -> {
			// Sync state to Resource Mode
			resourceModePanel.setZealotRobesSelected(goalModePanel.isZealotRobesSelected());
			onZealotRobesChanged();
		});

		resourceModePanel.getUiCheckboxZealotRobes().addActionListener(e -> {
			// Sync state to Goal Mode
			goalModePanel.setZealotRobesSelected(resourceModePanel.isZealotRobesSelected());
			onZealotRobesChanged();
		});

		// Set up synchronized scan button handlers
		goalModePanel.getUiButtonScanInventory().addActionListener(e -> performUnifiedInventoryScan());
		resourceModePanel.getUiButtonScanResources().addActionListener(e -> performUnifiedInventoryScan());
	}

	private void performUnifiedInventoryScan() {
		// Performs unified inventory scan that updates both Goal Mode and Resource Mode
		// Perform the resource scan (this updates Resource Mode)
		performResourceScan();

		// Also trigger Goal Mode resource planning scan if it's expanded
		if (goalModePanel.isResourcePlanningExpanded()) {
			goalModePanel.performInventoryScan();
		}
	}
}