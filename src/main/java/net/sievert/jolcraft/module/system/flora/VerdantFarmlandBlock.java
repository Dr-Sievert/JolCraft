package net.sievert.jolcraft.module.system.flora;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

import net.sievert.jolcraft.stereotype.Actor;

/**
 * Farmland that is always moist, never dries, is never trampled and, under a solid block, turns to verdant soil where
 * vanilla's turns to dirt. Every vanilla path to dirt (the tick, the placement, the random tick, the fall) is
 * overridden here.
 */
@Actor
public class VerdantFarmlandBlock extends FarmlandBlock {

    public static final MapCodec<FarmlandBlock> CODEC = simpleCodec(VerdantFarmlandBlock::new);

    public VerdantFarmlandBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MOISTURE, MAX_MOISTURE));
    }

    @Override
    public MapCodec<FarmlandBlock> codec() {
        return CODEC;
    }

    /** Only restores the moisture if anything lowered it; it never dries and never turns to soil on its own. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(MOISTURE) < MAX_MOISTURE) {
            level.setBlock(pos, state.setValue(MOISTURE, MAX_MOISTURE), UPDATE_CLIENTS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            turnToVerdantSoil(null, state, level, pos);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return !defaultBlockState().canSurvive(context.getLevel(), context.getClickedPos())
                ? FloraContent.VERDANT_SOIL.get().defaultBlockState()
                : super.getStateForPlacement(context);
    }

    /** Only the fall damage {@code Block.fallOn} gives; nothing tramples this farmland. */
    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        entity.causeFallDamage(fallDistance, 1.0F, entity.damageSources().fall());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        VerdantSoilBlock.sparkle(level, pos, random);
    }

    /** The verdant twin of {@code FarmlandBlock.turnToDirt}: entities pushed up, a block update and a game event. */
    public static void turnToVerdantSoil(@Nullable Entity sourceEntity, BlockState state, Level level, BlockPos pos) {
        BlockState soil = pushEntitiesUp(state, FloraContent.VERDANT_SOIL.get().defaultBlockState(), level, pos);
        level.setBlockAndUpdate(pos, soil);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(sourceEntity, soil));
    }
}
