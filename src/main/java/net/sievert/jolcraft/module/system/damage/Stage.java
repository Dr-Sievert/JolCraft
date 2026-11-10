package net.sievert.jolcraft.module.system.damage;

import net.neoforged.bus.api.EventPriority;

import net.sievert.jolcraft.stereotype.Value;

/**
 * The one place the order of the mod's damage handlers is written: a system registers its listener at the stage it
 * belongs to, so two systems never need to know of each other. The first four stages order handlers of
 * {@code LivingIncomingDamageEvent}, before shields and armour, the last two of {@code LivingDamageEvent.Pre}, after
 * them; {@code LivingDamageEvent.Post} is read-only and has no stage.
 */
@Value
public final class Stage {

    /** Incoming, first: what the attacker adds. */
    public static final EventPriority ATTACK = EventPriority.HIGHEST;
    /** Incoming: the shape of the blow. */
    public static final EventPriority SHAPE = EventPriority.HIGH;
    /** Incoming: what the target's effects do before mitigation. */
    public static final EventPriority TARGET = EventPriority.NORMAL;
    /** Incoming, last: flat defences. */
    public static final EventPriority DEFENSE = EventPriority.LOW;
    /** Pre, first: the damage affinity rows ({@link Damage}). */
    public static final EventPriority AFFINITY = EventPriority.HIGH;
    /** Pre, last: the final defences, after affinity. */
    public static final EventPriority FINAL = EventPriority.NORMAL;

    private Stage() {
    }
}
