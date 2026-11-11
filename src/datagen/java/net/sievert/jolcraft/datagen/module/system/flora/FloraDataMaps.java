package net.sievert.jolcraft.datagen.module.system.flora;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import net.sievert.jolcraft.datagen.Contributes;
import net.sievert.jolcraft.datagen.contribute.DataMapContributor;
import net.sievert.jolcraft.datagen.provider.DataMaps;
import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.module.system.flora.FloraContent;
import net.sievert.jolcraft.module.system.flora.FloraData;
import net.sievert.jolcraft.module.system.flora.FloraTags;
import net.sievert.jolcraft.module.system.flora.HopsVariety;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * What the composter makes of the flora system's items: seeds a quarter, a harvest less than half, the hay bale as vanilla's
 * own, the flowers and mushrooms as vanilla's, the huge blocks and stems as the game's mushroom block and stem, and
 * bloodroot as hanging roots. And what plants a block: rotten flesh plants the festerling crop. And what bonemeal grows: on
 * grass a skybell in the biomes of the sprouting tag, on warped nylium a cyanella.
 */
@Contributes(Dictionary.FLORA)
public final class FloraDataMaps implements DataMapContributor {

    private static final float SEEDS = 0.25F;
    private static final float HARVEST = 0.45F;
    /** The chance the game's hay bale has in NeoForge's {@code compostables}. */
    private static final float HAY_BALE = 0.85F;
    /** The chance of the game's flowers and mushrooms. */
    private static final float PLANT = 0.65F;
    /** The chance of the game's mushroom block, and so of the huge blocks. */
    private static final float MUSHROOM_BLOCK = 0.85F;
    /** The chance of the game's mushroom stem, and so of the huge stems. */
    private static final float MUSHROOM_STEM = 0.65F;
    /** The chance of the game's hanging roots. */
    private static final float ROOTS = 0.3F;

    @Override
    public void dataMaps(DataMaps maps) {
        DataMapProvider.Builder<Compostable, Item> compost = maps.builder(NeoForgeDataMaps.COMPOSTABLES);
        compost.add(FloraContent.BARLEY_SEEDS, new Compostable(SEEDS), false);
        compost.add(FloraContent.BARLEY, new Compostable(HARVEST), false);
        compost.add(FloraContent.BARLEY_BLOCK_ITEM, new Compostable(HAY_BALE), false);
        for (HopsVariety variety : HopsVariety.values()) {
            compost.add(FloraContent.seeds(variety), new Compostable(SEEDS), false);
            compost.add(FloraContent.hops(variety), new Compostable(HARVEST), false);
        }
        compost.add(FloraContent.SKYBELL_ITEM, new Compostable(PLANT), false);
        compost.add(FloraContent.CYANELLA_ITEM, new Compostable(PLANT), false);
        compost.add(FloraContent.DUSKCAP_ITEM, new Compostable(PLANT), false);
        compost.add(FloraContent.FESTERLING_ITEM, new Compostable(PLANT), false);
        compost.add(FloraContent.DUSKCAP_BLOCK_ITEM, new Compostable(MUSHROOM_BLOCK), false);
        compost.add(FloraContent.FESTERLING_BLOCK_ITEM, new Compostable(MUSHROOM_BLOCK), false);
        compost.add(FloraContent.DUSKCAP_STEM_ITEM, new Compostable(MUSHROOM_STEM), false);
        compost.add(FloraContent.FESTERLING_STEM_ITEM, new Compostable(MUSHROOM_STEM), false);
        compost.add(FloraContent.BLOODROOT_ITEM, new Compostable(ROOTS), false);

        maps.builder(FloraData.PLANTS).add(BuiltInRegistries.ITEM.wrapAsHolder(Items.ROTTEN_FLESH), new FloraData.Plants(FloraContent.FESTERLING_CROP.get()), false);

        DataMapProvider.Builder<List<FloraData.Sprout>, Block> bonemeal = maps.builder(FloraData.BONEMEAL);
        bonemeal.add(BuiltInRegistries.BLOCK.wrapAsHolder(Blocks.GRASS_BLOCK), List.of(new FloraData.Sprout(placed(Dictionary.SKYBELL, Dictionary.BONEMEAL), Optional.of(FloraTags.SPROUTS_SKYBELL))), false);
        bonemeal.add(BuiltInRegistries.BLOCK.wrapAsHolder(Blocks.WARPED_NYLIUM), List.of(new FloraData.Sprout(placed(Dictionary.CYANELLA, Dictionary.BONEMEAL), Optional.empty())), false);
    }

    private static ResourceKey<PlacedFeature> placed(String... words) {
        return Ids.key(Registries.PLACED_FEATURE, Dictionary.JOLCRAFT, words);
    }
}
