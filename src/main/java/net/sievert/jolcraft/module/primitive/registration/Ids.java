package net.sievert.jolcraft.module.primitive.registration;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import net.sievert.jolcraft.stereotype.Helper;
import net.sievert.jolcraft.util.text.ModString;

/**
 * Identifiers, registry keys and tag keys built from {@code Dictionary} words, so no id is ever a string literal.
 * The namespace is passed in: {@code Dictionary.JOLCRAFT} for the mod's own, {@code Dictionary.C} for the common
 * tags mods share, {@code Dictionary.MINECRAFT} for vanilla's.
 */
@Helper
public final class Ids {

    private Ids() {
    }

    /** {@code namespace:word_word}. */
    public static Identifier of(String namespace, String... words) {
        return Identifier.fromNamespaceAndPath(namespace, ModString.underscored(words));
    }

    /** {@code namespace:folder/name}, for the paths with folders that tags such as {@code c:ingots/copper} use. */
    public static Identifier path(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    /** The key of a registry of the mod's own. */
    public static <T> ResourceKey<Registry<T>> registry(String namespace, String... words) {
        return ResourceKey.createRegistryKey(of(namespace, words));
    }

    /** The key of one entry in a registry. */
    public static <T> ResourceKey<T> key(ResourceKey<? extends Registry<T>> registry, String namespace, String... words) {
        return ResourceKey.create(registry, of(namespace, words));
    }

    public static <T> TagKey<T> tag(ResourceKey<? extends Registry<T>> registry, String namespace, String... words) {
        return TagKey.create(registry, of(namespace, words));
    }

    public static <T> TagKey<T> tag(ResourceKey<? extends Registry<T>> registry, Identifier id) {
        return TagKey.create(registry, id);
    }
}
