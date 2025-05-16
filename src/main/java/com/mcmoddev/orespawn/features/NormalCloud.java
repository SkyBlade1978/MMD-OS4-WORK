package com.mcmoddev.orespawn.features;

import com.mcmoddev.orespawn.features.configs.NormalCloudConfiguration;
import com.mcmoddev.orespawn.misc.M;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.HashSet;
import java.util.Set;

public class NormalCloud extends Feature<NormalCloudConfiguration> {
    public NormalCloud() {
        super(NormalCloudConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NormalCloudConfiguration> ctx) {
        NormalCloudConfiguration cfg = ctx.config();
        WorldGenLevel world = (WorldGenLevel)ctx.level();
        RandomSource rand = ctx.random();
        BlockPos origin = ctx.origin();

        int r = cfg.spread / 2;
        int total = Math.min(cfg.size, (int)(Math.PI * r * r));
        int minY = world.getMinBuildHeight(), maxY = world.getMaxBuildHeight();

        Set<BlockPos> seen = new HashSet<>();
        int attempts = 0;
        while (seen.size() < total && attempts++ < total * 2) {
            int x = origin.getX() + M.getPoint(0, cfg.spread, r, rand);
            int z = origin.getZ() + M.getPoint(0, cfg.spread, r, rand);
            int y = rand.nextInt(maxY - minY) + minY;
            BlockPos pos = new BlockPos(x, y, z);
            if (!seen.add(pos)) continue;

            BlockState existing = world.getBlockState(pos);
            for (var tgt : cfg.targetStates) {
                if (tgt.target.test(existing, rand)) {
                    world.setBlock(pos, tgt.state, 2);
                }
            }
        }

        return true;
    }

}
