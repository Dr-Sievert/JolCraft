package net.sievert.jolcraft.mixin;

import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
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
 * The spell goal of the spellcasting illagers (the evoker's fangs and summon goals defer to it; its wololo goal does not and stays outside).
 * Holds no module: when the game's goal answers that it can run, {@link GoalEvent} is posted and a cancelled
 * event turns the answer into false, so the game stops the goal through its own {@code stop()}.
 */
@Mixin(SpellcasterIllager.SpellcasterUseSpellGoal.class)
public abstract class SpellcasterUseSpellGoalMixin {

    /** The synthetic reference to the illager the inner class belongs to. */
    @Shadow(aliases = "this$0")
    @Final
    private SpellcasterIllager jolcraft$spellcaster;

    @Inject(target = {@Desc(value = "canUse", ret = boolean.class), @Desc(value = "canContinueToUse", ret = boolean.class)}, at = @At("RETURN"), cancellable = true)
    private void jolcraft$control(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && NeoForge.EVENT_BUS.post(new GoalEvent(jolcraft$spellcaster, Stop.USE)).isCanceled()) {
            cir.setReturnValue(false);
        }
    }
}
