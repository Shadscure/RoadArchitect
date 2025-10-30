package net.oxcodsnet.roadarchitect.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Configuration for {@link RoadFeature}.
 */
public record RoadFeatureConfig(int orthWidth, int forwardLength, GenerationPhase phase) implements FeatureConfiguration {

    /**
     * Codec for serialising the configuration.
     */
    public static final Codec<RoadFeatureConfig> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    Codec.INT.fieldOf("orth_width").forGetter(RoadFeatureConfig::orthWidth),
                    Codec.INT.fieldOf("forward_length").forGetter(RoadFeatureConfig::forwardLength),
                    GenerationPhase.CODEC.fieldOf("phase").forGetter(RoadFeatureConfig::phase)
            ).apply(i, RoadFeatureConfig::new)
    );

    /**
     * Distinguishes between the preparation pass and the finishing pass.
     */
    public enum GenerationPhase implements StringRepresentable {
        PREPARE("prepare"),
        FINALIZE("finalize");

        private final String id;

        GenerationPhase(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }

        /**
         * Codec for serialising the enum.
         */
        public static final EnumCodec<GenerationPhase> CODEC = StringRepresentable.fromEnum(GenerationPhase::values);
    }
}
