package net.sievert.jolcraft.module.primitive.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

/** The round trip a recipe kind offers datagen: what its codec refuses, datagen refuses. */
class RecipeCheckTest {

    /** A recipe whose codec refuses a count above ten on the way in only, so the check's read half is what refuses it. */
    record Counted(int count) implements Recipe.Custom<SingleRecipeInput> {
        static final Codec<Integer> AT_MOST_TEN = Codec.INT.flatXmap(count -> count > 10 ? DataResult.error(() -> "count " + count + " is above ten") : DataResult.success(count), DataResult::success);
        static final MapCodec<Counted> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                AT_MOST_TEN.fieldOf("count").forGetter(Counted::count)).apply(instance, Counted::new));
        static final StreamCodec<RegistryFriendlyByteBuf, Counted> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Counted::new, Counted::count).cast();

        @Override
        public RecipeKey<SingleRecipeInput, Counted> key() {
            return COUNTED;
        }

        @Override
        public boolean matches(SingleRecipeInput input, Level level) {
            return false;
        }
    }

    static final RecipeKey<SingleRecipeInput, Counted> COUNTED = RecipeKey.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "counted"), Counted.CODEC, Counted.STREAM_CODEC);

    private final HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    @Test
    void aRecipeItsCodecAcceptsComesBackEqual() {
        Assertions.assertEquals(new Counted(5), COUNTED.check(registries, new Counted(5)).getOrThrow());
    }

    @Test
    void aRecipeItsCodecRefusesOnTheWayBackIsAnErrorNamingWhy() {
        DataResult<Counted> refused = COUNTED.check(registries, new Counted(11));
        Assertions.assertTrue(refused.isError());
        Assertions.assertTrue(refused.error().orElseThrow().message().contains("11"), refused.error().orElseThrow().message());
    }
}
