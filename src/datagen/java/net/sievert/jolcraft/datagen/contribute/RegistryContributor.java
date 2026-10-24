package net.sievert.jolcraft.datagen.contribute;

import net.minecraft.core.RegistrySetBuilder;

/** A module's datapack-registry entries (its own registries and vanilla's, such as biomes or structures), one add per registry. */
public interface RegistryContributor {

    void registries(RegistrySetBuilder builder);
}
