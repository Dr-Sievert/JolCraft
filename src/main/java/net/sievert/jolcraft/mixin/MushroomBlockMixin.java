package net.sievert.jolcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;

import net.sievert.jolcraft.module.system.flora.GrowthEvent;

/**
 * Hands a mushroom's chance to spread and its chance to grow huge from bonemeal to {@link GrowthEvent}. A chance no
 * listener moved leaves the game's own roll untouched. Holds no module: whoever listens decides. The host methods are
 * named by their bare names because MixinExtras' injectors take no {@code @Desc} method target; each name is unique in
 * {@link MushroomBlock}, and the call inside is matched by {@code @Desc}.
 */
@Mixin(MushroomBlock.class)
public abstract class MushroomBlockMixin {

    /** The game rolls {@code nextInt(25) == 0}; a moved chance is rolled on the same random and answered as 0 or 1. */
    @ModifyExpressionValue(method = "randomTick",
            at = @At(value = "INVOKE", desc = @Desc(owner = RandomSource.class, value = "nextInt", args = int.class, ret = int.class), ordinal = 0))
    private int jolcraft$spread(int roll, BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        GrowthEvent.Spread event = NeoForge.EVENT_BUS.post(new GrowthEvent.Spread(level, pos));
        if (!event.changed()) {
            return roll;
        }
        return random.nextFloat() < event.chance() ? 0 : 1;
    }

    /** The game compares {@code nextFloat()} with the double 0.4. */
    @ModifyExpressionValue(method = "isBonemealSuccess", at = @At(value = "CONSTANT", args = "doubleValue=0.4"))
    private double jolcraft$bonemeal(double chance, Level level, RandomSource random, BlockPos pos, BlockState state) {
        GrowthEvent.Bonemeal event = NeoForge.EVENT_BUS.post(new GrowthEvent.Bonemeal(level, pos));
        return event.chance() == GrowthEvent.Bonemeal.VANILLA ? chance : event.chance();
    }
}
