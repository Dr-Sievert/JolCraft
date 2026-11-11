package net.sievert.jolcraft.module.system.outcome;

import net.sievert.jolcraft.module.composite.loot.YieldKinds;
import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.stereotype.Keys;
import net.sievert.jolcraft.util.text.Dictionary;

/** The mod's result handles: the one {@link YieldKinds}, which the root declares to {@code Result} and every host's records embed. */
@Keys
public final class OutcomeKeys {

    /**
     * The mod's yield registries, {@code jolcraft:yield} of kinds and {@code jolcraft:yield_hook} of hooks. It reads
     * nothing of a host, so a host's keys, then its record, then this set initialise one way.
     */
    public static final YieldKinds YIELDS = YieldKinds.of(Ids.of(Dictionary.JOLCRAFT, Dictionary.YIELD));

    private OutcomeKeys() {
    }
}
