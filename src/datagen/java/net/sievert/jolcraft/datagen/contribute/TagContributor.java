package net.sievert.jolcraft.datagen.contribute;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

/** A module's block, item and biome tags, and which block tags its item tags copy. The tags of another registry get a method here when a module needs one. */
public interface TagContributor {

    default void blockTags(Tags<Block> tags) {
    }

    default void itemTags(Tags<Item> tags) {
    }

    /** Biome tags, which name biomes by key since biomes are a datapack registry: vanilla biomes, vanilla's biome tags and optional ones. */
    default void biomeTags(KeyTags<Biome> tags) {
    }

    /** Block tags whose contents the matching item tags take over; copied after every block tag is declared. */
    default void copies(TagCopier copier) {
    }
}
