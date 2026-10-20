package net.sievert.jolcraft.world.recipe.base.output.hook;

import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.storage.loot.LootContext;

public interface RecipeHook {

    /**
     * Precondition, checked by the recipe executor before anything is consumed. A hook that
     * cannot run for this input (for example no undiscovered structure for the compass) refuses
     * here, so the player is not charged for a recipe that never ran. This is distinct from
     * {@link #apply} returning false during generation, which only drops that one output.
     * A refusal blocks the whole recipe, whichever output the hook is attached to, and so does an
     * unregistered hook id.
     */
    default boolean canApply(
            LootContext context,
            RecipeInput input
    ) {
        return true;
    }

    boolean apply(
            LootContext context,
            Object generatedOutput,
            RecipeInput input
    );
}