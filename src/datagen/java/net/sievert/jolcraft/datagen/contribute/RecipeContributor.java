package net.sievert.jolcraft.datagen.contribute;

import net.sievert.jolcraft.datagen.provider.Recipes;

/** A module's recipes, written with vanilla's recipe builders and the helpers {@link Recipes.Provider} opens. */
public interface RecipeContributor {

    void recipes(Recipes.Provider recipes);
}
