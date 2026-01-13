package com.boneshardhelper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;

import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

@Singleton
public class BoneShardOverlay2D extends OverlayPanel {
        private static final int REGION_ID = 5681;
        private final Client client;
        private final BoneShardHelperPlugin plugin;
        private final BoneShardHelperConfig config;
        private final BoneShardTrainingState state;

        @Inject
        private BoneShardOverlay2D(
                        Client client,
                        BoneShardHelperPlugin plugin,
                        BoneShardHelperConfig config,
                        BoneShardTrainingState state) {
                this.client = client;
                this.plugin = plugin;
                this.config = config;
                this.state = state;
                this.setPosition(OverlayPosition.BOTTOM_LEFT);
        }

        @Override
        public Dimension render(Graphics2D graphics) {
                if (!config.toggleInfobox()) {
                        return null;
                }

                if (client.getLocalPlayer().getWorldLocation().getRegionID() != REGION_ID) {
                        return null;
                }

                boolean trainingActive = (state.hasBlessedWines() || state.hasUnblessedWines() || state.hasWineInBowl())
                                && state.hasShards();

                if (config.infoboxTitle()) {
                        panelComponent.getChildren().add(TitleComponent.builder().text("Bone Shard Helper").build());
                }

                // Show components regardless of training state for testing
                panelComponent.getChildren().add(
                                LineComponent.builder().left("Stage: ")
                                                .right(state.getCurrentTrainingState().getDisplayName())
                                                .build());

                if (config.infoboxActionsLeft()) {
                        panelComponent.getChildren().add(
                                        LineComponent.builder().left("Actions left")
                                                        .right(state.getActionsRemaining() + "").build());
                }
                return super.render(graphics);
        }
}
