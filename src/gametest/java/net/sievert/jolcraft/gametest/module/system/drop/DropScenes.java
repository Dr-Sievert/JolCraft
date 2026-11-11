package net.sievert.jolcraft.gametest.module.system.drop;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
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
import net.sievert.jolcraft.module.system.drop.DropKeys;

/**
 * The replace modifier against the real data pack, on fixture tables of the scene pack that no other scene rolls. The
 * pack replaces {@code drop/target} by {@code drop/replacement} (100 sticks) at a priority above the default, adds
 * {@code drop/addition} (a diamond) to {@code drop/target} through NeoForge's {@code add_table} at the default, adds the
 * dev mod's star to {@code drop/replacement}, and replaces {@code drop/kept} by a table it lacks. A replace that rolled
 * its table with the modifiers under the target's queried id would meet itself again and overflow the stack, so such a
 * scene fails as a crash; one that rolled them under a fresh context would give the star.
 */
public final class DropScenes {

    private static final ResourceKey<LootTable> TARGET = table("drop/target");
    private static final ResourceKey<LootTable> REPLACEMENT = table("drop/replacement");
    private static final ResourceKey<LootTable> KEPT = table("drop/kept");

    private static ResourceKey<LootTable> table(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, Ids.path(JolCraftGameTests.MOD_ID, path));
    }

    private static List<ItemStack> roll(TestScene scene, ResourceKey<LootTable> table) {
        ServerLevel level = scene.level();
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, scene.origin().getCenter()).create(LootContextParamSets.CHEST);
        return JolCraftGameTests.loot().roll(level, table, params);
    }

    private static List<Integer> counts(List<ItemStack> rolled, Item item) {
        return rolled.stream().filter(stack -> stack.is(item)).map(ItemStack::getCount).sorted().toList();
    }

    @Scene(maxTicks = 20)
    public void aReplaceTakesThePlaceOfTheTablesOwnRoll(TestScene scene) {
        scene.check("the kind is registered", () -> scene.assertTrue(DropKeys.REPLACE_TABLE.isRegistered(), "registered"));
        List<ItemStack> rolled = roll(scene, TARGET);
        scene.check("the table's own roll is cleared", () -> scene.assertTrue(counts(rolled, Items.APPLE).isEmpty(), "rolled " + rolled));
        scene.check("the replacement's 100 sticks come as 36 and 64, through the splitter", () -> scene.assertTrue(counts(rolled, Items.STICK).equals(List.of(36, 64)), "rolled " + rolled));
        scene.check("the add at the default runs after the replace and stays", () -> scene.assertTrue(counts(rolled, Items.DIAMOND).equals(List.of(1)), "rolled " + rolled));
        scene.check("the replacement is rolled raw, without its own modifier's star", () -> scene.assertTrue(counts(rolled, Items.NETHER_STAR).isEmpty(), "rolled " + rolled));
        List<ItemStack> alone = roll(scene, REPLACEMENT);
        scene.check("rolled by itself the replacement gets its star", () -> scene.assertTrue(counts(alone, Items.NETHER_STAR).equals(List.of(1)), "rolled " + alone));
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void aReplaceWhoseTableIsMissingLeavesTheDropAlone(TestScene scene) {
        List<ItemStack> rolled = roll(scene, KEPT);
        scene.check("the table's own apple is all there is", () -> scene.assertTrue(rolled.size() == 1 && counts(rolled, Items.APPLE).equals(List.of(1)), "rolled " + rolled));
        scene.succeed();
    }
}
