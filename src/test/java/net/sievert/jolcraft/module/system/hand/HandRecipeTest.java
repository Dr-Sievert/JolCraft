package net.sievert.jolcraft.module.system.hand;

import java.util.List;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.module.composite.loot.ItemYield;
import net.sievert.jolcraft.module.composite.loot.SoundYield;
import net.sievert.jolcraft.module.composite.loot.Yield;
import net.sievert.jolcraft.module.composite.loot.Yielded;
import net.sievert.jolcraft.module.primitive.recipe.Fate;
import net.sievert.jolcraft.module.primitive.recipe.ItemInput;

/**
 * The hand recipe's codec over the main mod's registries: what it round trips and what it refuses at load. Matching
 * needs stacks, which this JVM cannot make, so the orientation, the sneak rule and the input are scenes.
 */
class HandRecipeTest {

    private static final String TOOL = "\"tool\":{\"predicate\":{\"items\":\"minecraft:shears\"},\"fate\":{\"kind\":\"damage\"}}";
    private static final String TARGET = "\"target\":{\"predicate\":{\"items\":\"minecraft:white_wool\"}}";
    private static final String RESULTS = "\"results\":[{\"type\":\"jolcraft:item\",\"pool\":{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"minecraft:string\"}]}}]";

    private final HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries); // an item set and a sound resolve only under registry-aware ops

    /** A kind outside the four, as another module's own would be. */
    record Foreign() implements Yield {

        @Override
        public MapCodec<Foreign> codec() {
            return MapCodec.unit(Foreign::new);
        }

        @Override
        public List<? extends Yielded> roll(LootContext context) {
            return List.of();
        }
    }

    private static ItemInput input(net.minecraft.world.item.Item item, Fate fate) {
        return ItemInput.of(ItemPredicate.Builder.item().of(BuiltInRegistries.ITEM, item), fate);
    }

    private DataResult<HandRecipe> parse(String... fields) {
        return HandRecipe.CODEC.codec().parse(ops, JsonParser.parseString("{" + String.join(",", fields) + "}"));
    }

    @Test
    void aRecipeRoundTripsThroughItsCodec() {
        HandRecipe recipe = new HandRecipe(input(Items.SHEARS, Fate.damage(1, 3)), input(Items.WHITE_WOOL, Fate.consume(1)),
                List.of(ItemYield.of(LootPool.lootPool().add(LootItem.lootTableItem(Items.STRING)))),
                Optional.of(SoundYield.of(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ANVIL_USE), SoundSource.PLAYERS, 1.0F, 1.0F)), Optional.empty(), true, 20);
        HandRecipe back = HandKeys.RECIPE.check(registries, recipe).getOrThrow();
        Assertions.assertEquals(Fate.Kind.DAMAGE, back.tool().fate().kind());
        Assertions.assertEquals(3, back.tool().fate().amount().maxInclusive());
        Assertions.assertEquals(recipe.target().predicate().items(), back.target().predicate().items());
        Assertions.assertEquals(1, back.results().size());
        Assertions.assertEquals(SoundEvents.ANVIL_USE, back.success().orElseThrow().sound().value());
        Assertions.assertTrue(back.failure().isEmpty() && back.sneaking() && back.cooldown() == 20, "failure, sneaking, cooldown " + back);
    }

    @Test
    void aRecipeLeftToItsDefaultsConsumesOneTargetAndWaitsTenTicks() {
        HandRecipe parsed = parse(TOOL, TARGET, RESULTS).getOrThrow();
        Assertions.assertEquals(Fate.consume(1), parsed.target().fate(), "the target is consumed by one");
        Assertions.assertTrue(!parsed.sneaking() && parsed.cooldown() == HandRecipe.COOLDOWN && parsed.success().isEmpty() && parsed.failure().isEmpty(),
                "sneaking, cooldown, sounds " + parsed);
    }

    @Test
    void noResultAZeroCooldownAndABoundedCountAreRefused() {
        Assertions.assertTrue(parse(TOOL, TARGET, "\"results\":[]").isError(), "no result");
        Assertions.assertTrue(parse(TOOL, TARGET, RESULTS, "\"cooldown\":0").isError(), "a cooldown of 0");
        Assertions.assertTrue(parse("\"tool\":{\"predicate\":{\"items\":\"minecraft:shears\",\"count\":{\"min\":2}}}", TARGET, RESULTS).isError(), "a bounded count");
    }

    @Test
    void aKindOutsideTheFourIsRefused() {
        Assertions.assertTrue(HandRecipe.deliverable(List.of(new Foreign())).isError(), "at load");
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HandRecipe(input(Items.SHEARS, Fate.keep()), input(Items.WHITE_WOOL, Fate.consume(1)),
                List.of(new Foreign()), Optional.empty(), Optional.empty(), false, HandRecipe.COOLDOWN), "and in code");
    }

    @Test
    void aSoundReadingAKeyOutsideTheShapeIsRefused() {
        String player = "\"success\":{\"sound\":\"minecraft:block.anvil.use\",\"volume\":{\"type\":\"minecraft:score\",\"target\":\"this\",\"score\":\"x\"}}";
        Assertions.assertTrue(parse(TOOL, TARGET, RESULTS, player).isSuccess(), "the player is in the shape");
        Assertions.assertTrue(parse(TOOL, TARGET, RESULTS, player.replace("\"this\"", "\"attacker\"")).isError(), "an attacker is not");
    }
}
