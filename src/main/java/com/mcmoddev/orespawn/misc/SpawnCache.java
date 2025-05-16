package com.mcmoddev.orespawn.misc;

import com.mcmoddev.orespawn.OreSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.Map;
import java.util.LinkedHashMap;

/**
 * listens for chunk load events and replays any deferred placements for that chunk.
 */
public class SpawnCache {
    private static final int MAX_ENTRIES = 1000;

    /**
     * Cache mapping a chunk position to a map of block positions -> desired block state.
     * Uses a bounded LinkedHashMap to limit memory usage.
     */
    private static final Map<ChunkPos, Map<BlockPos, BlockState>> deferredPlacements =
        new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<ChunkPos, Map<BlockPos, BlockState>> eldest) {
                return size() > MAX_ENTRIES;
            }
        };


    /**
     * Listens for chunk load events and replays any deferred placements for that chunk.
     * Runs on the server main thread.
     *
     * @param event the chunk load event
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel server)) {
            return;
        }

        ChunkPos loadedChunkPos = event.getChunk().getPos();
        Map<BlockPos, BlockState> placements;

        synchronized (deferredPlacements) {
            placements = deferredPlacements.remove(loadedChunkPos);
        }
        if (placements == null || placements.isEmpty()) {
            return;
        }

        // Schedule replay on the server thread to avoid threading issues
        server.getServer().execute(() -> {
            try (BulkSectionAccess sectionAccess = new BulkSectionAccess(server)) {
                for (Map.Entry<BlockPos, BlockState> entry : placements.entrySet()) {
                    BlockPos pos = entry.getKey();
                    BlockState state = entry.getValue();
                    LevelChunkSection section = sectionAccess.getSection(pos);
                    if (section != null) {
                        int rx = SectionPos.sectionRelative(pos.getX());
                        int ry = SectionPos.sectionRelative(pos.getY());
                        int rz = SectionPos.sectionRelative(pos.getZ());
                        section.setBlockState(rx, ry, rz, state, false);
                    }
                }
            } catch (Exception ex) {
                OreSpawn.LOGGER.error(
                    "Error replaying deferred placements for chunk {}: {}",
                    loadedChunkPos,
                    ex.getMessage(),
                    ex
                );
            }
        });
    }
}
