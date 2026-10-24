package net.sievert.jolcraft.datagen.contribute;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** A module's block and item tags, and which block tags its item tags copy. Other registries' tags get a method here when a module needs one. */
public interface TagContributor {

    default void blockTags(Tags<Block> tags) {
    }

    default void itemTags(Tags<Item> tags) {
    }

    /** Block tags whose contents the matching item tags take over; copied after every block tag is declared. */
    default void copies(TagCopier copier) {
    }
}
