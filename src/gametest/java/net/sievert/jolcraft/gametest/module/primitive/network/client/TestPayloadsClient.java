package net.sievert.jolcraft.gametest.module.primitive.network.client;

import net.neoforged.bus.api.IEventBus;

import net.sievert.jolcraft.gametest.module.primitive.network.TestPayloads;
import net.sievert.jolcraft.module.primitive.network.client.ClientNetwork;
import net.sievert.jolcraft.stereotype.Helper;

/**
 * The client half of the test payloads: gives {@code Ping} its handler, so a dev client that loads the game-test
 * mod starts. The scenes run on a server, where no ping is ever handled.
 */
@Helper
public final class TestPayloadsClient {

    private TestPayloadsClient() {
    }

    public static void bootstrap(IEventBus modBus) {
        ClientNetwork.handle(modBus, TestPayloads.Ping.TYPE, (_, _) -> { });
    }
}
