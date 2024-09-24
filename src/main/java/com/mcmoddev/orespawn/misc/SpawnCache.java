package com.mcmoddev.orespawn.misc;

import com.mcmoddev.orespawn.OreSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnCache {
    private static final Map<ChunkPos, Map<BlockPos, BlockState>> cache = new ConcurrentHashMap<>();

    public static void spawnOrCache(ServerLevel lvl, LevelChunkSection section, BlockPos pos, BlockState state) {
        OreSpawn.LOGGER.info("In SpawnCache.spawnOrCache({}, {}, {}, {})", lvl, section, pos, state);
        ChunkPos chunkPos = new ChunkPos(pos);

        //System.out.println("Chunk at "+chunkPos+" is loaded? "+ lvl.isAreaLoaded(pos, 1) +" (range: 1)");
        OreSpawn.LOGGER.info("Chunk at {} is loaded? {}", chunkPos, lvl.isAreaLoaded(pos, 1));
        if (lvl.isAreaLoaded(pos, 1)) {
            int relX = SectionPos.sectionRelative(pos.getX());
            int relY = SectionPos.sectionRelative(pos.getY());
            int relZ = SectionPos.sectionRelative(pos.getZ());
            section.setBlockState(relX, relY, relZ, state, false);
        } else {
            Map<BlockPos, BlockState> sector = cache.getOrDefault(chunkPos, new ConcurrentHashMap<>());
            if (sector.containsKey(pos)) {
                sector.replace(pos, state);
            } else {
                sector.put(pos, state);
            }
            cache.put(chunkPos, sector);
        }
    }

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load event) {
        LevelAccessor lvl = event.getLevel();
        ChunkPos p = event.getChunk().getPos();
        if (!cache.containsKey(p)) return;

        Map<BlockPos, BlockState> work = cache.get(p);

        try(BulkSectionAccess bsa = new BulkSectionAccess(lvl)) {
            work.entrySet().parallelStream().forEach(bp -> {
                BlockPos pos = bp.getKey();
                int relX = SectionPos.sectionRelative(pos.getX());
                int relY = SectionPos.sectionRelative(pos.getY());
                int relZ = SectionPos.sectionRelative(pos.getZ());
                BlockState targ = bp.getValue();
                LevelChunkSection section = bsa.getSection(pos);
                section.setBlockState(relX, relY, relZ, targ);
            });
            cache.remove(p);
        } catch(Exception e) {
            OreSpawn.LOGGER.error("Exception trying to work data for cached chunk at position {}: {}", p, e.getMessage());
        }
    }
}
