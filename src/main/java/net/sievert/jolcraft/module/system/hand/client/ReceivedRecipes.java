package net.sievert.jolcraft.module.system.hand.client;

import java.util.Collection;
import java.util.List;

import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import net.sievert.jolcraft.module.system.hand.HandInput;
import net.sievert.jolcraft.module.system.hand.HandRecipe;
import net.sievert.jolcraft.stereotype.Internal;

/** The hand recipes the server last sent this client, held by {@link HandClient}'s listeners: none before the first sync and after leaving a world. */
@Internal
final class ReceivedRecipes {

    private List<HandRecipe> recipes = List.of();

    void keep(Collection<RecipeHolder<HandRecipe>> sent) {
        recipes = sent.stream().map(RecipeHolder::value).toList();
    }

    void drop() {
        recipes = List.of();
    }

    /** Whether any kept recipe matches the hands, by the recipe's own {@code matches}, the server's. */
    boolean anyMatches(HandInput input, Level level) {
        return recipes.stream().anyMatch(recipe -> recipe.matches(input, level));
    }
}
