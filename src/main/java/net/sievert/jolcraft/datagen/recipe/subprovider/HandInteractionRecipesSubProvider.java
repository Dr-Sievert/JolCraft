package net.sievert.jolcraft.datagen.recipe.subprovider;

import net.sievert.jolcraft.datagen.recipe.RecipeSubProvider;
import net.minecraft.data.recipes.RecipeOutput;
import net.sievert.jolcraft.data.id.recipe.JolCraftRecipeIds;
import net.sievert.jolcraft.datagen.base.JolCraftDataProvider;
import net.sievert.jolcraft.datagen.JolCraftDataGenerator;
import net.sievert.jolcraft.datagen.base.JolCraftSubDataProvider;
import net.sievert.jolcraft.datagen.recipe.subprovider.hand.CompassHandInteractions;
import net.sievert.jolcraft.datagen.recipe.subprovider.hand.SpannerHandInteractions;
import net.sievert.jolcraft.datagen.recipe.subprovider.hand.TestHandInteractions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record HandInteractionRecipesSubProvider(
        JolCraftDataProvider<RecipeOutput> parent) implements RecipeSubProvider {

    public HandInteractionRecipesSubProvider(@NotNull JolCraftDataProvider<RecipeOutput> parent) {
        this.parent = parent;
    }

    @Override
    public @NotNull JolCraftDataProvider<RecipeOutput> parent() {
        return parent;
    }

    @Override
    public @NotNull String id() {
        return folder();
    }

    @Override
    public @NotNull String folder() {
        return JolCraftRecipeIds.HAND_INTERACTION;
    }

    @Override
    public @NotNull List<? extends JolCraftSubDataProvider<RecipeOutput>> subProviders() {
        List<JolCraftSubDataProvider<RecipeOutput>> providers = new ArrayList<>(List.of(
                new CompassHandInteractions(this),
                new SpannerHandInteractions(this)
        ));

        // The dev_key test recipe is for manual testing; it is emitted only when the dev flag is set.
        if (JolCraftDataGenerator.includeDevData()) {
            providers.add(new TestHandInteractions(this));
        }

        return List.copyOf(providers);
    }
}
