package net.sievert.jolcraft.datagen.provider;

import java.util.List;
import java.util.stream.Stream;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.datagen.Handmade;
import net.sievert.jolcraft.datagen.contribute.ModelContributor;

/**
 * Every module's models through vanilla's provider, which itself fails the run for any block of the mod without a
 * block state and any item without a model; the hand-made ones are taken out of what it knows, per registry.
 */
public final class Models extends ModelProvider {

    private final List<ModelContributor> contributors;

    public Models(PackOutput output, List<ModelContributor> contributors) {
        super(output, JolCraft.MOD_ID);
        this.contributors = contributors;
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        contributors.forEach(contributor -> contributor.models(blockModels, itemModels));
    }

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return super.getKnownBlocks().filter(holder -> !Handmade.blockModel(holder.getKey().identifier()));
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return super.getKnownItems().filter(holder -> !Handmade.itemModel(holder.getKey().identifier()));
    }
}
