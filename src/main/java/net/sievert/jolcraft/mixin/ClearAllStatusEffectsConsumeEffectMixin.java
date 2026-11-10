package net.sievert.jolcraft.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.sievert.jolcraft.module.system.effect.ClearEvent;

/**
 * Hands the game's clear of every status effect (milk, a totem's death protection, any item using it) to
 * {@link ClearEvent} and returns the answer in place of the game's own clear. Holds no module: whoever listens decides
 * what stays.
 */
@Mixin(ClearAllStatusEffectsConsumeEffect.class)
public abstract class ClearAllStatusEffectsConsumeEffectMixin {

    @Inject(target = @Desc(value = "apply", ret = boolean.class, args = {Level.class, ItemStack.class, LivingEntity.class}), at = @At("HEAD"), cancellable = true)
    private void jolcraft$clear(Level level, ItemStack stack, LivingEntity user, CallbackInfoReturnable<Boolean> cir) {
        ClearEvent event = NeoForge.EVENT_BUS.post(new ClearEvent(user, stack));
        cir.setReturnValue(event.cleared());
    }
}
