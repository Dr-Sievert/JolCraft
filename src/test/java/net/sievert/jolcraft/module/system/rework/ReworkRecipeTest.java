package net.sievert.jolcraft.module.system.rework;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.module.primitive.recipe.RecipeKey;

/**
 * The rework recipe's codecs under the main mod's registries, what it leaves to defaults and refuses at load, and what
 * it gives the recipe book. Matching, assembling and remainders need stacks, which this JVM cannot make, so they are
 * scenes.
 */
class ReworkRecipeTest {

    private static final String BOOTS = "\"base\":\"minecraft:leather_boots\",\"result\":\"minecraft:leather_boots\"";

    private final RegistryAccess.Frozen registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries); // an item set resolves only under registry-aware ops

    private DataResult<ReworkRecipe> parse(String fields) {
        return ReworkRecipe.CODEC.codec().parse(ops, JsonParser.parseString("{" + fields + "}"));
    }

    private JsonElement written(ReworkRecipe recipe) {
        return ReworkRecipe.CODEC.codec().encodeStart(ops, recipe).getOrThrow();
    }

    private static String sticks(int count) {
        return "\"ingredients\":[" + String.join(",", Collections.nCopies(count, "\"minecraft:stick\"")) + "]";
    }

    /** A leather helmet and an iron ingot made an iron helmet, carrying by the rule given. */
    private static ReworkRecipe helmet(Optional<List<DataComponentType<?>>> keep, List<DataComponentType<?>> remove) {
        return new ReworkRecipe(new Recipe.CommonInfo(true), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""), Ingredient.of(Items.LEATHER_HELMET),
                List.of(Ingredient.of(Items.IRON_INGOT)), new ItemStackTemplate(Items.IRON_HELMET), keep, remove, true);
    }

    @Test
    void aRecipeKeepingTypesRoundTripsWithEveryOtherField() {
        ReworkRecipe recipe = new ReworkRecipe(new Recipe.CommonInfo(false), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.EQUIPMENT, "helmets"),
                Ingredient.of(Items.LEATHER_HELMET), List.of(Ingredient.of(Items.IRON_INGOT), Ingredient.of(Items.STRING)), new ItemStackTemplate(Items.IRON_HELMET, 2),
                Optional.of(List.of(DataComponents.CUSTOM_NAME, DataComponents.DYED_COLOR)), List.of(), false);
        ReworkRecipe back = RecipeKey.check(registries, ReworkRecipe.CODEC, recipe).getOrThrow();
        Assertions.assertEquals(written(recipe), written(back), "written the same both times");
        Assertions.assertTrue(!back.showNotification() && back.category() == CraftingBookCategory.EQUIPMENT && back.group().equals("helmets"), "book info " + written(back));
        Assertions.assertEquals(2, back.ingredients().size(), "ingredients");
        Assertions.assertEquals(2, back.result().count(), "result count");
        Assertions.assertEquals(Optional.of(List.of(DataComponents.CUSTOM_NAME, DataComponents.DYED_COLOR)), back.keep(), "kept types");
        Assertions.assertFalse(back.baseRemainder(), "the base's remainder held back");
    }

    @Test
    void removeRoundTripsAndAnEmptyKeepStaysApartFromAnAbsentOne() {
        ReworkRecipe removing = RecipeKey.check(registries, ReworkRecipe.CODEC, helmet(Optional.empty(), List.of(DataComponents.DYED_COLOR))).getOrThrow();
        Assertions.assertTrue(removing.keep().isEmpty() && removing.remove().equals(List.of(DataComponents.DYED_COLOR)), "removed types " + written(removing));
        ReworkRecipe keepingNothing = RecipeKey.check(registries, ReworkRecipe.CODEC, helmet(Optional.of(List.of()), List.of())).getOrThrow();
        Assertions.assertEquals(Optional.of(List.of()), keepingNothing.keep(), "an empty keep, carrying nothing " + written(keepingNothing));
    }

    @Test
    void aRecipeLeftToItsDefaultsCarriesEverythingAndGivesTheBaseRemainderBack() {
        ReworkRecipe parsed = parse("\"base\":\"minecraft:clock\",\"result\":\"minecraft:compass\"").getOrThrow();
        Assertions.assertTrue(parsed.showNotification() && parsed.category() == CraftingBookCategory.MISC && parsed.group().isEmpty(), "book info " + written(parsed));
        Assertions.assertTrue(parsed.ingredients().isEmpty() && parsed.keep().isEmpty() && parsed.remove().isEmpty() && parsed.baseRemainder(), "rework fields " + written(parsed));
        Assertions.assertEquals(1, parsed.result().count(), "one result");
    }

    @Test
    void keepWithRemoveNineIngredientsAndATransientTypeAreRefused() {
        Assertions.assertTrue(parse(BOOTS + ",\"keep\":[\"minecraft:custom_name\"]").isSuccess(), "keep alone");
        Assertions.assertTrue(parse(BOOTS + ",\"remove\":[\"minecraft:dyed_color\"]").isSuccess(), "remove alone");
        Assertions.assertTrue(parse(BOOTS + ",\"keep\":[\"minecraft:custom_name\"],\"remove\":[\"minecraft:dyed_color\"]").isError(), "keep with remove");
        Assertions.assertTrue(parse(BOOTS + "," + sticks(8)).isSuccess(), "eight fill the grid with the base");
        Assertions.assertTrue(parse(BOOTS + "," + sticks(9)).isError(), "nine ingredients beside the base");
        Assertions.assertTrue(parse(BOOTS + ",\"keep\":[\"minecraft:creative_slot_lock\"]").isError(), "a transient type");
    }

    @Test
    void theSerializersStreamCodecCarriesARecipeOverTheWire() {
        ReworkRecipe recipe = helmet(Optional.empty(), List.of(DataComponents.DYED_COLOR));
        @SuppressWarnings("deprecation") // the game marks the stream codec deprecated; NeoForge still sends recipes a mod asks for through it
        StreamCodec<RegistryFriendlyByteBuf, ReworkRecipe> wire = ReworkContent.REWORK.get().streamCodec();
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries, ConnectionType.OTHER);
        wire.encode(buffer, recipe);
        Assertions.assertEquals(written(recipe), written(wire.decode(buffer)), "read back as written");
    }

    @Test
    void theRecipeBookPlacesTheBaseThenTheIngredientsAndShowsTheTemplateAtTheTable() {
        ReworkRecipe recipe = helmet(Optional.empty(), List.of());
        Assertions.assertFalse(recipe.placementInfo().isImpossibleToPlace(), "placeable");
        Assertions.assertEquals(List.of(recipe.base(), recipe.ingredients().getFirst()), recipe.placementInfo().ingredients(), "the base, then the ingredients");
        Assertions.assertEquals(1, recipe.display().size(), "one display");
        ShapelessCraftingRecipeDisplay display = (ShapelessCraftingRecipeDisplay) recipe.display().getFirst();
        Assertions.assertEquals(List.of(recipe.base().display(), recipe.ingredients().getFirst().display()), display.ingredients(), "a slot for the base, then the ingot");
        Assertions.assertEquals(new SlotDisplay.ItemStackSlotDisplay(recipe.result()), display.result(), "the result's template");
        Assertions.assertEquals(new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE), display.craftingStation(), "at the crafting table");
    }
}
