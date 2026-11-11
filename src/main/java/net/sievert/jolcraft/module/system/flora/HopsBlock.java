package net.sievert.jolcraft.module.system.flora;

import java.util.function.Function;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import net.sievert.jolcraft.stereotype.Actor;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * A hops plant, modelled on vanilla's pitcher crop: one block of ten ages that becomes two from {@link #DOUBLE_AGE} on,
 * the upper half carrying the lower half's age. It grows in the dark, or over verdant ground, and only with room above
 * it. Only the lower half ticks. The four varieties are four blocks of this one class.
 */
@Actor
public class HopsBlock extends DoublePlantBlock implements BonemealableBlock {

    public static final MapCodec<HopsBlock> CODEC = simpleCodec(HopsBlock::new);
    public static final int MAX_AGE = 9;
    /** The first age at which the plant is two blocks high. */
    public static final int DOUBLE_AGE = 5;
    public static final IntegerProperty AGE = IntegerProperty.create(Dictionary.AGE, 0, MAX_AGE);
    public static final EnumProperty<DoubleBlockHalf> HALF = DoublePlantBlock.HALF;

    private static final int BONEMEAL_MIN = 2;
    private static final int BONEMEAL_MAX = 5;

    private final Function<BlockState, VoxelShape> shapes = makeShapes();

    public HopsBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<HopsBlock> codec() {
        return CODEC;
    }

    /** Whether a plant of this age is two blocks high. */
    public static boolean isDouble(int age) {
        return age >= DOUBLE_AGE;
    }

    /** The stage, 0 to 4, that the upper half shows at this age. */
    public static int upperStage(int age) {
        return Math.max(0, age - DOUBLE_AGE);
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        return getShapeForEachState(state -> {
            int age = state.getValue(AGE);
            return switch (state.getValue(HALF)) {
                case LOWER -> Block.column(16.0, 0.0, isDouble(age) ? 16.0 : 3.0 * (age + 1));
                case UPPER -> Block.column(16.0, 0.0, 2.0 + 3.0 * upperStage(age));
            };
        });
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return false;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.apply(state);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.SUPPORTS_CROPS);
    }

    /** The upper half stands on its lower half; the lower on ground that holds a crop, verdant or dark. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return super.canSurvive(state, level, pos);
        }
        return super.canSurvive(state, level, pos) && (level.getBlockState(pos.below()).is(FloraTags.VERDANT) || Darkness.at(level, pos));
    }

    /** While single the plant is not removed for lacking an upper half. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (isDouble(state.getValue(AGE))) {
            return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        }
        return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
        super.createBlockStateDefinition(builder);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && state.getValue(AGE) < MAX_AGE;
    }

    /** Grows one age when the ground is verdant or the block dark, with room above, at the crop's own roll through the crop-grow events. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1) || state.getValue(AGE) >= MAX_AGE) {
            return;
        }
        if (!(level.getBlockState(pos.below()).is(FloraTags.VERDANT) || Darkness.at(level, pos)) || !roomAbove(level, pos)) {
            return;
        }
        float speed = CropBlock.getGrowthSpeed(state, level, pos);
        if (CommonHooks.canCropGrow(level, pos, state, random.nextInt((int) (25.0F / speed) + 1) == 0)) {
            grow(level, state, pos, 1);
            CommonHooks.fireCropGrowPost(level, pos, state);
        }
    }

    /** Whether the block above a lower half is air or its own upper half, and inside the build height. */
    private boolean roomAbove(LevelReader level, BlockPos lowerPos) {
        BlockPos above = lowerPos.above();
        BlockState state = level.getBlockState(above);
        return level.isInsideBuildHeight(above) && (state.isAir() || state.is(this) && state.getValue(HALF) == DoubleBlockHalf.UPPER);
    }

    /** Sets the lower half's age and, when it is double, the upper half above it with the same age. */
    private void grow(Level level, BlockState lowerState, BlockPos lowerPos, int increase) {
        int age = Math.min(lowerState.getValue(AGE) + increase, MAX_AGE);
        BlockState grown = lowerState.setValue(AGE, age);
        level.setBlock(lowerPos, grown, Block.UPDATE_CLIENTS);
        if (isDouble(age)) {
            level.setBlock(lowerPos.above(), grown.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        }
    }

    private BlockPos lowerPos(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        BlockPos lowerPos = lowerPos(pos, state);
        BlockState lower = level.getBlockState(lowerPos);
        return lower.is(this) && lower.getValue(HALF) == DoubleBlockHalf.LOWER && lower.getValue(AGE) < MAX_AGE && roomAbove(level, lowerPos);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (isValidBonemealTarget(level, pos, state)) {
            BlockPos lowerPos = lowerPos(pos, state);
            grow(level, level.getBlockState(lowerPos), lowerPos, Mth.nextInt(random, BONEMEAL_MIN, BONEMEAL_MAX));
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (level instanceof ServerLevel serverLevel && entity instanceof Ravager && EventHooks.canEntityGrief(serverLevel, entity)) {
            serverLevel.destroyBlock(pos, true, entity);
        }
        super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
    }
}
