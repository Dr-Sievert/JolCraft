package net.sievert.jolcraft.module.system.effect;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;

import net.sievert.jolcraft.stereotype.Value;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The sources that hold an infinite effect on an entity: the attachment {@link Effect#hold} keeps, saved with the entity.
 * Immutable; a change is a new value.
 */
@Value
public record Owned(Set<Identifier> sources) {

    public static final Owned NONE = new Owned(Set.of());

    public static final MapCodec<Owned> CODEC = Identifier.CODEC.listOf()
            .xmap(list -> new Owned(Set.copyOf(list)), owned -> List.copyOf(owned.sources()))
            .fieldOf(Dictionary.SOURCES);

    public Owned {
        sources = Set.copyOf(sources);
    }

    public boolean has(Identifier source) {
        return sources.contains(source);
    }

    public Owned with(Identifier source) {
        Set<Identifier> added = new LinkedHashSet<>(sources);
        added.add(source);
        return new Owned(added);
    }

    public Owned without(Identifier source) {
        Set<Identifier> removed = new LinkedHashSet<>(sources);
        removed.remove(source);
        return new Owned(removed);
    }
}
