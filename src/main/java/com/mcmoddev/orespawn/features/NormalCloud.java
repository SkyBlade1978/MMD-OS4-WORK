package com.mcmoddev.orespawn.features;

import com.mcmoddev.orespawn.OreSpawn;
import com.mcmoddev.orespawn.features.configs.NormalCloudConfiguration;
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

import java.util.HashSet;
import java.util.Set;

public class NormalCloud extends Feature<NormalCloudConfiguration> {
    public NormalCloud() {
        super(NormalCloudConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NormalCloudConfiguration> ctx) {
        NormalCloudConfiguration config = ctx.config();
        RandomSource rand = ctx.random();
        WorldGenLevel world = ctx.level();
        BlockPos origin = ctx.origin();

        int r = config.spread / 2;
        int total = Math.min(config.size, (int)(Math.PI * r * r));
        int maxAttempts = total * 2;
        int attempts = 0;

        OreSpawn.LOGGER.info("[NormalCloud] START at {} spread={} size={} → total={}, maxAttempts={}",
            origin, config.spread, config.size, total, maxAttempts);

        int minY = world.getMinBuildHeight();
        int maxY = world.getMaxBuildHeight(); // exclusive

        try (BulkSectionAccess access = new BulkSectionAccess(world)) {
            Set<BlockPos> processed = new HashSet<>();

            while (total-- > 0 && attempts < maxAttempts) {
                attempts++;
//                OreSpawn.LOGGER.debug("[NormalCloud] loop#{} remaining={} attempts={}", 
//                    (config.size - total), total, attempts);

                int xOff = M.getPoint(0, config.spread, r, rand);
                int zOff = M.getPoint(0, config.spread, r, rand);
                int yOff = rand.nextInt(maxY - minY) + minY;

                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(
                    origin.getX() + xOff,
                    yOff,
                    origin.getZ() + zOff
                );

                if (processed.contains(pos)) {
                    //OreSpawn.LOGGER.debug("[NormalCloud] skip duplicate {}", pos);
                    continue;
                }
                processed.add(pos);
                //OreSpawn.LOGGER.debug("[NormalCloud] trying {}", pos);

                if (!world.ensureCanWrite(pos)) {
                    //OreSpawn.LOGGER.debug("[NormalCloud] cannot write {}", pos);
                    continue;
                }
                //OreSpawn.LOGGER.debug("[NormalCloud] canWrite {}", pos);

                LevelChunkSection section = access.getSection(pos);
                if (section == null) {
                    //OreSpawn.LOGGER.debug("[NormalCloud] null section at {}", pos);
                    continue;
                }
                //OreSpawn.LOGGER.debug("[NormalCloud] got section at {}", pos);

                int relX = SectionPos.sectionRelative(pos.getX());
                int relY = SectionPos.sectionRelative(pos.getY());
                int relZ = SectionPos.sectionRelative(pos.getZ());
                BlockState current = section.getBlockState(relX, relY, relZ);

                boolean placedOne = false;
                for (VeinConfiguration.TargetBlockState tgt : config.targetStates) {
                    if (tgt.target.test(current, rand)) {
                        placedOne = true;
                        SpawnCache.spawnOrCache(world, section, pos, tgt.state);
                       // OreSpawn.LOGGER.debug("[NormalCloud] placed at {}", pos);
                    }
                }
               // if (!placedOne) {
                    //OreSpawn.LOGGER.debug("[NormalCloud] no match at {}", pos);
               // }
            }

//            if (attempts >= maxAttempts) {
//                OreSpawn.LOGGER.warn("[NormalCloud] aborted after too many attempts: {} of {}", attempts, maxAttempts);
//            } else {
//                OreSpawn.LOGGER.info("[NormalCloud] completed {} loops in {} attempts", config.size, attempts);
//            }
        } catch (Exception e) {
            OreSpawn.LOGGER.error("[NormalCloud] Exception in placement", e);
        }

        OreSpawn.LOGGER.info("[NormalCloud] END at {}", origin);
        return true;
    }
}
