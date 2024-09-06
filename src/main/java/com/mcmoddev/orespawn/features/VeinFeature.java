package com.mcmoddev.orespawn.features;

import com.mcmoddev.orespawn.features.configs.VeinConfiguration;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
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
            double startX = (double)blockpos.getX() * Math.sin(base) * (double)sizeMod;
            double startY = (double)(blockpos.getY() * randomsource.nextInt(3) - 2);
            double startZ = (double)blockpos.getX() * Math.sin(base) * (double)sizeMod;
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
        seen.forEach(bp -> makeNodeAt(pLevel, pConfig, bp, pRandom));
        return false;
    }

    private void makeNodeAt(WorldGenLevel pLevel, VeinConfiguration pConfig, Pair<BlockPos, Integer> pLoc, RandomSource pRandom) {
        // the right side of each loc determines if the node generates vertical or horizontal
        switch(pLoc.getRight()) {
            case 1:
            case 2:
                makeHorizontal(pLevel, pLoc.getLeft(), pConfig, pRandom);
                break;
            case 3:
            case 4:
                makeVertical(pLevel, pLoc.getLeft(), pConfig, pRandom, true);
                break;
            case 5:
            case 6:
                makeVertical(pLevel, pLoc.getLeft(), pConfig, pRandom, false);
                break;
            default:
                throw new RuntimeException(String.format(eMess, pLoc.getRight()));
        }
    }

    private static double getRadiusOfArea(int area) {
        return Math.sqrt(area/Math.PI);
    }

    private static Pair<Double, Double> paraCirc(double r, double curT) {
        double rsinT = r * Math.sin(curT);
        double rcosT = r * Math.cos(curT);
        Pair<Double, Double> coordOffs = Pair.of(rsinT, rcosT);
        return coordOffs;
    }

    private static Pair<Integer, Integer> paraCircCoords(int cx, int cy, double r, double curT) {
        Pair<Double, Double> base = paraCirc(r, curT);
        int ax = (int)(cx + base.getLeft());
        int ay = (int)(cy + base.getLeft());
        Pair<Integer, Integer> coords = Pair.of(ax, ay);
        return coords;
    }

    private void makeHorizontal(WorldGenLevel pLevel, BlockPos pos, VeinConfiguration pConfig, RandomSource pRandom) {
        BulkSectionAccess bulksectionaccess = new BulkSectionAccess(pLevel);
        List<Pair<Integer,Integer>> placed = new LinkedList<>();

        BlockPos.MutableBlockPos accessPos = new BlockPos.MutableBlockPos();

        for (double r = 0; r <= getRadiusOfArea(pConfig.size); r++) {
            for (double c = 0; c <= Math.PI * 2; c += 0.01) {
                int left = pos.getX();
                int right = pos.getZ();
                Pair<Integer, Integer> tl = paraCircCoords(left, right, r, c);
                accessPos.set(pos.getX()+tl.getLeft(), pos.getY(), pos.getZ()+tl.getRight());
                if (pLevel.ensureCanWrite(accessPos)) {
                    LevelChunkSection section = bulksectionaccess.getSection(accessPos);
                    if (section != null) {
                        BlockState blockstate = section.getBlockState(accessPos.getX(), accessPos.getY(), accessPos.getZ());
                        if (!placed.contains(tl)) {
                            for (VeinConfiguration.TargetBlockState tgt : pConfig.targetStates) {
                                if (tgt.target.test(blockstate, pRandom)) {
                                    placed.add(tl);
                                    section.setBlockState(accessPos.getX(), accessPos.getY(), accessPos.getZ(), tgt.state);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void makeVertical(WorldGenLevel pLevel, BlockPos pos, VeinConfiguration pConfig, RandomSource pRandom, boolean northSouth) {
        // northsouth == true, manipulate XY else manipulate ZY
        BulkSectionAccess bulksectionaccess = new BulkSectionAccess(pLevel);
        List<Pair<Integer,Integer>> placed = new LinkedList<>();
        BlockPos.MutableBlockPos accessPos = new BlockPos.MutableBlockPos();

        for (double r = 0; r <= getRadiusOfArea(pConfig.size); r++) {
            for (double c = 0; c <= Math.PI * 2; c += 0.01) {
                int left = pos.getX();
                int right = pos.getZ();
                Pair<Integer, Integer> tl = paraCircCoords(left, right, r, c);
                if (northSouth) {
                    accessPos.set(pos.getX()+tl.getLeft(), pos.getY()+tl.getRight(), pos.getZ());
                } else {
                    accessPos.set(pos.getX(), pos.getY()+tl.getRight(), pos.getZ()+tl.getLeft());
                }

                if (pLevel.ensureCanWrite(accessPos)) {
                    LevelChunkSection section = bulksectionaccess.getSection(accessPos);
                    if (section != null) {
                        BlockState blockstate = section.getBlockState(accessPos.getX(), accessPos.getY(), accessPos.getZ());
                        if (!placed.contains(tl)) {
                            for (VeinConfiguration.TargetBlockState tgt : pConfig.targetStates) {
                                if (tgt.target.test(blockstate, pRandom)) {
                                    placed.add(tl);
                                    section.setBlockState(accessPos.getX(), accessPos.getY(), accessPos.getZ(), tgt.state);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
