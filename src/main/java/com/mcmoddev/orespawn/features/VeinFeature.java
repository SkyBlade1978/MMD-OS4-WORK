package com.mcmoddev.orespawn.features;

import com.mcmoddev.orespawn.features.configs.VeinConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import java.util.ArrayList;
import java.util.List;

public class VeinFeature extends Feature<VeinConfiguration> {
    public VeinFeature() {
        super(VeinConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<VeinConfiguration> ctx) {
        VeinConfiguration cfg      = ctx.config();
        WorldGenLevel    world     = (WorldGenLevel) ctx.level();
        RandomSource     rand      = ctx.random();
        BlockPos         origin    = ctx.origin();

        int minY = world.getMinBuildHeight();
        int maxY = world.getMaxBuildHeight();  // exclusive

        // pick random vein length
        int length = rand.nextInt(cfg.minLength, cfg.maxLength);
        List<BlockPos> nodes = new ArrayList<>(length);

        // 1) generate the vein path
        BlockPos cur = origin;
        for (int i = 0; i < length; i++) {
            int dir = rand.nextInt(6);
            cur = switch (dir) {
                case 0 -> cur.above();
                case 1 -> cur.below();
                case 2 -> cur.east();
                case 3 -> cur.west();
                case 4 -> cur.north();
                case 5 -> cur.south();
                default -> cur;
            };
            if (cur.getY() < minY || cur.getY() >= maxY) break;
            nodes.add(cur.immutable());
        }

        // 2) carve/place the vein directly
        for (BlockPos pos : nodes) {
            BlockState existing = world.getBlockState(pos);
            for (VeinConfiguration.TargetBlockState tgt : cfg.targetStates) {
                if (tgt.target.test(existing, rand)) {
                    // the “2” flag means no physics, no neighbor updates—
                    world.setBlock(pos, tgt.state, 2);
                }
            }
        }

        return true;
    }
}
