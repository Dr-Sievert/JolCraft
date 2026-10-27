package net.sievert.jolcraft.module.composite.loot;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import net.sievert.jolcraft.stereotype.Contract;

/**
 * A value a yield rolled, carried in a {@link Delivery} for the system to act on. The four the composite's own kinds
 * yield are here; a module's own kind yields a value of its own that implements this.
 */
@Contract
public interface Yielded {

    /** One stack an item pool rolled. */
    record Stack(ItemStack stack) implements Yielded {
    }

    /** Entities of a type to spawn, as many as rolled. */
    record Entities(EntityType<?> type, int count) implements Yielded {
    }

    /** A sound to play, with its volume and pitch rolled. */
    record Sound(Holder<SoundEvent> sound, SoundSource source, float volume, float pitch) implements Yielded {
    }

    /** An effect instance to apply, a copy of the yield's. */
    record Effect(MobEffectInstance effect) implements Yielded {
    }
}
