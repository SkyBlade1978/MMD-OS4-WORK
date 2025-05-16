package com.mcmoddev.orespawn.misc;

import com.mcmoddev.orespawn.OreSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages deferred block placements for chunks that are not yet loaded.
 * <p>
 * During world generation, writes directly to chunk sections.
 * At runtime, writes immediately if a chunk is loaded, otherwise caches
 * the placement and replays it when the chunk loads.
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
     * Attempts to place a block immediately or defers it if the target chunk is not loaded.
     * <ul>
     *   <li>If running during world generation (WorldGenLevel), writes directly to the section.</li>
     *   <li>Else if running on a live server and the chunk is loaded, writes directly.</li>
     *   <li>Otherwise, caches the placement for later replay.</li>
     * </ul>
     *
     * @param levelAccessor  the world or region context
     * @param chunkSection   the section of the chunk containing the target position
     * @param targetPos      the absolute block position to place
     * @param newState       the block state to set
     */
    public static void placeOrCache(
        LevelAccessor levelAccessor,
        LevelChunkSection chunkSection,
        BlockPos targetPos,
        BlockState newState
    ) {
        // Compute section-relative coordinates (0..15)
        int x = SectionPos.sectionRelative(targetPos.getX());
        int y = SectionPos.sectionRelative(targetPos.getY());
        int z = SectionPos.sectionRelative(targetPos.getZ());

        // 1) During world generation: write directly into the chunk section buffer
        if (levelAccessor instanceof WorldGenLevel) {
            chunkSection.setBlockState(x, y, z, newState, false);
            return;
        }

        // 2) At runtime, if the chunk is already loaded, write directly
        if (levelAccessor instanceof ServerLevel server && server.isAreaLoaded(targetPos, 1)) {
            chunkSection.setBlockState(x, y, z, newState, false);
            return;
        }

        // 3) Otherwise, defer placement until the chunk loads
        ChunkPos chunkPos = new ChunkPos(targetPos);
        synchronized (deferredPlacements) {
            deferredPlacements
                .computeIfAbsent(chunkPos, cp -> new ConcurrentHashMap<>())
                .put(targetPos, newState);
        }
    }

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
