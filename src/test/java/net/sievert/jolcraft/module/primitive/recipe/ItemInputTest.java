package net.sievert.jolcraft.module.primitive.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** The values and their codecs; covering, matching and applying need stacks and a level and are a scene in the game-test mod. */
class ItemInputTest {

    @Test
    void aFateIsItsKindAndAmount() {
        Assertions.assertEquals(Fate.consume(3), roundTrip(Fate.consume(3)));
        Fate uniform = roundTrip(Fate.damage(2, 5));
        Assertions.assertEquals(Fate.Kind.DAMAGE, uniform.kind());
        Assertions.assertEquals(5, uniform.amount().maxInclusive());
        JsonElement json = Fate.CODEC.encodeStart(JsonOps.INSTANCE, Fate.consume(3)).getOrThrow();
        Assertions.assertEquals("consume", json.getAsJsonObject().get("kind").getAsString());
        Assertions.assertTrue(Fate.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"kind\":\"consume\",\"amount\":0}")).isError(), "an amount is at least one");
    }

    @Test
    void anInputKeepsThePredicateUnboundedSoTheFateIsTheOneAmount() {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)); // an item set resolves only under registry-aware ops
        ItemInput sticks = ItemInput.of(ItemPredicate.Builder.item().of(BuiltInRegistries.ITEM, Items.STICK), Fate.consume(2));
        ItemInput back = ItemInput.CODEC.parse(ops, ItemInput.CODEC.encodeStart(ops, sticks).getOrThrow()).getOrThrow();
        Assertions.assertEquals(sticks.fate(), back.fate());
        Assertions.assertEquals(sticks.predicate().items(), back.predicate().items());
        Assertions.assertTrue(ItemInput.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"predicate\":{\"count\":{\"min\":2}}}")).isError(), "a bounded count is refused");
        Assertions.assertEquals(Fate.consume(1), ItemInput.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"predicate\":{}}")).getOrThrow().fate(), "one consumed by default");
        Assertions.assertThrows(IllegalArgumentException.class, () -> ItemInput.of(ItemPredicate.Builder.item().withCount(MinMaxBounds.Ints.atLeast(2)), Fate.keep()), "and in code");
    }

    private static Fate roundTrip(Fate fate) {
        return Fate.CODEC.parse(JsonOps.INSTANCE, Fate.CODEC.encodeStart(JsonOps.INSTANCE, fate).getOrThrow()).getOrThrow();
    }
}
