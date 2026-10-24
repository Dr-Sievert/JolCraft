package net.sievert.jolcraft.datagen.provider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagCopyingItemTagProvider;

import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.datagen.contribute.TagContributor;

/** Every module's item tags, and the block tags they copy; the copies come after every block tag is declared. */
public final class ItemTags extends BlockTagCopyingItemTagProvider {

    private final List<TagContributor> contributors;

    public ItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
            CompletableFuture<TagsProvider.TagLookup<Block>> blockTags, List<TagContributor> contributors) {
        super(output, lookup, blockTags, JolCraft.MOD_ID);
        this.contributors = contributors;
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        contributors.forEach(contributor -> contributor.itemTags(this::tag));
        contributors.forEach(contributor -> contributor.copies(this::copy));
    }

    @Override
    public void copy(TagKey<Block> blockTag, TagKey<Item> itemTag) {
        super.copy(blockTag, itemTag);
    }
}
