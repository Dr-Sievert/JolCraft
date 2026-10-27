package net.sievert.jolcraft.module.composite.loot;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;

import net.sievert.jolcraft.module.primitive.registration.Dispatch;
import net.sievert.jolcraft.stereotype.Handle;
import net.sievert.jolcraft.util.text.Dictionary;
import net.sievert.jolcraft.util.text.ModString;

/**
 * The registries of a root's yields: one of kinds, named by the id, and one of hooks, named by the id with
 * {@code _hook}. A root declares it through {@link Result} under its own namespace, so the mod's and the game-test
 * mod's do not clash, and every host of that root builds its codecs from it: {@link #codec()} reads one yield by the
 * kind {@code type} names and refuses a hook id no module declared, {@link #listCodec()} a list, {@link #choice()}
 * a weighted list one of which is rolled, {@link #single} a field typed as one kind with the same hook check, and
 * {@link #streamCodec()} the wire form a recipe kind hands to its key. A kind's bare codec checks no hook, so a host
 * never embeds one. The registries are resolved only when a codec is first used, so a record keeps its codec as a
 * constant.
 */
@Handle
public sealed interface YieldKinds {

    static YieldKinds of(Identifier id) {
        return new Spec(id, ResourceKey.createRegistryKey(id), ResourceKey.createRegistryKey(id.withPath(path -> ModString.underscored(path, Dictionary.HOOK))));
    }

    Identifier id();

    ResourceKey<Registry<MapCodec<? extends Yield>>> kinds();

    ResourceKey<Registry<ResultHook>> hooks();

    Codec<Yield> codec();

    /** The hook check alone, for a codec of one kind: a hook id no module declared is an error naming it. */
    <Y extends Yield> Function<Y, DataResult<Y>> hooksValidator();

    /** A field typed as one kind, read with that kind's codec and the hook check. */
    default <Y extends Yield> Codec<Y> single(MapCodec<Y> kind) {
        return kind.codec().validate(hooksValidator());
    }

    default Codec<List<Yield>> listCodec() {
        return codec().listOf();
    }

    default Codec<WeightedList<Yield>> choice() {
        return WeightedList.codec(codec());
    }

    default StreamCodec<RegistryFriendlyByteBuf, Yield> streamCodec() {
        return ByteBufCodecs.fromCodecWithRegistries(codec());
    }

    record Spec(Identifier id, ResourceKey<Registry<MapCodec<? extends Yield>>> kinds, ResourceKey<Registry<ResultHook>> hooks) implements YieldKinds {

        @Override
        public Codec<Yield> codec() {
            return Dispatch.<MapCodec<? extends Yield>, Yield>codec(kinds, Dictionary.TYPE, Yield::codec, codec -> codec).validate(hooksValidator());
        }

        @Override
        public <Y extends Yield> Function<Y, DataResult<Y>> hooksValidator() {
            return this::hooksDeclared;
        }

        private <Y extends Yield> DataResult<Y> hooksDeclared(Y yield) {
            Optional<? extends Registry<?>> registry = BuiltInRegistries.REGISTRY.getOptional(hooks.identifier());
            if (registry.isEmpty()) {
                return DataResult.error(() -> hooks.identifier() + " is no registry the game knows: the root declares its yield kinds through Result before registration is attached");
            }
            List<Identifier> unknown = yield.hooks().stream().filter(hook -> !registry.get().containsKey(hook)).toList();
            return unknown.isEmpty() ? DataResult.success(yield)
                    : DataResult.error(() -> "no hook " + unknown + " is declared through Result; a yield names hooks a module declared while it was built");
        }

        @Override
        public String toString() {
            return id.toString();
        }
    }
}
