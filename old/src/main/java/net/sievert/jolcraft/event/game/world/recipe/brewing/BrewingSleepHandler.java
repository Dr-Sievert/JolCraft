package net.sievert.jolcraft.event.game.world.recipe.brewing;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.sievert.jolcraft.util.log.JolCraftLogs;
import net.sievert.jolcraft.util.log.JolCraftLogTags;
import net.sievert.jolcraft.world.block.entity.custom.brewing.FermentingBarrelBlockEntity;
import net.sievert.jolcraft.world.block.entity.custom.brewing.FermentingCauldronBlockEntity;

/**
 * Advances every brewing block entity in a loaded chunk after the world skips time through
 * sleeping.
 *
 * The skip moves the level's day time, which the brewing block entities do not see (they age
 * by game time), so the bonus is handed to them here. Every loaded chunk of the level is
 * walked once; chunks that are not loaded do not tick either and keep aging by game time
 * alone when they come back.
 */
public final class BrewingSleepHandler {

    private BrewingSleepHandler() {}

    /**
     * Fast-forwards brewing progress by the amount of time skipped during sleep.
     */
    public static void handleSleepFinished(
            ServerLevel level,
            long newTime
    ) {
        long skippedTicks =
                newTime - level.getDayTime();

        if (skippedTicks <= 0L) {
            return;
        }

        int[] advancedCounts = new int[2];

        // getChunks is opened by the access transformer; it is the live view of every loaded chunk.
        for (ChunkHolder holder : level.getChunkSource().chunkMap.getChunks()) {
            if (holder.getChunkIfPresent(ChunkStatus.FULL) instanceof LevelChunk chunk) {
                processChunk(
                        chunk,
                        skippedTicks,
                        advancedCounts
                );
            }
        }

        JolCraftLogs.debug(
                JolCraftLogTags.BLOCK_ENTITY,
                "Sleep advanced time by {} ticks for {} brewing fermenting cauldron(s) and {} aging fermenting barrel(s)",
                skippedTicks,
                advancedCounts[0],
                advancedCounts[1]
        );
    }

    /**
     * Advances all supported brewing block entities contained within the chunk.
     */
    private static void processChunk(
            LevelChunk chunk,
            long skippedTicks,
            int[] advancedCounts
    ) {
        for (
                BlockEntity blockEntity :
                chunk.getBlockEntities().values()
        ) {
            processBlockEntity(
                    blockEntity,
                    skippedTicks,
                    advancedCounts
            );
        }
    }

    /**
     * Fast-forwards a supported brewing block entity when it currently has
     * active work in progress.
     */
    private static void processBlockEntity(
            BlockEntity blockEntity,
            long skippedTicks,
            int[] advancedCounts
    ) {
        if (blockEntity instanceof
                FermentingBarrelBlockEntity barrel) {
            if (!barrel.hasBrew()) {
                return;
            }

            if (barrel.fastForwardAge(
                    skippedTicks
            )) {
                advancedCounts[1]++;
            }

            return;
        }

        if (!(blockEntity instanceof
                FermentingCauldronBlockEntity cauldron)
                || !cauldron.isBrewing()) {
            return;
        }

        cauldron.fastForwardBrew(
                skippedTicks
        );

        advancedCounts[0]++;
    }
}
