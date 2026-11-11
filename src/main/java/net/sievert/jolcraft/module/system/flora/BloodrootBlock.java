package net.sievert.jolcraft.module.system.flora;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HangingRootsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.sievert.jolcraft.stereotype.Actor;

/**
 * Bloodroot: the game's hanging roots, which hang waterlogged or dry from any sturdy face, with a shape of its own and
 * a spread. Bonemeal on a bloodroot under {@link FloraTags#BLOODROOT_SPREADS_UNDER} puts another beside it, at the same
 * height, under a ceiling of the same kind, one try in {@link #BONEMEAL_ODDS}.
 */
@Actor
public class BloodrootBlock extends HangingRootsBlock implements BonemealableBlock {

    /** Typed as the game's roots, whose {@code codec()} fixes the type to their own class. */
    public static final MapCodec<HangingRootsBlock> CODEC = simpleCodec(BloodrootBlock::new);
    /** Bonemeal spreads the root one time in this many. */
    public static final int BONEMEAL_ODDS = 5;

    private static final VoxelShape SHAPE = Block.box(1.0, 7.0, 1.0, 15.0, 16.0, 15.0);

    public BloodrootBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<HangingRootsBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return underSpreadingCeiling(level, pos) && !spreadTargets(level, pos).isEmpty();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextInt(BONEMEAL_ODDS) == 0;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        List<BlockPos> targets = spreadTargets(level, pos);
        if (underSpreadingCeiling(level, pos) && !targets.isEmpty()) {
            BlockPos target = targets.get(random.nextInt(targets.size()));
            // waterlogged in source water alone, as the game's roots are placed: flowing water would become a source
            level.setBlock(target, defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(target).is(Fluids.WATER)), Block.UPDATE_ALL);
        }
    }

    private static boolean underSpreadingCeiling(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.above()).is(FloraTags.BLOODROOT_SPREADS_UNDER);
    }

    /** The eight cells around the root, at its height, that are free (air or water), have a spreading ceiling and would hold a bloodroot. */
    private List<BlockPos> spreadTargets(LevelReader level, BlockPos pos) {
        List<BlockPos> targets = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos target = pos.offset(dx, 0, dz);
                BlockState there = level.getBlockState(target);
                if ((dx != 0 || dz != 0) && (there.isAir() || there.is(Blocks.WATER)) && underSpreadingCeiling(level, target)
                        && defaultBlockState().canSurvive(level, target)) {
                    targets.add(target);
                }
            }
        }
        return targets;
    }
}
