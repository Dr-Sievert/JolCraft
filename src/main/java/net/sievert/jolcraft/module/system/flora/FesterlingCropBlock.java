package net.sievert.jolcraft.module.system.flora;

import java.util.function.Function;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.sievert.jolcraft.stereotype.Actor;

/**
 * The festerling crop, grown from rotten flesh planted on festerling ground: four ages and then the festerling. It
 * stands where {@link FesterlingBlock#grounded} holds and grows faster on verdant ground, with verdant ground around it
 * and with logs standing beside it ({@link #speed}). It has no item of its own; picked, it is the festerling.
 */
@Actor
public class FesterlingCropBlock extends VegetationBlock implements BonemealableBlock {

    public static final MapCodec<FesterlingCropBlock> CODEC = simpleCodec(FesterlingCropBlock::new);
    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;

    /** The speed on ground that is none of the following. */
    public static final float BASE_SPEED = 1.0F;
    /** What the speed is multiplied by when the ground under the crop is verdant. */
    public static final float VERDANT_GROUND_FACTOR = 1.5F;
    /** What each verdant block around the ground adds to the speed. */
    public static final float VERDANT_AROUND_BONUS = 2.0F;
    /** What each upright log beside the crop adds to the speed. */
    public static final float LOG_BONUS = 0.5F;

    private static final int BONEMEAL_ODDS = 5;
    private static final int BONEMEAL_WINS = 2;
    private static final int BONEMEAL_MIN = 1;
    private static final int BONEMEAL_MAX = 2;
    private static final int BURST = 5;

    private final Function<BlockState, VoxelShape> shapes = makeShapes();

    public FesterlingCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    public MapCodec<FesterlingCropBlock> codec() {
        return CODEC;
    }

    /**
     * The speed at which the crop grows: {@link #BASE_SPEED}, times {@link #VERDANT_GROUND_FACTOR} on verdant ground, plus
     * {@link #VERDANT_AROUND_BONUS} for each of the eight blocks around the ground that are verdant and {@link #LOG_BONUS}
     * for each of the four blocks beside the crop that are upright logs.
     */
    public static float speed(boolean verdantGround, int verdantAround, int logsBeside) {
        return BASE_SPEED * (verdantGround ? VERDANT_GROUND_FACTOR : 1.0F) + VERDANT_AROUND_BONUS * verdantAround + LOG_BONUS * logsBeside;
    }

    private static float speedAt(BlockGetter level, BlockPos pos) {
        BlockPos groundPos = pos.below();
        int verdantAround = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && level.getBlockState(groundPos.offset(dx, 0, dz)).is(FloraTags.VERDANT)) {
                    verdantAround++;
                }
            }
        }
        int logsBeside = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (FesterlingBlock.uprightLog(level.getBlockState(pos.relative(side)))) {
                logsBeside++;
            }
        }
        return speed(level.getBlockState(groundPos).is(FloraTags.VERDANT), verdantAround, logsBeside);
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        return getShapeForEachState(state -> switch (state.getValue(AGE)) {
            case 0 -> Block.column(12.0, 0.0, 3.0);
            case 1 -> Block.column(12.0, 0.0, 4.0);
            case 2 -> Block.column(4.0, 0.0, 6.0);
            default -> Block.column(4.0, 0.0, 8.0);
        });
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.apply(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return FesterlingBlock.grounded(state);
    }

    /** At the last age the crop becomes the festerling, in a burst of particles; before that it gains an age at the speed of its surroundings. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) {
            return;
        }
        int age = state.getValue(AGE);
        if (age >= MAX_AGE) {
            level.setBlock(pos, FloraContent.FESTERLING.get().defaultBlockState(), Block.UPDATE_ALL);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5, BURST, 0.35, 0.15, 0.35, 0.0);
        } else if (random.nextInt((int) (25.0F / speedAt(level, pos)) + 1) == 0) {
            level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextInt(BONEMEAL_ODDS) < BONEMEAL_WINS;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int age = Math.min(state.getValue(AGE) + Mth.nextInt(random, BONEMEAL_MIN, BONEMEAL_MAX), MAX_AGE);
        level.setBlock(pos, state.setValue(AGE, age), Block.UPDATE_CLIENTS);
    }

    /** The crop has no item, so the pick-block is the festerling at every age. The game's own hook for it, which NeoForge deprecates for a player-sensitive one that calls it. */
    @SuppressWarnings("deprecation")
    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(FloraContent.FESTERLING_ITEM.get());
    }
}
