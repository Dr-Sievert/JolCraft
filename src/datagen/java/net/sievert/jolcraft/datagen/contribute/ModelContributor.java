package net.sievert.jolcraft.datagen.contribute;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;

/** A module's block states, block models and item models, with vanilla's generators. */
public interface ModelContributor {

    void models(BlockModelGenerators blocks, ItemModelGenerators items);
}
