package com.boneshardhelper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Map;
import java.util.regex.Pattern;
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
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.FlatTextField;

@Getter
class ResourceModePanel extends JPanel {
	private static final Pattern NON_NUMERIC = Pattern.compile("\\D");

	private final JTextField uiFieldCurrentLevel;
	private final JTextField uiFieldCurrentXP;
	private final JCheckBox uiCheckboxSunfireWine;
	private final JCheckBox uiCheckboxZealotRobes;
	private final JButton uiButtonScanResources;

	// Reference to parent panel for resource scanning
	private BoneShardHelperPanel parentPanel;

	// Config reference for debug mode
	private BoneShardHelperConfig config;

	// Debug section (only visible when debug mode is enabled)
	private FoldingSection debugSection;
	private JTextField uiFieldDebugShardOverride;
	
	// Store the last scanned shard total for debug override functionality
	private int lastScannedShardTotal = 0;

	// Item manager for icons
	private net.runelite.client.game.ItemManager itemManager;
	private JTable referenceTable;
	private final JTable resourceBreakdownTable;
	private final JLabel totalShardsLabel;
	private final JLabel totalXPValueLabel;
	private final JLabel totalWineLabel;
	private final JLabel achievableLevelLabel;
	private final JLabel zealotRobesWarningLabel;
	private final JLabel debugStatusLabel;

