package net.sievert.jolcraft.module.composite.loot;

import java.util.List;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.neoforged.bus.api.BusBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.module.primitive.log.ModLog;
import net.sievert.jolcraft.module.primitive.loot.ModLoot;
import net.sievert.jolcraft.module.primitive.loot.Shape;
import net.sievert.jolcraft.module.primitive.registration.ModRegistration;
import net.sievert.jolcraft.util.text.Dictionary;

/** The handles, the guards, the four kinds' codec round trips and the item kind's check against a shape; rolling, the hook check and the choice and wire forms need a level and registries and are scenes in the game-test mod. */
class ResultTest {

    private final YieldKinds yieldKinds = YieldKinds.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, Dictionary.YIELD));
    private final Identifier hook = Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "gilding");

    private Result result(ModRegistration registration) {
        return new Result(new ModLoot(), registration, new ModLog(LogUtils.getLogger()));
    }

    @Test
    void theYieldKindsNameTwoRegistriesUnderTheirId() {
        Assertions.assertEquals("jolcraft:yield", yieldKinds.kinds().identifier().toString());
        Assertions.assertEquals("jolcraft:yield_hook", yieldKinds.hooks().identifier().toString());
        Assertions.assertEquals("jolcraft:yield", yieldKinds.toString());
    }

    @Test
    void declaringGoesThroughRegistrationAndTheGuardsHold() {
        ModRegistration registration = new ModRegistration();
        Result result = result(registration);
        Assertions.assertThrows(IllegalStateException.class, () -> result.register(BusBuilder.builder().build()), "nothing declared");
        result.declare(yieldKinds);
        Assertions.assertEquals(2, registration.declared(), "the kinds and the hooks registries");
        Assertions.assertEquals(4, result.declared(), "the composite's own kinds");
        MapCodec<EntityYield> own = MapCodec.unit(() -> EntityYield.of(EntityType.ARMOR_STAND, 1));
        result.declare(YieldKind.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "tally"), own));
        ResultHook gilding = new ResultHook() { };
        result.declare(hook, gilding);
        Assertions.assertEquals(6, result.declared());
        Assertions.assertThrows(IllegalStateException.class, () -> result.declare(YieldKinds.of(Identifier.fromNamespaceAndPath("other", Dictionary.YIELD))), "one yield kinds per root");
        Assertions.assertThrows(IllegalStateException.class, () -> result.declare(YieldKind.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, Dictionary.ITEM), own)), "the item kind is the composite's");
        Assertions.assertThrows(IllegalStateException.class, () -> result.declare(YieldKind.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "shared"), ItemYield.CODEC)), "a codec serves one kind");
        Assertions.assertThrows(IllegalStateException.class, () -> result.declare(hook, new ResultHook() { }), "a hook id twice");
        Assertions.assertThrows(IllegalStateException.class, () -> result.declare(hook.withPath("twin"), gilding), "a hook instance under two ids");
        result.register(BusBuilder.builder().build());
        Assertions.assertThrows(IllegalStateException.class, () -> result.register(BusBuilder.builder().build()), "registered twice");
        Assertions.assertThrows(IllegalStateException.class, () -> result.declare(hook.withPath("late"), new ResultHook() { }), "a hook declared after registering");
        Assertions.assertThrows(IllegalStateException.class, () -> result.declare(YieldKind.of(hook.withPath("late"), MapCodec.unit(() -> EntityYield.of(EntityType.ARMOR_STAND, 2)))), "a kind declared after registering");
        Assertions.assertThrows(IllegalStateException.class, () -> result.declare(YieldKinds.of(Identifier.fromNamespaceAndPath("late", Dictionary.YIELD))), "yield kinds declared after registering");
        Assertions.assertEquals(Dictionary.RESULT, result.id());
    }

    @Test
    void eachKindRoundTripsThroughItsCodec() {
        ItemYield item = ItemYield.of(LootPool.lootPool().add(LootItem.lootTableItem(Items.STICK)), hook);
        Assertions.assertEquals(List.of(hook), roundTrip(item).hooks());
        EntityYield entity = EntityYield.of(EntityType.ARMOR_STAND, 2);
        Assertions.assertEquals(entity, roundTrip(entity));
        SoundYield sound = SoundYield.of(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ANVIL_USE), SoundSource.BLOCKS, 0.5F, 1.5F);
        SoundYield soundBack = roundTrip(sound);
        Assertions.assertEquals(SoundSource.BLOCKS, soundBack.source());
        Assertions.assertEquals(sound.volume(), soundBack.volume());
        Assertions.assertEquals(sound.pitch(), soundBack.pitch());
        Assertions.assertEquals(sound.sound().value(), soundBack.sound().value(), "the holder comes back direct without a registry context; the event is the same");
        Assertions.assertEquals("block", SoundYield.SOURCE_CODEC.encodeStart(JsonOps.INSTANCE, SoundSource.BLOCKS).getOrThrow().getAsString(), "the game's own name for the source");
        Assertions.assertTrue(SoundYield.SOURCE_CODEC.parse(JsonOps.INSTANCE, JsonOps.INSTANCE.createString("nowhere")).isError(), "an unknown source is an error");
        EffectYield effect = EffectYield.of(new MobEffectInstance(MobEffects.SPEED, 100));
        Assertions.assertEquals(effect, roundTrip(effect));
    }

    @Test
    void aYieldIsCheckedAgainstTheShapeItWillBeRolledWith() {
        Shape placeOnly = Shape.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "place_only"), LootContextParams.ORIGIN);
        ItemYield aboutThisEntity = ItemYield.of(LootPool.lootPool().add(LootItem.lootTableItem(Items.STICK))
                .when(LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity())));
        Assertions.assertTrue(placeOnly.check(aboutThisEntity).isError(), "the pool's condition reads an entity the shape lacks");
        Assertions.assertTrue(placeOnly.allowing(LootContextParams.THIS_ENTITY).check(aboutThisEntity).isSuccess());
        Assertions.assertTrue(placeOnly.listValidator().apply(List.of(aboutThisEntity)).isError(), "and so over a list");
    }

    @Test
    void aDeliverySortsWhatWasYielded() {
        Yielded.Stack stack = new Yielded.Stack(ItemStack.EMPTY); // item components are not bound in this JVM, so no stack but the empty one can be made
        Yielded.Entities entities = new Yielded.Entities(EntityType.ARMOR_STAND, 2);
        Delivery delivery = new Delivery(List.of(stack, entities), false);
        Assertions.assertEquals(List.of(stack.stack()), delivery.stacks());
        Assertions.assertEquals(List.of(entities), delivery.entities());
        Assertions.assertTrue(delivery.sounds().isEmpty() && delivery.effects().isEmpty() && !delivery.refused());
        Assertions.assertTrue(Delivery.REFUSED.refused() && Delivery.REFUSED.isEmpty());
    }

    @SuppressWarnings("unchecked")
    private static <Y extends Yield> Y roundTrip(Y yield) {
        JsonElement json = ((com.mojang.serialization.MapCodec<Y>) yield.codec()).codec().encodeStart(JsonOps.INSTANCE, yield).getOrThrow();
        return ((com.mojang.serialization.MapCodec<Y>) yield.codec()).codec().parse(JsonOps.INSTANCE, json).getOrThrow();
    }
}
