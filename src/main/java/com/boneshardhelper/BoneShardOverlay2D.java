package com.boneshardhelper;

import java.awt.Color;
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
                if (!config.toggleInfobox() || state.getCurrentTrainingState() == BoneShardTrainingState.TrainingState.NO_STATE) {
                        return null;
                }

                if (!state.inTrainingRegion()) {
                        return null;
                }

                if (config.infoboxTitle()) {
                        panelComponent.getChildren().add(TitleComponent.builder().text("Bone Shard Helper").build());
                }

                // Show components regardless of training state for testing
                BoneShardTrainingState.TrainingState currentState = state.getCurrentTrainingState();
                Color stageColor;
                
                switch (currentState) {
                        case BLESS_WINES:
                                stageColor = config.blessWinesColor();
                                break;
                        case RECHARGE_PRAYER:
                                stageColor = config.rechargePrayerColor();
                                break;
                        case SACRIFICE_SHARDS:
                                stageColor = config.sacrificeShardsColor();
                                break;
                        case RESUPPLY:
                                stageColor = config.resupplyColor();
                                break;
                        default:
                                stageColor = Color.WHITE;
                                break;
                }
                
                panelComponent.getChildren().add(
                                LineComponent.builder().left("Stage: ")
                                                .right(currentState.getDisplayName())
                                                .rightColor(stageColor)
                                                .build());

                if (config.infoboxActionsLeft() && state.getCurrentTrainingState() == BoneShardTrainingState.TrainingState.SACRIFICE_SHARDS) {
                        panelComponent.getChildren().add(
                                        LineComponent.builder().left("Actions left: ")
                                                        .right(state.getActionsRemaining() + "").build());
                }

                if (config.infoboxInventoriesLeft() 
                                && state.getCurrentTrainingState() != BoneShardTrainingState.TrainingState.NO_STATE) {
                        panelComponent.getChildren().add(
                                        LineComponent.builder().left("Inventories left: ")
                                                        .right(state.getInventoriesRemaining() + "").build());
                }

                if (config.debugMode()) {
                        panelComponent.getChildren().add(
                                        LineComponent.builder().left("Wine in bowl: ")
                                                        .right(state.getWineActionsInBowl() + "").build());
                }

                // Show regular wine warning if enabled and in a valid training state
                if (config.infoboxRegularWineWarning() 
                                && state.getCurrentTrainingState() != BoneShardTrainingState.TrainingState.NO_STATE
                                && state.hasRegularWines()) {
                        panelComponent.getChildren().add(
                                        TitleComponent.builder()
                                                        .text("Holding regular wine!")
                                                        .color(Color.RED)
                                                        .build());
                }
                return super.render(graphics);
        }
}
