package com.mcmoddev.orespawn.features;

import com.mcmoddev.orespawn.features.configs.NormalCloudConfiguration;
import com.mcmoddev.orespawn.features.configs.VeinConfiguration;
import com.mcmoddev.orespawn.misc.M;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class NormalCloud extends Feature<NormalCloudConfiguration> {
    public NormalCloud() {
        super(NormalCloudConfiguration.CODEC);
    }

    @Override
    // (FeaturePlaceContext<VeinConfiguration> pContext)
    public boolean place(FeaturePlaceContext<NormalCloudConfiguration> placeContext) {
        NormalCloudConfiguration conf = placeContext.config();
        int r = conf.spread / 2;
        RandomSource rand = placeContext.random();
        int count = Math.min(conf.size, (int)Math.round(Math.PI * Math.pow(r, 2)));
        int buildLimit = placeContext.level().getMaxBuildHeight();
        int minBuild = placeContext.level().getMinBuildHeight();
        BlockPos p = placeContext.origin();
        try(BulkSectionAccess access = new BulkSectionAccess(placeContext.level())) {
            while (--count >= 0) {
                int xPlace = M.getPoint(0, conf.spread, r, rand);
                int yPlace = M.getPoint(minBuild, buildLimit, (buildLimit - Math.abs(minBuild)) / 2, rand);
                int zPlace = M.getPoint(0, conf.spread, r, rand);

                BlockPos.MutableBlockPos acc = p.mutable();
                acc.offset(xPlace, yPlace, zPlace);
                if (placeContext.level().ensureCanWrite(p)) {
                    LevelChunkSection section = access.getSection(p);
                    if (section != null) {
                        BlockState blockstate = section.getBlockState(acc.getX(), acc.getY(), acc.getZ());
                        for (VeinConfiguration.TargetBlockState tgt : conf.targetStates) {
                            if (tgt.target.test(blockstate, rand)) {
                                section.setBlockState(acc.getX(), acc.getY(), acc.getZ(), tgt.state);
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
}
