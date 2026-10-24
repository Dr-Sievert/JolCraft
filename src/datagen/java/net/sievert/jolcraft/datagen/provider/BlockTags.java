package net.sievert.jolcraft.datagen.provider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.datagen.contribute.TagContributor;
import org.jetbrains.annotations.NotNull;

/** Every module's block tags. */
public final class BlockTags extends BlockTagsProvider {

    private final List<TagContributor> contributors;

    public BlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, List<TagContributor> contributors) {
        super(output, lookup, JolCraft.MOD_ID);
        this.contributors = contributors;
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider registries) {
        contributors.forEach(contributor -> contributor.blockTags(this::tag));
    }
}
