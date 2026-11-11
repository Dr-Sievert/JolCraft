package net.sievert.jolcraft.module.system.flora;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;

import net.sievert.jolcraft.stereotype.Actor;

/**
 * Soil that plants love: a hoe tills it to verdant farmland and it sparkles now and then. What may stand on it is
 * the tags' business ({@link FloraTags#VERDANT} and the vanilla ones the tag contributor fills), so it overrides no
 * {@code canSustainPlant}.
 */
@Actor
public class VerdantSoilBlock extends Block {

    public static final MapCodec<VerdantSoilBlock> CODEC = simpleCodec(VerdantSoilBlock::new);

    /** One animate tick in this many shows a sparkle. */
    private static final int SPARKLE_ODDS = 100;

    public VerdantSoilBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends VerdantSoilBlock> codec() {
        return CODEC;
    }

    /**
     * A hoe that can till turns the soil to verdant farmland when the block above is air, and does nothing otherwise;
     * never the game's own hoe branch, which would give vanilla farmland or dirt. Every other ability is the game's.
     */
    @Override
    public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility ability, boolean simulate) {
        if (ability == ItemAbilities.HOE_TILL) {
            boolean tills = context.getItemInHand().canPerformAction(ability) && context.getLevel().getBlockState(context.getClickedPos().above()).isAir();
            return tills ? FloraContent.VERDANT_FARMLAND.get().defaultBlockState() : null;
        }
        return super.getToolModifiedState(state, context, ability, simulate);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        sparkle(level, pos, random);
    }

    /** One happy-villager particle just above the block's top, at a random spot, one call in a hundred. Shared with the farmland. */
    static void sparkle(Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(SPARKLE_ODDS) == 0) {
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, pos.getX() + random.nextDouble(), pos.getY() + 1.1, pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
        }
    }
}
