package net.sievert.jolcraft.gametest.module.primitive.network;

import java.util.List;

import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestPlayer;
import net.sievert.jolcraft.gametest.scene.TestScene;

/**
 * The network primitive against a real player: a payload the server sends reaches the player's connection, and a
 * payload the player sends reaches the module's handler on the server thread with that player, whichever thread
 * it arrived on. Isolated: they add players.
 */
public final class NetworkScenes {

    @Scene(maxTicks = 40, isolated = true)
    public void aPayloadSentToAPlayerReachesItsConnection(TestScene scene) {
        TestPlayer player = scene.addPlayer("Receiver");
        player.clearReceived();
        PacketDistributor.sendToPlayer(player.player(), new TestPayloads.Ping(3));
        scene.check("the ping left as a custom payload packet",
                () -> scene.assertEquals(List.of(new TestPayloads.Ping(3)), payloads(player), "payloads the player received"));
        scene.succeed();
    }

    @Scene(maxTicks = 40, isolated = true)
    public void aPayloadFromAPlayerIsHandledWithThatPlayer(TestScene scene) {
        TestPlayer player = scene.addPlayer("Sender");
        TestPlayer bystander = scene.addPlayer("Bystander");
        player.clearReceived();
        // What the server's packet listener does when this player's client sends a pong; on the server thread it runs the handler at once.
        player.player().connection.handleCustomPayload(new ServerboundCustomPayloadPacket(new TestPayloads.Pong(7)));
        scene.check("the handler ran with the sending player and answered it",
                () -> scene.assertEquals(List.of(new TestPayloads.Ping(7)), payloads(player), "payloads the player received"));
        scene.check("and no one else", () -> scene.assertEquals(List.of(), payloads(bystander), "payloads the bystander received"));
        scene.succeed();
    }

    @Scene(maxTicks = 40, isolated = true)
    public void aPayloadArrivingOffTheServerThreadIsHandledOnItNextTick(TestScene scene) {
        TestPlayer player = scene.addPlayer("Sender");
        player.clearReceived();
        TestPayloads.forgetHandlerThread();
        Thread networkThread = new Thread(
                () -> player.player().connection.handleCustomPayload(new ServerboundCustomPayloadPacket(new TestPayloads.Pong(9))), "test-network");
        networkThread.start();
        try {
            networkThread.join();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while the network thread delivered the pong", interrupted);
        }
        scene.check("nothing ran on the network thread", () -> scene.assertTrue(TestPayloads.handlerThread() == null, "the handler must wait for the server thread"));
        scene.runAtTick(2, () -> {
            scene.check("the handler ran on the server thread", () -> scene.assertEquals(Thread.currentThread(), TestPayloads.handlerThread(), "handler thread"));
            scene.check("and answered the player", () -> scene.assertEquals(List.of(new TestPayloads.Ping(9)), payloads(player), "payloads the player received"));
            scene.succeed();
        });
    }

    /** The test payloads the player received; the game sends payloads of its own alongside, chunk light among them. */
    private static List<CustomPacketPayload> payloads(TestPlayer player) {
        return player.received(ClientboundCustomPayloadPacket.class).stream()
                .map(ClientboundCustomPayloadPacket::payload)
                .filter(payload -> payload.type().id().getNamespace().equals(JolCraftGameTests.MOD_ID))
                .toList();
    }
}
