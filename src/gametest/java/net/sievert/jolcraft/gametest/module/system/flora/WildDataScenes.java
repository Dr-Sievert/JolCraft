package net.sievert.jolcraft.gametest.module.system.flora;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.system.flora.FloraContent;
import net.sievert.jolcraft.module.system.flora.FloraData;
import net.sievert.jolcraft.module.system.flora.FloraTags;

/** The data of the wild plants as the level holds it: tags, compost chances, the two dye recipes and what rotten flesh plants. Each scene builds a little and runs alone in its batch, as the other flora scenes that build do. */
public final class WildDataScenes {

    private static ResourceKey<Recipe<?>> recipe(String path) {
        return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("jolcraft", path));
    }

    private static float compost(Item item) {
        Compostable chance = BuiltInRegistries.ITEM.getData(NeoForgeDataMaps.COMPOSTABLES, BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow());
        return chance == null ? -1.0F : chance.chance();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theFlowersAreSmallFlowersInTheItemTagToo(TestScene scene) {
        scene.check("the item tag small_flowers holds the skybell and the cyanella", () -> scene.assertTrue(new ItemStack(FloraContent.SKYBELL_ITEM.get()).is(ItemTags.SMALL_FLOWERS)
                && new ItemStack(FloraContent.CYANELLA_ITEM.get()).is(ItemTags.SMALL_FLOWERS), "tags"));
        scene.check("the block tag does", () -> scene.assertTrue(FloraContent.SKYBELL.get().defaultBlockState().is(BlockTags.SMALL_FLOWERS)
                && FloraContent.CYANELLA.get().defaultBlockState().is(BlockTags.SMALL_FLOWERS), "tags"));
        scene.check("the duskcap and the festerling are common mushrooms", () -> scene.assertTrue(new ItemStack(FloraContent.DUSKCAP_ITEM.get()).is(Tags.Items.MUSHROOMS)
                && new ItemStack(FloraContent.FESTERLING_ITEM.get()).is(Tags.Items.MUSHROOMS), "tags"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theWildBlocksAreInTheTagsTheirBehaviourReads(TestScene scene) {
        BlockState bloodroot = FloraContent.BLOODROOT.get().defaultBlockState();
        scene.check("bloodroot is cut with an axe and is efficient to a sword", () -> scene.assertTrue(bloodroot.is(BlockTags.MINEABLE_WITH_AXE) && bloodroot.is(BlockTags.SWORD_EFFICIENT), "tags"));
        scene.check("the four huge blocks are mined with an axe", () -> scene.assertTrue(FloraContent.DUSKCAP_BLOCK.get().defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE)
                && FloraContent.DUSKCAP_STEM.get().defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE) && FloraContent.FESTERLING_BLOCK.get().defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE)
                && FloraContent.FESTERLING_STEM.get().defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE), "tags"));
        scene.check("the festerling crop is a crop", () -> scene.assertTrue(FloraContent.FESTERLING_CROP.get().defaultBlockState().is(BlockTags.CROPS), "tags"));
        scene.check("the four pots are flower pots", () -> scene.assertTrue(FloraContent.POTTED_SKYBELL.get().defaultBlockState().is(BlockTags.FLOWER_POTS)
                && FloraContent.POTTED_CYANELLA.get().defaultBlockState().is(BlockTags.FLOWER_POTS) && FloraContent.POTTED_DUSKCAP.get().defaultBlockState().is(BlockTags.FLOWER_POTS)
                && FloraContent.POTTED_FESTERLING.get().defaultBlockState().is(BlockTags.FLOWER_POTS), "tags"));
        scene.check("the mushrooms and their blocks are replaced by a huge mushroom, as the game's are", () -> scene.assertTrue(FloraContent.DUSKCAP.get().defaultBlockState().is(BlockTags.REPLACEABLE_BY_MUSHROOMS)
                && FloraContent.FESTERLING.get().defaultBlockState().is(BlockTags.REPLACEABLE_BY_MUSHROOMS) && FloraContent.DUSKCAP_BLOCK.get().defaultBlockState().is(BlockTags.REPLACEABLE_BY_MUSHROOMS)
                && FloraContent.FESTERLING_BLOCK.get().defaultBlockState().is(BlockTags.REPLACEABLE_BY_MUSHROOMS), "tags"));
        scene.check("cyanella stands on warped nylium and verdant soil, and not on a grass block", () -> scene.assertTrue(Blocks.WARPED_NYLIUM.defaultBlockState().is(FloraTags.SUPPORTS_CYANELLA)
                && FloraContent.VERDANT_SOIL.get().defaultBlockState().is(FloraTags.SUPPORTS_CYANELLA) && !Blocks.GRASS_BLOCK.defaultBlockState().is(FloraTags.SUPPORTS_CYANELLA), "tags"));
        scene.check("the festerling stands on verdant soil and what a mushroom ignores light on", () -> scene.assertTrue(FloraContent.VERDANT_SOIL.get().defaultBlockState().is(FloraTags.SUPPORTS_FESTERLING)
                && Blocks.MYCELIUM.defaultBlockState().is(FloraTags.SUPPORTS_FESTERLING) && !Blocks.STONE.defaultBlockState().is(FloraTags.SUPPORTS_FESTERLING), "tags"));
        scene.check("bloodroot spreads under what vegetation stands on", () -> scene.assertTrue(Blocks.DIRT.defaultBlockState().is(FloraTags.BLOODROOT_SPREADS_UNDER)
                && !Blocks.STONE.defaultBlockState().is(FloraTags.BLOODROOT_SPREADS_UNDER), "tags"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theCompostChancesAreTheGamesForFlowersMushroomBlocksStemsAndRoots(TestScene scene) {
        scene.check("a flower and a mushroom compost at 0.65", () -> scene.assertTrue(compost(FloraContent.SKYBELL_ITEM.get()) == 0.65F && compost(FloraContent.CYANELLA_ITEM.get()) == 0.65F
                && compost(FloraContent.DUSKCAP_ITEM.get()) == 0.65F && compost(FloraContent.FESTERLING_ITEM.get()) == 0.65F, "chances"));
        scene.check("a huge block composts as the game's mushroom block", () -> scene.assertTrue(compost(Items.BROWN_MUSHROOM_BLOCK) == 0.85F
                && compost(FloraContent.DUSKCAP_BLOCK_ITEM.get()) == compost(Items.BROWN_MUSHROOM_BLOCK) && compost(FloraContent.FESTERLING_BLOCK_ITEM.get()) == compost(Items.BROWN_MUSHROOM_BLOCK), "chances"));
        scene.check("a stem composts as the game's mushroom stem", () -> scene.assertTrue(compost(Items.MUSHROOM_STEM) == 0.65F
                && compost(FloraContent.DUSKCAP_STEM_ITEM.get()) == compost(Items.MUSHROOM_STEM) && compost(FloraContent.FESTERLING_STEM_ITEM.get()) == compost(Items.MUSHROOM_STEM), "chances"));
        scene.check("bloodroot composts as hanging roots", () -> scene.assertTrue(compost(Items.HANGING_ROOTS) == 0.3F && compost(FloraContent.BLOODROOT_ITEM.get()) == compost(Items.HANGING_ROOTS), "chances"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theTwoDyeRecipesAreInTheLevelWithTheirResults(TestScene scene) {
        ServerLevel level = scene.level();
        CraftingInput cyanella = CraftingInput.of(1, 1, List.of(new ItemStack(FloraContent.CYANELLA_ITEM.get())));
        RecipeHolder<?> cyan = level.recipeAccess().byKey(recipe("cyan_dye_from_cyanella")).orElseThrow();
        ItemStack cyanMade = ((CraftingRecipe) cyan.value()).assemble(cyanella);
        scene.check("a cyanella makes one cyan dye", () -> scene.assertTrue(((CraftingRecipe) cyan.value()).matches(cyanella, level) && cyanMade.is(Items.CYAN_DYE) && cyanMade.getCount() == 1, "made " + cyanMade));
        CraftingInput skybell = CraftingInput.of(1, 1, List.of(new ItemStack(FloraContent.SKYBELL_ITEM.get())));
        RecipeHolder<?> blue = level.recipeAccess().byKey(recipe("light_blue_dye_from_skybell")).orElseThrow();
        ItemStack blueMade = ((CraftingRecipe) blue.value()).assemble(skybell);
        scene.check("a skybell makes one light blue dye", () -> scene.assertTrue(((CraftingRecipe) blue.value()).matches(skybell, level) && blueMade.is(Items.LIGHT_BLUE_DYE) && blueMade.getCount() == 1, "made " + blueMade));
        scene.check("and neither flower makes the other's dye", () -> scene.assertFalse(((CraftingRecipe) cyan.value()).matches(skybell, level) || ((CraftingRecipe) blue.value()).matches(cyanella, level), "matched"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void rottenFleshPlantsTheFesterlingCropAccordingToTheDataMap(TestScene scene) {
        FloraData.Plants plants = BuiltInRegistries.ITEM.getData(FloraData.PLANTS, BuiltInRegistries.ITEM.getResourceKey(Items.ROTTEN_FLESH).orElseThrow());
        scene.check("rotten flesh has a plants entry, the festerling crop", () -> scene.assertTrue(plants != null && plants.block() == FloraContent.FESTERLING_CROP.get(), "entry " + plants));
        scene.check("a stick has none", () -> scene.assertTrue(BuiltInRegistries.ITEM.getData(FloraData.PLANTS, BuiltInRegistries.ITEM.getResourceKey(Items.STICK).orElseThrow()) == null, "an entry"));
        scene.succeed();
    }
}
