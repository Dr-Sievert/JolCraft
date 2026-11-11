package net.sievert.jolcraft.module.system.flora;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.sievert.jolcraft.stereotype.Actor;

/** Barley: wheat's eight ages, shapes and light rules, with its own seeds. */
@Actor
public class BarleyBlock extends CropBlock {

    public static final MapCodec<BarleyBlock> CODEC = simpleCodec(BarleyBlock::new);

    public BarleyBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<BarleyBlock> codec() {
        return CODEC;
    }

    /** The seeds are the block item registered for this block, so the crop is its own seed. */
    @Override
    protected ItemLike getBaseSeedId() {
        return this;
    }
}
