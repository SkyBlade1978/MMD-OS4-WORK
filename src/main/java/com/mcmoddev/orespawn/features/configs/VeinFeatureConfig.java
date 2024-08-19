package com.mcmoddev.orespawn.features.configs;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration.TargetBlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import java.util.List;

public class VeinFeatureConfig implements FeatureConfiguration {
    public final List<TargetBlockState> targetStates;
    public final int size;
    public final int minTries;
    public final int maxTries;
    public final int variation;
    public final float frequency;
    public final int minHeight;
    public final int maxHeight;

    public static final Codec<VeinFeatureConfig> CODEC = RecordCodecBuilder.create(
            (builder) -> {
                return builder.group(
                        Codec.list(TargetBlockState.CODEC).fieldOf("targets").forGetter((config) -> { return config.targetStates; }),
                        Codec.INT.fieldOf("size").forGetter((config) -> config.size),
                        Codec.INT.fieldOf("variation").forGetter((config) -> config.variation),
                        Codec.INT.fieldOf("maxTries").forGetter((config) -> config.maxTries),
                        Codec.INT.fieldOf("minTries").forGetter((config) -> config.minTries),
                        Codec.INT.fieldOf("minHeight").forGetter((config) -> config.minHeight),
                        Codec.INT.fieldOf("maxHeight").forGetter((config) -> config.maxHeight),
                        Codec.FLOAT.fieldOf("frequency").forGetter((config) -> config.frequency)
                ).apply(builder, VeinFeatureConfig::new);
            }
    );

    public VeinFeatureConfig(List<TargetBlockState> targets, int size, int variation, int maxTries, int minTries, int minHeight, int maxHeight, float frequency) {
        this.targetStates = targets;
        this.size = size;
        this.variation = variation;
        this.minTries = minTries;
        this.maxTries = maxTries;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.frequency = frequency;
    };

    public VeinFeatureConfig(RuleTest target, BlockState state, int size, int variation, int maxTries, int minTries, int minHeight, int maxHeight, float frequency) {
        this(ImmutableList.of(new TargetBlockState(target, state)), size, variation, maxTries, minTries, minHeight, maxHeight, frequency);
    }
}
