package net.sievert.jolcraft.datagen.module.system.flora;

import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import net.sievert.jolcraft.datagen.Contributes;
import net.sievert.jolcraft.datagen.contribute.KeyTags;
import net.sievert.jolcraft.datagen.contribute.TagContributor;
import net.sievert.jolcraft.datagen.contribute.TagCopier;
import net.sievert.jolcraft.datagen.contribute.Tags;
import net.sievert.jolcraft.module.system.flora.FloraContent;
import net.sievert.jolcraft.module.system.flora.FloraTags;
import net.sievert.jolcraft.module.system.flora.HopsVariety;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * What stands on verdant ground, and what the crops are. The soil joins the tags of flowers, bamboo, azaleas and
 * mushrooms, and not {@code substrate_overworld} or {@code dirt}, which would have a tree or a feature turn it into
 * something else. The farmland joins every block tag vanilla's own farmland is named in. The bulbs stand on deepslate
 * and verdant soil, are mined with an iron pickaxe and are raw materials; the crops are crops, and barley and the hops
 * keep the farmland under them. The seeds and crops of the mod join the tags that mods share, and so do the two mushrooms.
 */
@Contributes(Dictionary.FLORA)
public final class FloraTagging implements TagContributor {

    @Override
    public void blockTags(Tags<Block> tags) {
        Block soil = FloraContent.VERDANT_SOIL.get();
        Block farmland = FloraContent.VERDANT_FARMLAND.get();
        tags.tag(FloraTags.VERDANT).add(soil).add(farmland);
        tags.tag(BlockTags.MINEABLE_WITH_SHOVEL).add(soil).add(farmland);

        tags.tag(BlockTags.SUPPORTS_VEGETATION).add(soil).add(farmland);
        tags.tag(BlockTags.SUPPORTS_BAMBOO).add(soil);
        tags.tag(BlockTags.AZALEA_GROWS_ON).add(soil);
        tags.tag(BlockTags.OVERRIDES_MUSHROOM_LIGHT_REQUIREMENT).add(soil);
        tags.tag(BlockTags.HUGE_BROWN_MUSHROOM_CAN_PLACE_ON).add(soil);
        tags.tag(BlockTags.HUGE_RED_MUSHROOM_CAN_PLACE_ON).add(soil);

        tags.tag(BlockTags.SUPPORTS_CROPS).add(farmland);
        tags.tag(BlockTags.GROWS_CROPS).add(farmland);
        tags.tag(BlockTags.SUPPORTS_BIG_DRIPLEAF).add(farmland);
        tags.tag(BlockTags.SUPPORT_OVERRIDE_CACTUS_FLOWER).add(farmland);

        Block barley = FloraContent.BARLEY_CROP.get();
        Block bulbs = FloraContent.DEEPSLATE_BULBS_CROP.get();
        tags.tag(FloraTags.SUPPORTS_DEEPSLATE_BULBS).add(Blocks.DEEPSLATE).add(soil);
        tags.tag(BlockTags.CROPS).add(barley).add(bulbs);
        tags.tag(BlockTags.MAINTAINS_FARMLAND).add(barley);
        for (HopsVariety variety : HopsVariety.values()) {
            Block hops = FloraContent.crop(variety).get();
            tags.tag(BlockTags.CROPS).add(hops);
            tags.tag(BlockTags.MAINTAINS_FARMLAND).add(hops);
        }
        tags.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(bulbs);
        tags.tag(BlockTags.NEEDS_IRON_TOOL).add(bulbs);
        tags.tag(BlockTags.MINEABLE_WITH_HOE).add(FloraContent.BARLEY_BLOCK.get());
        wild(tags, soil);
    }

