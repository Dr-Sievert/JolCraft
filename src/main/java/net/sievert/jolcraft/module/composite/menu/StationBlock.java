package net.sievert.jolcraft.module.composite.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import net.sievert.jolcraft.stereotype.Actor;

/**
 * The block of a station. A concrete block gives its {@code codec()}, its {@code newBlockEntity} and the {@link #type()}
 * of its block entity; this base opens the menu on use, ticks a {@link Tick} block entity on the server, and gives the
 * comparator the fill of the cells. It drops nothing itself: the game drops a container's contents when the block is
 * removed, unless the block entity says otherwise.
 */
@Actor
public abstract class StationBlock extends BaseEntityBlock {

    protected StationBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** The type of this block's block entity, which must be a {@link StationBlockEntity}. */
    protected abstract BlockEntityType<? extends StationBlockEntity> type();

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel && player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof StationBlockEntity station) {
            station.open(server);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return Tick.ticker(level, type, type());
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
}
