package com.boneshardhelper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.FlatTextField;
import net.runelite.client.ui.components.IconTextField;

@Getter
@Slf4j
class GoalModePanel extends JPanel {
	private static final Pattern NON_NUMERIC = Pattern.compile("\\D");

	private final JTextField uiFieldCurrentLevel;
	private final JTextField uiFieldCurrentXP;
	private final JTextField uiFieldTargetLevel;
	private final JTextField uiFieldTargetXP;
	private final JCheckBox uiCheckboxSunfireWine;
	private final JCheckBox uiCheckboxZealotRobes;

	// Results display components
	private FoldingSection resourcesRequiredSection;
	private JLabel boneShardsLabel;
	private JLabel wineLabel;
	private final JLabel warningLabel;

	// Item manager for icons
	private ItemManager itemManager;

	// Resource Planning components
	private FoldingSection resourcePlanningSection;
	private JButton scanInventoryButton;
	private JLabel resourceStatusLabel;
	private JTable recommendationsTable;
	private DefaultTableModel recommendationsTableModel;

	// Resource scanner reference (injected later)
	private BoneResourceScanner resourceScanner;

	// Calculation engine for centralized calculation logic
	private final PrayerCalculationEngine calculationEngine;

	// Debug panel components (only visible in debug mode)
	private FoldingSection debugSection;
	private IconTextField hiscoreLookupField;
	private JLabel debugStatusLabel;

	GoalModePanel() {
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Initialize calculation engine
		this.calculationEngine = new PrayerCalculationEngine();

		// Create input panel with BorderLayout to stack components
		JPanel inputPanel = new JPanel(new BorderLayout());
		inputPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		inputPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

		// Create fields panel with 2x2 grid layout for level/XP fields
		JPanel fieldsPanel = new JPanel(new GridLayout(2, 2, 7, 7));
		fieldsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Input fields in 2x2 grid layout
		uiFieldCurrentLevel = addComponent(fieldsPanel, "Current Level");
		uiFieldCurrentXP = addComponent(fieldsPanel, "Current Experience");
		uiFieldTargetLevel = addComponent(fieldsPanel, "Target Level");
		uiFieldTargetXP = addComponent(fieldsPanel, "Target Experience");

		// Create checkboxes panel with vertical layout
		JPanel checkboxPanel = new JPanel(new GridLayout(2, 1, 0, 5));
		checkboxPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		checkboxPanel.setBorder(new EmptyBorder(10, 0, 0, 0)); // Add some top spacing

		// Checkboxes stacked vertically
		uiCheckboxSunfireWine = addCheckboxComponent(checkboxPanel, "Sunfire Wine (6 XP/shard)");
		uiCheckboxZealotRobes = addCheckboxComponent(checkboxPanel, "Zealot's Robes");

		// Add panels to input panel
		inputPanel.add(fieldsPanel, BorderLayout.CENTER);
		inputPanel.add(checkboxPanel, BorderLayout.SOUTH);

		// Create results panel with BorderLayout to avoid equal height distribution
		JPanel resultsPanel = new JPanel(new BorderLayout());
		resultsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		resultsPanel.setBorder(new EmptyBorder(10, 5, 5, 5));

		// Create resources required section
		resourcesRequiredSection = createResourcesRequiredSection();

		warningLabel = new JLabel("<html>&nbsp;</html>"); // Non-breaking space to maintain consistent height
		warningLabel.setForeground(Color.ORANGE);
		warningLabel.setFont(FontManager.getRunescapeSmallFont());
		warningLabel.setBorder(new EmptyBorder(3, 0, 0, 0)); // Small top margin for spacing

		resultsPanel.add(resourcesRequiredSection, BorderLayout.NORTH);
		resultsPanel.add(warningLabel, BorderLayout.CENTER);

		// Create Resource Planning section
		resourcePlanningSection = createResourcePlanningSection();

		// Create debug section (initially hidden)
		debugSection = createDebugSection();

		// Create main content panel to hold results, resource planning, and debug panel
		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		bottomPanel.add(resultsPanel, BorderLayout.NORTH);

		// Create center panel for resource planning and debug
		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		centerPanel.add(resourcePlanningSection, BorderLayout.NORTH);
		centerPanel.add(debugSection, BorderLayout.SOUTH);

		bottomPanel.add(centerPanel, BorderLayout.CENTER);

		// Add panels to main layout
		add(inputPanel, BorderLayout.CENTER);
		add(bottomPanel, BorderLayout.SOUTH);
	}

