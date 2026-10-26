package net.sievert.jolcraft.gametest.module.primitive.recipe;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.primitive.registration.Ids;

/**
 * The recipe primitive against the real data pack: the game-test mod's one doubling recipe is found for the item it
 * names and not for another, assembles its result, and is listed with the kind; the kind's type, serializer and
 * category are registered under its id.
 */
public final class RecipeScenes {

    @Scene(maxTicks = 20)
    public void theKindIsRegisteredAndItsRecipeIsFoundAssembledAndListed(TestScene scene) {
        scene.check("the kind's type, serializer and category are registered under its id", () -> scene.assertTrue(
                TestRecipes.DOUBLING.id().equals(BuiltInRegistries.RECIPE_TYPE.getKey(TestRecipes.DOUBLING.type()))
                        && TestRecipes.DOUBLING.id().equals(BuiltInRegistries.RECIPE_SERIALIZER.getKey(TestRecipes.DOUBLING.serializer()))
                        && TestRecipes.DOUBLING.id().equals(BuiltInRegistries.RECIPE_BOOK_CATEGORY.getKey(TestRecipes.DOUBLING.category())), "registrations"));
        Optional<RecipeHolder<TestRecipes.Doubling>> found = TestRecipes.DOUBLING.find(scene.level(), new SingleRecipeInput(new ItemStack(Items.STICK)));
        scene.check("a stick finds the doubling recipe", () -> scene.assertTrue(
                found.isPresent() && found.get().id().identifier().equals(Ids.of(JolCraftGameTests.MOD_ID, "double_stick")), "found " + found));
        scene.check("which assembles two sticks", () -> scene.assertTrue(
                found.isPresent() && found.get().value().assemble(new SingleRecipeInput(new ItemStack(Items.STICK))).is(Items.STICK)
                        && found.get().value().assemble(new SingleRecipeInput(new ItemStack(Items.STICK))).getCount() == 2, "assembled"));
        scene.check("and answers the game with its registered serializer and category", () -> scene.assertTrue(
                found.isPresent() && found.get().value().getSerializer() == TestRecipes.DOUBLING.serializer() && found.get().value().recipeBookCategory() == TestRecipes.DOUBLING.category(), "serializer and category"));
        scene.check("a stone finds nothing", () -> scene.assertTrue(
                TestRecipes.DOUBLING.find(scene.level(), new SingleRecipeInput(new ItemStack(Items.STONE))).isEmpty(), "stone"));
        List<RecipeHolder<TestRecipes.Doubling>> all = TestRecipes.DOUBLING.all(scene.level()).toList();
        scene.check("the kind lists its one recipe", () -> scene.assertTrue(all.size() == 1 && all.getFirst().value().item() == Items.STICK, "all " + all));
        scene.succeed();
    }
}
