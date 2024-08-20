package com.mcmoddev.orespawn.features.configs;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import java.util.List;

public class VeinConfiguration implements FeatureConfiguration {
    public final List<TargetBlockState> targetStates;
    public final int size;
    public final int maxLength;
    public final int minLength;
    public final float frequency;

    public static final Codec<VeinConfiguration> CODEC = RecordCodecBuilder.create(
            (builder) -> {
                return builder.group(
                        Codec.list(OreConfiguration.TargetBlockState.CODEC).fieldOf("targets").forGetter((config) -> { return config.targetStates; }),
                        Codec.INT.fieldOf("size").forGetter((config) -> config.size),
                        Codec.INT.fieldOf("max_length").forGetter((config) -> config.maxLength),
                        Codec.INT.fieldOf("min_length").forGetter((config) -> config.minLength),
                        Codec.FLOAT.fieldOf("frequency").forGetter((config) -> config.frequency)
                ).apply(builder, VeinConfiguration::new);
            }
    );

    public VeinConfiguration(List<TargetBlockState> targets, int size, int maxlength, int minlength, float frequency) {
        this.targetStates = targets;
        this.size = size;
        this.maxLength = maxlength;
        this.minLength = minlength;
        this.frequency = frequency;
    };

    public VeinConfiguration(RuleTest target, BlockState state, int size, int maxlength, int minlength, float frequency) {
        this(ImmutableList.of(new OreConfiguration.TargetBlockState(target, state)), size, maxlength, minlength, frequency);
    }
}
