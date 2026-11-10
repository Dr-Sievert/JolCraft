package net.sievert.jolcraft.module.system.worldgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import net.sievert.jolcraft.stereotype.Actor;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * Turns an air block of the piece into a cobweb, keeping its tag, at {@code chance}, when at least two of its six
 * neighbours in the piece block motion; the world around the piece is not consulted. The chance is drawn from the
 * settings' random at the block, for air blocks only, as the game's own chance processors draw. Runs over the whole
 * piece, answers a mutable list of the same size in the given order.
 */
@Actor
public final class RandomCobwebProcessor extends StructureProcessor {

    private static final int BLOCKING_NEIGHBOURS = 2;

    public static final MapCodec<RandomCobwebProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf(Dictionary.CHANCE).forGetter(processor -> processor.chance)
    ).apply(instance, RandomCobwebProcessor::new));

    private final float chance;

    public RandomCobwebProcessor(float chance) {
        this.chance = chance;
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos position, BlockPos referencePos,
            List<StructureTemplate.StructureBlockInfo> original, List<StructureTemplate.StructureBlockInfo> processed, StructurePlaceSettings settings) {
        Map<BlockPos, BlockState> states = new HashMap<>();
        for (StructureTemplate.StructureBlockInfo info : processed) {
            states.put(info.pos(), info.state());
        }
        List<StructureTemplate.StructureBlockInfo> answer = new ArrayList<>(processed.size());
        for (StructureTemplate.StructureBlockInfo info : processed) {
            if (info.state().isAir() && settings.getRandom(info.pos()).nextFloat() < chance && blockingNeighbours(states, info.pos()) >= BLOCKING_NEIGHBOURS) {
                answer.add(new StructureTemplate.StructureBlockInfo(info.pos(), Blocks.COBWEB.defaultBlockState(), info.nbt()));
            } else {
                answer.add(info);
            }
        }
        return answer;
    }

    private static int blockingNeighbours(Map<BlockPos, BlockState> states, BlockPos pos) {
        int count = 0;
        for (Direction direction : Direction.values()) {
            BlockState neighbour = states.get(pos.relative(direction));
            if (neighbour != null && neighbour.blocksMotion()) {
                count++;
            }
        }
        return count;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return WorldgenContent.RANDOM_COBWEB.get();
    }
}
