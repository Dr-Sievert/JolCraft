package net.sievert.jolcraft.datagen.contribute;

import net.minecraft.data.tags.TagAppender;
import net.minecraft.tags.TagKey;

/** Where a tag contributor appends: one tag builder per key. */
@FunctionalInterface
public interface Tags<T> {

    TagAppender<T, T> tag(TagKey<T> key);
}
