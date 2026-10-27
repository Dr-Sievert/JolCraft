package net.sievert.jolcraft.module.primitive.loot;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.mojang.serialization.DataResult;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.Validatable;
import org.jetbrains.annotations.NotNull;

import net.sievert.jolcraft.stereotype.Value;

/**
 * The shape of a loot context: a named set of the keys its data may read, required or allowed. One shape serves
 * both ends of a piece of data that reads a context. At load a codec validates the data against it, through
 * {@link #validator()} or {@link #check}, the game's own check; at use the code that fires the data builds its
 * context from it through {@code Loot.context}. Both check the same set, but building enforces only the required
 * keys, and a context may carry keys beyond the shape, as NeoForge allows: so a key the data always reads goes
 * under required, not {@link #allowing}. A module keeps its shapes as constants beside it, over the game's keys or
 * keys of its own, and a host puts what its data may read, such as the stacks it matched, into the context under
 * keys of its shape.
 */
@Value
public record Shape(Identifier id, ContextKeySet keys) {

    public Shape {
        Objects.requireNonNull(id, "an id");
        Objects.requireNonNull(keys, "the keys");
    }

    /** A shape requiring the keys; nothing else may be read until {@link #allowing} says so. */
    public static Shape of(Identifier id, ContextKey<?>... required) {
        ContextKeySet.Builder builder = new ContextKeySet.Builder();
        for (ContextKey<?> key : required) {
            builder.required(key);
        }
        return new Shape(id, builder.build());
    }

    /** This shape with the keys allowed as well, provided or not; a key already required is refused. */
    public Shape allowing(ContextKey<?>... optional) {
        ContextKeySet.Builder builder = new ContextKeySet.Builder();
        keys.required().forEach(builder::required);
        for (ContextKey<?> key : keys.allowed()) {
            if (!keys.required().contains(key)) {
                builder.optional(key);
            }
        }
        for (ContextKey<?> key : optional) {
            builder.optional(key);
        }
        return new Shape(id, builder.build());
    }

    /** The keys a context under this shape must carry. */
    public Set<ContextKey<?>> required() {
        return keys.required();
    }

    /** Every key data under this shape may read, the required ones included. */
    public Set<ContextKey<?>> allowed() {
        return keys.allowed();
    }

    /**
     * The game's check of a context user (a condition, a function, a pool, a table, or data of the mod's own)
     * against this shape, for a codec: {@code codec.validate(shape.validator())}. A key the data reads that the shape
     * does not allow is an error naming the key, so the data is refused at load rather than failing when fired.
     */
    public <T extends Validatable> Function<T, DataResult<T>> validator() {
        return Validatable.validatorForContext(keys);
    }

    /** The same check over a list, for a codec of lists: {@code codec.listOf().validate(shape.listValidator())}. */
    public <T extends Validatable> Function<List<T>, DataResult<List<T>>> listValidator() {
        return Validatable.listValidatorForContext(keys);
    }

    /** The same check once, for a value in hand. */
    public <T extends Validatable> DataResult<T> check(T validatable) {
        return this.<T>validator().apply(validatable);
    }

    @Override
    public @NotNull String toString() {
        return id.toString();
    }
}
