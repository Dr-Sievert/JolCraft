package net.sievert.jolcraft.module.primitive.recipe;

import java.util.Optional;
import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.sievert.jolcraft.stereotype.Handle;
import org.jetbrains.annotations.NotNull;

/**
 * One kind of recipe: its id, under which the game knows its type, its serializer and its recipe-book category,
 * and the codecs of the module's recipe record, one for the data pack and one for the wire. A key is a constant
 * where it is used, declared through {@link Recipe} while its module is built, and finds recipes on a level once
 * the game has registered it: {@link #find} the first that matches an input, {@link #all} every one of the kind.
 */
@Handle
public sealed interface RecipeKey<I extends RecipeInput, R extends Recipe.Custom<I>> {

    static <I extends RecipeInput, R extends Recipe.Custom<I>> RecipeKey<I, R> of(Identifier id, MapCodec<R> codec, StreamCodec<RegistryFriendlyByteBuf, R> streamCodec) {
        return new Spec<>(id, codec, streamCodec);
    }

    Identifier id();

    /** The codec the data pack writes a recipe of this kind with. */
    MapCodec<R> codec();

    /** The codec a recipe of this kind crosses the wire with. */
    StreamCodec<RegistryFriendlyByteBuf, R> streamCodec();

    /** The registered type, there once the game registered what a module declared. */
    RecipeType<@NotNull R> type();

    RecipeSerializer<@NotNull R> serializer();

    RecipeBookCategory category();

    /** The game's type for this kind, built once by {@link Recipe} when the game registers. */
    RecipeType<@NotNull R> buildType();

    /** The game's serializer for this kind, over the key's codecs. */
    RecipeSerializer<@NotNull R> buildSerializer();

    /**
     * The first recipe of this kind that matches the input on the level's data pack, if any. An input whose
     * {@code isEmpty()} is true matches nothing, since the game asks no recipe then; an input not made of stacks
     * overrides {@code isEmpty()}.
     */
    default Optional<RecipeHolder<@NotNull R>> find(ServerLevel level, I input) {
        return level.recipeAccess().getRecipeFor(type(), input, level);
    }

    /** Every recipe of this kind on the level's data pack. */
    default Stream<RecipeHolder<@NotNull R>> all(ServerLevel level) {
        return level.recipeAccess().recipeMap().byType(type()).stream();
    }

    /** The key with its codecs; the holders resolve the registered objects lazily. */
    record Spec<I extends RecipeInput, R extends Recipe.Custom<I>>(Identifier id, MapCodec<R> codec, StreamCodec<RegistryFriendlyByteBuf, R> streamCodec,
                                                                   DeferredHolder<RecipeType<?>, @org.jetbrains.annotations.NotNull RecipeType<@org.jetbrains.annotations.NotNull R>> deferredType, DeferredHolder<RecipeSerializer<?>, @org.jetbrains.annotations.NotNull RecipeSerializer<@org.jetbrains.annotations.NotNull R>> deferredSerializer,
                                                                   DeferredHolder<RecipeBookCategory, @org.jetbrains.annotations.NotNull RecipeBookCategory> deferredCategory) implements RecipeKey<I, R> {

        Spec(Identifier id, MapCodec<R> codec, StreamCodec<RegistryFriendlyByteBuf, R> streamCodec) {
            this(id, codec, streamCodec, DeferredHolder.create(Registries.RECIPE_TYPE, id), DeferredHolder.create(Registries.RECIPE_SERIALIZER, id),
                    DeferredHolder.create(Registries.RECIPE_BOOK_CATEGORY, id));
        }

        @Override
        public RecipeType<@NotNull R> type() {
            return bound(deferredType);
        }

        @Override
        public RecipeSerializer<@NotNull R> serializer() {
            return bound(deferredSerializer);
        }

        @Override
        public RecipeBookCategory category() {
            return bound(deferredCategory);
        }

        private <K, T extends K> T bound(DeferredHolder<K, @org.jetbrains.annotations.NotNull T> holder) {
            if (!holder.isBound()) {
                throw new IllegalStateException(id + " is not registered: no module declared it through Recipe,"
                        + " or it is used before the game registered recipe kinds, which happens after the mod constructors ran");
            }
            return holder.get();
        }

        @Override
        public RecipeType<@NotNull R> buildType() {
            return RecipeType.simple(id);
        }

        @Override
        public RecipeSerializer<@NotNull R> buildSerializer() {
            return new RecipeSerializer<>(codec, streamCodec);
        }

        @Override
        public @NotNull String toString() {
            return id.toString();
        }
    }
}
