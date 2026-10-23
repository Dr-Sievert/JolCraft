package net.sievert.jolcraft.world.worldgen.structure.custom;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.sievert.jolcraft.world.worldgen.structure.JolCraftStructures;
import net.sievert.jolcraft.world.worldgen.structure.util.AbstractJigsawStructure;
import net.sievert.jolcraft.world.worldgen.structure.util.SinglePlacementPart;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DwarvenFortressStructure extends AbstractJigsawStructure {

    private static final int START_STRUCTURE_HEIGHT = 27;
    private static final int GENERATION_PADDING_Y = 2;
    private static final int MIN_TERRAIN_COVER = 8;

    public static final MapCodec<DwarvenFortressStructure> CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                    AbstractJigsawStructure.codec(
                            instance,
                            DwarvenFortressStructure::new
                    )
            );

    public DwarvenFortressStructure(
            StructureSettings config,
            Holder<StructureTemplatePool> startPool,
            Optional<ResourceLocation> startJigsawName,
            int size,
            HeightProvider startHeight,
            Optional<Heightmap.Types> projectStartToHeightmap,
            int maxDistanceFromCenter,
            DimensionPadding dimensionPadding,
            LiquidSettings liquidSettings,
            List<SinglePlacementPart> singlePlacementParts
    ) {
        super(
                config,
                startPool,
                startJigsawName,
                size,
                startHeight,
                projectStartToHeightmap,
                maxDistanceFromCenter,
                dimensionPadding,
                liquidSettings,
                singlePlacementParts
        );
    }

    @Override
    protected boolean extraSpawningChecks(GenerationContext context) {
        return true;
    }

    @Override
    protected int minTerrainCover() {
        return MIN_TERRAIN_COVER;
    }

    private BlockPos spawnOrigin(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();

        int x = chunkPos.getMinBlockX();
        int z = chunkPos.getMinBlockZ();

        int y = context.chunkGenerator().getFirstOccupiedHeight(
                x,
                z,
                Heightmap.Types.OCEAN_FLOOR_WG,
                context.heightAccessor(),
                context.randomState()
        );

        var column = context.chunkGenerator().getBaseColumn(
                x,
                z,
                context.heightAccessor(),
                context.randomState()
        );

        while (y > context.heightAccessor().getMinBuildHeight()
                && !column.getBlock(y).is(BlockTags.BASE_STONE_OVERWORLD)) {
            y--;
        }

        // start_height is not a world Y for this structure: the sampled value shifts the
        // terrain-derived origin, so the generated absolute(0) means no shift. Only absolute
        // anchors are meaningful here.
        int offset = this.startHeight.sample(
                context.random(),
                new WorldGenerationContext(
                        context.chunkGenerator(),
                        context.heightAccessor()
                )
        );

        return new BlockPos(
                x,
                y - START_STRUCTURE_HEIGHT - GENERATION_PADDING_Y + offset,
                z
        );
    }

    @Override
    protected BlockPos startPos(GenerationContext context) {
        return spawnOrigin(context);
    }

    @Override
    public StructureType<?> type() {
        return JolCraftStructures.DWARVEN_FORTRESS.type().get();
    }
}