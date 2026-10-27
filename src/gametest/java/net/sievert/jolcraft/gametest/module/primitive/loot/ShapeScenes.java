package net.sievert.jolcraft.gametest.module.primitive.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestScene;

/**
 * A context built under a shape on the real level: it is the level's, carries what was given and nothing else,
 * rolls with the level's random unless the caller brings one, rolls a table of the data pack, and is refused by the
 * shape's name when a required key is missing, while a key beyond the shape is carried.
 */
public final class ShapeScenes {

    private static final ResourceKey<LootTable> DUNGEON = ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/simple_dungeon"));

    @Scene(maxTicks = 20)
    public void aContextIsBuiltUnderItsShapeOnTheLevel(TestScene scene) {
        ServerLevel level = scene.level();
        Vec3 here = scene.origin().getCenter();
        LootContext context = JolCraftGameTests.loot().context(level, TestShapes.AT_A_PLACE, params -> params.withParameter(LootContextParams.ORIGIN, here));
        scene.check("the context is the level's and carries what was given, nothing else", () -> scene.assertTrue(
                context.getLevel() == level && here.equals(context.getParameter(LootContextParams.ORIGIN)) && !context.hasParameter(LootContextParams.THIS_ENTITY), "context"));
        scene.check("and rolls with the level's random", () -> scene.assertTrue(context.getRandom() == level.getRandom(), "random"));

        ArmorStand stand = scene.helper().spawn(EntityType.ARMOR_STAND, new BlockPos(0, 1, 0));
        RandomSource own = RandomSource.create(7);
        LootContext repeatable = JolCraftGameTests.loot().context(level, TestShapes.AT_A_PLACE, own,
                params -> params.withParameter(LootContextParams.ORIGIN, here).withOptionalParameter(LootContextParams.THIS_ENTITY, stand));
        scene.check("a random of the caller's is used, and an allowed key is carried", () -> scene.assertTrue(
                repeatable.getRandom() == own && repeatable.getParameter(LootContextParams.THIS_ENTITY) == stand, "own random and entity"));

        List<ItemStack> rolled = new ArrayList<>();
        level.getServer().reloadableRegistries().getLootTable(DUNGEON).getRandomItems(repeatable, rolled::add);
        scene.check("a table of the data pack rolls its own stacks against it", () -> scene.assertTrue(rolled.stream().anyMatch(stack -> !stack.is(Items.NETHER_STAR)), "rolled " + rolled));
        scene.check("a context missing a required key is refused by the shape's name", () -> scene.assertTrue(
                refusal(() -> JolCraftGameTests.loot().context(level, TestShapes.AT_A_PLACE, _ -> { })).map(message -> message.contains("at_a_place") && message.contains("origin")).orElse(false), "a refusal naming the shape and the key"));
        LootContext wider = JolCraftGameTests.loot().context(level, TestShapes.AT_A_PLACE, params -> params.withParameter(LootContextParams.ORIGIN, here).withParameter(LootContextParams.EXPLOSION_RADIUS, 1.0F));
        scene.check("while a key beyond the shape is carried, since NeoForge lets a context carry more than its set names", () -> scene.assertTrue(
                wider.hasParameter(LootContextParams.EXPLOSION_RADIUS), "the extra key"));
        scene.succeed();
    }

    private static Optional<String> refusal(Runnable call) {
        try {
            call.run();
            return Optional.empty();
        } catch (IllegalArgumentException refused) {
            return Optional.ofNullable(refused.getMessage());
        }
    }
}
