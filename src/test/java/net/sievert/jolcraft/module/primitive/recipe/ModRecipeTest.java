package net.sievert.jolcraft.module.primitive.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

/** The bookkeeping and the recipe contract's defaults; the game's registries are frozen here, so scenes in the game-test mod match real recipes. */
class ModRecipeTest {

    record Nothing(RecipeKey<SingleRecipeInput, Nothing> key) implements Recipe.Custom<SingleRecipeInput> {
        @Override
        public boolean matches(SingleRecipeInput input, Level level) {
            return input.item().isEmpty();
        }
    }

    private final RecipeKey<SingleRecipeInput, Nothing> nothing = RecipeKey.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "nothing"),
            MapCodec.unit(() -> new Nothing(null)), StreamCodec.unit(new Nothing(null)));
    private final RecipeKey<SingleRecipeInput, Nothing> less = RecipeKey.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "less"),
            MapCodec.unit(() -> new Nothing(null)), StreamCodec.unit(new Nothing(null)));
    private final IEventBus bus = BusBuilder.builder().build();

    @Test
    void kindsAreCountedOncePerId() {
        ModRecipe recipe = new ModRecipe();
        recipe.declare(nothing);
        recipe.declare(less);
        Assertions.assertEquals(2, recipe.declared());
        Assertions.assertThrows(IllegalStateException.class, () -> recipe.declare(nothing));
    }

    @Test
    void theGamesNamespaceIsRefused() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ModRecipe().declare(
                RecipeKey.of(Identifier.withDefaultNamespace("nothing"), MapCodec.unit(() -> new Nothing(null)), StreamCodec.unit(new Nothing(null)))));
    }

    @Test
    void registeringTwiceOrDeclaringAfterItIsRefused() {
        ModRecipe recipe = new ModRecipe();
        recipe.declare(nothing);
        recipe.register(bus);
        Assertions.assertThrows(IllegalStateException.class, () -> recipe.register(bus));
        Assertions.assertThrows(IllegalStateException.class, () -> recipe.declare(less));
    }

    @Test
    @SuppressWarnings("deprecation") // the serializer's stream codec component is deprecated by the game and still what it reads
    void aKeyBuildsItsTypeAndSerializerAndIsNotThereBeforeTheGameRegistered() {
        Assertions.assertSame(nothing.codec(), nothing.buildSerializer().codec());
        Assertions.assertSame(nothing.streamCodec(), nothing.buildSerializer().streamCodec());
        Assertions.assertEquals("jolcraft:nothing", nothing.buildType().toString());
        IllegalStateException refused = Assertions.assertThrows(IllegalStateException.class, nothing::type);
        Assertions.assertTrue(refused.getMessage().contains("declared"), refused.getMessage());
        Assertions.assertThrows(IllegalStateException.class, nothing::serializer);
        Assertions.assertThrows(IllegalStateException.class, nothing::category);
    }

    @Test
    void aCustomRecipeAnswersTheGamesQuestionsForAKindOutsideTheRecipeBook() {
        Nothing recipe = new Nothing(nothing);
        Assertions.assertTrue(recipe.isSpecial());
        Assertions.assertFalse(recipe.showNotification());
        Assertions.assertEquals("", recipe.group());
        Assertions.assertSame(PlacementInfo.NOT_PLACEABLE, recipe.placementInfo());
        Assertions.assertTrue(recipe.assemble(new SingleRecipeInput(ItemStack.EMPTY)).isEmpty());
        Assertions.assertSame(nothing, recipe.key());
    }

    @Test
    void theIdIsTheFolderName() {
        Assertions.assertEquals(Dictionary.RECIPE, new ModRecipe().id());
    }
}
