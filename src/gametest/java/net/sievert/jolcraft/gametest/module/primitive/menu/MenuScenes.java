package net.sievert.jolcraft.gametest.module.primitive.menu;

import java.util.List;

import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import java.util.OptionalInt;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.neoforged.neoforge.network.payload.AdvancedContainerSetDataPayload;
import net.neoforged.neoforge.network.payload.AdvancedOpenScreenPayload;
import net.neoforged.neoforge.network.connection.ConnectionType;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestPlayer;
import net.sievert.jolcraft.gametest.scene.TestScene;

/**
 * The menu primitive against a real player: opening the declared menu gives the player the server-side menu under
 * the window id the game picked, tells the client to open the type under the title, and syncs the menu's data on the
 * next tick. Isolated: it adds a player.
 */
public final class MenuScenes {

    @Scene(maxTicks = 40, isolated = true)
    public void openingAMenuGivesThePlayerTheMenuAndTellsTheClient(TestScene scene) {
        TestPlayer player = scene.addPlayer("Opener");
        player.clearReceived();
        OptionalInt window = TestMenus.COUNTER.open(player.player(), Component.literal("Counter"), (windowId, _, _) -> new TestMenus.Counter(windowId, 7), data -> data.writeVarInt(7));
        scene.check("the game opened it", () -> scene.assertTrue(window.isPresent(), "window id"));
        scene.check("the player holds the server-side menu under that id", () -> scene.assertTrue(
                player.player().containerMenu instanceof TestMenus.Counter counter && counter.containerId == window.orElse(-1) && counter.count() == 7, "container menu"));
        // A menu opened with data for the client travels as NeoForge's own payload, not the game's open-screen packet.
        List<AdvancedOpenScreenPayload> opened = player.received(ClientboundCustomPayloadPacket.class).stream()
                .map(ClientboundCustomPayloadPacket::payload).filter(AdvancedOpenScreenPayload.class::isInstance).map(AdvancedOpenScreenPayload.class::cast).toList();
        scene.check("the client was told to open the type under the title, with the data", () -> scene.assertTrue(
                opened.size() == 1 && opened.getFirst().menuType() == TestMenus.COUNTER.type() && opened.getFirst().windowId() == window.orElse(-1)
                        && opened.getFirst().name().getString().equals("Counter") && opened.getFirst().additionalData().length > 0, "open payloads " + opened));
        scene.check("the client side builds its copy from the data the server wrote", () -> scene.assertTrue(
                opened.getFirst().menuType().create(opened.getFirst().windowId(), player.player().getInventory(),
                        new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(opened.getFirst().additionalData()), scene.level().registryAccess(), ConnectionType.OTHER)) instanceof TestMenus.Counter copy
                        && copy.count() == 7, "client copy"));
        scene.runAtTick(1, () -> {
            player.clearReceived();
            ((TestMenus.Counter) player.player().containerMenu).count(8);
        });
        scene.runAtTick(3, () -> {
            // Menu data travels as NeoForge's own payload too, which carries the value whole where the game's packet would clip it.
            List<AdvancedContainerSetDataPayload> data = player.received(ClientboundCustomPayloadPacket.class).stream()
                    .map(ClientboundCustomPayloadPacket::payload).filter(AdvancedContainerSetDataPayload.class::isInstance).map(AdvancedContainerSetDataPayload.class::cast).toList();
            scene.check("a change to the menu's data reached the client on the next tick", () -> scene.assertTrue(
                    data.stream().anyMatch(payload -> payload.containerId() == (byte) window.orElse(-1) && payload.value() == 8), "data payloads " + data));
            scene.succeed();
        });
    }
}
