package com.mcmoddev.orespawn.features;

import com.mcmoddev.orespawn.features.configs.ClusterConfiguration;
import com.mcmoddev.orespawn.misc.M;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class Clusters extends Feature<ClusterConfiguration> {
    public Clusters() {
        super(ClusterConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<ClusterConfiguration> ctx) {
        ClusterConfiguration cfg = ctx.config();
        WorldGenLevel world = (WorldGenLevel)ctx.level();
        RandomSource rand = ctx.random();
        BlockPos origin = ctx.origin();

        int r = cfg.spread / 2;
        int count = Math.min(cfg.size, (int)(Math.PI * r * r));
        int minY = world.getMinBuildHeight(), maxY = world.getMaxBuildHeight();

        for (int i = 0; i < count; i++) {
            int x = origin.getX() + M.getPoint(0, cfg.spread, r, rand);
            int z = origin.getZ() + M.getPoint(0, cfg.spread, r, rand);
            int y = rand.nextInt(maxY - minY) + minY;
            BlockPos pos = new BlockPos(x, y, z);

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
