package net.sievert.jolcraft.datagen.provider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.datagen.contribute.RecipeContributor;

/** Every module's recipes through one vanilla runner; contributors write against {@link Provider}. */
public final class Recipes extends RecipeProvider.Runner {

    private final List<RecipeContributor> contributors;

    public Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, List<RecipeContributor> contributors) {
        super(output, registries);
        this.contributors = contributors;
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return new Provider(registries, output, contributors);
    }

    @Override
    public String getName() {
        return "Recipes - " + JolCraft.MOD_ID;
    }

    /** Vanilla's recipe provider with the helpers every recipe needs opened to contributors: the output, the lookups, unlock criteria, builders. */
    public static final class Provider extends RecipeProvider {

        private final List<RecipeContributor> contributors;

        Provider(HolderLookup.Provider registries, RecipeOutput output, List<RecipeContributor> contributors) {
            super(registries, output);
            this.contributors = contributors;
        }

        @Override
        protected void buildRecipes() {
            contributors.forEach(contributor -> contributor.recipes(this));
        }

        public RecipeOutput output() {
            return output;
        }

        public HolderLookup.Provider registries() {
            return registries;
        }

        public HolderGetter<Item> items() {
            return items;
        }

        @Override
        public Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike item) {
            return super.has(item);
        }

        @Override
        public Criterion<InventoryChangeTrigger.TriggerInstance> has(TagKey<Item> tag) {
            return super.has(tag);
        }

        @Override
        public Ingredient tag(TagKey<Item> tag) {
            return super.tag(tag);
        }

        @Override
        public ShapedRecipeBuilder shaped(RecipeCategory category, ItemLike item) {
            return super.shaped(category, item);
        }

        @Override
        public ShapedRecipeBuilder shaped(RecipeCategory category, ItemLike item, int count) {
            return super.shaped(category, item, count);
        }

        @Override
        public ShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike item) {
            return super.shapeless(category, item);
        }

        @Override
        public ShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike item, int count) {
            return super.shapeless(category, item, count);
        }
    }
}
