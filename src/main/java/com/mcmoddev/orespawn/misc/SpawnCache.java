package com.mcmoddev.orespawn.misc;

import com.mcmoddev.orespawn.OreSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.Map;
import java.util.concurrent.*;

public class SpawnCache {
    private static final Map<ChunkPos, Map<BlockPos, BlockState>> cache = new ConcurrentHashMap<>();
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static void spawnOrCache(ServerLevel lvl, LevelChunkSection section, BlockPos pos, BlockState state) {
        OreSpawn.LOGGER.info("In SpawnCache.spawnOrCache({}, {}, {}, {})", lvl, section, pos, state);
        ChunkPos chunkPos = new ChunkPos(pos);

        OreSpawn.LOGGER.info("Chunk at {} is loaded? {}", chunkPos, lvl.isAreaLoaded(pos, 1));
        if (lvl.isAreaLoaded(pos, 1)) {
            int relX = SectionPos.sectionRelative(pos.getX());
            int relY = SectionPos.sectionRelative(pos.getY());
            int relZ = SectionPos.sectionRelative(pos.getZ());
            section.setBlockState(relX, relY, relZ, state, false);
        } else {
            cache.compute(chunkPos, (key, sector) -> {
                if (sector == null) {
                    sector = new ConcurrentHashMap<>();
                }
                sector.put(pos, state);
                return sector;
            });
        }
    }

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load event) {
        LevelAccessor lvl = event.getLevel();
        ChunkPos p = event.getChunk().getPos();

        Map<BlockPos, BlockState> work;
        synchronized (cache) {
            work = cache.remove(p);
        }

        if (work == null) return;

        threadPool.submit(() -> {
            try (BulkSectionAccess bsa = new BulkSectionAccess(lvl)) {
                work.entrySet().forEach(bp -> {
                    BlockPos pos = bp.getKey();
                    int relX = SectionPos.sectionRelative(pos.getX());
                    int relY = SectionPos.sectionRelative(pos.getY());
                    int relZ = SectionPos.sectionRelative(pos.getZ());
                    BlockState targ = bp.getValue();
                    LevelChunkSection section = bsa.getSection(pos);
                    if (section != null) {
                        section.setBlockState(relX, relY, relZ, targ);
                    }
                });
            } catch (Exception e) {
                OreSpawn.LOGGER.error("Exception processing cached chunk at position {}: {}", p, e.getMessage());
            }
        });
    }

    public static void shutdown() {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }
    }
}
