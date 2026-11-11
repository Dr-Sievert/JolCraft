package net.sievert.jolcraft.module.system.flora;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.sievert.jolcraft.stereotype.Actor;

/** The duskcap: the game's mushroom with a taller shape and its own huge form, {@link FloraFeatures#HUGE_DUSKCAP}. It follows the game's rules for light, spread and bonemeal. */
@Actor
public class DuskcapBlock extends MushroomBlock {

    /** Typed as the game's mushroom, whose {@code codec()} fixes the type to its own class. */
    public static final MapCodec<MushroomBlock> CODEC = simpleCodec(DuskcapBlock::new);

    private static final VoxelShape SHAPE = Block.column(6.0, 0.0, 12.0);

    public DuskcapBlock(BlockBehaviour.Properties properties) {
        super(FloraFeatures.HUGE_DUSKCAP, properties);
    }

    @Override
    public MapCodec<MushroomBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
