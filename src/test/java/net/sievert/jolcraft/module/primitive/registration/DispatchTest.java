package net.sievert.jolcraft.module.primitive.registration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

/** The dispatch codec over a registry the game has, and what it does over one it has not. */
class DispatchTest {

    private final Codec<LootItemCondition> conditions = Dispatch.codec(Registries.LOOT_CONDITION_TYPE, "condition", LootItemCondition::codec, codec -> codec);

    @Test
    void aValueIsWrittenWithItsKindNamedAndReadBackByIt() {
        LootItemCondition chance = new LootItemRandomChanceCondition(ConstantValue.exactly(0.5F));
        JsonElement json = conditions.encodeStart(JsonOps.INSTANCE, chance).getOrThrow();
        Assertions.assertEquals("minecraft:random_chance", json.getAsJsonObject().get("condition").getAsString());
        Assertions.assertEquals(chance, conditions.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void aKindTheRegistryLacksIsAnErrorNotAThrow() {
        JsonObject json = new JsonObject();
        json.addProperty("condition", "jolcraft:no_such_kind");
        Assertions.assertTrue(conditions.parse(JsonOps.INSTANCE, json).isError());
    }

    @Test
    void theRegistryIsResolvedWhenTheCodecIsFirstUsedNotWhenItIsBuilt() {
        ResourceKey<Registry<MapCodec<? extends LootItemCondition>>> nothing = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "nothing"));
        Codec<LootItemCondition> over = Assertions.assertDoesNotThrow(() -> Dispatch.codec(nothing, "condition", LootItemCondition::codec, codec -> codec));
        IllegalStateException refused = Assertions.assertThrows(IllegalStateException.class,
                () -> over.encodeStart(JsonOps.INSTANCE, new LootItemRandomChanceCondition(ConstantValue.exactly(1.0F))));
        Assertions.assertTrue(refused.getMessage().contains("jolcraft:nothing") && refused.getMessage().contains("Registration"), refused.getMessage());
    }
}
