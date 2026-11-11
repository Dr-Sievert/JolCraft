package net.sievert.jolcraft.datagen.provider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.KeyTagProvider;
import net.minecraft.world.level.biome.Biome;

import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.datagen.contribute.TagContributor;
import org.jetbrains.annotations.NotNull;

/** Every module's biome tags; biomes are a datapack registry, so a tag names them by key. */
public final class BiomeTags extends KeyTagProvider<Biome> {

    private final List<TagContributor> contributors;

    public BiomeTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, List<TagContributor> contributors) {
        super(output, Registries.BIOME, lookup, JolCraft.MOD_ID);
        this.contributors = contributors;
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider registries) {
        contributors.forEach(contributor -> contributor.biomeTags(this::tag));
    }
}
