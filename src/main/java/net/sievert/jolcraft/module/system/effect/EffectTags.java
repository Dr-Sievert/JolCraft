package net.sievert.jolcraft.module.system.effect;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;

import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.stereotype.Value;
import net.sievert.jolcraft.util.text.Dictionary;

/** The tags the effect system reads; the features that own effects fill them in their data packs. */
@Value
public final class EffectTags {

    /** The effects a clear leaves: milk, a totem and any item using the game's clear-all consume effect. A feature's cure, {@code /effect clear}, expiry and death still remove them. */
    public static final TagKey<MobEffect> INCURABLE = Ids.tag(Registries.MOB_EFFECT, Dictionary.JOLCRAFT, Dictionary.INCURABLE);

    private EffectTags() {
    }
}
