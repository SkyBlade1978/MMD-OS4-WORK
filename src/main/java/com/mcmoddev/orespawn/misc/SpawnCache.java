package com.mcmoddev.orespawn.misc;

import com.mcmoddev.orespawn.OreSpawn;
import com.mojang.logging.LogUtils;
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
import java.util.concurrent.ConcurrentHashMap;

public class SpawnCache {
    private static final Map<ChunkPos, Map<BlockPos, BlockState>> cache = new ConcurrentHashMap<>();

    public static void spawnOrCache(ServerLevel lvl, LevelChunkSection section, BlockPos pos, BlockState state) {
        ChunkPos chunkPos = lvl.getChunk(pos).getPos();
        System.out.println("Chunk at "+chunkPos+" is loaded? "+ lvl.isAreaLoaded(pos, 1) +" (range: 1)");
        if (lvl.isAreaLoaded(pos, 1)) {
            int relX = SectionPos.sectionRelative(pos.getX());
            int relY = SectionPos.sectionRelative(pos.getY());
            int relZ = SectionPos.sectionRelative(pos.getZ());
            section.setBlockState(relX, relY, relZ, state);
        } else {
            Map<BlockPos, BlockState> sction = cache.getOrDefault(chunkPos, new ConcurrentHashMap<>());
            if (sction.containsKey(pos)) {
                sction.replace(pos, state);
            } else {
                sction.put(pos, state);
            }
            cache.put(chunkPos, sction);
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