	public void triggerResourcePlanningRecalculation() {
		if (resourcePlanningSection.isOpen() && resourceScanner != null) {
			performInventoryScanInternal();
		}
	}

	int getCurrentLevelInput() {
		return getInput(uiFieldCurrentLevel);
	}

	void setCurrentLevelInput(int value) {
		setInput(uiFieldCurrentLevel, value);
	}

	int getCurrentXPInput() {
		return getInput(uiFieldCurrentXP);
	}

	void setCurrentXPInput(Object value) {
		setInput(uiFieldCurrentXP, value);
	}

	int getTargetLevelInput() {
		return getInput(uiFieldTargetLevel);
	}

	void setTargetLevelInput(Object value) {
		setInput(uiFieldTargetLevel, value);
	}

	int getTargetXPInput() {
		return getInput(uiFieldTargetXP);
	}

	void setTargetXPInput(Object value) {
		setInput(uiFieldTargetXP, value);
	}

	// Getter methods for UI fields (needed for BoneShardHelperPanel)
	JTextField getUiFieldCurrentLevel() {
		return uiFieldCurrentLevel;
	}

	JTextField getUiFieldCurrentXP() {
		return uiFieldCurrentXP;
	}

	JTextField getUiFieldTargetLevel() {
		return uiFieldTargetLevel;
	}

	JTextField getUiFieldTargetXP() {
		return uiFieldTargetXP;
	}

	boolean isSunfireWineSelected() {
		return uiCheckboxSunfireWine.isSelected();
	}

	void setSunfireWineSelected(boolean selected) {
		uiCheckboxSunfireWine.setSelected(selected);
	}

	boolean isZealotRobesSelected() {
		return uiCheckboxZealotRobes.isSelected();
	}

	void setZealotRobesSelected(boolean selected) {
		uiCheckboxZealotRobes.setSelected(selected);
	}

	// Getter methods for checkboxes (needed for action listener setup)
	JCheckBox getUiCheckboxSunfireWine() {
		return uiCheckboxSunfireWine;
	}

	JCheckBox getUiCheckboxZealotRobes() {
		return uiCheckboxZealotRobes;
	}

	JButton getUiButtonScanInventory() {
		return scanInventoryButton;
	}

	public boolean isResourcePlanningExpanded() {
		return resourcePlanningSection.isOpen();
	}

	public void performInventoryScan() {
		performInventoryScanInternal();
	}

	public void updateBoneShardsRequired() {
		try {
			// Clear warning first
			clearValidationError();

			// Create PrayerData from current inputs
			PrayerData prayerData = new PrayerData();
			prayerData.setCurrentLevel(getCurrentLevelInput());
			prayerData.setCurrentXP(getCurrentXPInput());
			prayerData.setTargetLevel(getTargetLevelInput());
			prayerData.setTargetXP(getTargetXPInput());
			prayerData.setUseSunfireWine(isSunfireWineSelected());
			prayerData.setUseZealotRobes(isZealotRobesSelected());

			// Use centralized calculation engine (single source of truth)
			CalculationResult result = calculationEngine.calculateForTarget(prayerData);

			// Handle special cases
			if (result.isGoalAlreadyAchieved()) {
				updateResourcesDisplay(result, prayerData.isUseSunfireWine(), prayerData.isUseZealotRobes(), true);
				return;
			}

			// Check for invalid inputs (engine validation will catch these)
			int shardsNeeded = result.getRequiredShards();
			if (shardsNeeded <= 0) {
				updateResourcesDisplay(result, prayerData.isUseSunfireWine(), prayerData.isUseZealotRobes(), true);
				return;
			}

			// Update the resources display with calculated values
			updateResourcesDisplay(result, prayerData.isUseSunfireWine(), prayerData.isUseZealotRobes(), false);

		} catch (Exception e) {
			log.error("Prayer Calculator: Error updating bone shards calculation", e);

			// Create empty result for error display
			CalculationResult errorResult = new CalculationResult();
			errorResult.setRequiredShards(0);
			errorResult.setWinesNeeded(0);

			boneShardsLabel.setText("Calculation error");
			wineLabel.setText("Calculation error");
			showValidationError("Calculation error: Please check your input values");
		}
	}

	public void clearResults() {
		boneShardsLabel.setText("Calculate to see results");
		boneShardsLabel.setIcon(null);
		wineLabel.setText("Calculate to see results");
		wineLabel.setIcon(null);
		warningLabel.setText("<html>&nbsp;</html>");
	}

