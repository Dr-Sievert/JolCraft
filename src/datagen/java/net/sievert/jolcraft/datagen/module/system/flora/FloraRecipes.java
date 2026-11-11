package net.sievert.jolcraft.datagen.module.system.flora;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Items;

import net.sievert.jolcraft.datagen.Contributes;
import net.sievert.jolcraft.datagen.contribute.RecipeContributor;
import net.sievert.jolcraft.datagen.provider.Recipes;
import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.module.system.flora.FloraContent;
import net.sievert.jolcraft.util.text.Dictionary;
import net.sievert.jolcraft.util.text.ModString;

/** Bread from three barley in a row; nine barley to a barley block and back, as wheat and the hay bale; a cyan dye from a cyanella and a light blue dye from a skybell, in the groups of the game's flower dyes. Each is unlocked by holding its first ingredient. */
@Contributes(Dictionary.FLORA)
public final class FloraRecipes implements RecipeContributor {

    private static final int BLOCK_SIZE = 9;

    @Override
    public void recipes(Recipes.Provider recipes) {
        String barley = Dictionary.BARLEY;
        String block = ModString.underscored(Dictionary.BARLEY, Dictionary.BLOCK);
        recipes.shaped(RecipeCategory.FOOD, Items.BREAD)
                .define('#', FloraContent.BARLEY.get())
                .pattern("###")
                .unlockedBy(barley, recipes.has(FloraContent.BARLEY.get()))
                .save(recipes.output(), Ids.key(Registries.RECIPE, Dictionary.JOLCRAFT, Dictionary.BREAD, Dictionary.FROM, Dictionary.BARLEY));
        recipes.shaped(RecipeCategory.BUILDING_BLOCKS, FloraContent.BARLEY_BLOCK_ITEM.get())
                .define('#', FloraContent.BARLEY.get())
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .unlockedBy(barley, recipes.has(FloraContent.BARLEY.get()))
                .save(recipes.output(), Ids.key(Registries.RECIPE, Dictionary.JOLCRAFT, Dictionary.BARLEY, Dictionary.BLOCK, Dictionary.FROM, Dictionary.BARLEY));
        recipes.shapeless(RecipeCategory.MISC, FloraContent.BARLEY.get(), BLOCK_SIZE)
                .requires(FloraContent.BARLEY_BLOCK_ITEM.get())
                .unlockedBy(block, recipes.has(FloraContent.BARLEY_BLOCK_ITEM.get()))
                .save(recipes.output(), Ids.key(Registries.RECIPE, Dictionary.JOLCRAFT, Dictionary.BARLEY, Dictionary.FROM, Dictionary.BARLEY, Dictionary.BLOCK));
        recipes.shapeless(RecipeCategory.MISC, Items.CYAN_DYE)
                .requires(FloraContent.CYANELLA_ITEM.get())
                .group(ModString.underscored(Dictionary.CYAN, Dictionary.DYE))
                .unlockedBy(Dictionary.CYANELLA, recipes.has(FloraContent.CYANELLA_ITEM.get()))
                .save(recipes.output(), Ids.key(Registries.RECIPE, Dictionary.JOLCRAFT, Dictionary.CYAN, Dictionary.DYE, Dictionary.FROM, Dictionary.CYANELLA));
        recipes.shapeless(RecipeCategory.MISC, Items.LIGHT_BLUE_DYE)
                .requires(FloraContent.SKYBELL_ITEM.get())
                .group(ModString.underscored(Dictionary.LIGHT, Dictionary.BLUE, Dictionary.DYE))
                .unlockedBy(Dictionary.SKYBELL, recipes.has(FloraContent.SKYBELL_ITEM.get()))
                .save(recipes.output(), Ids.key(Registries.RECIPE, Dictionary.JOLCRAFT, Dictionary.LIGHT, Dictionary.BLUE, Dictionary.DYE, Dictionary.FROM, Dictionary.SKYBELL));
    }
}
