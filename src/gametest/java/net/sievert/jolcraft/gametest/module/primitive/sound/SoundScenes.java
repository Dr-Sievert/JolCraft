package net.sievert.jolcraft.gametest.module.primitive.sound;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestPlayer;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.primitive.sound.SoundKey;

/**
 * The sound primitive against real players: what a key plays in the world, at an entity and for one player arrives
 * as the game's sound packet, with the key's event, the source, volume, pitch and place asked for; a ranged key
 * reaches a far player with the volume that makes a client hear it; a silenced entity sends nothing. Isolated: they add players.
 */
public final class SoundScenes {

    @Scene(maxTicks = 40, isolated = true)
    public void aSoundPlayedInTheWorldReachesThePlayerInRange(TestScene scene) {
        TestPlayer player = scene.addPlayer("Listener");
        player.clearReceived();
        Vec3 at = scene.origin().getCenter();
        TestSounds.PING.play(scene.level(), scene.origin(), SoundSource.BLOCKS, 0.5F, 1.25F);
        List<ClientboundSoundPacket> heard = heard(player, TestSounds.PING);
        scene.check("the ping arrived once", () -> scene.assertEquals(1, heard.size(), "ping packets"));
        ClientboundSoundPacket packet = heard.getFirst();
        scene.check("with the source, volume and pitch asked for", () -> scene.assertTrue(
                packet.getSource() == SoundSource.BLOCKS && packet.getVolume() == 0.5F && packet.getPitch() == 1.25F, "source, volume and pitch"));
        scene.check("at the block's centre", () -> scene.assertTrue(at(packet, at), "position " + packet.getX() + "," + packet.getY() + "," + packet.getZ() + " for " + at));
        scene.check("the registered events carry the keys' shapes", () -> scene.assertTrue(
                TestSounds.PING.event().location().equals(TestSounds.PING.id()) && TestSounds.HORN.event().fixedRange().equals(Optional.of(64.0F)), "events"));
        scene.succeed();
    }

    @Scene(maxTicks = 40, isolated = true)
    public void aRangedSoundReachesAFarPlayerLoudEnoughToBeHeardThere(TestScene scene) {
        TestPlayer far = scene.addPlayer("Far");
        Vec3 origin = scene.origin().getCenter();
        far.player().snapTo(origin.x + 40, origin.y, origin.z);
        far.clearReceived();
        TestSounds.PING.play(scene.level(), origin, SoundSource.BLOCKS, 1.0F, 1.0F);
        scene.check("a sound heard by volume does not reach forty blocks", () -> scene.assertTrue(heard(far, TestSounds.PING).isEmpty(), "ping packets at forty blocks"));
        TestSounds.HORN.play(scene.level(), origin, SoundSource.BLOCKS, 1.0F, 1.0F);
        List<ClientboundSoundPacket> horn = heard(far, TestSounds.HORN);
        scene.check("the horn does", () -> scene.assertEquals(1, horn.size(), "horn packets at forty blocks"));
        scene.check("sent loud enough for the client to hear sixty-four blocks", () -> scene.assertEquals(4.0F, horn.getFirst().getVolume(), "volume sent"));
        scene.succeed();
    }

    @Scene(maxTicks = 40, isolated = true)
    public void aSoundPlayedAtAnEntitySpeaksWithItsVoiceAndASilencedOneSaysNothing(TestScene scene) {
        TestPlayer player = scene.addPlayer("Listener");
        ArmorStand stand = scene.helper().spawn(EntityType.ARMOR_STAND, new BlockPos(1, 1, 1));
        player.clearReceived();
        TestSounds.PING.play(stand, 0.75F, 0.8F);
        List<ClientboundSoundPacket> heard = heard(player, TestSounds.PING);
        scene.check("the ping arrived once", () -> scene.assertEquals(1, heard.size(), "ping packets"));
        scene.check("from the entity's place with its voice, volume and pitch", () -> scene.assertTrue(
                heard.getFirst().getSource() == stand.getSoundSource() && heard.getFirst().getVolume() == 0.75F && heard.getFirst().getPitch() == 0.8F
                        && at(heard.getFirst(), stand.position()), "source, volume, pitch and position"));
        stand.setSilent(true);
        player.clearReceived();
        TestSounds.PING.play(stand, 1.0F, 1.0F);
        scene.check("a silenced entity sends nothing", () -> scene.assertTrue(heard(player, TestSounds.PING).isEmpty(), "ping packets from a silenced stand"));
        scene.succeed();
    }

    @Scene(maxTicks = 40, isolated = true)
    public void aSoundPlayedAtAPlayerIsHeardByThatPlayerToo(TestScene scene) {
        TestPlayer speaker = scene.addPlayer("Speaker");
        TestPlayer other = scene.addPlayer("Other");
        speaker.clearReceived();
        other.clearReceived();
        ServerPlayer player = speaker.player();
        TestSounds.PING.play(player, 1.0F, 1.0F);
        scene.check("the speaker heard themself", () -> scene.assertTrue(
                heard(speaker, TestSounds.PING).size() == 1 && heard(speaker, TestSounds.PING).getFirst().getSource() == player.getSoundSource(), "speaker's packets"));
        scene.check("and so did the other", () -> scene.assertEquals(1, heard(other, TestSounds.PING).size(), "other's packets"));
        scene.succeed();
    }

    @Scene(maxTicks = 40, isolated = true)
    public void aSoundPlayedForOnePlayerReachesThatPlayerAlone(TestScene scene) {
        TestPlayer listener = scene.addPlayer("Listener");
        TestPlayer bystander = scene.addPlayer("Bystander");
        listener.clearReceived();
        bystander.clearReceived();
        Vec3 at = listener.player().position().add(3, 0, 5);
        TestSounds.PING.playFor(listener.player(), at, SoundSource.UI, 0.5F, 1.25F);
        List<ClientboundSoundPacket> heard = heard(listener, TestSounds.PING);
        scene.check("the listener heard it once", () -> scene.assertEquals(1, heard.size(), "listener's packets"));
        scene.check("with the source, volume, pitch and place asked for", () -> scene.assertTrue(
                heard.getFirst().getSource() == SoundSource.UI && heard.getFirst().getVolume() == 0.5F && heard.getFirst().getPitch() == 1.25F
                        && at(heard.getFirst(), at), "source, volume, pitch and position"));
        scene.check("the bystander did not", () -> scene.assertTrue(heard(bystander, TestSounds.PING).isEmpty(), "bystander's packets"));
        scene.succeed();
    }

    /** Whether the packet carries the point, as the packet keeps it: eighths of a block, read back as floats. */
    private static boolean at(ClientboundSoundPacket packet, Vec3 point) {
        return packet.getX() == (int) (point.x * 8.0) / 8.0F && packet.getY() == (int) (point.y * 8.0) / 8.0F && packet.getZ() == (int) (point.z * 8.0) / 8.0F;
    }

    private static List<ClientboundSoundPacket> heard(TestPlayer player, SoundKey key) {
        return player.received(ClientboundSoundPacket.class).stream()
                .filter(packet -> packet.getSound().value().location().equals(key.id()))
                .toList();
    }
}
