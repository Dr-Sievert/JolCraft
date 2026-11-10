package net.sievert.jolcraft.module.system.control;

import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.ICancellableEvent;

import net.sievert.jolcraft.stereotype.Event;

/**
 * A vanilla mob goal (melee, bow, crossbow, spell) answered that it can run, for a mob on the server. Posted by a mixin at
 * the return of the goal's {@code canUse} and {@code canContinueToUse}; whoever listens cancels it to refuse the goal,
 * which the game then stops through the goal's own {@code stop()}. {@link #stop()} names what the goal does.
 */
@Event
public final class GoalEvent extends net.neoforged.bus.api.Event implements ICancellableEvent {

    private final Mob mob;
    private final Stop stop;

    public GoalEvent(Mob mob, Stop stop) {
        this.mob = mob;
        this.stop = stop;
    }

    public Mob mob() {
        return mob;
    }

    public Stop stop() {
        return stop;
    }
}