	public void showValidationError(String message) {
		// Validation message in case inputs are invalid, like if current xp > target xp
		warningLabel.setText("<html>!! " + message + "</html>");
		warningLabel.setForeground(Color.RED);
	}

	private void updateResourcesDisplay(CalculationResult result, boolean useSunfireWine, boolean useZealotRobes,
			boolean goalAchieved) {
		// Updates the resources display with bone shards and wine requirements.
		try {
			// Get values from centralized calculation result
			int shardsNeeded = result.getRequiredShards();
			int winesNeeded = result.getWinesNeeded();

			// Update bone shards display - use blessed bone shards item ID (29381)
			if (itemManager != null) {
				try {
					// Clear any existing icon first
					boneShardsLabel.setIcon(null);
					// Use blessed bone shards item ID for the icon
					itemManager.getImage(29381).addTo(boneShardsLabel);
				} catch (Exception e) {
					log.error("Error loading bone shard icon", e);
				}
			}

			String shardsText;
			if (goalAchieved) {
				shardsText = "0 (goal achieved)";
			} else {
				shardsText = String.format("%,d bone shards", shardsNeeded);
			}

			boneShardsLabel.setText(shardsText);

			// Update wine display - use correct wine item IDs
			if (itemManager != null) {
				try {
					// Clear any existing icon first
					wineLabel.setIcon(null);
					// Use correct wine item IDs: Jug of sunfire wine (29384) for sunfire, Jug of
					// wine (1993) for regular
					int wineItemId = useSunfireWine ? 29384 : 1993;
					itemManager.getImage(wineItemId).addTo(wineLabel);
				} catch (Exception e) {
					log.error("Error loading wine icon", e);
				}
			}

			// Calculate wine display text
			String wineText;
			String wineType = useSunfireWine ? "Sunfire wine" : "Regular wine";

			if (goalAchieved) {
				wineText = String.format("0 %s", wineType.toLowerCase());
			} else {
				wineText = String.format("%,d %s", winesNeeded, wineType.toLowerCase());
			}

			wineLabel.setText(wineText);

			// Update zealot robes warning
			updateZealotRobesWarning();

		} catch (Exception e) {
			log.error("Prayer Calculator: Error updating resources display", e);
			boneShardsLabel.setText("Error loading display");
			wineLabel.setText("Error loading display");
		}
	}

	public void clearValidationError() {
		warningLabel.setText("<html>&nbsp;</html>");
		warningLabel.setForeground(Color.ORANGE);
	}

	private FoldingSection createResourcesRequiredSection() {
		// Create labels for the content
		boneShardsLabel = new JLabel("Calculate to see results");
		boneShardsLabel.setForeground(Color.WHITE);
		boneShardsLabel.setFont(FontManager.getRunescapeSmallFont());

		wineLabel = new JLabel("Calculate to see results");
		wineLabel.setForeground(Color.WHITE);
		wineLabel.setFont(FontManager.getRunescapeSmallFont());

		// Create the folding section with the labels as content
		return new FoldingSection(
				"Total Resources Needed",
				"Bone shards and wines required to reach your goal",
				boneShardsLabel,
				wineLabel);
	}

	public void setItemManager(ItemManager itemManager) {
		this.itemManager = itemManager;
		// Refresh icons if we have results displayed
		refreshIcons();
	}

	private void refreshIcons() {
		// Refreshes the item icons if ItemManager is available
		if (itemManager == null) {
			return;
		}

		// Only refresh if we have actual results (not the default "Calculate to see
		// results" text)
		if (!boneShardsLabel.getText().equals("Calculate to see results") &&
				!boneShardsLabel.getText().equals("Calculation error")) {

			try {
				// Refresh bone shard icon
				boneShardsLabel.setIcon(null);
				itemManager.getImage(29381).addTo(boneShardsLabel);
			} catch (Exception e) {
				log.error("Error refreshing bone shard icon", e);
			}
		}

		// Only refresh wine icon if we have actual results
		if (!wineLabel.getText().equals("Calculate to see results") &&
				!wineLabel.getText().equals("Calculation error")) {

			try {
				// Determine wine type from current checkbox state
				boolean useSunfireWine = isSunfireWineSelected();
				int wineItemId = useSunfireWine ? 29384 : 1993;

				wineLabel.setIcon(null);
				itemManager.getImage(wineItemId).addTo(wineLabel);
			} catch (Exception e) {
				log.error("Error refreshing wine icon", e);
			}
		}
	}

	public void setResourceScanner(BoneResourceScanner scanner) {
		this.resourceScanner = scanner;
	}

