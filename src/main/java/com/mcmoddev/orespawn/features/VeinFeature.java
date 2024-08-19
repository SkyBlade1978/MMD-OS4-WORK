package com.mcmoddev.orespawn.features;

import com.mcmoddev.orespawn.features.configs.VeinFeatureConfig;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class VeinFeature extends Feature<VeinFeatureConfig> {
    public VeinFeature(Codec<VeinFeatureConfig> pCodec) { super(pCodec); }

    @Override
    public boolean place(FeaturePlaceContext<VeinFeatureConfig> featurePlaceContext) {
        return false;
    }


}
