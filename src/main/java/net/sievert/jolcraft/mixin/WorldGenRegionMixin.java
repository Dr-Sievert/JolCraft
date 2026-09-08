package net.sievert.jolcraft.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.sievert.jolcraft.world.worldgen.structure.util.FeaturePlacementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin {

    @Inject(
            method = "setBlock",
            at = @At("HEAD"),
            cancellable = true
    )
    private void jolcraft$preventFeaturePlacementInsideProtectedStructure(
            BlockPos pos,
            BlockState state,
            int flags,
            int recursionLeft,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!FeaturePlacementContext.isPlacingFeature()) {
            return;
        }

        WorldGenRegion region = (WorldGenRegion) (Object) this;

        if (!region.ensureCanWrite(pos)) {
            cir.setReturnValue(false);
            return;
        }

        if (FeaturePlacementContext.isProtected(region, pos)) {
            cir.setReturnValue(false);
        }
    }
}
