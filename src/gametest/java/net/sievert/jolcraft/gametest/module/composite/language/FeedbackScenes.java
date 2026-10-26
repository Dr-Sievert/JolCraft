package net.sievert.jolcraft.gametest.module.composite.language;

import java.util.List;

import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.gametest.module.primitive.sound.TestSounds;
import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestPlayer;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.primitive.language.LanguageKey;
import net.sievert.jolcraft.util.text.Dictionary;

/** The feedback composite against a real player: a told line arrives in chat with its key and arguments, a shown line over the hotbar, and the sound for that player. Isolated: it adds a player. */
public final class FeedbackScenes {

    private static final LanguageKey GREETING = LanguageKey.of(JolCraftGameTests.MOD_ID, Dictionary.TITLE);

    @Scene(maxTicks = 40, isolated = true)
    public void toldAndShownLinesReachThePlayerWithTheirKeyAndSound(TestScene scene) {
        TestPlayer player = scene.addPlayer("Listener");
        TestPlayer bystander = scene.addPlayer("Bystander");
        player.clearReceived();
        bystander.clearReceived();
        JolCraftGameTests.feedback().tell(player.player(), GREETING, 3, "x");
        scene.check("the told line is in chat, not over the hotbar, with its key and arguments", () -> scene.assertTrue(player.received(ClientboundSystemChatPacket.class).stream().anyMatch(packet ->
                !packet.overlay() && packet.content().getContents() instanceof TranslatableContents contents && contents.getKey().equals(GREETING.key())
                        && contents.getArgs().length == 2 && contents.getArgs()[0].equals(3) && contents.getArgs()[1].equals("x")), "chat packets " + player.received(ClientboundSystemChatPacket.class)));
        scene.check("a plain line brings no sound", () -> scene.assertTrue(player.received(ClientboundSoundPacket.class).isEmpty(), "sound packets"));
        player.clearReceived();
        JolCraftGameTests.feedback().tell(player.player(), TestSounds.PING, GREETING);
        scene.check("the told line with a sound is itself in chat", () -> scene.assertTrue(player.received(ClientboundSystemChatPacket.class).stream().anyMatch(packet ->
                !packet.overlay() && packet.content().getContents() instanceof TranslatableContents contents && contents.getKey().equals(GREETING.key())), "chat packets " + player.received(ClientboundSystemChatPacket.class)));
        scene.check("a told line with a sound brings exactly one, for the player, where they stand", () -> scene.assertTrue(
                player.received(ClientboundSoundPacket.class).size() == 1 && player.received(ClientboundSoundPacket.class).getFirst().getSound().value().location().equals(TestSounds.PING.id())
                        && near(player.received(ClientboundSoundPacket.class).getFirst(), player.player())
                        && player.received(ClientboundSoundPacket.class).getFirst().getSource() == net.minecraft.sounds.SoundSource.PLAYERS
                        && player.received(ClientboundSoundPacket.class).getFirst().getVolume() == 1.0F && player.received(ClientboundSoundPacket.class).getFirst().getPitch() == 1.0F, "sound packets"));
        player.clearReceived();
        JolCraftGameTests.feedback().show(player.player(), TestSounds.PING, GREETING, 5, "y");
        // The game carries a hotbar line as a system chat packet flagged as overlay.
        List<ClientboundSystemChatPacket> bar = player.received(ClientboundSystemChatPacket.class).stream().filter(ClientboundSystemChatPacket::overlay).toList();
        scene.check("the shown line went over the hotbar", () -> scene.assertTrue(bar.size() == 1
                && bar.getFirst().content().getContents() instanceof TranslatableContents contents && contents.getKey().equals(GREETING.key())
                && contents.getArgs().length == 2 && contents.getArgs()[0].equals(5) && contents.getArgs()[1].equals("y"), "overlay packets " + bar));
        scene.check("with the sound for the player, in the players' channel at full volume", () -> scene.assertTrue(player.received(ClientboundSoundPacket.class).size() == 1 && player.received(ClientboundSoundPacket.class).stream()
                .anyMatch(packet -> packet.getSound().value().location().equals(TestSounds.PING.id()) && packet.getSource() == net.minecraft.sounds.SoundSource.PLAYERS && packet.getVolume() == 1.0F && packet.getPitch() == 1.0F), "sound packets"));
        scene.check("the bystander heard and read nothing", () -> scene.assertTrue(
                bystander.received(ClientboundSoundPacket.class).isEmpty() && bystander.received(ClientboundSystemChatPacket.class).isEmpty(), "bystander"));
        scene.succeed();
    }

    /** The packet stores a position in eighths of a block, so a sound sent where the player stands lands within a block of them. */
    private static boolean near(ClientboundSoundPacket packet, net.minecraft.server.level.ServerPlayer player) {
        return Math.abs(packet.getX() - player.getX()) < 1 && Math.abs(packet.getY() - player.getY()) < 1 && Math.abs(packet.getZ() - player.getZ()) < 1;
    }
}
