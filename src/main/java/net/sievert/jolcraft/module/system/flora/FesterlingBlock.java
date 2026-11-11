package net.sievert.jolcraft.module.system.flora;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.TriState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.sievert.jolcraft.stereotype.Actor;

/**
 * The festerling: the game's mushroom with a small shape, its own huge form, {@link FloraFeatures#HUGE_FESTERLING}, and
 * its own ground, {@link #grounded}, in any light. The festerling crop stands on the same ground.
 */
@Actor
public class FesterlingBlock extends MushroomBlock {

    /** Typed as the game's mushroom, whose {@code codec()} fixes the type to its own class. */
    public static final MapCodec<MushroomBlock> CODEC = simpleCodec(FesterlingBlock::new);

    private static final VoxelShape SHAPE = Block.column(4.0, 0.0, 6.0);

    public FesterlingBlock(BlockBehaviour.Properties properties) {
        super(FloraFeatures.HUGE_FESTERLING, properties);
    }

    @Override
    public MapCodec<MushroomBlock> codec() {
        return CODEC;
    }

    /** Whether a block is festerling ground: in {@link FloraTags#SUPPORTS_FESTERLING}, or a log standing on its end. */
    public static boolean grounded(BlockState ground) {
        return ground.is(FloraTags.SUPPORTS_FESTERLING) || uprightLog(ground);
    }

    /** Whether a block is a log standing on its end: one with no axis, or with the vertical one. */
    public static boolean uprightLog(BlockState state) {
        return state.is(BlockTags.LOGS) && (!state.hasProperty(BlockStateProperties.AXIS) || state.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** The ground's own say first, as the game's mushroom asks it; then festerling ground, whatever the light. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState ground = level.getBlockState(belowPos);
        TriState decision = ground.canSustainPlant(level, belowPos, Direction.UP, state);
        return decision.isDefault() ? grounded(ground) : decision.isTrue();
    }
}
