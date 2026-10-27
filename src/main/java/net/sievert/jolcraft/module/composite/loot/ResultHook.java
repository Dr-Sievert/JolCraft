package net.sievert.jolcraft.module.composite.loot;

import java.util.Optional;

import net.minecraft.world.level.storage.loot.LootContext;

import net.sievert.jolcraft.stereotype.Contract;

/**
 * A behaviour a yield names by id: a module declares it through {@link Result} while it is built, and data refers to
 * it. {@link #canApply} is asked of each hook of each yield, in order, before anything is rolled, until one refuses;
 * a refusal refuses the whole delivery, so the system consumes nothing. {@link #apply} runs on each rolled value of the yields that name it and
 * may rewrite the value or drop it; dropping one value leaves the delivery otherwise whole.
 */
@Contract
public interface ResultHook {

    /** Whether the yields may be applied at all against this context; true unless the hook says otherwise. */
    default boolean canApply(LootContext context) {
        return true;
    }

    /** The rolled value as this hook leaves it: the same, another, or none to drop it. */
    default Optional<Yielded> apply(LootContext context, Yielded rolled) {
        return Optional.of(rolled);
    }
}
