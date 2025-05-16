package com.mcmoddev.orespawn.features;

import com.mcmoddev.orespawn.features.configs.VeinConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates ore veins by creating a randomised path and replacing matching blocks along that path.
 * <p>
 * During world generation, writes directly into the chunk using fast no-physics updates.
 */
public class VeinFeature extends Feature<VeinConfiguration> {
    public VeinFeature() {
        super(VeinConfiguration.CODEC);
    }

    /**
     * Called during world-gen to place an ore vein.
     * <p>
     * 1) Picks a random length between configured min and max.
     * 2) Walks step-by-step in random directions from the origin, stopping if out of height bounds.
     * 3) Collects positions along the path and replaces blocks matching target states.
     *
     * @param context Contains world, random, origin, and vein configuration
     * @return always true (ignored by Minecraft)
     */
    @Override
    public boolean place(FeaturePlaceContext<VeinConfiguration> context) {
        VeinConfiguration config = context.config();
        WorldGenLevel world      = (WorldGenLevel) context.level();
        RandomSource random      = context.random();
        BlockPos startPos        = context.origin();

        int minY = world.getMinBuildHeight();
        int maxY = world.getMaxBuildHeight(); // exclusive upper bound

        // Determine vein length
        int veinLength = random.nextInt(config.minLength, config.maxLength);
        List<BlockPos> pathPositions = new ArrayList<>(veinLength);

        // 1) Generate vein path
        BlockPos currentPos = startPos;
        for (int step = 0; step < veinLength; step++) {
            // Choose random direction: 0=up,1=down,2=east,3=west,4=north,5=south
            int direction = random.nextInt(6);
            currentPos = switch (direction) {
                case 0 -> currentPos.above();
                case 1 -> currentPos.below();
                case 2 -> currentPos.east();
                case 3 -> currentPos.west();
                case 4 -> currentPos.north();
                case 5 -> currentPos.south();
                default -> currentPos;
            };
            // Stop if Y out of world height range
            int y = currentPos.getY();
            if (y < minY || y >= maxY) {
                break;
            }
            pathPositions.add(currentPos.immutable());
        }

        // 2) Place vein blocks directly
        for (BlockPos pos : pathPositions) {
            BlockState existing = world.getBlockState(pos);
            for (VeinConfiguration.TargetBlockState target : config.targetStates) {
                // Test if the existing block matches one of the configured target states
                if (target.target.test(existing, random)) {
                    // Write block with flag=2: no physics, no neighbour updates (fast world-gen)
                    world.setBlock(pos, target.state, 2);
                }
            }
        }

        return true;
    }
}
