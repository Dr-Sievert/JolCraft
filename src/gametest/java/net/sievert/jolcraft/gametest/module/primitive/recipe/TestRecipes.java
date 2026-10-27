package net.sievert.jolcraft.gametest.module.primitive.recipe;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import net.sievert.jolcraft.module.composite.loot.Yield;
import net.sievert.jolcraft.gametest.module.primitive.loot.TestShapes;
import net.sievert.jolcraft.gametest.module.composite.loot.TestYields;
import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.module.primitive.recipe.Recipe;
import net.sievert.jolcraft.module.primitive.recipe.RecipeKey;
import net.sievert.jolcraft.module.primitive.registration.Ids;
import org.jetbrains.annotations.NotNull;

/** The recipe kinds the recipe and yield scenes use: doubling one item, and yielding what the data names; the game-test mod declares them in its root and its data pack holds their recipes. */
public final class TestRecipes {

    /** One item in, two of it out. */
    public record Doubling(Item item) implements Recipe.Custom<SingleRecipeInput> {
        public static final MapCodec<Doubling> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Doubling::item)).apply(instance, Doubling::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Doubling> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(Registries.ITEM), Doubling::item, Doubling::new);

        @Override
        public RecipeKey<SingleRecipeInput, Doubling> key() {
            return DOUBLING;
        }

        @Override
        public boolean matches(SingleRecipeInput input, Level level) {
            return input.item().is(item);
        }

        @Override
        public @NotNull ItemStack assemble(SingleRecipeInput input) {
            return new ItemStack(item, 2);
        }
    }

    public static final RecipeKey<SingleRecipeInput, Doubling> DOUBLING = RecipeKey.of(Ids.of(JolCraftGameTests.MOD_ID, "doubling"), Doubling.CODEC, Doubling.STREAM_CODEC);

    /** A stick in, the yields the data names out; validated against the place-and-entity shape the yield scenes roll it with. */
    public record Yielding(List<Yield> yields) implements Recipe.Custom<SingleRecipeInput> {
        public static final MapCodec<Yielding> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                TestYields.KINDS.listCodec().validate(TestShapes.AT_A_PLACE.listValidator()).fieldOf("yields").forGetter(Yielding::yields)).apply(instance, Yielding::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Yielding> STREAM_CODEC = StreamCodec.composite(
                TestYields.KINDS.streamCodec().apply(ByteBufCodecs.list()), Yielding::yields, Yielding::new);

        @Override
        public RecipeKey<SingleRecipeInput, Yielding> key() {
            return YIELDING;
        }

        @Override
        public boolean matches(SingleRecipeInput input, Level level) {
            return input.item().is(Items.STICK);
        }
    }

    public static final RecipeKey<SingleRecipeInput, Yielding> YIELDING = RecipeKey.of(Ids.of(JolCraftGameTests.MOD_ID, "yielding"), Yielding.CODEC, Yielding.STREAM_CODEC);

    private TestRecipes() {
    }

    public static void declare(Recipe recipe) {
        recipe.declare(DOUBLING);
        recipe.declare(YIELDING);
    }
}
