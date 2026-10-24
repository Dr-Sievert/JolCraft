package net.sievert.jolcraft.datagen.contribute;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Copies a block tag's contents into an item tag, so the two never drift apart. */
@FunctionalInterface
public interface TagCopier {

    void copy(TagKey<Block> blockTag, TagKey<Item> itemTag);
}
