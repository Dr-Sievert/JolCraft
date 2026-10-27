package net.sievert.jolcraft.gametest.module.primitive.recipe;

import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.payload.RecipeContentPayload;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestPlayer;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.primitive.recipe.Fate;
import net.sievert.jolcraft.module.primitive.recipe.ItemInput;

/**
 * Item inputs on the real server: an input matches a stack that covers its fate, consuming shrinks, damaging wears,
 * keeping leaves, a worn-out stack empties, a creative holder pays nothing; and the declared recipe kinds are requested
 * whenever the game syncs a player and reach one who joins.
 */
public final class InputScenes {

    @Scene(maxTicks = 20, isolated = true)
    public void anInputMatchesAStackThatCoversItsFateAndTheFateIsApplied(TestScene scene) {
        ServerLevel level = scene.level();
        ItemInput twoSticks = ItemInput.of(ItemPredicate.Builder.item().of(BuiltInRegistries.ITEM, Items.STICK), Fate.consume(2));
        scene.check("a stack of three sticks is the input, one stick is not, a stone is not", () -> scene.assertTrue(
                twoSticks.matches(new ItemStack(Items.STICK, 3)) && !twoSticks.matches(new ItemStack(Items.STICK)) && !twoSticks.matches(new ItemStack(Items.STONE, 3)), "matching"));
        scene.check("a consume fate is covered by the count, a damage fate by being damageable, a keep by anything", () -> scene.assertTrue(
                Fate.consume(2, 5).covers(new ItemStack(Items.STICK, 5)) && !Fate.consume(2, 5).covers(new ItemStack(Items.STICK, 4))
                        && Fate.damage(2, 5).covers(new ItemStack(Items.IRON_PICKAXE)) && !Fate.damage(2, 5).covers(new ItemStack(Items.STICK, 64))
                        && Fate.keep().covers(new ItemStack(Items.STICK)), "covering"));
        ItemStack sticks = new ItemStack(Items.STICK, 3);
        int consumed = twoSticks.fate().apply(sticks, level, null);
        scene.check("consuming shrinks the stack by the amount", () -> scene.assertTrue(consumed == 2 && sticks.getCount() == 1, "consumed " + consumed + ", left " + sticks.getCount()));
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        int worn = Fate.damage(5).apply(pickaxe, level, null);
        scene.check("damaging wears the stack by the amount", () -> scene.assertTrue(worn == 5 && pickaxe.getDamageValue() == 5, "worn " + worn + ", damage " + pickaxe.getDamageValue()));
        ItemStack nearlyWorn = new ItemStack(Items.IRON_PICKAXE);
        nearlyWorn.setDamageValue(nearlyWorn.getMaxDamage() - 3);
        Fate.damage(5).apply(nearlyWorn, level, null);
        scene.check("and goes through the game's own wear, so a worn-out stack empties", () -> scene.assertTrue(nearlyWorn.isEmpty(), "worn out"));
        ItemStack kept = new ItemStack(Items.STICK, 3);
        scene.check("keeping leaves it", () -> scene.assertTrue(Fate.keep().apply(kept, level, null) == 0 && kept.getCount() == 3, "kept"));
        ItemStack ranged = new ItemStack(Items.STICK, 9);
        int rolled = Fate.consume(2, 4).apply(ranged, level, null);
        scene.check("a ranged amount rolls within its bounds and is what was taken", () -> scene.assertTrue(rolled >= 2 && rolled <= 4 && ranged.getCount() == 9 - rolled, "rolled " + rolled));
        TestPlayer creative = scene.addPlayer("Creative");
        ItemStack theirs = new ItemStack(Items.STICK, 3);
        scene.check("a creative holder pays nothing", () -> scene.assertTrue(creative.player().getAbilities().instabuild
                && twoSticks.fate().apply(theirs, level, creative.player()) == 0 && theirs.getCount() == 3, "creative"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theDeclaredRecipeKindsAreRequestedWhenTheGameSyncsAPlayerAndSentToOneWhoJoins(TestScene scene) {
        OnDatapackSyncEvent sync = new OnDatapackSyncEvent(scene.level().getServer().getPlayerList(), null);
        NeoForge.EVENT_BUS.post(sync);
        scene.check("the primitive asks for every declared kind", () -> scene.assertTrue(
                sync.getRecipeTypesToSend().contains(TestRecipes.DOUBLING.type()) && sync.getRecipeTypesToSend().contains(TestRecipes.YIELDING.type()), "requested " + sync.getRecipeTypesToSend()));
        // The game sends what was requested only over a NeoForge connection, which a mock player's is, by its join cookie.
        TestPlayer joiner = scene.addPlayer("Joiner");
        scene.check("and a player who joins receives them", () -> scene.assertTrue(
                joiner.received(ClientboundCustomPayloadPacket.class).stream().anyMatch(packet -> packet.payload().type() == RecipeContentPayload.TYPE),
                "payloads " + joiner.received(ClientboundCustomPayloadPacket.class).stream().map(packet -> packet.payload().type().id()).toList()));
        scene.succeed();
    }
}