	private void updateZealotRobesWarning() {
		if (isZealotRobesSelected()) {
			warningLabel.setText(
					"<html>!! Zealot robes: Actual XP values may vary slightly due to resource-save triggers.</html>");
		} else {
			warningLabel.setText("<html>&nbsp;</html>"); // Non-breaking space maintains height
		}
	}

	public void updateZealotRobesWarningDisplay() {
		updateZealotRobesWarning();
	}

	private FoldingSection createResourcePlanningSection() {
		// Creates the collapsible Resource Planning section using FoldingSection.
		JPanel content = createResourcePlanningContent();

		// Create the folding section with the content
		FoldingSection section = new FoldingSection(
				"Resource Planning",
				"Scan your inventory to see how many more bones you need for your goal",
				content);

		// Set to closed by default (matching original behavior)
		section.setOpen(false);

		return section;
	}

	private JPanel createResourcePlanningContent() {
		JPanel content = new JPanel(new BorderLayout());
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		content.setBorder(new EmptyBorder(5, 10, 5, 5)); // Indent content

		// Create scan button (will be moved below table)
		scanInventoryButton = new JButton("Scan Inventory");
		scanInventoryButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		scanInventoryButton.setForeground(Color.WHITE);
		scanInventoryButton.setFont(FontManager.getRunescapeSmallFont());
		scanInventoryButton.setBorder(new EmptyBorder(5, 10, 5, 10));

		// Add descriptive text above the table
		JLabel descriptionLabel = new JLabel(
				"<html>Click the \"Scan Inventory\" button to find out how many more bones you need for your goal.<br/><br/>Note: This tool assumes that all bones in your inventory will be broken down into bone shards and used for Prayer training.</html>");
		descriptionLabel.setForeground(Color.LIGHT_GRAY);
		descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
		descriptionLabel.setBorder(new EmptyBorder(0, 5, 8, 5)); // Add some spacing below the text

		// Create status label (will be moved below button)
		resourceStatusLabel = new JLabel("Click 'Scan Inventory' to begin.");
		resourceStatusLabel.setForeground(Color.LIGHT_GRAY);
		resourceStatusLabel.setFont(FontManager.getRunescapeSmallFont());

		// Create recommendations table
		String[] columnNames = { "Bone Type", "# Needed" };
		recommendationsTableModel = new DefaultTableModel(columnNames, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false; // Make table read-only
			}
		};

		recommendationsTable = new JTable(recommendationsTableModel);
		recommendationsTable.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		recommendationsTable.setForeground(Color.WHITE);
		recommendationsTable.setFont(FontManager.getRunescapeSmallFont());
		recommendationsTable.setGridColor(ColorScheme.LIGHT_GRAY_COLOR);
		recommendationsTable.getTableHeader().setBackground(ColorScheme.DARKER_GRAY_COLOR);
		recommendationsTable.getTableHeader().setForeground(Color.WHITE);
		recommendationsTable.getTableHeader().setFont(FontManager.getRunescapeSmallFont());
		recommendationsTable.setRowHeight(20);

		// Configure column widths and alignment
		setupTableColumns();

