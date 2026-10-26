package net.sievert.jolcraft.module.primitive.loot;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.sievert.jolcraft.stereotype.Handle;
import org.jetbrains.annotations.NotNull;

/**
 * One kind the mod adds to loot: a global loot modifier, a loot condition or a loot function, named by its id in
 * the data pack and read through its codec. A key is a constant where it is used, declared through {@link Loot}
 * while its module is built; the kind's class answers the game's {@code codec()} with the key's.
 */
@Handle
public sealed interface LootKey<T> {

    /** A modifier that changes what a table rolled, in the data pack's {@code loot_modifiers}. */
    static <M extends IGlobalLootModifier> LootKey<M> modifier(Identifier id, MapCodec<M> codec) {
        return new Spec<>(id, Kind.MODIFIER, codec);
    }

    /** A condition a table or a modifier may ask. */
    static <C extends LootItemCondition> LootKey<C> condition(Identifier id, MapCodec<C> codec) {
        return new Spec<>(id, Kind.CONDITION, codec);
    }

    /** A function a table may apply to what it rolled. */
    static <F extends LootItemFunction> LootKey<F> function(Identifier id, MapCodec<F> codec) {
        return new Spec<>(id, Kind.FUNCTION, codec);
    }

    Identifier id();

    Kind kind();

    MapCodec<T> codec();

    /** Whether the game has registered the kind; it does after the mod constructors ran. */
    default boolean isRegistered() {
        return switch (kind()) {
            case MODIFIER -> NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS.containsKey(id());
            case CONDITION -> BuiltInRegistries.LOOT_CONDITION_TYPE.containsKey(id());
            case FUNCTION -> BuiltInRegistries.LOOT_FUNCTION_TYPE.containsKey(id());
        };
    }

    /** The three registries a kind can belong to. */
    enum Kind {
        MODIFIER,
        CONDITION,
        FUNCTION
    }

    /** The key with its codec. */
    record Spec<T>(Identifier id, Kind kind, MapCodec<T> codec) implements LootKey<T> {

        @Override
        public @NotNull String toString() {
            return kind.name().toLowerCase(java.util.Locale.ROOT) + " " + id;
        }
    }
}
