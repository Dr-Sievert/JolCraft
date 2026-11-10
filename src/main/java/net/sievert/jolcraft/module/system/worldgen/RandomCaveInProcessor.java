package net.sievert.jolcraft.module.system.worldgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import net.sievert.jolcraft.stereotype.Actor;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * Collapses columns of a piece, once per piece at {@code chance}, seeded from the level seed forked at the piece's
 * position as the game's capped processor seeds. It works on a map of the piece by position, where a block absent
 * from the piece counts as not blocking motion and as open. A top is a block with nothing above it in the map, one
 * of the four blocks below it not blocking motion, and an open side (a horizontal neighbour that is air or absent)
 * at it or within three below. One top is drawn, then a half-width from 1 to 3; the top's column is carved, then,
 * each at 0.65, every other column of the square of that half-width around it whose top qualifies against the map as
 * carved so far. Carving walks down from the column's first block that blocks motion: every block without a block
 * entity is dropped, a block entity is kept and the walk goes on past it, and at the first air or absent block the
 * last dropped block becomes air instead; the walk stops at the level's minimum Y. The answer is the given list in
 * its order less the dropped blocks, with the air in place, so the world's own block stays where a block was dropped.
 * It changes the list's size: a capped processor goes before it, never after.
 */
@Actor
public final class RandomCaveInProcessor extends StructureProcessor {

    private static final int SOLID_DEPTH = 4;
    private static final int OPEN_SIDE_DEPTH = 3;
    private static final int MAX_HALF_WIDTH = 3;
    private static final float NEIGHBOUR_COLUMN_CHANCE = 0.65F;

    public static final MapCodec<RandomCaveInProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf(Dictionary.CHANCE).forGetter(processor -> processor.chance)
    ).apply(instance, RandomCaveInProcessor::new));

    private final float chance;

    public RandomCaveInProcessor(float chance) {
        this.chance = chance;
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos position, BlockPos referencePos,
            List<StructureTemplate.StructureBlockInfo> original, List<StructureTemplate.StructureBlockInfo> processed, StructurePlaceSettings settings) {
        RandomSource random = RandomSource.createThreadLocalInstance(level.getLevel().getSeed()).forkPositional().at(position);
        if (random.nextFloat() >= chance) {
            return processed;
        }
        Map<BlockPos, StructureTemplate.StructureBlockInfo> blocks = new HashMap<>();
        for (StructureTemplate.StructureBlockInfo info : processed) {
            blocks.put(info.pos(), info);
        }
        List<BlockPos> tops = new ArrayList<>();
        for (StructureTemplate.StructureBlockInfo info : processed) {
            if (canCollapse(blocks, info.pos())) {
                tops.add(info.pos());
            }
        }
        if (tops.isEmpty()) {
            return processed;
        }
        Set<BlockPos> dropped = new HashSet<>();
        BlockPos top = tops.get(random.nextInt(tops.size()));
        int halfWidth = 1 + random.nextInt(MAX_HALF_WIDTH);
        carve(blocks, dropped, top, level.getMinY());
        for (int x = -halfWidth; x <= halfWidth; x++) {
            for (int z = -halfWidth; z <= halfWidth; z++) {
                if ((x == 0 && z == 0) || random.nextFloat() > NEIGHBOUR_COLUMN_CHANCE) {
                    continue;
                }
                BlockPos neighbour = top.offset(x, 0, z);
                if (canCollapse(blocks, neighbour)) {
                    carve(blocks, dropped, neighbour, level.getMinY());
                }
            }
        }
        List<StructureTemplate.StructureBlockInfo> answer = new ArrayList<>(processed.size());
        for (StructureTemplate.StructureBlockInfo info : processed) {
            if (!dropped.contains(info.pos())) {
                answer.add(blocks.get(info.pos()));
            }
        }
        return answer;
    }

    private static void carve(Map<BlockPos, StructureTemplate.StructureBlockInfo> blocks, Set<BlockPos> dropped, BlockPos start, int minY) {
        BlockPos.MutableBlockPos cursor = start.mutable();
        StructureTemplate.StructureBlockInfo ceiling = null;
        while (cursor.getY() >= minY) {
            StructureTemplate.StructureBlockInfo info = blocks.get(cursor);
            if (info != null && info.state().blocksMotion()) {
                ceiling = info;
                break;
            }
            cursor.move(Direction.DOWN);
        }
        if (ceiling == null) {
            return;
        }
        cursor.set(ceiling.pos());
        StructureTemplate.StructureBlockInfo last = null;
        while (cursor.getY() >= minY) {
            StructureTemplate.StructureBlockInfo info = blocks.get(cursor);
            if (info == null || info.state().isAir()) {
                if (last != null) {
                    dropped.remove(last.pos());
                    blocks.put(last.pos(), new StructureTemplate.StructureBlockInfo(last.pos(), Blocks.AIR.defaultBlockState(), null));
                }
                return;
            }
            if (!info.state().hasBlockEntity()) {
                dropped.add(info.pos());
                blocks.put(info.pos(), new StructureTemplate.StructureBlockInfo(info.pos(), Blocks.STRUCTURE_VOID.defaultBlockState(), null));
                last = info;
            }
            cursor.move(Direction.DOWN);
        }
    }

    private static boolean canCollapse(Map<BlockPos, StructureTemplate.StructureBlockInfo> blocks, BlockPos pos) {
        return isTop(blocks, pos) && hasOpenSide(blocks, pos);
    }

    private static boolean isTop(Map<BlockPos, StructureTemplate.StructureBlockInfo> blocks, BlockPos pos) {
        if (blocks.containsKey(pos.above())) {
            return false;
        }
        for (int depth = 1; depth <= SOLID_DEPTH; depth++) {
            StructureTemplate.StructureBlockInfo below = blocks.get(pos.below(depth));
            if (below == null || !below.state().blocksMotion()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasOpenSide(Map<BlockPos, StructureTemplate.StructureBlockInfo> blocks, BlockPos pos) {
        for (int depth = 0; depth <= OPEN_SIDE_DEPTH; depth++) {
            BlockPos at = pos.below(depth);
            for (Direction side : Direction.Plane.HORIZONTAL) {
                StructureTemplate.StructureBlockInfo info = blocks.get(at.relative(side));
                if (info == null || info.state().isAir()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return WorldgenContent.RANDOM_CAVE_IN.get();
    }
}
