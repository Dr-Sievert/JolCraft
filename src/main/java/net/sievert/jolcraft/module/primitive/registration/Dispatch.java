package net.sievert.jolcraft.module.primitive.registration;

import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;

import net.sievert.jolcraft.stereotype.Helper;

/**
 * Codecs over a registry of kinds: data that names its kind in a field and is read and written with that kind's codec,
 * the way the game reads a loot condition by its {@code condition}. The registry is resolved by key through the
 * game's root registry when the codec is first used, not when it is built, so a record may hold the codec as a
 * constant while the module that owns the registry declares it through {@link Registration#custom} in its constructor:
 * a custom registry, since the root registry holds those and not the datapack ones. A codec used before the game
 * created the registries, or over a custom registry no module declared, fails naming the registry; a kind the data
 * names that the registry lacks is a decoding error, as the game reports one. The field naming the kind sits beside
 * the kind's own fields, so no kind's codec may have a field of that name.
 */
@Helper
public final class Dispatch {

    private Dispatch() {
    }

    /**
     * A codec for values whose kind is an entry of the registry: {@code field} names the kind in the data,
     * {@code kindOf} answers a value's kind, and {@code codecOf} the codec a kind reads and writes its values with.
     */
    public static <K, V> Codec<V> codec(ResourceKey<? extends Registry<K>> registry, String field,
                                       Function<? super V, ? extends K> kindOf, Function<? super K, ? extends MapCodec<? extends V>> codecOf) {
        return Codec.lazyInitialized(() -> Dispatch.<K>registry(registry).byNameCodec().dispatch(field, kindOf, codecOf));
    }

    @SuppressWarnings("unchecked")
    private static <K> Registry<K> registry(ResourceKey<? extends Registry<K>> key) {
        return (Registry<K>) BuiltInRegistries.REGISTRY.getOptional(key.identifier()).orElseThrow(() -> new IllegalStateException(
                key.identifier() + " is no custom registry the game knows: no module declared it through Registration.custom,"
                        + " or the codec was used before the game created the registries, which happens after the mod constructors ran"));
    }
}
