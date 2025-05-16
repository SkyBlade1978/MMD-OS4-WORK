package com.mcmoddev.orespawn.misc;

import com.mcmoddev.orespawn.OreSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnCache {
	private static final int MAX_CACHE_SIZE = 1000;

	// Bounded, thread-safe cache for truly off-chunk writes at runtime:
	private static final Map<ChunkPos, Map<BlockPos, BlockState>> cache = new LinkedHashMap<>() {
		@Override
		protected boolean removeEldestEntry(Map.Entry<ChunkPos, java.util.Map<BlockPos, BlockState>> eldest) {
			return size() > MAX_CACHE_SIZE;
		}
	};

	public static void spawnOrCache(LevelAccessor lvl, LevelChunkSection section, BlockPos pos, BlockState state) {
		int relX = SectionPos.sectionRelative(pos.getX());
		int relY = SectionPos.sectionRelative(pos.getY());
		int relZ = SectionPos.sectionRelative(pos.getZ());

// 1) During world-gen, always write into the section buffer
		if (lvl instanceof WorldGenLevel) {
			section.setBlockState(relX, relY, relZ, state, false);
			return;
		}

// 2) At runtime on the server, if the chunk's loaded, write directly
		if (lvl instanceof ServerLevel server && server.isAreaLoaded(pos, 1)) {
			section.setBlockState(relX, relY, relZ, state, false);
			return;
		}

// 3) Otherwise, cache for replay when the chunk actually loads
		ChunkPos cp = new ChunkPos(pos);
		synchronized (cache) {
			cache.computeIfAbsent(cp, k -> new ConcurrentHashMap<>()).put(pos, state);
		}
	}

	/** Replay any cached writes when a real ServerLevel chunk finally loads. */
	@SubscribeEvent
	public static void chunkLoaded(ChunkEvent.Load event) {
		if (!(event.getLevel() instanceof ServerLevel server))
			return;
		ChunkPos pos = event.getChunk().getPos();

		Map<BlockPos, BlockState> toReplay;
		synchronized (cache) {
			toReplay = cache.remove(pos);
		}
		if (toReplay == null)
			return;

		server.getServer().execute(() -> {
			try (BulkSectionAccess bsa = new BulkSectionAccess(server)) {
				for (var e : toReplay.entrySet()) {
					BlockPos bp = e.getKey();
					BlockState bs = e.getValue();
					LevelChunkSection sec = bsa.getSection(bp);
					if (sec != null) {
						sec.setBlockState(SectionPos.sectionRelative(bp.getX()), SectionPos.sectionRelative(bp.getY()),
								SectionPos.sectionRelative(bp.getZ()), bs, false);
					}
				}
			} catch (Exception ex) {
				OreSpawn.LOGGER.error("Error replaying cached spawns for chunk {}: {}", pos, ex.getMessage());
			}
		});
	}
}
