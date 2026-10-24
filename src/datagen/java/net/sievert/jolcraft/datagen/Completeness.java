package net.sievert.jolcraft.datagen;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

import net.sievert.jolcraft.JolCraft;

/** The registry walk the completeness checks share: everything of the mod's namespace in a registry, and how a gap fails the run. */
public final class Completeness {

    private Completeness() {
    }

    /** The mod's own entries of a registry. */
    public static <T> Stream<Holder.Reference<T>> ours(Registry<T> registry) {
        return registry.listElements().filter(holder -> holder.key().identifier().getNamespace().equals(JolCraft.MOD_ID));
    }

    public static Identifier id(Holder.Reference<?> holder) {
        return holder.key().identifier();
    }

    /** Fails the data run, naming every missing thing, unless nothing is missing. */
    public static void require(String what, List<?> missing) {
        if (!missing.isEmpty()) {
            throw new IllegalStateException(missing.size() + " missing " + what + ": " + missing
                    + ". Add it in a contributor.");
        }
    }

    public static <T> Predicate<Holder.Reference<T>> notExempt(Predicate<Identifier> exempt) {
        return holder -> !exempt.test(id(holder));
    }
}
