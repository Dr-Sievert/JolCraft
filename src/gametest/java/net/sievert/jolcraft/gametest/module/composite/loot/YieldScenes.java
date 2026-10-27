package net.sievert.jolcraft.gametest.module.composite.loot;

import java.util.List;
import java.util.Optional;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.network.connection.ConnectionType;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.gametest.module.primitive.loot.TestShapes;
import net.sievert.jolcraft.gametest.module.primitive.recipe.TestRecipes;
import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.composite.loot.Delivery;
import net.sievert.jolcraft.module.composite.loot.EffectYield;
import net.sievert.jolcraft.module.composite.loot.EntityYield;
import net.sievert.jolcraft.module.composite.loot.ItemYield;
import net.sievert.jolcraft.module.composite.loot.SoundYield;
import net.sievert.jolcraft.module.composite.loot.Yield;

/**
 * The result composite on the real server: the four kinds and one of the game-test mod's own roll values against a
 * context without acting, hooks rewrite a value or refuse the whole, a weighted choice rolls the entry the random picks,
 * a recipe of the data pack carries yields decoded through the declared registries while one naming an unknown hook is
 * absent, a field typed as one kind checks its hooks too, a choice is read from data, and yields cross the wire whole.
 */
public final class YieldScenes {

    @Scene(maxTicks = 20)
    public void yieldsRollValuesAgainstAContextAndHooksRewriteOrRefuse(TestScene scene) {
        ServerLevel level = scene.level();
        LootContext context = JolCraftGameTests.loot().context(level, TestShapes.AT_A_PLACE, params -> params.withParameter(LootContextParams.ORIGIN, scene.origin().getCenter()));
        MobEffectInstance speed = new MobEffectInstance(MobEffects.SPEED, 100);
        List<Yield> yields = List.of(
                ItemYield.of(LootPool.lootPool().add(LootItem.lootTableItem(Items.STICK).apply(SetItemCountFunction.setCount(ConstantValue.exactly(3.0F)))), TestYields.GILDING),
                EntityYield.of(EntityType.ARMOR_STAND, 2),
                SoundYield.of(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ANVIL_USE), SoundSource.BLOCKS, 0.5F, 1.5F),
                EffectYield.of(speed),
                new TestYields.TallyYield(4, List.of()));
        Delivery delivery = JolCraftGameTests.result().apply(yields, context);
        scene.check("the item pool rolled, rewritten by its hook into gold nuggets of the same count", () -> scene.assertTrue(
                delivery.stacks().size() == 1 && delivery.stacks().getFirst().is(Items.GOLD_NUGGET) && delivery.stacks().getFirst().getCount() == 3, "stacks " + delivery.stacks()));
        scene.check("the entity kind yields a type and a count and spawns nothing", () -> scene.assertTrue(
                delivery.entities().size() == 1 && delivery.entities().getFirst().type() == EntityType.ARMOR_STAND && delivery.entities().getFirst().count() == 2
                        && level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class, scene.helper().getBounds()).isEmpty(), "entities"));
        scene.check("the sound kind yields the sound with its rolled volume and pitch", () -> scene.assertTrue(
                delivery.sounds().size() == 1 && delivery.sounds().getFirst().volume() == 0.5F && delivery.sounds().getFirst().pitch() == 1.5F
                        && delivery.sounds().getFirst().source() == SoundSource.BLOCKS && delivery.sounds().getFirst().sound().value() == SoundEvents.ANVIL_USE, "sounds"));
        scene.check("the effect kind yields a copy", () -> scene.assertTrue(
                delivery.effects().size() == 1 && delivery.effects().getFirst() != speed && delivery.effects().getFirst().getDuration() == 100, "effects"));
        scene.check("a kind of the mod's own yields its own value", () -> scene.assertEquals(List.of(new TestYields.Tally(4)), delivery.of(TestYields.Tally.class), "tallies"));
        scene.check("and nothing refused", () -> scene.assertFalse(delivery.refused(), "refused"));

        Delivery refused = JolCraftGameTests.result().apply(List.of(new TestYields.TallyYield(1, List.of()), new TestYields.TallyYield(2, List.of(TestYields.REFUSING))), context);
        scene.check("a refusing hook refuses the whole delivery, so nothing is rolled", () -> scene.assertTrue(refused.refused() && refused.isEmpty(), "refused " + refused));

        WeightedList<Yield> choice = WeightedList.of(List.of(new Weighted<>(new TestYields.TallyYield(1, List.of()), 1), new Weighted<>(new TestYields.TallyYield(2, List.of()), 1)));
        LootContext seeded = JolCraftGameTests.loot().context(level, TestShapes.AT_A_PLACE, RandomSource.create(7), params -> params.withParameter(LootContextParams.ORIGIN, scene.origin().getCenter()));
        Delivery one = JolCraftGameTests.result().apply(choice, seeded);
        int picked = ((TestYields.TallyYield) choice.getRandom(RandomSource.create(7)).orElseThrow()).number();
        scene.check("a weighted choice rolls the entry the context's random picks", () -> scene.assertEquals(List.of(new TestYields.Tally(picked)), one.of(TestYields.Tally.class), "one of " + one));
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void aRecipeCarryingYieldsIsFoundAndAppliedAndOneNamingAnUnknownHookIsAbsent(TestScene scene) {
        ServerLevel level = scene.level();
        List<RecipeHolder<TestRecipes.Yielding>> all = TestRecipes.YIELDING.all(level).toList();
        scene.check("the data pack's yielding recipe loaded and the one naming an unknown hook did not", () -> scene.assertTrue(
                all.size() == 1 && all.getFirst().id().identifier().getPath().equals("yielding"), "recipes " + all.stream().map(holder -> holder.id().identifier()).toList()));
        Optional<RecipeHolder<TestRecipes.Yielding>> found = TestRecipes.YIELDING.find(level, new SingleRecipeInput(new ItemStack(Items.STICK)));
        scene.check("it is found for a stick", () -> scene.assertTrue(found.isPresent(), "found"));
        Delivery delivery = JolCraftGameTests.result().apply(found.orElseThrow().value().yields(), level, TestShapes.AT_A_PLACE,
                params -> params.withParameter(LootContextParams.ORIGIN, scene.origin().getCenter()));
        scene.check("its yields decoded through the declared registries roll: three nuggets through the hook, a sound, a tally, two stands, an effect", () -> scene.assertTrue(
                delivery.stacks().size() == 1 && delivery.stacks().getFirst().is(Items.GOLD_NUGGET) && delivery.stacks().getFirst().getCount() == 3
                        && delivery.sounds().size() == 1 && delivery.sounds().getFirst().volume() == 0.5F && delivery.sounds().getFirst().pitch() == 1.5F
                        && delivery.of(TestYields.Tally.class).equals(List.of(new TestYields.Tally(4)))
                        && delivery.entities().size() == 1 && delivery.entities().getFirst().type() == EntityType.ARMOR_STAND && delivery.entities().getFirst().count() == 2
                        && delivery.effects().size() == 1 && delivery.effects().getFirst().getDuration() == 100, "delivery " + delivery));
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess()); // a sound's holder resolves only under registry-aware ops, as at load
        scene.check("a field typed as one kind checks its hooks too", () -> scene.assertTrue(
                TestYields.KINDS.single(SoundYield.CODEC).parse(ops, sound("jolcraft_gametest:no_such_hook")).isError()
                        && TestYields.KINDS.single(SoundYield.CODEC).parse(ops, sound("jolcraft_gametest:gilding")).isSuccess(), "single"));
        WeightedList<Yield> choice = TestYields.KINDS.choice().parse(ops,
                JsonParser.parseString("[{\"data\":{\"type\":\"jolcraft_gametest:tally\",\"number\":7},\"weight\":1}]")).getOrThrow();
        scene.check("a weighted choice read from data rolls its one entry", () -> scene.assertEquals(List.of(new TestYields.Tally(7)),
                JolCraftGameTests.result().apply(choice, JolCraftGameTests.loot().context(level, TestShapes.AT_A_PLACE, params -> params.withParameter(LootContextParams.ORIGIN, scene.origin().getCenter()))).of(TestYields.Tally.class), "choice"));
        StreamCodec<RegistryFriendlyByteBuf, List<Yield>> wire = TestYields.KINDS.streamCodec().apply(ByteBufCodecs.list());
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.OTHER);
        wire.encode(buffer, found.orElseThrow().value().yields());
        List<Yield> back = wire.decode(buffer);
        scene.check("the yields cross the wire whole", () -> scene.assertTrue(back.size() == 5 && back.get(2) instanceof TestYields.TallyYield tally && tally.number() == 4
                && back.get(1) instanceof SoundYield sound && sound.source() == SoundSource.BLOCKS, "wire " + back));
        scene.succeed();
    }

    private static com.google.gson.JsonElement sound(String hook) {
        return JsonParser.parseString("{\"sound\":\"minecraft:block.anvil.use\",\"hooks\":[\"" + hook + "\"]}");
    }
}
