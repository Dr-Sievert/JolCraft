package net.sievert.jolcraft.world.worldgen.structure.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.sievert.jolcraft.data.JolCraftTags;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tracks whether the current thread is inside ConfiguredFeature#place, and answers whether a
 * position falls inside a structure tagged FEATURE_PROTECTED.
 *
 * Worldgen is multi-threaded, hence the ThreadLocal.
 */
public final class FeaturePlacementContext {

    private static final ThreadLocal<Scope> SCOPE = ThreadLocal.withInitial(Scope::new);

    private FeaturePlacementContext() {
    }

    private record ChunkGuard(boolean blockAll, List<BoundingBox> boxes) {
        static final ChunkGuard BLOCK_ALL = new ChunkGuard(true, List.of());
        static final ChunkGuard NONE = new ChunkGuard(false, List.of());

        boolean covers(BlockPos pos) {
            if (this.blockAll) {
                return true;
            }

            for (BoundingBox box : this.boxes) {
                if (box.isInside(pos)) {
                    return true;
                }
            }

            return false;
        }
    }

    private static final class Scope {
        int depth;

        // Structure starts and references are fixed before feature placement runs, so a chunk's
        // answer cannot change within one placement. Without this the mixin re-walked every
        // structure reference and piece on every setBlock.
        final Long2ObjectMap<ChunkGuard> guardsByChunk = new Long2ObjectOpenHashMap<>();
    }

    public static void enter() {
        SCOPE.get().depth++;
    }

    public static void exit() {
        Scope scope = SCOPE.get();
        scope.depth--;

        if (scope.depth <= 0) {
            SCOPE.remove();
        }
    }

    public static boolean isPlacingFeature() {
        return SCOPE.get().depth > 0;
    }

    public static boolean isProtected(WorldGenRegion region, BlockPos pos) {
        Scope scope = SCOPE.get();
        long chunkKey = ChunkPos.asLong(pos);

        ChunkGuard guard = scope.guardsByChunk.get(chunkKey);
        if (guard == null) {
            guard = collectGuard(region, pos);
            scope.guardsByChunk.put(chunkKey, guard);
        }

        return guard.covers(pos);
    }

    private static ChunkGuard collectGuard(WorldGenRegion region, BlockPos pos) {
        ChunkAccess targetChunk = region.getChunk(pos);

        if (targetChunk.getPersistedStatus().isBefore(ChunkStatus.STRUCTURE_REFERENCES)) {
            return ChunkGuard.BLOCK_ALL;
        }

        Registry<Structure> structures = region.registryAccess()
                .registryOrThrow(Registries.STRUCTURE);

        List<BoundingBox> boxes = new ArrayList<>();

        for (Map.Entry<Structure, LongSet> entry : targetChunk.getAllReferences().entrySet()) {
            Structure structure = entry.getKey();

            if (!structures.wrapAsHolder(structure).is(JolCraftTags.Structures.FEATURE_PROTECTED)) {
                continue;
            }

            for (long reference : entry.getValue()) {
                ChunkPos startPos = new ChunkPos(reference);

                if (!region.hasChunk(startPos.x, startPos.z)) {
                    return ChunkGuard.BLOCK_ALL;
                }

                ChunkAccess startChunk = region.getChunk(
                        startPos.x,
                        startPos.z,
                        ChunkStatus.STRUCTURE_STARTS
                );
                StructureStart start = startChunk.getStartForStructure(structure);

                if (start != null && start.isValid()) {
                    for (StructurePiece piece : start.getPieces()) {
                        boxes.add(piece.getBoundingBox());
                    }
                }
            }
        }

        return boxes.isEmpty() ? ChunkGuard.NONE : new ChunkGuard(false, List.copyOf(boxes));
    }
}
