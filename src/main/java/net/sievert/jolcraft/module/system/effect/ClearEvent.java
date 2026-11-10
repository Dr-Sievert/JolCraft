package net.sievert.jolcraft.module.system.effect;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import net.sievert.jolcraft.stereotype.Event;

/**
 * The game's clear-all-effects consume effect (milk, a totem's death protection) is about to clear an entity, on the
 * server. Posted by a mixin in place of the game's own clear; whoever listens clears what it will and
 * {@link #setCleared} answers whether it had effects to clear, which the mixin returns as the game's own clear would.
 */
@Event
public final class ClearEvent extends net.neoforged.bus.api.Event {

    private final LivingEntity entity;
    private final ItemStack stack;
    private boolean cleared;

    public ClearEvent(LivingEntity entity, ItemStack stack) {
        this.entity = entity;
        this.stack = stack;
    }

    public LivingEntity entity() {
        return entity;
    }

    /** The item that carried the consume effect; the totem, the milk bucket. */
    public ItemStack stack() {
        return stack;
    }

    /** Whether a listener cleared; false until one says so. */
    public boolean cleared() {
        return cleared;
    }

    public void setCleared(boolean cleared) {
        this.cleared = cleared;
    }
}
