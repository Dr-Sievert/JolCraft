package net.sievert.jolcraft.module.composite.menu;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.sievert.jolcraft.stereotype.Helper;

/** Sends a block entity's state to the clients that track it. Serves any block entity, a station or not. */
@Helper
public final class Sync {

    private Sync() {
    }

    /** Marks the block entity changed and tells the tracking clients to ask for its update packet; nothing on a client level. */
    public static void send(BlockEntity blockEntity) {
        if (blockEntity.getLevel() instanceof ServerLevel level) {
            blockEntity.setChanged();
            BlockState state = blockEntity.getBlockState();
            level.sendBlockUpdated(blockEntity.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
    }
}
