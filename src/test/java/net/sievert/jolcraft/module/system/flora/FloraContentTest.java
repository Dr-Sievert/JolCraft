package net.sievert.jolcraft.module.system.flora;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.FarmlandBlock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** The flora system's blocks and items are registered under their ids, each crop is its seed, the farmland is moist from the start, and the tag keeps its id. */
class FloraContentTest {

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("jolcraft", path);
    }

    @Test
    void theBlocksAndTheirItemsAreRegisteredUnderTheirIds() {
        Assertions.assertSame(FloraContent.VERDANT_SOIL.get(), BuiltInRegistries.BLOCK.getValue(id("verdant_soil")));
        Assertions.assertSame(FloraContent.VERDANT_FARMLAND.get(), BuiltInRegistries.BLOCK.getValue(id("verdant_farmland")));
        Assertions.assertSame(FloraContent.VERDANT_SOIL_ITEM.get(), BuiltInRegistries.ITEM.getValue(id("verdant_soil")));
        Assertions.assertSame(FloraContent.VERDANT_FARMLAND_ITEM.get(), BuiltInRegistries.ITEM.getValue(id("verdant_farmland")));
    }

    @Test
    void theCropBlocksAndTheirItemsAreRegisteredUnderTheirIds() {
        Assertions.assertSame(FloraContent.BARLEY_CROP.get(), BuiltInRegistries.BLOCK.getValue(id("barley_crop")));
        Assertions.assertSame(FloraContent.BARLEY_BLOCK.get(), BuiltInRegistries.BLOCK.getValue(id("barley_block")));
        Assertions.assertSame(FloraContent.ASGARNIAN_HOPS_CROP.get(), BuiltInRegistries.BLOCK.getValue(id("asgarnian_hops_crop")));
        Assertions.assertSame(FloraContent.DUSKHOLD_HOPS_CROP.get(), BuiltInRegistries.BLOCK.getValue(id("duskhold_hops_crop")));
        Assertions.assertSame(FloraContent.KRANDONIAN_HOPS_CROP.get(), BuiltInRegistries.BLOCK.getValue(id("krandonian_hops_crop")));
        Assertions.assertSame(FloraContent.YANILLIAN_HOPS_CROP.get(), BuiltInRegistries.BLOCK.getValue(id("yanillian_hops_crop")));
        Assertions.assertSame(FloraContent.DEEPSLATE_BULBS_CROP.get(), BuiltInRegistries.BLOCK.getValue(id("deepslate_bulbs_crop")));

        Assertions.assertSame(FloraContent.BARLEY_SEEDS.get(), BuiltInRegistries.ITEM.getValue(id("barley_seeds")));
        Assertions.assertSame(FloraContent.BARLEY.get(), BuiltInRegistries.ITEM.getValue(id("barley")));
        Assertions.assertSame(FloraContent.BARLEY_BLOCK_ITEM.get(), BuiltInRegistries.ITEM.getValue(id("barley_block")));
        Assertions.assertSame(FloraContent.ASGARNIAN_SEEDS.get(), BuiltInRegistries.ITEM.getValue(id("asgarnian_seeds")));
        Assertions.assertSame(FloraContent.DUSKHOLD_SEEDS.get(), BuiltInRegistries.ITEM.getValue(id("duskhold_seeds")));
        Assertions.assertSame(FloraContent.KRANDONIAN_SEEDS.get(), BuiltInRegistries.ITEM.getValue(id("krandonian_seeds")));
        Assertions.assertSame(FloraContent.YANILLIAN_SEEDS.get(), BuiltInRegistries.ITEM.getValue(id("yanillian_seeds")));
        Assertions.assertSame(FloraContent.ASGARNIAN_HOPS.get(), BuiltInRegistries.ITEM.getValue(id("asgarnian_hops")));
        Assertions.assertSame(FloraContent.DUSKHOLD_HOPS.get(), BuiltInRegistries.ITEM.getValue(id("duskhold_hops")));
        Assertions.assertSame(FloraContent.KRANDONIAN_HOPS.get(), BuiltInRegistries.ITEM.getValue(id("krandonian_hops")));
        Assertions.assertSame(FloraContent.YANILLIAN_HOPS.get(), BuiltInRegistries.ITEM.getValue(id("yanillian_hops")));
        Assertions.assertSame(FloraContent.DEEPSLATE_BULBS.get(), BuiltInRegistries.ITEM.getValue(id("deepslate_bulbs")));
    }

    @Test
    void eachCropBlockIsItsSeedItem() {
        Assertions.assertSame(FloraContent.BARLEY_SEEDS.get(), FloraContent.BARLEY_CROP.get().asItem());
        Assertions.assertSame(FloraContent.DEEPSLATE_BULBS.get(), FloraContent.DEEPSLATE_BULBS_CROP.get().asItem());
        for (HopsVariety variety : HopsVariety.values()) {
            Assertions.assertSame(FloraContent.seeds(variety).get(), FloraContent.crop(variety).get().asItem(), variety.name());
        }
    }

    @Test
    void aSeedIsNamedAsAnItemAndTheBarleyBlockItemAsItsBlock() {
        Assertions.assertEquals("item.jolcraft.barley_seeds", FloraContent.BARLEY_SEEDS.get().getDescriptionId());
        Assertions.assertEquals("item.jolcraft.asgarnian_seeds", FloraContent.ASGARNIAN_SEEDS.get().getDescriptionId());
        Assertions.assertEquals("item.jolcraft.deepslate_bulbs", FloraContent.DEEPSLATE_BULBS.get().getDescriptionId());
        Assertions.assertEquals("block.jolcraft.barley_block", FloraContent.BARLEY_BLOCK_ITEM.get().getDescriptionId());
    }

    @Test
    void theFarmlandStartsAtFullMoisture() {
        Assertions.assertEquals(FarmlandBlock.MAX_MOISTURE, FloraContent.VERDANT_FARMLAND.get().defaultBlockState().getValue(FarmlandBlock.MOISTURE));
    }

    @Test
    void theVerdantTagIsTheModsOwn() {
        Assertions.assertEquals(id("verdant"), FloraTags.VERDANT.location());
    }

    @Test
    void theWildPlantsAndTheirItemsAreRegisteredUnderTheirIds() {
        Assertions.assertSame(FloraContent.BLOODROOT.get(), BuiltInRegistries.BLOCK.getValue(id("bloodroot")));
        Assertions.assertSame(FloraContent.SKYBELL.get(), BuiltInRegistries.BLOCK.getValue(id("skybell")));
        Assertions.assertSame(FloraContent.CYANELLA.get(), BuiltInRegistries.BLOCK.getValue(id("cyanella")));
        Assertions.assertSame(FloraContent.DUSKCAP.get(), BuiltInRegistries.BLOCK.getValue(id("duskcap")));
        Assertions.assertSame(FloraContent.DUSKCAP_BLOCK.get(), BuiltInRegistries.BLOCK.getValue(id("duskcap_block")));
        Assertions.assertSame(FloraContent.DUSKCAP_STEM.get(), BuiltInRegistries.BLOCK.getValue(id("duskcap_stem")));
        Assertions.assertSame(FloraContent.FESTERLING.get(), BuiltInRegistries.BLOCK.getValue(id("festerling")));
        Assertions.assertSame(FloraContent.FESTERLING_BLOCK.get(), BuiltInRegistries.BLOCK.getValue(id("festerling_block")));
        Assertions.assertSame(FloraContent.FESTERLING_STEM.get(), BuiltInRegistries.BLOCK.getValue(id("festerling_stem")));
        Assertions.assertSame(FloraContent.FESTERLING_CROP.get(), BuiltInRegistries.BLOCK.getValue(id("festerling_crop")));
        Assertions.assertSame(FloraContent.POTTED_SKYBELL.get(), BuiltInRegistries.BLOCK.getValue(id("potted_skybell")));
        Assertions.assertSame(FloraContent.POTTED_CYANELLA.get(), BuiltInRegistries.BLOCK.getValue(id("potted_cyanella")));
        Assertions.assertSame(FloraContent.POTTED_DUSKCAP.get(), BuiltInRegistries.BLOCK.getValue(id("potted_duskcap")));
        Assertions.assertSame(FloraContent.POTTED_FESTERLING.get(), BuiltInRegistries.BLOCK.getValue(id("potted_festerling")));

        Assertions.assertSame(FloraContent.BLOODROOT_ITEM.get(), BuiltInRegistries.ITEM.getValue(id("bloodroot")));
        Assertions.assertSame(FloraContent.SKYBELL_ITEM.get(), BuiltInRegistries.ITEM.getValue(id("skybell")));
        Assertions.assertSame(FloraContent.CYANELLA_ITEM.get(), BuiltInRegistries.ITEM.getValue(id("cyanella")));
        Assertions.assertSame(FloraContent.DUSKCAP_ITEM.get(), BuiltInRegistries.ITEM.getValue(id("duskcap")));
        Assertions.assertSame(FloraContent.DUSKCAP_BLOCK_ITEM.get(), BuiltInRegistries.ITEM.getValue(id("duskcap_block")));
        Assertions.assertSame(FloraContent.DUSKCAP_STEM_ITEM.get(), BuiltInRegistries.ITEM.getValue(id("duskcap_stem")));
        Assertions.assertSame(FloraContent.FESTERLING_ITEM.get(), BuiltInRegistries.ITEM.getValue(id("festerling")));
        Assertions.assertSame(FloraContent.FESTERLING_BLOCK_ITEM.get(), BuiltInRegistries.ITEM.getValue(id("festerling_block")));
        Assertions.assertSame(FloraContent.FESTERLING_STEM_ITEM.get(), BuiltInRegistries.ITEM.getValue(id("festerling_stem")));
    }

    @Test
    void theFesterlingCropAndThePotsHaveNoItemAndTheOthersHaveTheirOwn() {
        Assertions.assertSame(Items.AIR, FloraContent.FESTERLING_CROP.get().asItem());
        Assertions.assertSame(Items.AIR, FloraContent.POTTED_SKYBELL.get().asItem());
        Assertions.assertSame(Items.AIR, FloraContent.POTTED_CYANELLA.get().asItem());
        Assertions.assertSame(Items.AIR, FloraContent.POTTED_DUSKCAP.get().asItem());
        Assertions.assertSame(Items.AIR, FloraContent.POTTED_FESTERLING.get().asItem());
        Assertions.assertSame(FloraContent.BLOODROOT_ITEM.get(), FloraContent.BLOODROOT.get().asItem());
        Assertions.assertSame(FloraContent.SKYBELL_ITEM.get(), FloraContent.SKYBELL.get().asItem());
        Assertions.assertSame(FloraContent.CYANELLA_ITEM.get(), FloraContent.CYANELLA.get().asItem());
        Assertions.assertSame(FloraContent.DUSKCAP_ITEM.get(), FloraContent.DUSKCAP.get().asItem());
        Assertions.assertSame(FloraContent.FESTERLING_ITEM.get(), FloraContent.FESTERLING.get().asItem());
        Assertions.assertSame(FloraContent.DUSKCAP_BLOCK_ITEM.get(), FloraContent.DUSKCAP_BLOCK.get().asItem());
        Assertions.assertSame(FloraContent.FESTERLING_STEM_ITEM.get(), FloraContent.FESTERLING_STEM.get().asItem());
    }

    @Test
    void theHugeMushroomFeaturesAreRegisteredUnderTheirIds() {
        Assertions.assertSame(FloraContent.HUGE_DUSKCAP.get(), BuiltInRegistries.FEATURE.getValue(id("huge_duskcap")));
        Assertions.assertSame(FloraContent.HUGE_FESTERLING.get(), BuiltInRegistries.FEATURE.getValue(id("huge_festerling")));
        Assertions.assertEquals(id("huge_duskcap"), FloraFeatures.HUGE_DUSKCAP.identifier());
        Assertions.assertEquals(id("huge_festerling"), FloraFeatures.HUGE_FESTERLING.identifier());
    }

    @Test
    void theNewTagsAreTheModsOwn() {
        Assertions.assertEquals(id("supports_cyanella"), FloraTags.SUPPORTS_CYANELLA.location());
        Assertions.assertEquals(id("supports_festerling"), FloraTags.SUPPORTS_FESTERLING.location());
        Assertions.assertEquals(id("bloodroot_spreads_under"), FloraTags.BLOODROOT_SPREADS_UNDER.location());
    }
}
