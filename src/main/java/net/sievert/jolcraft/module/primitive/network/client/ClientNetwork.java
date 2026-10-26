package net.sievert.jolcraft.module.primitive.network.client;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

import net.sievert.jolcraft.stereotype.Helper;

/**
 * The client half of {@code network}: a module's client bootstrap gives each payload the server sends its
 * handler here, run on the client's main thread with the local player. A payload declared {@code toClient}
 * without a handler here stops the client at startup, naming it.
 */
@Helper
public final class ClientNetwork {

    private ClientNetwork() {
    }

    public static <T extends CustomPacketPayload> void handle(IEventBus modBus, CustomPacketPayload.Type<@org.jetbrains.annotations.NotNull T> type, ClientHandler<T> handler) {
        modBus.addListener(RegisterClientPayloadHandlersEvent.class,
                event -> event.register(type, (payload, context) -> handler.handle(payload, context.player())));
    }

    /** What a module's client side does with a payload the server sent; runs on the client's main thread. */
    @FunctionalInterface
    public interface ClientHandler<T extends CustomPacketPayload> {

        void handle(T payload, Player player);
    }
}