	ResourceModePanel(BoneShardHelperConfig config) {
		this.config = config;
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Create top panel for input fields and controls
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		topPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

		// Create main grid panel with BorderLayout to stack components
		JPanel gridPanel = new JPanel(new BorderLayout());
		gridPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Create fields panel with 1x2 grid layout for level/XP fields only
		JPanel fieldsPanel = new JPanel(new GridLayout(1, 2, 7, 7));
		fieldsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Input fields (shared with Goal Mode)
		uiFieldCurrentLevel = addComponent(fieldsPanel, "Current Level");
		uiFieldCurrentXP = addComponent(fieldsPanel, "Current Experience");

		// Create checkboxes panel with vertical layout
		JPanel checkboxPanel = new JPanel(new GridLayout(2, 1, 0, 5));
		checkboxPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		checkboxPanel.setBorder(new EmptyBorder(10, 0, 0, 0)); // Add some top spacing

		// Checkboxes stacked vertically (shared with Goal Mode)
		uiCheckboxSunfireWine = addCheckboxComponent(checkboxPanel, "Sunfire Wine (6 XP/shard)");
		uiCheckboxZealotRobes = addCheckboxComponent(checkboxPanel, "Zealot's Robes");

		uiButtonScanResources = createStyledScanButton("Scan Inventory");

		debugStatusLabel = new JLabel("Status: Ready to scan");
		debugStatusLabel.setForeground(Color.YELLOW);
		debugStatusLabel.setFont(FontManager.getRunescapeSmallFont());

		// Create a panel to hold just checkboxes (no button/status)
		JPanel checkboxAndFieldsPanel = new JPanel(new BorderLayout());
		checkboxAndFieldsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		checkboxAndFieldsPanel.add(fieldsPanel, BorderLayout.CENTER);
		checkboxAndFieldsPanel.add(checkboxPanel, BorderLayout.SOUTH);

		// Add panels to grid panel
		gridPanel.add(checkboxAndFieldsPanel, BorderLayout.CENTER);

		// Add grid panel to top panel
		topPanel.add(gridPanel, BorderLayout.CENTER);

		// Create collapsible reference table section
		FoldingSection collapsibleReferenceSection = createCollapsibleReferenceSection();

		// Create resource breakdown table (initially empty)
		resourceBreakdownTable = createResourceBreakdownTable();
		JScrollPane breakdownScrollPane = new JScrollPane(resourceBreakdownTable);
		breakdownScrollPane.setPreferredSize(new Dimension(0, 100));
		breakdownScrollPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		// Initialize calculation result labels
		totalShardsLabel = new JLabel("Scan inventory to see results");
		totalXPValueLabel = new JLabel("Scan inventory to see results");
		totalWineLabel = new JLabel("Scan inventory to see results");
		achievableLevelLabel = new JLabel("Scan inventory to see results");

		// Create Goal Mode-style results section
		FoldingSection resultsSection = createCalculationResultsSection();

		// Create warning panel below the results section
		JPanel warningPanel = new JPanel(new BorderLayout());
		warningPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		warningPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

		zealotRobesWarningLabel = new JLabel("<html>&nbsp;</html>");
		zealotRobesWarningLabel.setForeground(Color.ORANGE);
		zealotRobesWarningLabel.setFont(FontManager.getRunescapeSmallFont());
		// Allow natural height for proper text wrapping

		warningPanel.add(zealotRobesWarningLabel, BorderLayout.CENTER);

		// Combine results section and warning panel
		JPanel resultsAndWarningPanel = new JPanel(new BorderLayout());
		resultsAndWarningPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		resultsAndWarningPanel.add(resultsSection, BorderLayout.NORTH);
		resultsAndWarningPanel.add(warningPanel, BorderLayout.CENTER);

		// Create inventory resources folding section
		FoldingSection inventoryResourcesSection = createInventoryResourcesSection(breakdownScrollPane);

		// Since we only have the inventory resources section now, use it directly
		JPanel tablesPanel = inventoryResourcesSection;

		// Create scan button panel (centered, with spacing)
		JPanel scanButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		scanButtonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scanButtonPanel.setBorder(new EmptyBorder(10, 0, 5, 0)); // Top and bottom spacing
		scanButtonPanel.add(uiButtonScanResources);

		// Create status panel (centered, below button)
		JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		statusPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		statusPanel.setBorder(new EmptyBorder(0, 0, 5, 0)); // Bottom spacing
		statusPanel.add(debugStatusLabel);

		// Create combined button and status panel
		JPanel buttonAndStatusPanel = new JPanel(new BorderLayout());
		buttonAndStatusPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		buttonAndStatusPanel.add(scanButtonPanel, BorderLayout.NORTH);
		buttonAndStatusPanel.add(statusPanel, BorderLayout.CENTER);

		// Create middle section with button/status and tables
		JPanel middleSection = new JPanel(new BorderLayout());
		middleSection.setBackground(ColorScheme.DARK_GRAY_COLOR);
		middleSection.add(buttonAndStatusPanel, BorderLayout.NORTH);
		middleSection.add(tablesPanel, BorderLayout.CENTER);

		// Create debug section (only if debug mode is enabled)
		debugSection = null;
		if (config.debugMode()) {
			debugSection = createDebugSection();
		}

		// Create bottom panel for reference section and optional debug section
		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		bottomPanel.add(collapsibleReferenceSection, BorderLayout.NORTH);

		if (debugSection != null) {
			bottomPanel.add(debugSection, BorderLayout.CENTER);
		}

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		centerPanel.add(resultsAndWarningPanel, BorderLayout.NORTH);
		centerPanel.add(middleSection, BorderLayout.CENTER);
		centerPanel.add(bottomPanel, BorderLayout.SOUTH);

		add(topPanel, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
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

	// Getter methods for UI fields (needed for BoneShardHelperPanel)
	JTextField getUiFieldCurrentLevel() {
		return uiFieldCurrentLevel;
	}

	JTextField getUiFieldCurrentXP() {
		return uiFieldCurrentXP;
	}

	JButton getUiButtonScanResources() {
		return uiButtonScanResources;
	}

	JTable getResourceBreakdownTable() {
		return resourceBreakdownTable;
	}

	// Debug field getter methods (only available when debug mode is enabled)
	JTextField getUiFieldDebugShardOverride() {
		return uiFieldDebugShardOverride;
	}

	FoldingSection getDebugSection() {
		return debugSection;
	}

	int getDebugShardOverrideInput() {
		if (uiFieldDebugShardOverride == null) {
			return 0;
		}
		return getInput(uiFieldDebugShardOverride);
	}

	void setDebugShardOverrideInput(Object value) {
		if (uiFieldDebugShardOverride != null) {
			setInput(uiFieldDebugShardOverride, value);
		}
	}

	boolean isDebugModeEnabled() {
		return config != null && config.debugMode();
	}

	public int getEffectiveTotalShards(int scannedTotalShards) {
		// If debug mode is enabled and debug override field has a non-zero value, use that
		if (isDebugModeEnabled() && uiFieldDebugShardOverride != null) {
			int debugOverride = getDebugShardOverrideInput();
			if (debugOverride > 0) {
				return debugOverride;
			}
		}
		return scannedTotalShards;
	}

	public void triggerDebugRecalculation() {
		// Get the effective shards (debug override if available, otherwise last scanned total)
		int currentXP = getCurrentXPInput();
		boolean useSunfireWine = isSunfireWineSelected();
		
		int effectiveShards = getEffectiveTotalShards(lastScannedShardTotal);
		updateAchievableLevel(effectiveShards, currentXP, useSunfireWine);
		
		updateTotalShardsLabel();
	}


	private void updateTotalShardsLabel() {
		try {
			int effectiveShards = getEffectiveTotalShards(lastScannedShardTotal);
			
			// Show different text based on whether debug override is active
			if (isDebugModeEnabled() && uiFieldDebugShardOverride != null && getDebugShardOverrideInput() > 0) {
				totalShardsLabel.setText(String.format("%,d shard value (DEBUG OVERRIDE)", effectiveShards));
			} else {
				// If no debug override, show the last scanned value or prompt to scan
				if (resourceBreakdownTable.getRowCount() > 0 || lastScannedShardTotal > 0) {
					totalShardsLabel.setText(String.format("%,d shard value in inventory", effectiveShards));
				} else {
					totalShardsLabel.setText("Scan inventory to see results");
				}
			}
		} catch (Exception e) {
			totalShardsLabel.setText("Calculation error");
		}
	}

	public void clearDebugOverride() {
		// Clear the debug shard override value and trigger recalculation.
		// Reverts to using scanned inventory values.
		if (uiFieldDebugShardOverride != null) {
			uiFieldDebugShardOverride.setText("");
			triggerDebugRecalculation();
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

	private static JButton createStyledScanButton(String buttonText) {
		JButton button = new JButton(buttonText);
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		button.setForeground(Color.WHITE);
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setBorder(new EmptyBorder(5, 10, 5, 10));
		button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

		// Add hover effect for better UX
		button.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseEntered(java.awt.event.MouseEvent e) {
				button.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e) {
				button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		});

		return button;
	}

	private FoldingSection createCalculationResultsSection() {
		// Creates a calculation results section with labels for total shards, XP, wine,
		// 		and achievable level enclosed in a FoldingSection.
		// Create a vertical panel for all the labels
		JPanel labelsPanel = new JPanel();
		labelsPanel.setLayout(new java.awt.GridLayout(4, 1, 0, 2)); // 4 rows, 1 column, 2px vertical gap
		labelsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Total Shard Value label with blessed bone shards icon
		totalShardsLabel.setText("Scan inventory to see results");
		totalShardsLabel.setForeground(Color.WHITE);
		totalShardsLabel.setFont(FontManager.getRunescapeSmallFont());
		labelsPanel.add(totalShardsLabel);

		// Total XP Value label with XP icon
		totalXPValueLabel.setText("Scan inventory to see results");
		totalXPValueLabel.setForeground(Color.WHITE);
		totalXPValueLabel.setFont(FontManager.getRunescapeSmallFont());
		labelsPanel.add(totalXPValueLabel);

		// Total Wine label with wine icon
		totalWineLabel.setText("Scan inventory to see results");
		totalWineLabel.setForeground(Color.WHITE);
		totalWineLabel.setFont(FontManager.getRunescapeSmallFont());
		labelsPanel.add(totalWineLabel);

		// Achievable Level label with Prayer skill icon
		achievableLevelLabel.setText("Scan inventory to see results");
		achievableLevelLabel.setForeground(Color.WHITE);
		achievableLevelLabel.setFont(FontManager.getRunescapeSmallFont());
		labelsPanel.add(achievableLevelLabel);

		// Create content panel to hold the labels
		JPanel contentPanel = new JPanel(new java.awt.BorderLayout());
		contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentPanel.add(labelsPanel, java.awt.BorderLayout.WEST);

		// Create the folding section
		FoldingSection section = new FoldingSection(
				"Calculation Results",
				"Summary of your inventory's shard value and achievable prayer level",
				contentPanel);

		// Set to expanded by default
		section.setOpen(true);

		return section;
	}

	private JTable createReferenceTable() {
		String[] columnNames = { "Bone Type", "Shards" };
		DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false; // Make table read-only
			}
		};

		java.util.List<BoneType> boneTypesList = new java.util.ArrayList<>();

		java.util.Set<BoneType> processedTypes = new java.util.HashSet<>();

		for (BoneType boneType : BoneType.values()) {
			// Skip bone shards themselves
			if (boneType == BoneType.BLESSED_BONE_SHARDS) {
				continue;
			}

			// Get the consolidated bone type
			BoneType consolidatedType = getConsolidatedBoneType(boneType);

			// Skip if we've already processed this consolidated type (necessary in some conditions)
			if (processedTypes.contains(consolidatedType)) {
				continue;
			}

			processedTypes.add(consolidatedType);
			boneTypesList.add(consolidatedType);

			model.addRow(new Object[] { consolidatedType.getDisplayName(), consolidatedType.getShardValue() });
		}

		JTable table = new JTable(model);
		table.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		table.setForeground(Color.WHITE);
		table.setFont(FontManager.getRunescapeSmallFont());
		table.setGridColor(ColorScheme.DARK_GRAY_COLOR);
		table.setSelectionBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
		table.setSelectionForeground(Color.WHITE);
		table.getTableHeader().setBackground(ColorScheme.DARKER_GRAY_COLOR);
		table.getTableHeader().setForeground(Color.WHITE);
		table.getTableHeader().setFont(FontManager.getRunescapeSmallFont());

		// Set custom cell renderer for the first column (Bone Type) to display icons
		table.getColumnModel().getColumn(0).setCellRenderer(new BoneTypeIconRenderer(boneTypesList));

		// Set fixed row height to prevent stretching from icons
		table.setRowHeight(20); // Consistent with other tables

		// Configure column widths and alignment (80% / 20%)
		setupReferenceTableColumns(table);

		return table;
	}

	private FoldingSection createInventoryResourcesSection(JScrollPane breakdownScrollPane) {
		// Creates the inventory resources folding section.
		// Create the content components
		JPanel content = createInventoryResourcesContent(breakdownScrollPane);

		// Create the folding section with the content
		FoldingSection section = new FoldingSection(
				"Inventory Resources",
				"Breakdown of bone types and their shard values found in your inventory",
				content);

		// Set to expanded by default
		section.setOpen(true);

		return section;
	}

	private JPanel createInventoryResourcesContent(JScrollPane breakdownScrollPane) {
		// Creates the content for the Inventory Resources section.
		JPanel content = new JPanel(new BorderLayout());
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		content.setBorder(new EmptyBorder(5, 10, 5, 5)); // Indent content

		content.add(breakdownScrollPane, BorderLayout.CENTER);

		return content;
	}

	private JTable createResourceBreakdownTable() {
		String[] columnNames = { "Qty", "Bone Type", "# Shards" };
		DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false; // Make table read-only
			}
		};

