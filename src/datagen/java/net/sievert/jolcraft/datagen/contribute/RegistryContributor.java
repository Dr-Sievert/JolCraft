package net.sievert.jolcraft.datagen.contribute;

import net.sievert.jolcraft.datagen.provider.DatapackEntries;

/**
 * A module's datapack-registry entries (its own registries and vanilla's, such as biomes or structures). A contributor
 * adds as many times per registry as it likes; the run adds each registry once, merging every contributor's adds in
 * discovery order, so two contributors may fill the same registry.
 */
public interface RegistryContributor {

    void registries(DatapackEntries entries);
}
