package com.mcmoddev.orespawn.features;

import com.mcmoddev.orespawn.OreSpawn;
import com.mcmoddev.orespawn.features.configs.ClusterConfiguration;
import com.mcmoddev.orespawn.features.configs.VeinConfiguration;
import com.mcmoddev.orespawn.misc.M;
import com.mcmoddev.orespawn.misc.SpawnCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class Clusters extends Feature<ClusterConfiguration> {
    public Clusters() {
        super(ClusterConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<ClusterConfiguration> pContext) {
        ClusterConfiguration conf = pContext.config();
        int r = conf.spread / 2;
        RandomSource rand = pContext.random();
        int count = Math.min(conf.size, (int)Math.round(Math.PI * Math.pow(r, 2)));
        int buildLimit = pContext.level().getMaxBuildHeight();
        int minBuild = pContext.level().getMinBuildHeight();
        BlockPos p = pContext.origin();
        try(BulkSectionAccess access = new BulkSectionAccess(pContext.level())) {
            while (--count >= 0) {
                int xPlace = M.getPoint(0, conf.spread, r, rand);
                int yPlace = M.getPoint(minBuild, buildLimit, (buildLimit - Math.abs(minBuild)) / 2, rand);
                int zPlace = M.getPoint(0, conf.spread, r, rand);
                OreSpawn.LOGGER.info("Cluster spawning at {}, {}, {}", xPlace, yPlace, zPlace);

                BlockPos.MutableBlockPos acc = p.mutable();
                acc.offset(xPlace, yPlace, zPlace);
                if (pContext.level().ensureCanWrite(p)) {
                    LevelChunkSection section = access.getSection(p);
                    if (section != null) {
                        int pX = SectionPos.sectionRelative(acc.getX());
                        int pY = SectionPos.sectionRelative(acc.getY());
                        int pZ = SectionPos.sectionRelative(acc.getZ());
                        BlockState blockstate = section.getBlockState(pX, pY, pZ);
                        for (VeinConfiguration.TargetBlockState tgt : conf.targetStates) {
                            if (tgt.target.test(blockstate, rand)) {
                                spawnChunk(acc, tgt.state, conf, section, rand, pContext.level().getLevel());
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    // the following chunks of raw data are carried over from OS3 via the Cyanocode of OS1 and is not well understood
    // Similarly the primary placement logic of spawnChunk is also carried over.
    protected static final Vec3i[] offsets_small = {
            new Vec3i(0, 0, 0), new Vec3i(1, 0, 0),
            new Vec3i(0, 1, 0), new Vec3i(1, 1, 0),

            new Vec3i(0, 0, 1), new Vec3i(1, 0, 1),
            new Vec3i(0, 1, 1), new Vec3i(1, 1, 1)
    };

    protected static final Vec3i[] offsets = {
            new Vec3i(-1, -1, -1), new Vec3i(0, -1, -1), new Vec3i(1, -1, -1),
            new Vec3i(-1, 0, -1), new Vec3i(0, 0, -1), new Vec3i(1, 0, -1),
            new Vec3i(-1, 1, -1), new Vec3i(0, 1, -1), new Vec3i(1, 1, -1),

            new Vec3i(-1, -1, 0), new Vec3i(0, -1, 0), new Vec3i(1, -1, 0),
            new Vec3i(-1, 0, 0), new Vec3i(0, 0, 0), new Vec3i(1, 0, 0),
            new Vec3i(-1, 1, 0), new Vec3i(0, 1, 0), new Vec3i(1, 1, 0),

            new Vec3i(-1, -1, 1), new Vec3i(0, -1, 1), new Vec3i(1, -1, 1),
            new Vec3i(-1, 0, 1), new Vec3i(0, 0, 1), new Vec3i(1, 0, 1),
            new Vec3i(-1, 1, 1), new Vec3i(0, 1, 1), new Vec3i(1, 1, 1)
    };

    protected static final int[] offsetIndexRef = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26};
    protected static final int[] offsetIndexRef_small = {0, 1, 2, 3, 4, 5, 6, 7};

    private void scramble(int[] target, RandomSource prng) {
        for (int i = target.length - 1; i > 0; i--) {
            int n = prng.nextInt(i);
            int temp = target[i];
            target[i] = target[n];
            target[n] = temp;
        }
    }
    private void spawnChunk(BlockPos.MutableBlockPos acc, BlockState state, ClusterConfiguration conf, LevelChunkSection section, RandomSource rand, ServerLevel pLevel) {
        int count = conf.nodeSize;
        int lutType = (count < 8) ? offsetIndexRef_small.length : offsetIndexRef.length;
        int[] lut = (count < 8) ? offsetIndexRef_small : offsetIndexRef;
        Vec3i[] offs = new Vec3i[lutType];

        System.arraycopy((count < 8) ? offsets_small : offsets, 0, offs, 0, lutType);
        if (count < 27) {
            int[] scrambledLUT = new int[lutType];
            System.arraycopy(lut, 0, scrambledLUT, 0, scrambledLUT.length);
            scramble(scrambledLUT, rand);

            while (count > 0) {
                BlockPos.MutableBlockPos p = acc;
                p.move(offs[scrambledLUT[--count]]);
                SpawnCache.spawnOrCache(pLevel, section, p, state);
//                section.setBlockState(p.getX(), p.getY(), p.getZ(), state, true);
                count--;
            }
        }
    }
}
