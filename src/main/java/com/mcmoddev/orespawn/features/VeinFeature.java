package com.mcmoddev.orespawn.features;

import com.mcmoddev.orespawn.features.configs.VeinConfiguration;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class VeinFeature extends Feature<VeinConfiguration> {
    private static final String eMess = "Value %d out of range (1-6)";
    public VeinFeature(Codec<VeinConfiguration> pCodec) { super(pCodec); }

    @Override
    public boolean place(FeaturePlaceContext<VeinConfiguration> pContext) {
        RandomSource randomsource = pContext.random();
        BlockPos blockpos = pContext.origin();
        WorldGenLevel worldgenlevel = pContext.level();
        VeinConfiguration veinconfiguration = pContext.config();

        // do we place ?
        if (randomsource.nextFloat() <= veinconfiguration.frequency / (veinconfiguration.frequency>1?100:1)) {
            float base = randomsource.nextFloat() * (float) Math.PI;
            float sizeMod = veinconfiguration.size / 4.0f;
            double startX = (double)blockpos.getX() * Math.sin((double)base) * (double)sizeMod;
            double startY = (double)(blockpos.getY() * randomsource.nextInt(3) - 2);
            double startZ = (double)blockpos.getX() * Math.sin((double)base) * (double)sizeMod;
            int length = randomsource.nextInt(veinconfiguration.minLength, veinconfiguration.maxLength);
            return doPlacement(worldgenlevel, randomsource, veinconfiguration, startX, startY, startZ, length);
        }

        return false;
    }

    private boolean doPlacement(WorldGenLevel pLevel, RandomSource pRandom, VeinConfiguration pConfig, double startX, double startY, double startZ, int length) {
        int cn = 0;
        int ls = -1;
        int ns = -1;
        BlockPos curpos = new BlockPos((int) startX, (int) startY, (int) startZ);
        List<Pair<BlockPos, Integer>> seen = new ArrayList<>();
        while(cn <= length) {
            ns = pRandom.nextInt(1, 6);
            if ( ns == ls )
                while( ns == ls )
                    ns = pRandom.nextInt(1, 6);

            if (ls == -1)
                seen.add(Pair.of(curpos, ns));
            curpos = switch (ns) {
                case 1 -> curpos.above();
                case 2 -> curpos.below();
                case 3 -> curpos.east();
                case 4 -> curpos.west();
                case 5 -> curpos.north();
                case 6 -> curpos.south();
                default -> throw new RuntimeException(String.format(eMess, ns));
            };

            ls = ns;
            if (seen.contains(Pair.of(curpos, ls))) break; // early exit, we've crossed our previous track
            cn += 1;
        }
        seen.add(Pair.of(curpos, ns));
        seen.forEach(bp -> makeNodeAt(pLevel, pConfig, bp));
        return false;
    }

    private void makeNodeAt(WorldGenLevel pLevel, VeinConfiguration pConfig, Pair<BlockPos, Integer> pLoc) {
        // the right side of each loc determines if the node generates vertical or horizontal
        switch(pLoc.getRight()) {
            case 1:
            case 2:
                makeHorizontal(pLoc.getLeft(), pConfig);
            case 3:
            case 4:
            case 5:
            case 6:
                makeVertical(pLoc.getLeft(), pConfig);
            default:
                throw new RuntimeException(String.format(eMess, pLoc.getRight()));
        }
    }

    private void makeHorizontal(BlockPos pos, VeinConfiguration pConfig) {

    }

    private void makeVertical(BlockPos pos, VeinConfiguration pConfig) {

    }
}
