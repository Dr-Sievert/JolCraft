package net.sievert.jolcraft.module.primitive.loot;

import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

/** The shape as a value and as the game's check; building a context, and its refusals, need a level and are a scene in the game-test mod. */
class ShapeTest {

    private final Shape atAPlace = Shape.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "at_a_place"), LootContextParams.ORIGIN).allowing(LootContextParams.THIS_ENTITY);
    private final Shape placeOnly = Shape.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "place_only"), LootContextParams.ORIGIN);

    @Test
    void aShapeNamesWhatItRequiresAndAllows() {
        Assertions.assertEquals(Set.of(LootContextParams.ORIGIN), atAPlace.required());
        Assertions.assertEquals(Set.of(LootContextParams.ORIGIN, LootContextParams.THIS_ENTITY), atAPlace.allowed());
        Assertions.assertEquals("jolcraft:at_a_place", atAPlace.toString());
        Assertions.assertThrows(IllegalArgumentException.class, () -> atAPlace.allowing(LootContextParams.ORIGIN), "a required key is not allowed as optional");
        Shape wider = atAPlace.allowing(LootContextParams.TOOL);
        Assertions.assertEquals(Set.of(LootContextParams.ORIGIN, LootContextParams.THIS_ENTITY, LootContextParams.TOOL), wider.allowed(), "a second allowing keeps the first");
        Assertions.assertEquals(Set.of(LootContextParams.ORIGIN), wider.required());
    }

    @Test
    void aContextUserIsCheckedAgainstTheShapeOnceOrInACodec() {
        LootItemCondition aboutThisEntity = LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity()).build();
        Assertions.assertTrue(atAPlace.check(aboutThisEntity).isSuccess(), "the shape allows the entity");
        DataResult<LootItemCondition> refused = placeOnly.check(aboutThisEntity);
        Assertions.assertTrue(refused.isError(), "the shape has no entity");
        Assertions.assertTrue(refused.error().orElseThrow().message().contains("this_entity"), "the key is named: " + refused.error().orElseThrow().message());
        Codec<LootItemCondition> checked = LootItemCondition.DIRECT_CODEC.validate(placeOnly.validator());
        Assertions.assertTrue(checked.encodeStart(JsonOps.INSTANCE, aboutThisEntity).isError(), "a codec refuses it too");
        Assertions.assertTrue(LootItemCondition.DIRECT_CODEC.validate(atAPlace.validator()).encodeStart(JsonOps.INSTANCE, aboutThisEntity).isSuccess());
    }
}
