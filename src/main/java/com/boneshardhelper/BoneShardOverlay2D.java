package com.boneshardhelper;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

@Singleton
public class BoneShardOverlay2D extends OverlayPanel {
        private final BoneShardHelperConfig config;
        private final BoneShardTrainingState state;

        @Inject
        private BoneShardOverlay2D(
                        BoneShardHelperConfig config,
                        BoneShardTrainingState state) {
                this.config = config;
                this.state = state;
                this.setPosition(OverlayPosition.BOTTOM_LEFT);
        }

        @Override
        public Dimension render(Graphics2D graphics) {
                if (!config.toggleInfobox()) {
                        return null;
                }

                if (!state.inTrainingRegion()) {
                        return null;
                }

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
                                        LineComponent.builder().left("Actions left: ")
                                                        .right(state.getActionsRemaining() + "").build());
                }

                // Key tracking values for verification
                panelComponent.getChildren().add(
                                LineComponent.builder().left("Wine in bowl: ")
                                                .right(state.getWineActionsInBowl() + "").build());

                panelComponent.getChildren().add(
                                LineComponent.builder().left("Prayer points: ")
                                                .right(state.getCurrentPrayerPoints() + "").build());

                // panelComponent.getChildren().add(
                // LineComponent.builder().left("Wine count: ")
                // .right(state.getBlessedWineCount() + "").build());
                return super.render(graphics);
        }
}
