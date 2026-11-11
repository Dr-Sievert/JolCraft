package net.sievert.jolcraft.module.system.flora;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.sievert.jolcraft.stereotype.Actor;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * A small flower with a flower's shape and offset that is not the game's {@code FlowerBlock}, which has to name a
 * stew effect of some other feature. It stands on the blocks of the tag it is built with: skybell on what vegetation
 * stands on, cyanella on {@link FloraTags#SUPPORTS_CYANELLA}.
 */
@Actor
public class BloomBlock extends VegetationBlock {

    public static final MapCodec<BloomBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            TagKey.hashedCodec(Registries.BLOCK).fieldOf(Dictionary.SUPPORTS).forGetter(bloom -> bloom.ground),
            propertiesCodec()
    ).apply(instance, BloomBlock::new));

    private static final VoxelShape SHAPE = Block.column(6.0, 0.0, 10.0);

    private final TagKey<Block> ground;

    public BloomBlock(TagKey<Block> ground, BlockBehaviour.Properties properties) {
        super(properties);
        this.ground = ground;
    }

    @Override
    public MapCodec<BloomBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.move(state.getOffset(pos));
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ground);
    }
}
