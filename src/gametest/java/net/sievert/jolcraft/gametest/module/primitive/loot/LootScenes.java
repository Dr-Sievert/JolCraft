package net.sievert.jolcraft.gametest.module.primitive.loot;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.primitive.registration.Ids;

/**
 * The loot primitive against the real data pack: the declared condition and modifier are registered, rolling the
 * dungeon chest through the primitive applies the game-test mod's modifier, which its data pack ties to that table
 * through the game's table-id condition beside the declared one, and a table the data pack lacks rolls nothing, not
 * even the second modifier the data pack ties to that missing table.
 */
public final class LootScenes {

    private static final ResourceKey<LootTable> DUNGEON = ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/simple_dungeon"));

    @Scene(maxTicks = 20)
    public void theKindsAreRegisteredAndTheModifierChangesWhatATableRolls(TestScene scene) {
        ServerLevel level = scene.level();
        scene.check("the condition, the function and the modifier are registered", () -> scene.assertTrue(TestLoot.ALWAYS.isRegistered() && TestLoot.SEVEN.isRegistered() && TestLoot.ADD_STAR.isRegistered(), "registered"));
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, scene.origin().getCenter()).create(LootContextParamSets.CHEST);
        List<ItemStack> rolled = JolCraftGameTests.loot().roll(level, DUNGEON, params);
        scene.check("the dungeon chest rolled its own stacks", () -> scene.assertTrue(rolled.stream().anyMatch(stack -> !stack.is(Items.NETHER_STAR)), "rolled " + rolled));
        scene.check("with the star the modifier adds", () -> scene.assertTrue(rolled.stream().anyMatch(stack -> stack.is(Items.NETHER_STAR)), "rolled " + rolled));
        List<ItemStack> nothing = JolCraftGameTests.loot().roll(level, ResourceKey.create(Registries.LOOT_TABLE, Ids.of(JolCraftGameTests.MOD_ID, "missing")), params);
        scene.check("a table the data pack lacks rolls nothing", () -> scene.assertTrue(nothing.isEmpty(), "missing table rolled " + nothing));
        scene.succeed();
    }
}
