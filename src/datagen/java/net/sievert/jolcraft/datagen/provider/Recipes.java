package net.sievert.jolcraft.datagen.provider;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
import org.jetbrains.annotations.NotNull;

/** Every module's recipes through one vanilla runner; contributors write against {@link Provider}. */
public final class Recipes extends RecipeProvider.Runner {

    private final List<RecipeContributor> contributors;

    public Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, List<RecipeContributor> contributors) {
        super(output, registries);
        this.contributors = contributors;
    }

    @Override
    protected @NotNull RecipeProvider createRecipeProvider(HolderLookup.@NotNull Provider registries, @NotNull RecipeOutput output) {
        return new Provider(registries, output, contributors);
    }

    @Override
    public @NotNull String getName() {
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

        /** A recipe of one of the mod's own kinds, under its id; the kind's codec writes it. */
        public void add(Identifier id, net.minecraft.world.item.crafting.Recipe<?> recipe) {
            output().accept(ResourceKey.create(Registries.RECIPE, id), recipe, null);
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
        public @NotNull Criterion<InventoryChangeTrigger.@NotNull TriggerInstance> has(@NotNull ItemLike item) {
            return super.has(item);
        }

        @Override
        public @NotNull Criterion<InventoryChangeTrigger.@NotNull TriggerInstance> has(@NotNull TagKey<Item> tag) {
            return super.has(tag);
        }

        @Override
        public @NotNull Ingredient tag(@NotNull TagKey<Item> tag) {
            return super.tag(tag);
        }

        @Override
        public @NotNull ShapedRecipeBuilder shaped(@NotNull RecipeCategory category, @NotNull ItemLike item) {
            return super.shaped(category, item);
        }

        @Override
        public @NotNull ShapedRecipeBuilder shaped(@NotNull RecipeCategory category, @NotNull ItemLike item, int count) {
            return super.shaped(category, item, count);
        }

        @Override
        public @NotNull ShapelessRecipeBuilder shapeless(@NotNull RecipeCategory category, @NotNull ItemLike item) {
            return super.shapeless(category, item);
        }

        @Override
        public @NotNull ShapelessRecipeBuilder shapeless(@NotNull RecipeCategory category, @NotNull ItemLike item, int count) {
            return super.shapeless(category, item, count);
        }
    }
}
