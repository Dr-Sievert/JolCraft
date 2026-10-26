package net.sievert.jolcraft.gametest.module.primitive.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.module.primitive.recipe.Recipe;
import net.sievert.jolcraft.module.primitive.recipe.RecipeKey;
import net.sievert.jolcraft.module.primitive.registration.Ids;
import org.jetbrains.annotations.NotNull;

/** The recipe kind the recipe scenes use: doubling one item; the game-test mod declares it in its root and its data pack holds one recipe of it. */
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

    private TestRecipes() {
    }

    public static void declare(Recipe recipe) {
        recipe.declare(DOUBLING);
    }
}
