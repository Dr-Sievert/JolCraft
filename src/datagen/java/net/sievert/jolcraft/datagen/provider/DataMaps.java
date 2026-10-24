package net.sievert.jolcraft.datagen.provider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;

import net.sievert.jolcraft.datagen.contribute.DataMapContributor;
import org.jetbrains.annotations.NotNull;

/** Every module's data map entries; {@code builder(type)} is vanilla NeoForge's and already public. */
public final class DataMaps extends DataMapProvider {

    private final List<DataMapContributor> contributors;

    public DataMaps(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, List<DataMapContributor> contributors) {
        super(output, lookup);
        this.contributors = contributors;
    }

    @Override
    protected void gather(HolderLookup.@NotNull Provider provider) {
        contributors.forEach(contributor -> contributor.dataMaps(this));
    }
}
