package net.sievert.jolcraft.datagen.provider;

import java.util.List;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeUnlockAdvancementBuilder;
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
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;

import net.sievert.jolcraft.module.primitive.recipe.RecipeKey;
import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.datagen.contribute.RecipeContributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

        /**
         * A recipe of one of the mod's own kinds that the recipe book does not show, under its id; its serializer's codec
         * writes it. A recipe the book shows needs a way to be unlocked and goes through the overload that takes one.
         */
        public void add(Identifier id, Recipe<?> recipe) {
            if (!recipe.isSpecial()) {
                throw new IllegalStateException(id + " is shown in the recipe book and needs a way to be unlocked: add it with its category and unlock criteria");
            }
            write(id, recipe, null);
        }

        /**
         * A recipe the recipe book shows, under its id, with the advancement that unlocks it, written as vanilla's builders
         * write theirs: under {@code recipes/<category>/}, granted by any one of the criteria, and refused with none.
         */
        public void add(Identifier id, Recipe<?> recipe, RecipeCategory category, RecipeUnlockAdvancementBuilder unlock) {
            write(id, recipe, unlock.build(output, ResourceKey.create(Registries.RECIPE, id), category));
        }

        private void write(Identifier id, Recipe<?> recipe, @Nullable AdvancementHolder advancement) {
            check(recipe).getOrThrow(message -> new IllegalStateException(id + " does not survive its own codec: " + message));
            output().accept(ResourceKey.create(Registries.RECIPE, id), recipe, advancement);
        }

        public RecipeOutput output() {
            return output;
        }

        /** The recipe through its serializer's codec under the registries, as at load: a kind's own, or a game type's. */
        private <T extends Recipe<?>> DataResult<T> check(T recipe) {
            @SuppressWarnings("unchecked")
            MapCodec<T> codec = (MapCodec<T>) recipe.getSerializer().codec();
            return RecipeKey.check(registries(), codec, recipe);
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
