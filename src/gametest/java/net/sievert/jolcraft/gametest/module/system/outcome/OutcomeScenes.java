package net.sievert.jolcraft.gametest.module.system.outcome;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestPlayer;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.composite.loot.Delivery;
import net.sievert.jolcraft.module.composite.loot.Yielded;
import net.sievert.jolcraft.module.system.outcome.Outcome;

/**
 * Delivering what results roll to a real player, from deliveries built by hand so no data is needed: the stacks given
 * together and the overflow of a full inventory tossed at the player, each entity of a count spawned where the player
 * stands, mob or not, an effect added in an instance of the player's own, a sound heard, and a refused delivery or a
 * value of another kind changing nothing.
 */
public final class OutcomeScenes {

    /** A kind of a host's own, which delivering leaves to the host. */
    private record Marker() implements Yielded {
    }

    private static Delivery of(Yielded... yielded) {
        return new Delivery(List.of(yielded), false);
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theStacksAreGivenTogetherAndTheOverflowOfAFullInventoryIsTossedAtThePlayer(TestScene scene) {
        Outcome outcome = JolCraftGameTests.outcome();
        ServerPlayer live = scene.addPlayer("Receiver").survival().player();
        net.minecraft.world.entity.player.Inventory items = live.getInventory();
        outcome.deliver(of(new Yielded.Stack(new ItemStack(Items.STONE, 10)), new Yielded.Stack(new ItemStack(Items.STONE, 5))), live);
        scene.check("the two stacks stack with each other in the first slot", () -> scene.assertTrue(items.getItem(0).is(Items.STONE) && items.getItem(0).getCount() == 15
                && items.getItem(1).isEmpty(), "slots " + items.getItem(0) + ", " + items.getItem(1)));
        for (int slot = 0; slot < 36; slot++) {
            items.setItem(slot, new ItemStack(Items.DIRT, 64));
        }
        outcome.deliver(of(new Yielded.Stack(new ItemStack(Items.STONE, 3))), live);
        List<ItemEntity> tossed = scene.helper().getEntities(EntityType.ITEM, new BlockPos(0, 1, 0), 2);
        scene.check("with no room it is tossed once, targeted at the player", () -> scene.assertTrue(tossed.size() == 1 && tossed.getFirst().getItem().is(Items.STONE)
                && tossed.getFirst().getItem().getCount() == 3 && live.getUUID().equals(tossed.getFirst().getTarget()), "tossed " + tossed));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void eachEntityOfACountIsSpawnedWhereThePlayerStandsAMobAndAnEntityAlike(TestScene scene) {
        Outcome outcome = JolCraftGameTests.outcome();
        ServerPlayer live = scene.addPlayer("Herder").survival().player();
        outcome.deliver(of(new Yielded.Entities(EntityType.CHICKEN, 2), new Yielded.Entities(EntityType.ARMOR_STAND, 1)), live);
        List<Chicken> chickens = scene.level().getEntities(EntityType.CHICKEN, live.getBoundingBox().inflate(2), _ -> true);
        List<ArmorStand> stands = scene.level().getEntities(EntityType.ARMOR_STAND, live.getBoundingBox().inflate(2), _ -> true);
        scene.check("two chickens and an armor stand stand at the player's own position", () -> scene.assertTrue(chickens.size() == 2 && stands.size() == 1
                && chickens.stream().allMatch(chicken -> chicken.position().equals(live.position())) && stands.getFirst().position().equals(live.position()),
                "chickens " + chickens + ", stands " + stands + ", player at " + live.position()));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void anEffectIsAddedInAnInstanceOfThePlayersOwnAndASoundIsHeard(TestScene scene) {
        Outcome outcome = JolCraftGameTests.outcome();
        TestPlayer player = scene.addPlayer("Listener").survival();
        ServerPlayer live = player.player();
        MobEffectInstance haste = new MobEffectInstance(MobEffects.HASTE, 200, 1);
        player.clearReceived();
        outcome.deliver(of(new Yielded.Effect(haste), new Yielded.Sound(SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 1.0F, 1.5F)), live);
        MobEffectInstance held = live.getEffect(MobEffects.HASTE);
        scene.check("the player has the effect, not the delivery's instance", () -> scene.assertTrue(held != null && held.getAmplifier() == 1 && held.getDuration() == 200
                && held != haste, "held " + held));
        List<ClientboundSoundPacket> sounds = player.received(ClientboundSoundPacket.class);
        scene.check("and hears the sound at its pitch", () -> scene.assertTrue(sounds.stream().anyMatch(packet -> packet.getSound().value() == SoundEvents.NOTE_BLOCK_BELL.value()
                && packet.getPitch() == 1.5F), "sounds " + sounds.size()));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aRefusedDeliveryAndAValueOfAnotherKindChangeNothing(TestScene scene) {
        Outcome outcome = JolCraftGameTests.outcome();
        TestPlayer player = scene.addPlayer("Refused").survival();
        ServerPlayer live = player.player();
        player.clearReceived();
        outcome.deliver(new Delivery(List.of(new Yielded.Stack(new ItemStack(Items.STONE)), new Yielded.Entities(EntityType.CHICKEN, 1),
                new Yielded.Effect(new MobEffectInstance(MobEffects.HASTE, 200)), new Yielded.Sound(SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 1.0F, 1.0F)), true), live);
        outcome.deliver(of(new Marker()), live);
        List<Chicken> chickens = scene.level().getEntities(EntityType.CHICKEN, live.getBoundingBox().inflate(2), _ -> true);
        List<ClientboundSoundPacket> sounds = player.received(ClientboundSoundPacket.class);
        scene.check("nothing is given, spawned, added or played", () -> scene.assertTrue(live.getInventory().isEmpty() && chickens.isEmpty() && live.getActiveEffects().isEmpty()
                && sounds.isEmpty(), "inventory empty " + live.getInventory().isEmpty() + ", chickens " + chickens + ", effects " + live.getActiveEffects() + ", sounds " + sounds.size()));
        scene.succeed();
    }
}
