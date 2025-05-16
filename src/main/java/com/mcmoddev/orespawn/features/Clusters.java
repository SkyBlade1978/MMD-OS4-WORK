package com.mcmoddev.orespawn.features;

import com.mcmoddev.orespawn.OreSpawn;
import com.mcmoddev.orespawn.features.configs.ClusterConfiguration;
import com.mcmoddev.orespawn.features.configs.VeinConfiguration;
import com.mcmoddev.orespawn.misc.M;
import com.mcmoddev.orespawn.misc.SpawnCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
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
    public boolean place(FeaturePlaceContext<ClusterConfiguration> ctx) {
        ClusterConfiguration config = ctx.config();
        RandomSource rand = ctx.random();
        WorldGenLevel world  = ctx.level();
        BlockPos origin      = ctx.origin();

        // logging entry
        OreSpawn.LOGGER.info("--- Clusters start at {}: spread={}, size={} ---",
            origin, config.spread, config.size);

        int r        = config.spread / 2;
        int maxCount = (int)Math.round(Math.PI * r * r);
        int count    = Math.min(config.size, maxCount);

        int minY = world.getMinBuildHeight();
        int maxY = world.getMaxBuildHeight();

        try (BulkSectionAccess access = new BulkSectionAccess(world)) {
            while (count-- > 0) {
                int xOff = M.getPoint(0, config.spread, r, rand);
                int zOff = M.getPoint(0, config.spread, r, rand);
                int yOff = rand.nextInt(maxY - minY) + minY;

                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(
                    origin.getX() + xOff,
                    yOff,
                    origin.getZ() + zOff
                );

                if (!world.ensureCanWrite(pos)) {
                    continue;
                }

                LevelChunkSection section = access.getSection(pos);
                if (section == null) {
                    continue;
                }

                int relX = SectionPos.sectionRelative(pos.getX());
                int relY = SectionPos.sectionRelative(pos.getY());
                int relZ = SectionPos.sectionRelative(pos.getZ());
                BlockState current = section.getBlockState(relX, relY, relZ);

                for (VeinConfiguration.TargetBlockState tgt : config.targetStates) {
                    if (tgt.target.test(current, rand)) {
                        SpawnCache.spawnOrCache(world, section, pos, tgt.state);
                    }
                }
            }
        } catch (Exception e) {
            OreSpawn.LOGGER.error("Exception in Clusters.place:", e);
        }

        OreSpawn.LOGGER.info("--- Clusters end at {} ---", origin);
        return true;
    }
}
