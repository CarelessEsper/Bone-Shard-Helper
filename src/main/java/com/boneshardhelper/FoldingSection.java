package com.boneshardhelper;

import com.google.common.collect.ImmutableList;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Collection;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import lombok.Getter;
import net.runelite.client.plugins.config.ConfigPlugin;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

// RuneLite's ConfigPanel Section, reimplemented
public class FoldingSection extends JPanel
{
    private static final ImageIcon SECTION_EXPAND_ICON;
    private static final ImageIcon SECTION_EXPAND_ICON_HOVER;
    private static final ImageIcon SECTION_RETRACT_ICON;
    private static final ImageIcon SECTION_RETRACT_ICON_HOVER;
    static
    {
        BufferedImage sectionRetractIcon = ImageUtil.loadImageResource(ConfigPlugin.class, "/util/arrow_right.png");
        sectionRetractIcon = ImageUtil.luminanceOffset(sectionRetractIcon, -121);
        SECTION_EXPAND_ICON = new ImageIcon(sectionRetractIcon);
        SECTION_EXPAND_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(sectionRetractIcon, -100));
        final BufferedImage sectionExpandIcon = ImageUtil.rotateImage(sectionRetractIcon, Math.PI / 2);
        SECTION_RETRACT_ICON = new ImageIcon(sectionExpandIcon);
        SECTION_RETRACT_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(sectionExpandIcon, -100));
    }
    private final JButton sectionToggle;
    private final JPanel sectionContents;

    @Getter
    private boolean isOpen = true;

    public FoldingSection(final String header, final String description, JComponent... components)
{
    this(header, description, ImmutableList.copyOf(components));
}

public FoldingSection(final String header, final String description, Collection<JComponent> components)
{
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    this.setMinimumSize(new Dimension(PluginPanel.PANEL_WIDTH, 0));

    final JPanel sectionHeader = new JPanel();
    sectionHeader.setLayout(new BorderLayout());
    sectionHeader.setMinimumSize(new Dimension(PluginPanel.PANEL_WIDTH, 0));
    sectionHeader.setBorder(new CompoundBorder(
        new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
        new EmptyBorder(0, 0, 3, 1)));
    this.add(sectionHeader, BorderLayout.NORTH);

    sectionToggle = new JButton();
    sectionToggle.setIcon(isOpen ? SECTION_RETRACT_ICON : SECTION_EXPAND_ICON);
    sectionToggle.setRolloverIcon(isOpen ? SECTION_RETRACT_ICON_HOVER : SECTION_EXPAND_ICON_HOVER);
    sectionToggle.setPreferredSize(new Dimension(18, 0));
    sectionToggle.setBorder(new EmptyBorder(0, 0, 0, 5));
    sectionToggle.setToolTipText(isOpen ? "Retract" : "Expand");
    SwingUtil.removeButtonDecorations(sectionToggle);
    sectionHeader.add(sectionToggle, BorderLayout.WEST);

    final JLabel sectionName = new JLabel(header);
    sectionName.setForeground(ColorScheme.BRAND_ORANGE);
    sectionName.setFont(FontManager.getRunescapeBoldFont());
    if (description != null)
    {
        sectionName.setToolTipText("<html>" + header + ":<br>" + description + "</html>");
    }
    sectionHeader.add(sectionName, BorderLayout.CENTER);

    sectionContents = new JPanel();
    sectionContents.setLayout(new DynamicGridLayout(0, 1, 0, 5));
    sectionContents.setMinimumSize(new Dimension(PluginPanel.PANEL_WIDTH, 0));
    sectionContents.setBorder(new CompoundBorder(
        new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
        new EmptyBorder(PluginPanel.BORDER_OFFSET, 0, PluginPanel.BORDER_OFFSET, 0)));
    sectionContents.setVisible(isOpen);
    for (final JComponent c : components)
    {
        sectionContents.add(c);
    }
    this.add(sectionContents, BorderLayout.SOUTH);

    // Add listeners to each part of the header so that it's easier to toggle them
    final MouseAdapter adapter = new MouseAdapter()
    {
    @Override
        public void mouseClicked(MouseEvent e)
        {
            toggle();
        }
    };
    sectionToggle.addActionListener(actionEvent -> toggle());
    sectionName.addMouseListener(adapter);
    sectionHeader.addMouseListener(adapter);
}

private void toggle()
{
    isOpen = !isOpen;
    sectionToggle.setIcon(isOpen ? SECTION_RETRACT_ICON : SECTION_EXPAND_ICON);
    sectionToggle.setRolloverIcon(isOpen ? SECTION_RETRACT_ICON_HOVER : SECTION_EXPAND_ICON_HOVER);
    sectionToggle.setToolTipText(isOpen ? "Retract" : "Expand");
    sectionContents.setVisible(isOpen);
}

public void setOpen(final boolean open)
{
    if (isOpen == open)
    {
        return;
    }

    toggle();
}
}