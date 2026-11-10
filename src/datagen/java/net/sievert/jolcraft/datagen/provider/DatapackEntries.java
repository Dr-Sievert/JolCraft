package net.sievert.jolcraft.datagen.provider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.resources.ResourceKey;

/**
 * What the contributors add to the datapack registries, collected so that several contributors may fill one registry.
 * The game's registry set builder refuses a registry added twice, so {@link #builder()} adds each registry once, with
 * a bootstrap that runs every contributor's bootstraps for it in the order they were added, which is discovery order.
 */
public final class DatapackEntries {

    private final Map<ResourceKey<? extends Registry<?>>, List<RegistrySetBuilder.RegistryBootstrap<?>>> bootstraps = new LinkedHashMap<>();

    /** Adds entries to {@code key}; any number of adds per registry, from any number of contributors. */
    public <T> void add(ResourceKey<? extends Registry<T>> key, RegistrySetBuilder.RegistryBootstrap<T> bootstrap) {
        bootstraps.computeIfAbsent(key, _ -> new ArrayList<>()).add(bootstrap);
    }

    /** One registry set builder holding each collected registry once. */
    public RegistrySetBuilder builder() {
        RegistrySetBuilder builder = new RegistrySetBuilder();
        bootstraps.forEach((key, list) -> addMerged(builder, key, list));
        return builder;
    }

    // The map keeps each key with the bootstraps added under it, so the types agree whatever the wildcards say.
    @SuppressWarnings("unchecked")
    private static <T> void addMerged(RegistrySetBuilder builder, ResourceKey<? extends Registry<?>> key, List<RegistrySetBuilder.RegistryBootstrap<?>> list) {
        builder.add((ResourceKey<? extends Registry<T>>) key, context -> {
            for (RegistrySetBuilder.RegistryBootstrap<?> bootstrap : list) {
                ((RegistrySetBuilder.RegistryBootstrap<T>) bootstrap).run(context);
            }
        });
    }
}
