package net.sievert.jolcraft.module.system.flora;

import java.util.function.Function;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;

import net.sievert.jolcraft.stereotype.Actor;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * Deepslate bulbs: a crop of ten ages that stands on {@link FloraTags#SUPPORTS_DEEPSLATE_BULBS}, on verdant ground
 * anywhere and elsewhere only at {@link #DEPTH_LIMIT} or below in the dark. It grows in the dark, where vanilla's crop
 * grows in the light, and like vanilla's crops it is checked for survival on placement and on a neighbour's change,
 * never on a random tick. No bonemeal grows it and nothing tramples it.
 */
@Actor
public class BulbsBlock extends CropBlock {

    public static final MapCodec<BulbsBlock> CODEC = simpleCodec(BulbsBlock::new);
    public static final int MAX_AGE = 9;
    /** The highest Y at which a bulb stands on ground that is not verdant. */
    public static final int DEPTH_LIMIT = 0;
    public static final IntegerProperty AGE = IntegerProperty.create(Dictionary.AGE, 0, MAX_AGE);

    private final Function<BlockState, VoxelShape> shapes = makeShapes();

    public BulbsBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<BulbsBlock> codec() {
        return CODEC;
    }

    /** Whether a bulb at this Y is deep enough to stand on ground that is not verdant. */
    public static boolean deepEnough(int y) {
        return y <= DEPTH_LIMIT;
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        int[] heights = {1, 2, 3, 4, 5, 6, 7, 10, 10, 11};
        return getShapeForEachState(state -> Block.column(16.0, 0.0, heights[getAge(state)]));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.apply(state);
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return MAX_AGE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(FloraTags.SUPPORTS_DEEPSLATE_BULBS);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);
        TriState decision = ground.canSustainPlant(level, below, Direction.UP, state);
        if (!decision.isDefault()) {
            return decision.isTrue();
        }
        return mayPlaceOn(ground, level, below) && (ground.is(FloraTags.VERDANT) || deepEnough(pos.getY()) && Darkness.at(level, pos));
    }

    /** Grows in the dark or over verdant ground, at the crop's own roll and through the crop-grow events; it never breaks itself. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) {
            return;
        }
        int age = getAge(state);
        if (age >= getMaxAge() || !(level.getBlockState(pos.below()).is(FloraTags.VERDANT) || Darkness.at(level, pos))) {
            return;
        }
        float speed = getGrowthSpeed(state, level, pos);
        if (CommonHooks.canCropGrow(level, pos, state, random.nextInt((int) (25.0F / speed) + 1) == 0)) {
            level.setBlock(pos, getStateForAge(age + 1), Block.UPDATE_CLIENTS);
            CommonHooks.fireCropGrowPost(level, pos, state);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return false;
    }

    /** The seeds are the block item registered for this block, so the crop is its own seed. */
    @Override
    protected ItemLike getBaseSeedId() {
        return this;
    }
}
