package net.sievert.jolcraft.datagen.module.system.flora;

import net.sievert.jolcraft.datagen.Contributes;
import net.sievert.jolcraft.datagen.contribute.LanguageContributor;
import net.sievert.jolcraft.datagen.provider.Translations;
import net.sievert.jolcraft.module.system.flora.FloraContent;
import net.sievert.jolcraft.module.system.flora.FloraHints;
import net.sievert.jolcraft.util.text.Dictionary;

/** The English names of the flora system's blocks and items, and its hints; a block item takes its block's name unless it is named as an item. */
@Contributes(Dictionary.FLORA)
public final class FloraText implements LanguageContributor {

    @Override
    public void english(Translations text) {
        text.add(FloraContent.VERDANT_SOIL, "Verdant Soil");
        text.add(FloraContent.VERDANT_FARMLAND, "Verdant Farmland");

        text.add(FloraContent.BARLEY_CROP, "Barley Crops");
        text.add(FloraContent.BARLEY_BLOCK, "Barley Hay Bale");
        text.add(FloraContent.ASGARNIAN_HOPS_CROP, "Asgarnian Hops");
        text.add(FloraContent.DUSKHOLD_HOPS_CROP, "Duskhold Hops");
        text.add(FloraContent.KRANDONIAN_HOPS_CROP, "Krandonian Hops");
        text.add(FloraContent.YANILLIAN_HOPS_CROP, "Yanillian Hops");
        text.add(FloraContent.DEEPSLATE_BULBS_CROP, "Deepslate Bulbs");

        text.add(FloraContent.BLOODROOT, "Bloodroot");
        text.add(FloraContent.SKYBELL, "Skybell");
        text.add(FloraContent.POTTED_SKYBELL, "Potted Skybell");
        text.add(FloraContent.CYANELLA, "Cyanella");
        text.add(FloraContent.POTTED_CYANELLA, "Potted Cyanella");
        text.add(FloraContent.DUSKCAP, "Duskcap");
        text.add(FloraContent.POTTED_DUSKCAP, "Potted Duskcap");
        text.add(FloraContent.DUSKCAP_BLOCK, "Duskcap Block");
        text.add(FloraContent.DUSKCAP_STEM, "Duskcap Stem");
        text.add(FloraContent.FESTERLING, "Festerling");
        text.add(FloraContent.POTTED_FESTERLING, "Potted Festerling");
        text.add(FloraContent.FESTERLING_BLOCK, "Festerling Block");
        text.add(FloraContent.FESTERLING_STEM, "Festerling Stem");
        text.add(FloraContent.FESTERLING_CROP, "Cultivated Festerling");

        text.add(FloraContent.BARLEY_SEEDS, "Barley Seeds");
        text.add(FloraContent.BARLEY, "Barley");
        text.add(FloraContent.ASGARNIAN_SEEDS, "Asgarnian Seeds");
        text.add(FloraContent.DUSKHOLD_SEEDS, "Duskhold Seeds");
        text.add(FloraContent.KRANDONIAN_SEEDS, "Krandonian Seeds");
        text.add(FloraContent.YANILLIAN_SEEDS, "Yanillian Seeds");
        text.add(FloraContent.ASGARNIAN_HOPS, "Asgarnian Hops");
        text.add(FloraContent.DUSKHOLD_HOPS, "Duskhold Hops");
        text.add(FloraContent.KRANDONIAN_HOPS, "Krandonian Hops");
        text.add(FloraContent.YANILLIAN_HOPS, "Yanillian Hops");
        text.add(FloraContent.DEEPSLATE_BULBS, "Deepslate Bulbs");

        text.add(FloraHints.HOPS_SEEDS, "Needs two blocks of vertical space and a light level of 8 or less to grow.");
        text.add(FloraHints.DEEPSLATE_BULBS, "Needs a light level of 8 or less and a Y-level of 0 or below to grow. Can only be planted on deepslate or verdant soil.");
    }
}
