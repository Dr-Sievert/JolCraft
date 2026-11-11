package net.sievert.jolcraft.datagen.contribute;

import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

/** Where a tag contributor appends to the tags of a datapack registry, whose entries are named by key: one tag builder per tag key. */
@FunctionalInterface
public interface KeyTags<T> {

    TagAppender<ResourceKey<T>, T> tag(TagKey<T> key);
}
