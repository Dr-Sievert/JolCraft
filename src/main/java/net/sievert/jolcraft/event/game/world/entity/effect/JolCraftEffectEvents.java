package net.sievert.jolcraft.event.game.world.entity.effect;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.ArmorHurtEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.sievert.jolcraft.event.game.world.entity.effect.util.harmful.JolCraftCrowdControlEventsHelper;
import net.sievert.jolcraft.event.game.world.entity.effect.util.harmful.JolCraftCurseEventsHelper;
import net.sievert.jolcraft.event.game.world.entity.effect.util.JolCraftEffectApplicationEventsHelper;
import net.sievert.jolcraft.event.game.world.entity.effect.util.JolCraftEffectDamageEventsHelper;
import net.sievert.jolcraft.event.game.world.entity.effect.util.JolCraftEffectDurationEventsHelper;
import net.sievert.jolcraft.event.game.world.entity.effect.util.JolCraftStackingEffectEventsHelper;

public final class JolCraftEffectEvents {

    private JolCraftEffectEvents() {}

    public static void onEffectAdded(MobEffectEvent.Added event) {
        JolCraftEffectApplicationEventsHelper.onEffectAdded(event);
        JolCraftStackingEffectEventsHelper.onEffectAdded(event);
        JolCraftEffectDurationEventsHelper.onEffectAdded(event);
        JolCraftCurseEventsHelper.onEffectAdded(event);
    }

    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        JolCraftCurseEventsHelper.onEffectRemoved(event);
    }

    public static void onEffectExpired(MobEffectEvent.Expired event) {
        JolCraftCurseEventsHelper.onEffectExpired(event);
    }

    public static void onEffectApplicable(
            MobEffectEvent.Applicable event
    ) {
        JolCraftEffectApplicationEventsHelper.onEffectApplicable(event);
        JolCraftCrowdControlEventsHelper.onEffectApplicable(event);
    }

    public static void applyPreMitigationTargetModifiers(
            LivingIncomingDamageEvent event
    ) {
        JolCraftCurseEventsHelper
                .applyPreMitigationTargetModifiers(event);
    }

    public static void applyEarlyFlatDefenses(
            LivingIncomingDamageEvent event
    ) {
        JolCraftEffectDamageEventsHelper
                .applyEarlyFlatDefenses(event);
    }

    public static void onArmorHurt(
            ArmorHurtEvent event
    ) {
        JolCraftEffectDamageEventsHelper.onArmorHurt(event);
    }

    public static void onLivingHeal(
            LivingHealEvent event
    ) {
        JolCraftCurseEventsHelper.onLivingHeal(event);
    }

    public static void onEntityTick(
            EntityTickEvent.Post event
    ) {
        JolCraftCrowdControlEventsHelper.onEntityTick(event);
        if (event.getEntity() instanceof LivingEntity entity) {
            JolCraftCurseEventsHelper.refreshVitalityCurseModifier(entity);
        }
    }
}
