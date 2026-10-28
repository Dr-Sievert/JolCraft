package net.sievert.jolcraft.module.composite.menu;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import net.sievert.jolcraft.stereotype.Contract;

/**
 * Implemented by a block entity that ages, brews or polls on the server. A block answers {@link #ticker} from its
 * {@code getTicker}. Serves any block entity, a station or not; a tick that must reach a module posts its feature's own event.
 */
@Contract
public interface Tick {

    /** Runs once per server tick while the block entity is loaded. */
    void tickServer(ServerLevel level);

    /**
     * The ticker a block answers: null on a client level and for a type other than the block's own, else one that calls
     * {@link #tickServer} on a block entity that is a {@code Tick}.
     */
    static <T extends BlockEntity, E extends BlockEntity> @Nullable BlockEntityTicker<T> ticker(Level level, BlockEntityType<T> actual, BlockEntityType<E> expected) {
        if (!(level instanceof ServerLevel server) || actual != expected) {
            return null;
        }
        return (_, _, _, blockEntity) -> {
            if (blockEntity instanceof Tick tick) {
                tick.tickServer(server);
            }
        };
    }
}
