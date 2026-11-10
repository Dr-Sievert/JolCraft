package net.sievert.jolcraft.module.system.worldgen;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import net.sievert.jolcraft.stereotype.Actor;

/**
 * Places a piece only where it hangs from something: it reads the world block above the first block of maximum Y in
 * the given order, and when that block blocks motion the piece is placed as it is, otherwise the answer is empty and
 * none of the piece's blocks is placed (its entities still are). It decides once per placement call, so it serves a
 * piece that lies within one chunk, as a hanging lantern does: such a piece is placed in one call, and the block
 * above it lies in the generating chunk.
 */
@Actor
public final class HangingProcessor extends StructureProcessor {

    public static final MapCodec<HangingProcessor> CODEC = MapCodec.unit(HangingProcessor::new);

    public HangingProcessor() {
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos position, BlockPos referencePos,
            List<StructureTemplate.StructureBlockInfo> original, List<StructureTemplate.StructureBlockInfo> processed, StructurePlaceSettings settings) {
        BlockPos top = null;
        for (StructureTemplate.StructureBlockInfo info : processed) {
            if (top == null || info.pos().getY() > top.getY()) {
                top = info.pos();
            }
        }
        if (top == null || level.getBlockState(top.above()).blocksMotion()) {
            return processed;
        }
        return new ArrayList<>();
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return WorldgenContent.HANGING.get();
    }
}
