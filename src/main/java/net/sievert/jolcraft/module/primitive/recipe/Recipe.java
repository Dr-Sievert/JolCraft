package net.sievert.jolcraft.module.primitive.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;

import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.Primitive;
import net.sievert.jolcraft.stereotype.Contract;
import net.sievert.jolcraft.util.text.Dictionary;
import org.jetbrains.annotations.NotNull;

/**
 * The mod's own kinds of recipe: what a bench, a cauldron, a dwarf or a bounty board matches against the data
 * pack. A module declares a {@link RecipeKey} per kind while it is built, with the codecs of its recipe record;
 * the root calls {@link #register(IEventBus)} once, which registers the kind's type, serializer and recipe-book
 * category under the key's id; from then on the key finds the recipes of its kind on a level. The recipe itself
 * is the module's record implementing {@link Custom}. The game's own class of the same name,
 * {@code net.minecraft.world.item.crafting.Recipe}, is reached through {@code Custom} and the key, so a module
 * rarely names it.
 */
@Primitive(Dictionary.RECIPE)
public interface Recipe extends Module {

    /** Declares a kind. Only while modules are built, once per id. */
    void declare(RecipeKey<?, ?> key);

    /** Registers every declared kind with the game. The root calls it once. */
    void register(IEventBus modBus);

    /**
     * What a module's recipe record implements: {@code matches} and, when the recipe yields an item,
     * {@code assemble}; the rest the game asks of a recipe is answered here, since a kind of the mod's own has no
     * recipe-book page and nothing a crafting grid could place.
     */
    @Contract
    // The game's packages are null-marked: where a type variable of ours enters one of its generics, the use says it is non-null.
    interface Custom<I extends RecipeInput> extends net.minecraft.world.item.crafting.Recipe<@NotNull I> {

        /** The kind this recipe belongs to. */
        RecipeKey<I, ? extends Custom<I>> key();

        /** The item the recipe yields; empty for a kind whose outcome is not an item. */
        @Override
        default @NotNull ItemStack assemble(I input) {
            return ItemStack.EMPTY;
        }

        /** Special, as the game calls recipes outside the recipe book, so it keeps them without a placement. */
        @Override
        default boolean isSpecial() {
            return true;
        }

        @Override
        default boolean showNotification() {
            return false;
        }

        @Override
        default @NotNull String group() {
            return "";
        }

        @Override
        default @NotNull PlacementInfo placementInfo() {
            return PlacementInfo.NOT_PLACEABLE;
        }

        @Override
        default @NotNull RecipeBookCategory recipeBookCategory() {
            return key().category();
        }

        @Override
        default @NotNull RecipeSerializer<? extends net.minecraft.world.item.crafting.Recipe<@NotNull I>> getSerializer() {
            return key().serializer();
        }

        @Override
        default @NotNull RecipeType<? extends net.minecraft.world.item.crafting.Recipe<@NotNull I>> getType() {
            return key().type();
        }
    }
}
