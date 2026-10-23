package net.sievert.jolcraft.world.recipe.base.output;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextUser;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.sievert.jolcraft.world.recipe.base.output.hook.JolCraftRecipeHooks;

import java.util.List;

public interface RecipeOutput extends LootContextUser {

    RecipeOutputType getType();

    List<ResourceLocation> hooks();

    /**
     * Reports every hook id that no registered hook answers to. The recipe codecs run this
     * through RecipeValidation.validateOutput, so a mistyped hook fails the recipe at load
     * instead of logging an error and refusing the output on first use.
     */
    @Override
    default void validate(
            ValidationContext context
    ) {
        LootContextUser.super.validate(context);

        for (ResourceLocation hookId : hooks()) {
            if (!JolCraftRecipeHooks.contains(hookId)) {
                context.reportProblem(
                        "Unknown recipe hook " + hookId
                );
            }
        }
    }

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