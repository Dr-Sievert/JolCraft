package net.sievert.jolcraft.module.primitive.stat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;

import net.sievert.jolcraft.stereotype.Handle;
import org.jetbrains.annotations.NotNull;

/**
 * One statistic of the mod: its id, under which the game keeps and names it, and how its count is shown, a plain
 * number unless {@link #shownAs(StatFormatter)} says time or distance. A key is a constant where it is used,
 * declared through {@link Stat} while its module is built.
 */
@Handle
public sealed interface StatKey {

    static StatKey of(Identifier id) {
        return new Spec(id, StatFormatter.DEFAULT);
    }

    Identifier id();

    StatFormatter formatter();

    /** Shown with the formatter: {@code StatFormatter.TIME} for ticks, {@code DISTANCE} for centimetres. */
    StatKey shownAs(StatFormatter formatter);

    /** The translation key of this statistic's name. */
    default String name() {
        return Stat.name(id());
    }

    /** Whether the game has registered the statistic; it does after the mod constructors ran. */
    default boolean isRegistered() {
        return BuiltInRegistries.CUSTOM_STAT.containsKey(id());
    }

    /** The game's statistic for this key, under the id the game registered; there once the game registered what a module declared. */
    default net.minecraft.stats.Stat<Identifier> stat() {
        Identifier registered = BuiltInRegistries.CUSTOM_STAT.getValue(id());
        if (registered == null) {
            throw new IllegalStateException(id() + " is not registered: no module declared it through Stat, or it is used before the game registered statistics, which happens after the mod constructors ran");
        }
        return Stats.CUSTOM.get(registered, formatter());
    }

    /** The key with its formatter. */
    record Spec(Identifier id, StatFormatter formatter) implements StatKey {

        @Override
        public StatKey shownAs(StatFormatter shown) {
            return new Spec(id, shown);
        }

        @Override
        public @NotNull String toString() {
            return id.toString();
        }
    }
}