		JScrollPane scrollPane = new JScrollPane(recommendationsTable);
		scrollPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR));
		scrollPane.setPreferredSize(new Dimension(0, 150));

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		buttonPanel.setBorder(new EmptyBorder(10, 0, 10, 0)); // Top and bottom spacing
		buttonPanel.add(scanInventoryButton);

		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		bottomPanel.add(buttonPanel, BorderLayout.NORTH);

		resourceStatusLabel.setBorder(new EmptyBorder(0, 10, 5, 10)); // Left and right padding
		bottomPanel.add(resourceStatusLabel, BorderLayout.SOUTH);

		content.add(descriptionLabel, BorderLayout.NORTH);
		content.add(scrollPane, BorderLayout.CENTER);
		content.add(bottomPanel, BorderLayout.SOUTH);

		return content;
	}

	private void setupTableColumns() {
		if (recommendationsTable.getColumnCount() >= 2) {
			recommendationsTable.getColumnModel().getColumn(0).setPreferredWidth(80); // Bone Type
			recommendationsTable.getColumnModel().getColumn(1).setPreferredWidth(20); // Needed

			// Disable column resizing to maintain proportions
			recommendationsTable.getTableHeader().setResizingAllowed(false);
			recommendationsTable.getTableHeader().setReorderingAllowed(false);

			// Set up right alignment for "Needed" column
			javax.swing.table.DefaultTableCellRenderer rightRenderer = new javax.swing.table.DefaultTableCellRenderer();
			rightRenderer.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
			rightRenderer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			rightRenderer.setForeground(Color.WHITE);

			// Apply right alignment to "Needed" column (column 1)
			recommendationsTable.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);
		}
	}

	private void performInventoryScanInternal() {
		// Performs inventory scan and calculates bone recommendations.
		if (resourceScanner == null) {
			resourceStatusLabel.setText("Error: Resource scanner not available");
			resourceStatusLabel.setForeground(Color.RED);
			return;
		}

		try {
			resourceStatusLabel.setText("Scanning inventory...");
			resourceStatusLabel.setForeground(Color.YELLOW);

			// Use ClientThread to safely access game data
			resourceScanner.getClientThread().invoke(() -> {
				try {
					// Scan inventory for bones
					Map<BoneType, Integer> inventoryBones = resourceScanner.scanInventory();

					// Calculate current total shards from inventory
					int currentShards = resourceScanner.calculateTotalShards(inventoryBones);

					// Calculate required shards for goal
					int requiredShards = calculateRequiredShards();

					// Update UI
					javax.swing.SwingUtilities.invokeLater(
							() -> updateResourceRecommendations(inventoryBones, currentShards, requiredShards));
				} catch (Exception e) {
					javax.swing.SwingUtilities.invokeLater(() -> {
						resourceStatusLabel.setText("Error scanning inventory: " + e.getMessage());
						resourceStatusLabel.setForeground(Color.RED);
					});
				}
			});
		} catch (Exception e) {
			resourceStatusLabel.setText("Error: " + e.getMessage());
			resourceStatusLabel.setForeground(Color.RED);
		}
	}

	private int calculateRequiredShards() {
		// Calculates the required shards based on current goal settings.
		try {
			// Create PrayerData from current inputs
			PrayerData prayerData = new PrayerData();
			prayerData.setCurrentLevel(getCurrentLevelInput());
			prayerData.setCurrentXP(getCurrentXPInput());
			prayerData.setTargetLevel(getTargetLevelInput());
			prayerData.setTargetXP(getTargetXPInput());
			prayerData.setUseSunfireWine(isSunfireWineSelected());
			prayerData.setUseZealotRobes(isZealotRobesSelected());

			// Use centralized calculation engine (single source of truth)
			CalculationResult result = calculationEngine.calculateForTarget(prayerData);

			return result.getRequiredShards();
		} catch (Exception e) {
			return 0;
		}
	}

	private void updateResourceRecommendations(Map<BoneType, Integer> inventoryBones, int currentShards,
			int requiredShards) {
		// Clear existing recommendations
		recommendationsTableModel.setRowCount(0);

		// First, always display the bone shard value found
		if (currentShards == 0) {
			resourceStatusLabel.setText("0 bone shard value found in inventory");
			resourceStatusLabel.setForeground(Color.ORANGE);
		} else {
			resourceStatusLabel.setText(String.format("%,d bone shard value found in inventory\n", currentShards));
			resourceStatusLabel.setForeground(Color.GREEN);
		}

		if (requiredShards <= 0) {
			// Keep the shard value message but don't show recommendations
			return;
		}

		// Check if goal is already achieved
		if (currentShards >= requiredShards) {
			// Update message to show both shard value and goal achievement
			int surplus = currentShards - requiredShards;
			if (surplus > 0) {
				resourceStatusLabel.setText(String.format(
						"<html>%,d bone shard value found in inventory<br/>Goal achieved! (%,d surplus shards)</html>",
						currentShards, surplus));
			} else {
				resourceStatusLabel.setText(String.format(
						"<html>%,d bone shard value found in inventory<br/>Goal achieved! (exactly %,d needed)</html>",
						currentShards, requiredShards));
			}
			resourceStatusLabel.setForeground(Color.GREEN);

			// Show all bone types with 0 needed since goal is achieved
			List<BoneRecommendation> zeroRecommendations = generateZeroBoneRecommendations();

			// Populate table with zero recommendations (sorted by efficiency)
			for (BoneRecommendation rec : zeroRecommendations) {
				recommendationsTableModel.addRow(new Object[] {
						rec.boneType.getDisplayName(),
						String.format("%,d", rec.quantityNeeded)
				});
			}
			return;
		}

		// Calculate additional shards needed
		int additionalShards = requiredShards - currentShards;

		// Calculate percentage
		double percentage = (double) currentShards / requiredShards * 100;

		// Update status with HTML for word wrapping - show shard value first, then goal
		// progress
		resourceStatusLabel.setText(String.format(
				"<html>Your inventory contains resources worth %,d blessed bone shards (%.1f%% of goal)<br/><br/>You need to get %,d more shards to reach your XP goals </html>",
				currentShards, percentage, additionalShards));
		resourceStatusLabel.setForeground(Color.ORANGE);

		List<BoneRecommendation> recommendations = generateBoneRecommendations(additionalShards);

		// Populate table with bone types (sorted by efficiency)
		for (BoneRecommendation rec : recommendations) {
			recommendationsTableModel.addRow(new Object[] {
					rec.boneType.getDisplayName(),
					String.format("%,d", rec.quantityNeeded)
			});
		}
	}

	private List<BoneRecommendation> generateBoneRecommendations(int additionalShards) {
		List<BoneRecommendation> recommendations = new ArrayList<>();
		Set<BoneType> processedTypes = new HashSet<>();

		// Generate recommendations for each bone type, using consolidated types
		for (BoneType boneType : BoneType.values()) {
			// Skip bone shards themselves
			if (boneType == BoneType.BLESSED_BONE_SHARDS) {
				continue;
			}

			// Get the consolidated bone type (this will map unblessed to blessed versions)
			BoneType consolidatedType = getConsolidatedBoneType(boneType);

			// Skip if we've already processed this consolidated type
			if (processedTypes.contains(consolidatedType)) {
				continue;
			}

			processedTypes.add(consolidatedType);

			int shardsPerBone = consolidatedType.getShardValue();
			int quantityNeeded = (int) Math.ceil((double) additionalShards / shardsPerBone);

			recommendations.add(new BoneRecommendation(consolidatedType, quantityNeeded, shardsPerBone));
		}

		// Sort by efficiency (highest shards per bone first)
		recommendations.sort(Comparator.comparingInt((BoneRecommendation r) -> r.efficiency).reversed());

		return recommendations;
	}

	private List<BoneRecommendation> generateZeroBoneRecommendations() {
		List<BoneRecommendation> recommendations = new ArrayList<>();
		Set<BoneType> processedTypes = new HashSet<>();

		// Generate recommendations for each bone type, using consolidated types
		for (BoneType boneType : BoneType.values()) {
			// Skip bone shards themselves
			if (boneType == BoneType.BLESSED_BONE_SHARDS) {
				continue;
			}

			// Get the consolidated bone type (this will map unblessed to blessed versions)
			BoneType consolidatedType = getConsolidatedBoneType(boneType);

			// Skip if we've already processed this consolidated type
			if (processedTypes.contains(consolidatedType)) {
				continue;
			}

			processedTypes.add(consolidatedType);

			int shardsPerBone = consolidatedType.getShardValue();
			// Set quantity needed to 0 since goal is already achieved
			int quantityNeeded = 0;

			recommendations.add(new BoneRecommendation(consolidatedType, quantityNeeded, shardsPerBone));
		}

		// Sort by efficiency (highest shards per bone first)
		recommendations.sort(Comparator.comparingInt((BoneRecommendation r) -> r.efficiency).reversed());

		return recommendations;
	}

	private BoneType getConsolidatedBoneType(BoneType boneType) {
		// Gets the consolidated bone type for display purposes.
		switch (boneType) {
			case BLESSED_BONE_STATUETTE1:
			case BLESSED_BONE_STATUETTE2:
			case BLESSED_BONE_STATUETTE3:
			case BLESSED_BONE_STATUETTE4:
				return BoneType.BLESSED_BONE_STATUETTE0; // Use the first one as the consolidated entry
			case BONES:
				return BoneType.BLESSED_BONES;
			case BAT_BONES:
				return BoneType.BLESSED_BAT_BONES;
			case BIG_BONES:
				return BoneType.BLESSED_BIG_BONES;
			case BABYDRAGON_BONES:
				return BoneType.BLESSED_BABYDRAGON_BONES;
			case DRAGON_BONES:
				return BoneType.BLESSED_DRAGON_BONES;
			case WYVERN_BONES:
				return BoneType.BLESSED_WYVERN_BONES;
			case DRAKE_BONES:
				return BoneType.BLESSED_DRAKE_BONES;
			case FAYRG_BONES:
				return BoneType.BLESSED_FAYRG_BONES;
			case LAVA_DRAGON_BONES:
				return BoneType.BLESSED_LAVA_DRAGON_BONES;
			case RAURG_BONES:
				return BoneType.BLESSED_RAURG_BONES;
			case DAGANNOTH_BONES:
				return BoneType.BLESSED_DAGANNOTH_BONES;
			case OURG_BONES:
				return BoneType.BLESSED_OURG_BONES;
			case SUPERIOR_DRAGON_BONES:
				return BoneType.BLESSED_SUPERIOR_DRAGON_BONES;
			case BABYWYRM_BONES:
				return BoneType.BLESSED_BABYWYRM_BONES;
			case WYRM_BONES:
				return BoneType.BLESSED_WYRM_BONES;
			case HYDRA_BONES:
				return BoneType.BLESSED_HYDRA_BONES;
			case ZOGRE_BONES:
				return BoneType.BLESSED_ZOGRE_BONES;
			default:
				return boneType; // Return original type for blessed bones and special types
		}
	}

	private static class BoneRecommendation {
		final BoneType boneType;
		final int quantityNeeded;
		final int efficiency;

		BoneRecommendation(BoneType boneType, int quantityNeeded, int efficiency) {
			this.boneType = boneType;
			this.quantityNeeded = quantityNeeded;
			this.efficiency = efficiency;
		}
	}

	private static int getInput(JTextField field) {
		try {
			return Integer.parseInt(NON_NUMERIC.matcher(field.getText()).replaceAll(""));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static void setInput(JTextField field, Object value) {
		field.setText(String.valueOf(value));
	}

	private JTextField addComponent(JPanel parent, String label) {
		final JPanel container = new JPanel();
		container.setLayout(new BorderLayout());
		container.setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JLabel uiLabel = new JLabel(label);
		final FlatTextField uiInput = new FlatTextField();

		uiInput.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		uiInput.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		uiInput.setBorder(new EmptyBorder(5, 7, 5, 7));

		uiLabel.setFont(FontManager.getRunescapeSmallFont());
		uiLabel.setBorder(new EmptyBorder(0, 0, 4, 0));
		uiLabel.setForeground(Color.WHITE);

		container.add(uiLabel, BorderLayout.NORTH);
		container.add(uiInput, BorderLayout.CENTER);

		parent.add(container);

		return uiInput.getTextField();
	}

	private JCheckBox addCheckboxComponent(JPanel parent, String label) {
		final JPanel container = new JPanel();
		container.setLayout(new BorderLayout());
		container.setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JCheckBox checkbox = new JCheckBox(label);
		checkbox.setBackground(ColorScheme.DARK_GRAY_COLOR);
		checkbox.setForeground(Color.WHITE);
		checkbox.setFont(FontManager.getRunescapeSmallFont());
		checkbox.setBorder(new EmptyBorder(5, 7, 5, 7));

		container.add(checkbox, BorderLayout.CENTER);
		parent.add(container);

		return checkbox;
	}

	private FoldingSection createDebugSection() {
		// Create hiscore lookup field. Only used when debugging is enabled
		hiscoreLookupField = new IconTextField();
		hiscoreLookupField.setIcon(IconTextField.Icon.SEARCH);
		hiscoreLookupField.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 25));
		hiscoreLookupField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		hiscoreLookupField.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		hiscoreLookupField.setMinimumSize(new Dimension(0, 25));
		hiscoreLookupField.addActionListener(e -> performHiscoreLookup());
		hiscoreLookupField.addClearListener(() -> {
			hiscoreLookupField.setIcon(IconTextField.Icon.SEARCH);
			hiscoreLookupField.setEditable(true);
			// Clear status message when field is cleared
			debugStatusLabel.setText("<html></html>");
		});

		// Create debug status label with word wrap
		debugStatusLabel = new JLabel("");
		debugStatusLabel.setForeground(Color.LIGHT_GRAY);
		debugStatusLabel.setFont(FontManager.getRunescapeSmallFont());
		debugStatusLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);

		// Add descriptive text above the search field
		JLabel descriptionLabel = new JLabel(
				"<html>Lookup a player's name to populate the Current Level/XP and Target Level/XP boxes with that player's Prayer level and a sensible default goal.</html>");
		descriptionLabel.setForeground(Color.LIGHT_GRAY);
		descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
		descriptionLabel.setBorder(new javax.swing.border.EmptyBorder(0, 5, 8, 5)); // Add some spacing below the text

		// Create content panel
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentPanel.add(descriptionLabel, BorderLayout.NORTH);
		contentPanel.add(hiscoreLookupField, BorderLayout.CENTER);
		contentPanel.add(debugStatusLabel, BorderLayout.SOUTH);

		// Create the folding section
		FoldingSection section = new FoldingSection(
				"DEBUG: PLAYER LOOKUP",
				"Player lookup functionality for testing and debugging",
				contentPanel);

		// Initially hidden and closed
		section.setVisible(false);
		section.setOpen(false);

		return section;
	}

	private void performHiscoreLookup() {
		// Simplified version of hiscore lookup used in debug mode. Look up player name,
		// 		fill in player's current level/xp and sets a reasonable goal.
		String username = hiscoreLookupField.getText().trim();
		if (username.isEmpty()) {
			return;
		}

		// Validate username length
		if (username.length() > 12) {
			hiscoreLookupField.setIcon(IconTextField.Icon.ERROR);
			return;
		}

		// Set loading state
		hiscoreLookupField.setEditable(false);
		hiscoreLookupField.setIcon(IconTextField.Icon.LOADING_DARKER);
		debugStatusLabel.setText("<html></html>"); // Clear previous status

		// Capture username for use in async callback
		final String finalUsername = username;

		// Perform async lookup using a simple HTTP request
		java.util.concurrent.CompletableFuture.supplyAsync(() -> {
			try {
				// Simple HTTP request to RuneScape hiscores API
				String url = "https://secure.runescape.com/m=hiscore_oldschool/index_lite.ws?player=" +
						java.net.URLEncoder.encode(finalUsername, "UTF-8");

				java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(url)
						.openConnection();
				connection.setRequestMethod("GET");
				connection.setConnectTimeout(5000);
				connection.setReadTimeout(5000);

				if (connection.getResponseCode() == 200) {
					try (java.io.BufferedReader reader = new java.io.BufferedReader(
							new java.io.InputStreamReader(connection.getInputStream()))) {

						String line;
						int skillIndex = 0;
						while ((line = reader.readLine()) != null && skillIndex <= 6) { // Prayer is index 6
							if (skillIndex == 6) { // Prayer skill
								String[] parts = line.split(",");
								if (parts.length >= 3) {
									return Long.parseLong(parts[2]); // XP is the third value
								}
							}
							skillIndex++;
						}
					}
				}
				return -1L; // Error case
			} catch (Exception e) {
				log.error("Error fetching hiscore data", e);
				return -1L;
			}
		}).whenCompleteAsync((prayerXP, ex) -> javax.swing.SwingUtilities.invokeLater(() -> {
			// Reset field state
			hiscoreLookupField.setEditable(true);

			if (prayerXP == null || prayerXP == -1L || ex != null) {
				hiscoreLookupField.setIcon(IconTextField.Icon.ERROR);
				debugStatusLabel.setText("<html></html>"); // Clear status on error
				return;
			}

			// Success - populate the current XP field
			hiscoreLookupField.setIcon(IconTextField.Icon.SEARCH);
			try {
				int prayerXPInt = (int) Math.min(prayerXP, Integer.MAX_VALUE);
				int prayerLevel = net.runelite.api.Experience.getLevelForXp(prayerXPInt);

				// Calculate target: next level for most players, or 200M XP for high-level
				// players
				int targetLevel;
				int targetXP;

				if (prayerLevel >= 99) {
					// For level 99+ players, default to 200M XP goal
					targetXP = net.runelite.api.Experience.MAX_SKILL_XP;
					targetLevel = net.runelite.api.Experience.getLevelForXp(targetXP);
				} else {
					// For lower level players, target next level
					targetLevel = Math.min(prayerLevel + 1, net.runelite.api.Experience.MAX_VIRT_LEVEL);
					targetXP = net.runelite.api.Experience.getXpForLevel(targetLevel);
				}

				// Update current XP and level fields
				setCurrentXPInput(String.format("%,d", prayerXPInt));
				setCurrentLevelInput(prayerLevel);

				// Update target XP and level fields
				setTargetLevelInput(targetLevel);
				setTargetXPInput(String.format("%,d", targetXP));

				// Update calculations
				updateBoneShardsRequired();

				// Show success status message with word wrap
				debugStatusLabel.setText(
						"<html>Current/target fields populated using Prayer level lookup for \"" + finalUsername
								+ "\"</html>");

			} catch (Exception e) {
				log.error("Error processing hiscore result", e);
				hiscoreLookupField.setIcon(IconTextField.Icon.ERROR);
			}
		}));
	}

	public void setDebugMode(boolean debugMode, BoneShardHelperPanel parentPanel) {
		if (debugSection != null) {
			debugSection.setVisible(debugMode);
			revalidate();
			repaint();
		}
	}

}