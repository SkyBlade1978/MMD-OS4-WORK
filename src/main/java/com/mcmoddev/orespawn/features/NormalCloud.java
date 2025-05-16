package com.mcmoddev.orespawn.features;

import java.util.HashSet;
import java.util.Set;

import com.mcmoddev.orespawn.OreSpawn;
import com.mcmoddev.orespawn.features.configs.NormalCloudConfiguration;
import com.mcmoddev.orespawn.features.configs.VeinConfiguration;
import com.mcmoddev.orespawn.misc.M;
import com.mcmoddev.orespawn.misc.SpawnCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
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
        try (BulkSectionAccess access = new BulkSectionAccess(placeContext.level())) {
            Set<BlockPos> processedPositions = new HashSet<>();
            int maxAttempts = count * 2; // Safeguard to prevent infinite loops
            int attempts = 0;

            while (--count >= 0 && attempts < maxAttempts) {
                attempts++;
                int xPlace = M.getPoint(0, conf.spread, r, rand);
                int yPlace = M.getPoint(minBuild, buildLimit, (buildLimit - Math.abs(minBuild)) / 2, rand);
                int zPlace = M.getPoint(0, conf.spread, r, rand);
                BlockPos.MutableBlockPos acc = p.mutable();
                acc.offset(xPlace, yPlace, zPlace);

                if (processedPositions.contains(acc)) {
                    OreSpawn.LOGGER.warn("Skipping already processed position {}", acc);
                    continue;
                }

                OreSpawn.LOGGER.info("Checking ensureCanWrite for position {}", acc);
                if (!placeContext.level().ensureCanWrite(acc)) {
                    OreSpawn.LOGGER.warn("ensureCanWrite failed for position {}", acc);
                    continue;
                }

                LevelChunkSection section = access.getSection(acc);
                if (section == null) {
                    OreSpawn.LOGGER.warn("Section is null for position {}", acc);
                    continue; // Skip and move to the next position
                }

                processedPositions.add(acc); // Mark position as processed
                OreSpawn.LOGGER.info("Section retrieved for position {}", acc);

                int relX = SectionPos.sectionRelative(acc.getX());
                int relY = SectionPos.sectionRelative(acc.getY());
                int relZ = SectionPos.sectionRelative(acc.getZ());
                BlockState blockstate = section.getBlockState(relX, relY, relZ);

                for (VeinConfiguration.TargetBlockState tgt : conf.targetStates) {
                    if (tgt.target.test(blockstate, rand)) {
                        OreSpawn.LOGGER.info("Target matched at position {}, placing block", acc);
                        SpawnCache.spawnOrCache(placeContext.level().getLevel(), section, acc, tgt.state);
                    }
                }
            }

            if (attempts >= maxAttempts) {
                OreSpawn.LOGGER.error("Aborting NormalCloud placement due to excessive attempts to find valid positions.");
            }
        } catch (Exception e) {
            OreSpawn.LOGGER.error("Exception in NormalCloud placement: {}", e.getMessage(), e);
        }

        return true;
    }
}
