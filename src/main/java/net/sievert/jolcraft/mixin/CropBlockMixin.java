package net.sievert.jolcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;

import net.sievert.jolcraft.module.system.flora.GrowthEvent;

/**
 * Hands the light a crop reads (when it grows and when it survives) and the speed it grows at to {@link GrowthEvent},
 * and returns the answer. Holds no module: whoever listens decides. The host methods are named by their bare names
 * because MixinExtras' injectors take no {@code @Desc} method target; each name is unique in {@link CropBlock}, and
 * the calls inside are matched by {@code @Desc}.
 */
@Mixin(CropBlock.class)
public abstract class CropBlockMixin {

    @ModifyExpressionValue(method = "randomTick",
            at = @At(value = "INVOKE", desc = @Desc(owner = ServerLevel.class, value = "getRawBrightness", args = {BlockPos.class, int.class}, ret = int.class)))
    private int jolcraft$growthLight(int brightness, BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        return light(level, pos, brightness);
    }

    @ModifyExpressionValue(method = "hasSufficientLight",
            at = @At(value = "INVOKE", desc = @Desc(owner = LevelReader.class, value = "getRawBrightness", args = {BlockPos.class, int.class}, ret = int.class)))
    private static int jolcraft$survivalLight(int brightness, LevelReader level, BlockPos pos) {
        return light(level, pos, brightness);
    }

    @ModifyReturnValue(method = "getGrowthSpeed", at = @At("RETURN"))
    private static float jolcraft$speed(float speed, BlockState state, BlockGetter level, BlockPos pos) {
        return NeoForge.EVENT_BUS.post(new GrowthEvent.Speed(level, pos, speed)).speed();
    }

    private static int light(BlockGetter level, BlockPos pos, int brightness) {
        return NeoForge.EVENT_BUS.post(new GrowthEvent.Light(level, pos, brightness)).brightness();
    }
}
