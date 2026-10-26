package net.sievert.jolcraft.module.primitive.advancement;

import java.util.function.Predicate;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.sievert.jolcraft.stereotype.Handle;
import org.jetbrains.annotations.NotNull;

/**
 * One kind of criterion the mod's features fire: its id, under which an advancement's data names it, and the codec
 * of its instance, the record a data pack writes under {@code conditions} and a firing is matched against. A key is
 * a constant where it is used, declared through {@link Advancement} while its module is built; {@link #fire} grants
 * every criterion of the kind whose instance the matcher accepts, and {@link #criterion} is datagen's way to put the
 * kind into an advancement.
 */
@Handle
public sealed interface TriggerKey<T extends SimpleCriterionTrigger.SimpleInstance> {

    static <T extends SimpleCriterionTrigger.SimpleInstance> TriggerKey<T> of(Identifier id, Codec<T> codec) {
        return new Spec<>(id, codec);
    }

    Identifier id();

    Codec<T> codec();

    /** The registered trigger, there once the game registered what a module declared. */
    Trigger<T> trigger();

    /** The game's trigger for this kind, built once by {@link Advancement} when the game registers. */
    Trigger<T> build();

    /** Fires for the player: every criterion of this kind on an advancement the player has not completed is granted when the matcher accepts its instance and the instance's own player predicate, if it has one, matches the player. */
    default void fire(ServerPlayer player, Predicate<T> matcher) {
        trigger().fire(player, matcher);
    }

    /** Fires for the player, matching every instance. */
    default void fire(ServerPlayer player) {
        fire(player, _ -> true);
    }

    /** A criterion of this kind with the instance, for an advancement datagen writes. */
    default Criterion<@NotNull T> criterion(T instance) {
        return trigger().createCriterion(instance);
    }

    /** The game's trigger over the key's codec, with the firing the game keeps protected opened to the key. */
    final class Trigger<T extends SimpleCriterionTrigger.SimpleInstance> extends SimpleCriterionTrigger<@NotNull T> {
        private final Codec<T> codec;

        Trigger(Codec<T> codec) {
            this.codec = codec;
        }

        @Override
        public @NotNull Codec<T> codec() {
            return codec;
        }

        public void fire(ServerPlayer player, Predicate<T> matcher) {
            trigger(player, matcher);
        }
    }

    /** The key with its codec; {@code deferred} resolves the registered trigger lazily. */
    record Spec<T extends SimpleCriterionTrigger.SimpleInstance>(Identifier id, Codec<T> codec, DeferredHolder<CriterionTrigger<?>, @NotNull Trigger<T>> deferred) implements TriggerKey<T> {

        Spec(Identifier id, Codec<T> codec) {
            this(id, codec, DeferredHolder.create(Registries.TRIGGER_TYPE, id));
        }

        @Override
        public Trigger<T> trigger() {
            if (!deferred.isBound()) {
                throw new IllegalStateException(id + " is not registered: no module declared it through Advancement,"
                        + " or it is used before the game registered criterion kinds, which happens after the mod constructors ran");
            }
            return deferred.get();
        }

        @Override
        public Trigger<T> build() {
            return new Trigger<>(codec);
        }

        @Override
        public @NotNull String toString() {
            return id.toString();
        }
    }
}
