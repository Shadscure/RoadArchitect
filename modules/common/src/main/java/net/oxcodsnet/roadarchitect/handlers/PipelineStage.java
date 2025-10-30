package net.oxcodsnet.roadarchitect.handlers;

import net.minecraft.network.chat.Component;

public enum PipelineStage {
    INITIALISATION(Component.translatable("roadarchitect.stage.initialisation")),
    SCANNING_STRUCTURES(Component.translatable("roadarchitect.stage.scanning")),
    PATH_FINDING(Component.translatable("roadarchitect.stage.pathfinding")),
    POST_PROCESSING(Component.translatable("roadarchitect.stage.postprocess")),
    COMPLETE(Component.translatable("roadarchitect.stage.complete"));

    private final Component label;

    PipelineStage(Component label) {
        this.label = label;
    }

    public Component label() {
        return label;
    }
}