    @Override
    public void itemTags(Tags<Item> tags) {
        for (HopsVariety variety : HopsVariety.values()) {
            tags.tag(FloraTags.HOPS).add(FloraContent.hops(variety).get());
            tags.tag(FloraTags.HOPS_SEEDS).add(FloraContent.seeds(variety).get());
        }
        tags.tag(net.neoforged.neoforge.common.Tags.Items.SEEDS).add(FloraContent.BARLEY_SEEDS.get()).addTag(FloraTags.HOPS_SEEDS);
        tags.tag(net.neoforged.neoforge.common.Tags.Items.CROPS).add(FloraContent.BARLEY.get()).add(FloraContent.DEEPSLATE_BULBS.get()).addTag(FloraTags.HOPS);
        tags.tag(net.neoforged.neoforge.common.Tags.Items.RAW_MATERIALS).add(FloraContent.DEEPSLATE_BULBS.get());
        tags.tag(net.neoforged.neoforge.common.Tags.Items.MUSHROOMS).add(FloraContent.DUSKCAP_ITEM.get()).add(FloraContent.FESTERLING_ITEM.get());
    }

    /**
     * The wild plants: cyanella stands on warped nylium and verdant soil, the festerling on verdant soil and what lets a
     * mushroom ignore light, bloodroot spreads under what vegetation stands on. The flowers are small flowers, the pots
     * pots, bloodroot is cut with an axe and a sword, the huge mushroom blocks with an axe, the festerling crop is a crop,
     * and the mushrooms and their blocks are what a huge mushroom replaces, as the game's own are.
     */
    private static void wild(Tags<Block> tags, Block soil) {
        Block bloodroot = FloraContent.BLOODROOT.get();
        Block skybell = FloraContent.SKYBELL.get();
        Block cyanella = FloraContent.CYANELLA.get();
        Block duskcap = FloraContent.DUSKCAP.get();
        Block festerling = FloraContent.FESTERLING.get();
        tags.tag(FloraTags.SUPPORTS_CYANELLA).add(Blocks.WARPED_NYLIUM).add(soil);
        tags.tag(FloraTags.SUPPORTS_FESTERLING).add(soil).addTag(BlockTags.OVERRIDES_MUSHROOM_LIGHT_REQUIREMENT);
        tags.tag(FloraTags.BLOODROOT_SPREADS_UNDER).addTag(BlockTags.SUPPORTS_VEGETATION);

        tags.tag(BlockTags.SMALL_FLOWERS).add(skybell).add(cyanella);
        tags.tag(BlockTags.FLOWER_POTS).add(FloraContent.POTTED_SKYBELL.get()).add(FloraContent.POTTED_CYANELLA.get())
                .add(FloraContent.POTTED_DUSKCAP.get()).add(FloraContent.POTTED_FESTERLING.get());
        tags.tag(BlockTags.MINEABLE_WITH_AXE).add(bloodroot).add(FloraContent.DUSKCAP_BLOCK.get()).add(FloraContent.DUSKCAP_STEM.get())
                .add(FloraContent.FESTERLING_BLOCK.get()).add(FloraContent.FESTERLING_STEM.get());
        tags.tag(BlockTags.SWORD_EFFICIENT).add(bloodroot);
        tags.tag(BlockTags.CROPS).add(FloraContent.FESTERLING_CROP.get());
        tags.tag(BlockTags.REPLACEABLE_BY_MUSHROOMS).add(duskcap).add(festerling).add(FloraContent.DUSKCAP_BLOCK.get()).add(FloraContent.FESTERLING_BLOCK.get());
    }

    /** The skybell patch generates in mountains and hills; bonemeal on grass sprouts a skybell there and in forests and taigas. */
    @Override
    public void biomeTags(KeyTags<Biome> tags) {
        tags.tag(FloraTags.GROWS_SKYBELL).addTag(BiomeTags.IS_MOUNTAIN).addTag(BiomeTags.IS_HILL);
        tags.tag(FloraTags.SPROUTS_SKYBELL).addTag(BiomeTags.IS_MOUNTAIN).addTag(BiomeTags.IS_HILL).addTag(BiomeTags.IS_FOREST).addTag(BiomeTags.IS_TAIGA);
    }

    @Override
    public void copies(TagCopier copier) {
        copier.copy(BlockTags.SMALL_FLOWERS, ItemTags.SMALL_FLOWERS);
    }
}