		JTable table = new JTable(model);
		table.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		table.setForeground(Color.WHITE);
		table.setFont(FontManager.getRunescapeSmallFont());
		table.setGridColor(ColorScheme.DARK_GRAY_COLOR);
		table.setSelectionBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
		table.setSelectionForeground(Color.WHITE);
		table.getTableHeader().setBackground(ColorScheme.DARKER_GRAY_COLOR);
		table.getTableHeader().setForeground(Color.WHITE);
		table.getTableHeader().setFont(FontManager.getRunescapeSmallFont());

		javax.swing.table.DefaultTableCellRenderer rightRenderer = new javax.swing.table.DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		rightRenderer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		rightRenderer.setForeground(Color.WHITE);
		rightRenderer.setFont(FontManager.getRunescapeSmallFont());
		table.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);

		table.getColumnModel().getColumn(1).setCellRenderer(new ResourceBreakdownIconRenderer());

		setupResourceBreakdownTableColumns(table);

		return table;
	}

	private void setupResourceBreakdownTableColumns(JTable table) {
		// Configures column widths and alignment for the resource breakdown table.
		table.getColumnModel().getColumn(0).setPreferredWidth(45); // Qty - 15%
		table.getColumnModel().getColumn(1).setPreferredWidth(180); // Bone Type - 60% (widest)
		table.getColumnModel().getColumn(2).setPreferredWidth(75); // Shard Value - 25%

		table.getColumnModel().getColumn(0).setMinWidth(35); // Qty minimum
		table.getColumnModel().getColumn(1).setMinWidth(100); // Bone Type minimum
		table.getColumnModel().getColumn(2).setMinWidth(55); // Shard Value minimum

		table.getColumnModel().getColumn(0).setMaxWidth(60);
	}

	public void updateResourceBreakdown(Map<BoneType, Integer> boneResources) {
		// Updates the resource breakdown table with scanned bone resources.
		try {
			if (boneResources == null) {
				updateDebugError("Bone resources data is null");
				return;
			}

			DefaultTableModel model = (DefaultTableModel) resourceBreakdownTable.getModel();
			model.setRowCount(0); // Clear existing data

			long totalShardsLong = 0;
			int validEntries = 0;

			for (Map.Entry<BoneType, Integer> entry : boneResources.entrySet()) {
				try {
					BoneType boneType = entry.getKey();
					Integer quantityObj = entry.getValue();

					if (boneType == null || quantityObj == null) {
						continue; // Skip invalid entries
					}

					int quantity = quantityObj;
					if (quantity <= 0) {
						continue; // Skip zero or negative quantities
					}

					// Calculate shard value with overflow protection
					long shardValueLong = (long) boneType.getShardValue() * quantity;
					if (shardValueLong > Integer.MAX_VALUE) {
						updateDebugError("Shard calculation overflow for " + boneType.getDisplayName());
						continue;
					}

					int shardValue = (int) shardValueLong;

					// Check for total overflow before adding
					if (totalShardsLong > Long.MAX_VALUE - shardValue) {
						updateDebugError("Total shards calculation overflow");
						break;
					}

					totalShardsLong += shardValue;
					validEntries++;

					model.addRow(new Object[] {
							String.format("%,d", quantity),
							boneType.getDisplayName(),
							String.format("%,d", shardValue)
					});

				} catch (Exception e) {
					// Skip this entry and continue with others
					System.err.println("Prayer Calculator: Error processing bone entry - " + e.getMessage());
				}
			}

			// Update total shards label with bounds checking
			if (totalShardsLong > Integer.MAX_VALUE) {
				totalShardsLabel.setText("Too many shards");
				updateDebugError("Total shards exceed maximum calculable amount");
			} else {
				int scannedShards = (int) totalShardsLong;
				
				// Store the scanned total for debug override functionality
				lastScannedShardTotal = scannedShards;
				
				int effectiveShards = getEffectiveTotalShards(scannedShards);
				
				// Show different text based on whether debug override is active
				if (isDebugModeEnabled() && uiFieldDebugShardOverride != null && getDebugShardOverrideInput() > 0) {
					totalShardsLabel.setText(String.format("%,d shard value (DEBUG OVERRIDE)", effectiveShards));
				} else {
					totalShardsLabel.setText(String.format("%,d shard value in inventory", effectiveShards));
				}

				if (validEntries == 0 && effectiveShards == 0) {
					updateDebugStatus("No valid bone resources found");
				}
			}

		} catch (Exception e) {
			System.err.println("Prayer Calculator: Error updating resource breakdown - " + e.getMessage());
			updateDebugError("Error updating resource breakdown: " + e.getMessage());
			totalShardsLabel.setText("Calculation error");
		}
	}

	public void updateAchievableLevel(int totalShards, int currentXP, boolean useSunfireWine) {
		// Updates the achievable level display based on available shards and wine type.
		try {
			// Validate inputs
			if (totalShards < 0) {
				updateDebugError("Invalid shard count: " + totalShards);
				return;
			}

			if (currentXP < 0) {
				updateDebugError("Invalid current XP: " + currentXP);
				return;
			}

			// Calculate XP per shard including zealot robes multiplier
			boolean useZealotRobes = isZealotRobesSelected();
			double baseXpPerShard = useSunfireWine ? 6.0 : 5.0;
			double xpPerShard = useZealotRobes ? baseXpPerShard * 1.05 : baseXpPerShard;

			// Calculate total XP gain with overflow protection
			long totalXPGainLong = (long) Math.round(totalShards * xpPerShard);
			if (totalXPGainLong > Integer.MAX_VALUE) {
				updateDebugError("XP calculation overflow - too many shards");
				totalXPValueLabel.setText("Calculation overflow");
				achievableLevelLabel.setText("Too many shards");
				return;
			}

			int totalXPGain = (int) totalXPGainLong;

			// Calculate final XP with overflow protection
			long finalXPLong = (long) currentXP + totalXPGain;
			if (finalXPLong > Integer.MAX_VALUE) {
				updateDebugError("Final XP calculation overflow");
				totalXPValueLabel.setText("Calculation overflow");
				achievableLevelLabel.setText("XP overflow");
				return;
			}

			int finalXP = (int) finalXPLong;

			int achievableLevel;
			try {
				achievableLevel = net.runelite.api.Experience.getLevelForXp(finalXP);

				// Validate the result
				if (achievableLevel < 1 || achievableLevel > 126) {
					updateDebugError("Calculated level out of bounds: " + achievableLevel);
					achievableLevel = Math.max(1, Math.min(126, achievableLevel));
				}
			} catch (Exception e) {
				updateDebugError("Error calculating level from XP: " + e.getMessage());
				achievableLevel = net.runelite.api.Experience.getLevelForXp(currentXP); // Fallback to current level
			}

			// Update displays with improved format
			String wineType = useSunfireWine ? "sunfire" : "regular";
			String robesText = useZealotRobes ? ", zealot's robes" : "";
			totalXPValueLabel.setText(String.format("%,d XP (%s wine%s)",
					totalXPGain, wineType, robesText));

			// Calculate wine requirements using existing calculation engine (400 shards per wine)
			PrayerCalculationEngine calculationEngine = new PrayerCalculationEngine();
			int winesNeeded = calculationEngine.calculateWinesNeeded(totalShards);
			String wineTypeCapitalized = useSunfireWine ? "Sunfire wine" : "Regular wine";
			totalWineLabel.setText(String.format("%,d %s", winesNeeded, wineTypeCapitalized.toLowerCase()));

			// Update wine icon based on current wine type
			if (itemManager != null) {
				try {
					int wineItemId = useSunfireWine ? 29384 : 1993; // Sunfire wine or regular wine
					totalWineLabel.setIcon(null);
					itemManager.getImage(wineItemId).addTo(totalWineLabel);
				} catch (Exception e) {
					System.err.println("Error loading wine icon: " + e.getMessage());
				}
			}

			// Calculate level gain and format with color
			int currentLevel = net.runelite.api.Experience.getLevelForXp(currentXP);
			int levelGain = achievableLevel - currentLevel;

			String levelGainText;
			if (levelGain <= 0) {
				levelGainText = "(+0)";
			} else {
				levelGainText = String.format("<font color='#00FF00'>(+%d)</font>", levelGain);
			}

			achievableLevelLabel.setText(String.format("<html>Ending Level: %d %s</html>",
					achievableLevel, levelGainText));

			// Update zealot robes warning
			updateZealotRobesWarning();

		} catch (Exception e) {
			System.err.println("Prayer Calculator: Error updating achievable level - " + e.getMessage());
			updateDebugError("Calculation error: " + e.getMessage());
			totalXPValueLabel.setText("Calculation error");
			achievableLevelLabel.setText("Calculation error");
		}
	}

	private void updateZealotRobesWarning() {
		if (isZealotRobesSelected()) {
			zealotRobesWarningLabel.setText(
					"<html>!! Zealot's robes: Actual XP values may vary slightly due to resource-save triggers.</html>");
		} else {
			zealotRobesWarningLabel.setText("<html>&nbsp;</html>"); // Non-breaking space maintains height
		}
	}

	public void updateZealotRobesWarningDisplay() {
		updateZealotRobesWarning();
	}

	public void updateDebugStatus(String status) {
		debugStatusLabel.setText("Status: " + status);
		debugStatusLabel.setForeground(Color.YELLOW);
	}

	public void updateDebugError(String error) {
		// Updates the debug status label with an error message if needed.
		debugStatusLabel.setText("Error: " + error);
		debugStatusLabel.setForeground(Color.RED);
	}

	public void updateDebugSuccess(String success) {
		debugStatusLabel.setText("Success: " + success);
		debugStatusLabel.setForeground(Color.GREEN);
	}


	public void clearResourceBreakdown() {
		// Clears the resource breakdown table and resets labels.
		DefaultTableModel model = (DefaultTableModel) resourceBreakdownTable.getModel();
		model.setRowCount(0);
		
		// Reset the stored scanned total
		lastScannedShardTotal = 0;
		
		totalShardsLabel.setText("Scan inventory to see results");
		totalXPValueLabel.setText("Scan inventory to see results");
		achievableLevelLabel.setText("Scan inventory to see results");
		zealotRobesWarningLabel.setText("<html>&nbsp;</html>"); // Clear warning

		// Clear icons
		totalShardsLabel.setIcon(null);
		totalXPValueLabel.setIcon(null);
		achievableLevelLabel.setIcon(null);
	}

	public void updateCalculations() {
		// Use the existing recalculateWithCurrentSettings method which properly handles debug override
		recalculateWithCurrentSettings();
	}

	public void setItemManager(net.runelite.client.game.ItemManager itemManager) {
		this.itemManager = itemManager;

		if (itemManager != null) {
			loadStaticIcons();
		}
	}


	public void recalculateWithCurrentSettings() {
		// Only recalculate if we have existing data (breakdown table has rows) or debug override is active
		if (resourceBreakdownTable.getRowCount() == 0 && 
			!(isDebugModeEnabled() && uiFieldDebugShardOverride != null && getDebugShardOverrideInput() > 0)) {
			return;
		}

		try {
			// Calculate total shards from the breakdown table (if any)
			int scannedShards = 0;
			if (resourceBreakdownTable.getRowCount() > 0) {
				DefaultTableModel model = (DefaultTableModel) resourceBreakdownTable.getModel();

				for (int i = 0; i < model.getRowCount(); i++) {
					String shardValueStr = (String) model.getValueAt(i, 2); // Shard # column
					// Remove commas and parse
					int shardValue = Integer.parseInt(shardValueStr.replaceAll(",", ""));
					scannedShards += shardValue;
				}
			} else {
				// Use the stored last scanned total if no table data
				scannedShards = lastScannedShardTotal;
			}

			// Get current settings
			int currentXP = getCurrentXPInput();
			boolean useSunfireWine = isSunfireWineSelected();

			// Use effective shards (respects debug override)
			int effectiveShards = getEffectiveTotalShards(scannedShards);
			updateAchievableLevel(effectiveShards, currentXP, useSunfireWine);
			
			// Also update the total shards label to reflect any debug override
			updateTotalShardsLabel();

		} catch (Exception e) {
			System.err.println("Prayer Calculator: Error recalculating with current settings - " + e.getMessage());
			updateDebugError("Error recalculating: " + e.getMessage());
		}
	}

	public void setParentPanel(BoneShardHelperPanel parentPanel) {
		this.parentPanel = parentPanel;
	}

	private void loadStaticIcons() {
		if (itemManager == null) {
			return;
		}

		try {
			// Load blessed bone shards icon immediately (item ID: 29381)
			totalShardsLabel.setIcon(null);
			itemManager.getImage(29381).addTo(totalShardsLabel);

			// Load overall skill icon from resources (for XP label) - use natural size to
			// match bone shard icon
			java.awt.image.BufferedImage overallIcon = net.runelite.client.util.ImageUtil
					.loadImageResource(getClass(), "/skill_icons/overall.png");
			if (overallIcon != null) {
				totalXPValueLabel.setIcon(new javax.swing.ImageIcon(overallIcon));
			}

			// Load wine icon based on current wine type (default to regular wine initially)
			boolean useSunfireWine = isSunfireWineSelected();
			int wineItemId = useSunfireWine ? 29384 : 1993; // Sunfire wine or regular wine
			totalWineLabel.setIcon(null);
			itemManager.getImage(wineItemId).addTo(totalWineLabel);

			// Load prayer skill icon from resources (for level label) - use natural size to
			// match bone shard icon
			java.awt.image.BufferedImage prayerIcon = net.runelite.client.util.ImageUtil
					.loadImageResource(getClass(), "/skill_icons/prayer.png");
			if (prayerIcon != null) {
				achievableLevelLabel.setIcon(new javax.swing.ImageIcon(prayerIcon));
			}
		} catch (Exception e) {
			System.err.println("Error loading static icons: " + e.getMessage());
		}
	}

	private FoldingSection createCollapsibleReferenceSection() {
		// Create reference table first
		referenceTable = createReferenceTable();

		// Create content panel for the reference table
		JPanel content = new JPanel(new BorderLayout());
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		content.setBorder(new EmptyBorder(5, 10, 5, 5)); // Indent content

		// Add descriptive text above the table
		JLabel descriptionLabel = new JLabel(
				"<html>This table shows how many blessed bone shards you get by breaking down various types of bones.</html>");
		descriptionLabel.setForeground(Color.LIGHT_GRAY);
		descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
		descriptionLabel.setBorder(new EmptyBorder(0, 0, 8, 0)); // Add some spacing below the text

		// Create scroll pane for the reference table
		JScrollPane referenceScrollPane = new JScrollPane(referenceTable);
		referenceScrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));

		// Add components to content panel
		content.add(descriptionLabel, BorderLayout.NORTH);
		content.add(referenceScrollPane, BorderLayout.CENTER);

		// Create the folding section with the content
		FoldingSection section = new FoldingSection(
				"Bone Shard Sources",
				"Reference table showing how many blessed bone shards each bone type provides after being broken down",
				content);

		// Set to collapsed by default (matching old behavior)
		section.setOpen(false);

		return section;
	}

	private FoldingSection createDebugSection() {
		// Create content panel for the debug section
		JPanel content = new JPanel(new BorderLayout());
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		content.setBorder(new EmptyBorder(5, 10, 5, 5)); // Indent content

		// Add descriptive text (same style as Bone Shard Sources description)
		JLabel descriptionLabel = new JLabel(
				"<html>Override the scanned bone shard value in inventory</html>");
		descriptionLabel.setForeground(Color.LIGHT_GRAY);
		descriptionLabel.setFont(FontManager.getRunescapeSmallFont());
		descriptionLabel.setBorder(new EmptyBorder(0, 0, 8, 0)); // Add some spacing below the text

		// Create the debug input field using the same style as other input fields
		JPanel inputPanel = new JPanel(new BorderLayout());
		inputPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		inputPanel.setBorder(new EmptyBorder(8, 0, 0, 0)); // Add spacing above the input field

		uiFieldDebugShardOverride = addComponent(inputPanel, "Bone Shard Override");
		
		// Add event listeners to trigger recalculation when debug value changes
		uiFieldDebugShardOverride.addActionListener(e -> triggerDebugRecalculation());
		uiFieldDebugShardOverride.addFocusListener(new java.awt.event.FocusAdapter() {
			@Override
			public void focusLost(java.awt.event.FocusEvent e) {
				triggerDebugRecalculation();
			}
		});

		// Create a container panel to hold both description and input
		JPanel containerPanel = new JPanel(new BorderLayout());
		containerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		containerPanel.add(descriptionLabel, BorderLayout.NORTH);
		containerPanel.add(inputPanel, BorderLayout.CENTER);

		// Add the container to content panel
		content.add(containerPanel, BorderLayout.NORTH);

		// Create the folding section with the content
		FoldingSection section = new FoldingSection(
				"DEBUG: CUSTOM SHARD AMOUNT",
				"Override the scanned bone shard value in inventory for testing purposes",
				content);

		// Set to collapsed by default
		section.setOpen(false);

		return section;
	}

	public void updateDebugSectionVisibility() {
		// Get the bottom panel that contains the reference section and debug section
		JPanel centerPanel = (JPanel) getComponent(1); // The center panel from constructor
		JPanel bottomPanel = (JPanel) centerPanel.getComponent(2); // The bottom panel from constructor
		
		boolean debugModeEnabled = config.debugMode();
		
		if (debugModeEnabled && debugSection == null) {
			// Debug mode was enabled - create and add the debug section
			debugSection = createDebugSection();
			bottomPanel.add(debugSection, BorderLayout.CENTER);
		} else if (!debugModeEnabled && debugSection != null) {
			// Debug mode was disabled - remove the debug section
			bottomPanel.remove(debugSection);
			debugSection = null;
			uiFieldDebugShardOverride = null;
		}
		
		// Refresh the layout
		bottomPanel.revalidate();
		bottomPanel.repaint();
		
		// If debug mode was disabled and we had a debug override active, trigger recalculation
		if (!debugModeEnabled) {
			triggerDebugRecalculation();
		}
	}

	private int getBoneItemId(BoneType boneType) {
		// Map blessed bone types to their unblessed item IDs for icon display
		switch (boneType) {
			case BLESSED_BONES:
				return 526; // Regular bones
			case BLESSED_BAT_BONES:
				return 530; // Bat bones
			case BLESSED_BIG_BONES:
				return 532; // Big bones
			case BLESSED_BABYDRAGON_BONES:
				return 534; // Babydragon bones
			case BLESSED_DRAGON_BONES:
				return 536; // Dragon bones
			case BLESSED_WYVERN_BONES:
				return 6812; // Wyvern bones
			case BLESSED_DRAKE_BONES:
				return 22783; // Drake bones
			case BLESSED_FAYRG_BONES:
				return 4830; // Fayrg bones
			case BLESSED_LAVA_DRAGON_BONES:
				return 11943; // Lava dragon bones
			case BLESSED_RAURG_BONES:
				return 4832; // Raurg bones
			case BLESSED_DAGANNOTH_BONES:
				return 6729; // Dagannoth bones
			case BLESSED_OURG_BONES:
				return 4834; // Ourg bones
			case BLESSED_SUPERIOR_DRAGON_BONES:
				return 22124; // Superior dragon bones
			case BLESSED_BABYWYRM_BONES:
				return 28899; // Babywyrm bones (wyrmling bones)
			case BLESSED_WYRM_BONES:
				return 22780; // Wyrm bones
			case BLESSED_HYDRA_BONES:
				return 22786; // Hydra bones
			case BLESSED_ZOGRE_BONES:
				return 4812; // Zogre bones
			case BLESSED_BONE_STATUETTE0:
			case BLESSED_BONE_STATUETTE1:
			case BLESSED_BONE_STATUETTE2:
			case BLESSED_BONE_STATUETTE3:
			case BLESSED_BONE_STATUETTE4:
				return 29338; // Use blessed bone statuette icon (no unblessed version)
			case BLESSED_BONE_SHARDS:
				return 29381; // Bone shards (no unblessed version)
			case SUN_KISSED_BONES:
				return 29380; // Sun-kissed bones (no unblessed version)
			default:
				// For unblessed bones and other types I may have missed, use regular version
				return boneType.getBaseItemId();
		}
	}

	private void setupReferenceTableColumns(JTable table) {
		if (table.getColumnCount() >= 2) {
			table.getColumnModel().getColumn(0).setPreferredWidth(225); // Bone Type - 75%
			table.getColumnModel().getColumn(1).setPreferredWidth(75); // Shard Value - 25%

			table.getColumnModel().getColumn(0).setMinWidth(150); // Bone Type minimum
			table.getColumnModel().getColumn(1).setMinWidth(50); // Shard Value minimum

			table.getColumnModel().getColumn(0).setMaxWidth(250); // Bone Type maximum
			table.getColumnModel().getColumn(1).setMaxWidth(100); // Shard Value maximum

			table.getTableHeader().setResizingAllowed(false);
			table.getTableHeader().setReorderingAllowed(false);

			javax.swing.table.DefaultTableCellRenderer rightRenderer = new javax.swing.table.DefaultTableCellRenderer();
			rightRenderer.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
			rightRenderer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			rightRenderer.setForeground(Color.WHITE);

			table.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);
		}
	}

	private BoneType getConsolidatedBoneType(BoneType boneType) {
		// Consolidate variations
		switch (boneType) {
			case BLESSED_BONE_STATUETTE1:
			case BLESSED_BONE_STATUETTE2:
			case BLESSED_BONE_STATUETTE3:
			case BLESSED_BONE_STATUETTE4:
				return BoneType.BLESSED_BONE_STATUETTE0;
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

	private class BoneTypeIconRenderer extends javax.swing.table.DefaultTableCellRenderer {
		private final java.util.List<BoneType> boneTypes;
		private static final int MAX_ICON_SIZE = 16; // Maximum icon size in pixels

		public BoneTypeIconRenderer(java.util.List<BoneType> boneTypes) {
			this.boneTypes = boneTypes;
		}

		@Override
		public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {

			// Get the default renderer component
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			// Set the background and foreground colors to match the table
			if (isSelected) {
				setBackground(table.getSelectionBackground());
				setForeground(table.getSelectionForeground());
			} else {
				setBackground(table.getBackground());
				setForeground(table.getForeground());
			}

			// Set the font
			setFont(table.getFont());

			// Add icon if we have a valid row and ItemManager is available
			if (row >= 0 && row < boneTypes.size() && itemManager != null) {
				BoneType boneType = boneTypes.get(row);
				try {
					int itemId = getBoneItemId(boneType);
					if (itemId > 0) {
						// Clear any existing icon first
						setIcon(null);
						// Load and scale the icon using ItemManager
						loadScaledIcon(itemId);
					}
				} catch (Exception e) {
					System.err.println("Error loading icon for " + boneType.getDisplayName() + ": " + e.getMessage());
					setIcon(null);
				}
			} else {
				setIcon(null);
			}

			return this;
		}

		private void loadScaledIcon(int itemId) {
			try {
				// Clear any existing icon first
				setIcon(null);
				// Load the icon using ItemManager
				itemManager.getImage(itemId).addTo(this);
				// Scale the icon after it's loaded
				scaleIconToMaxSize(MAX_ICON_SIZE);
			} catch (Exception e) {
				System.err.println("Error loading icon for item " + itemId + ": " + e.getMessage());
				setIcon(null);
			}
		}

		private void scaleIconToMaxSize(int maxSize) {
			javax.swing.Icon currentIcon = getIcon();
			if (currentIcon == null) {
				return;
			}

			int iconWidth = currentIcon.getIconWidth();
			int iconHeight = currentIcon.getIconHeight();

			// Only scale if the icon is larger than maxSize
			if (iconWidth <= maxSize && iconHeight <= maxSize) {
				return;
			}

			try {
				// Calculate scaling factor to maintain aspect ratio
				double scaleX = (double) maxSize / iconWidth;
				double scaleY = (double) maxSize / iconHeight;
				double scale = Math.min(scaleX, scaleY);

				int newWidth = (int) (iconWidth * scale);
				int newHeight = (int) (iconHeight * scale);

				// Create scaled image if the icon is an ImageIcon
				if (currentIcon instanceof javax.swing.ImageIcon) {
					javax.swing.ImageIcon imageIcon = (javax.swing.ImageIcon) currentIcon;
					java.awt.Image originalImage = imageIcon.getImage();
					java.awt.Image scaledImage = originalImage.getScaledInstance(newWidth, newHeight,
							java.awt.Image.SCALE_SMOOTH);
					setIcon(new javax.swing.ImageIcon(scaledImage));
				}
			} catch (Exception e) {
				System.err.println("Error scaling icon: " + e.getMessage());
			}
		}

	}

	private class ResourceBreakdownIconRenderer extends javax.swing.table.DefaultTableCellRenderer {
		private static final int MAX_ICON_SIZE = 16; // Maximum icon size in pixels

		@Override
		public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {

			// Get the default renderer component
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			// Set the background and foreground colors to match the table
			if (isSelected) {
				setBackground(table.getSelectionBackground());
				setForeground(table.getSelectionForeground());
			} else {
				setBackground(table.getBackground());
				setForeground(table.getForeground());
			}

			// Set the font
			setFont(table.getFont());

			// Add icon if we have a valid value and ItemManager is available
			if (value != null && itemManager != null) {
				String boneTypeName = value.toString();
				try {
					// Find the bone type by display name
					BoneType boneType = findBoneTypeByDisplayName(boneTypeName);
					if (boneType != null) {
						int itemId = getBoneItemId(boneType);
						if (itemId > 0) {
							// Clear any existing icon first
							setIcon(null);
							// Load and scale the icon using ItemManager
							loadScaledIcon(itemId);
						}
					}
				} catch (Exception e) {
					System.err.println("Error loading icon for " + boneTypeName + ": " + e.getMessage());
					setIcon(null);
				}
			} else {
				setIcon(null);
			}

			return this;
		}

		private BoneType findBoneTypeByDisplayName(String displayName) {
			for (BoneType boneType : BoneType.values()) {
				if (boneType.getDisplayName().equals(displayName)) {
					return boneType;
				}
			}
			return null;
		}

		private void loadScaledIcon(int itemId) {
			try {
				// Clear any existing icon first
				setIcon(null);
				// Load the icon using ItemManager
				itemManager.getImage(itemId).addTo(this);
				// Scale the icon after it's loaded
				scaleIconToMaxSize(MAX_ICON_SIZE);
			} catch (Exception e) {
				System.err.println("Error loading icon for item " + itemId + ": " + e.getMessage());
				setIcon(null);
			}
		}

		private void scaleIconToMaxSize(int maxSize) {
			javax.swing.Icon currentIcon = getIcon();
			if (currentIcon == null) {
				return;
			}

			int iconWidth = currentIcon.getIconWidth();
			int iconHeight = currentIcon.getIconHeight();

			// Only scale if the icon is larger than maxSize
			if (iconWidth <= maxSize && iconHeight <= maxSize) {
				return;
			}

			try {
				// Calculate scaling factor to maintain aspect ratio
				double scaleX = (double) maxSize / iconWidth;
				double scaleY = (double) maxSize / iconHeight;
				double scale = Math.min(scaleX, scaleY);

				int newWidth = (int) (iconWidth * scale);
				int newHeight = (int) (iconHeight * scale);

				// Create scaled image if the icon is an ImageIcon
				if (currentIcon instanceof javax.swing.ImageIcon) {
					javax.swing.ImageIcon imageIcon = (javax.swing.ImageIcon) currentIcon;
					java.awt.Image originalImage = imageIcon.getImage();
					java.awt.Image scaledImage = originalImage.getScaledInstance(newWidth, newHeight,
							java.awt.Image.SCALE_SMOOTH);
					setIcon(new javax.swing.ImageIcon(scaledImage));
				}
			} catch (Exception e) {
				System.err.println("Error scaling icon: " + e.getMessage());
			}
		}
	}

}