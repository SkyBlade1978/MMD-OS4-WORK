package com.mcmoddev.orespawn.features;

import com.mcmoddev.orespawn.features.configs.VeinConfiguration;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class VeinFeature extends Feature<VeinConfiguration> {
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
            double startX = (double)blockpos.getX() * Math.sin((double)f) * (double)sizeMod;
            double startY = (double)(blockpos.getY() * randomsource.nextInt(3) - 2);
            double startZ = (double)blockpos.getX() * Math.sin((double)f) * (double)sizeMod;
            int length = randomsource.nextInt(veinconfiguration.minLength, veinconfiguration.maxLength);
            return doPlacement(worldgenlevel, randomsource, veinconfiguration, startX, startY, startZ, length);
        }

        return false;
    }

    private boolean doPlacement(WorldGenLevel pLevel, RandomSource pRandom, VeinConfiguration pConfig, double startX, double startY, double startZ, int length) {
        int cn = 0;
        int ls = -1;
        int ns = -1;
        BlockPos curpos = new BlockPos(startX, startY, startZ);
        List<Pair<BlockPos, Integer>> seen = new ArrayList<>();
        while(cn <= length) {
            while( ns == ls )
                ns = pRandom.nextInt(1, 6);
            cn += 1;
            seen.add(Pair.of(curpos, ls==-1?ns:ls));
            switch(ns) {
                case 1:
                    curpos = curpos.above();
                    break;
                case 2:
                    curpos = curpos.below();
                    break;
                case 3:
                    curpos = curpos.east();
                    break;
                case 4:
                    curpos = curpos.west();
                    break;
                case 5:
                    curpos = curpos.north();
                    break;
                case 6:
                    curpos = curpos.south();
                    break;
                default:
                    throw new RuntimeException(String.format("Value %i out of range (1-6)", ns));
            }
            if (seen.contains(curpos)) cn = length+1; // early exit, we've crossed ourself
            ls = ns;
        }
        seen.stream().forEach(bp -> makeNodeAt(pLevel, pConfig, bp));
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
                throw new RuntimeException(String.format("Value %i out of range (1-6)", ns));
        }
    }

    private void makeHorizontal(BlockPos pos, VeinConfiguration pConfig) {

    }

    private void makeVertical(BlockPos pos, VeinConfiguration pConfig) {

    }
}
