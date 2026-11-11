package net.sievert.jolcraft.module.system.flora;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

import net.sievert.jolcraft.stereotype.Actor;

/**
 * The base of the mod's huge mushrooms, over the game's own so that a mushroom's bonemeal check works untouched. A
 * subclass holds the {@link MushroomShape}; this places it. The game's clearance check reads the shape's radius at
 * every height, so a mushroom grows only where its whole shape is free, and the blocks go in through the game's
 * {@code placeMushroomBlock}, into air and what mushrooms replace. A face of a block shows the cap or the stem when the
 * neighbouring cell in that direction is no part of the mushroom, one rule for every block.
 */
@Actor
public abstract class LayeredMushroomFeature extends AbstractHugeMushroomFeature {

    protected LayeredMushroomFeature(Codec<HugeMushroomFeatureConfiguration> codec) {
        super(codec);
    }

    /** The shape this feature builds. */
    public abstract MushroomShape shape();

    @Override
    protected int getTreeHeight(RandomSource random) {
        return shape().stem();
    }

    @Override
    protected int getTreeRadiusForHeight(int trunkHeight, int treeHeight, int leafRadius, int yo) {
        return shape().radiusAt(yo);
    }

    @Override
    protected void placeTrunk(WorldGenLevel level, RandomSource random, BlockPos origin, HugeMushroomFeatureConfiguration config, int treeHeight,
            BlockPos.MutableBlockPos blockPos) {
        for (int dy = 0; dy < shape().stem(); dy++) {
            place(level, origin, blockPos, 0, dy, 0, config.stemProvider().getState(level, random, origin));
        }
    }

    @Override
    protected void makeCap(WorldGenLevel level, RandomSource random, BlockPos origin, int treeHeight, BlockPos.MutableBlockPos blockPos,
            HugeMushroomFeatureConfiguration config) {
        for (MushroomShape.Ring ring : shape().rings()) {
            for (int dx = -ring.radius(); dx <= ring.radius(); dx++) {
                for (int dz = -ring.radius(); dz <= ring.radius(); dz++) {
                    if (ring.contains(dx, dz)) {
                        place(level, origin, blockPos, dx, ring.height(), dz, config.capProvider().getState(level, random, origin));
                    }
                }
            }
        }
        place(level, origin, blockPos, 0, shape().stem(), 0, config.capProvider().getState(level, random, origin));
    }

    /** Places the block of the mushroom at this offset from the origin, with its six faces set by the one rule. */
    private void place(WorldGenLevel level, BlockPos origin, BlockPos.MutableBlockPos blockPos, int dx, int dy, int dz, BlockState state) {
        BlockState faced = state;
        for (Direction side : Direction.values()) {
            BooleanProperty face = PipeBlock.PROPERTY_BY_DIRECTION.get(side);
            if (faced.hasProperty(face)) {
                faced = faced.setValue(face, !shape().contains(dx + side.getStepX(), dy + side.getStepY(), dz + side.getStepZ()));
            }
        }
        blockPos.setWithOffset(origin, dx, dy, dz);
        placeMushroomBlock(level, blockPos, faced);
    }
}
