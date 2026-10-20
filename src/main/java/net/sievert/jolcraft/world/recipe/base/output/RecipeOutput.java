package net.sievert.jolcraft.world.recipe.base.output;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextUser;
import net.sievert.jolcraft.world.recipe.base.output.hook.JolCraftRecipeHooks;

import java.util.List;

public interface RecipeOutput extends LootContextUser {

    RecipeOutputType getType();

    List<ResourceLocation> hooks();

    /**
     * Precondition pass over every hook of this output. Recipe executors call it before they
     * consume inputs, so a refusal costs the player nothing.
     */
    default boolean hooksCanApply(
            LootContext context,
            RecipeInput recipeInput
    ) {
        for (ResourceLocation hookId : hooks()) {
            if (!JolCraftRecipeHooks.canApply(
                    hookId,
                    context,
                    recipeInput
            )) {
                return false;
            }
        }

        return true;
    }

    default boolean applyHooks(
            LootContext context,
            Object generatedOutput,
            RecipeInput recipeInput
    ) {
        for (ResourceLocation hookId : hooks()) {
            if (!JolCraftRecipeHooks.apply(
                    hookId,
                    context,
                    generatedOutput,
                    recipeInput
            )) {
                return false;
            }
        }

        return true;
    }
}