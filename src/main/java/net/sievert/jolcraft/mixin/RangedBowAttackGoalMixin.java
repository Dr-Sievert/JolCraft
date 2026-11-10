package net.sievert.jolcraft.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.sievert.jolcraft.module.system.control.GoalEvent;
import net.sievert.jolcraft.module.system.control.Stop;

/**
 * The bow goal of the skeletons and every mob that draws a bow.
 * Holds no module: when the game's goal answers that it can run, {@link GoalEvent} is posted and a cancelled
 * event turns the answer into false, so the game stops the goal through its own {@code stop()}.
 */
@Mixin(RangedBowAttackGoal.class)
public abstract class RangedBowAttackGoalMixin {

    @Shadow
    @Final
    private Mob mob;

    @Inject(target = {@Desc(value = "canUse", ret = boolean.class), @Desc(value = "canContinueToUse", ret = boolean.class)}, at = @At("RETURN"), cancellable = true)
    private void jolcraft$control(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && NeoForge.EVENT_BUS.post(new GoalEvent(mob, Stop.RANGED)).isCanceled()) {
            cir.setReturnValue(false);
        }
    }
}
