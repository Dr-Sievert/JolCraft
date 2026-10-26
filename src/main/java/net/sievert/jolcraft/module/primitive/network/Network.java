package net.sievert.jolcraft.module.primitive.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;

import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.Primitive;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The mod's payloads: what the client and the server send each other beyond what the game sends itself. A
 * module declares each payload with its direction while it is built, a payload being a {@code @Value} record
 * with its type and stream codec; the root calls {@link #register(IEventBus, String)} once, under the version both
 * sides must share. Sending is the game's own: {@code PacketDistributor} on the server, and the client's
 * distributor inside a client package. A clientbound payload gets its handler from the module's client bootstrap
 * through {@code client.ClientNetwork}.
 */
@Primitive(Dictionary.NETWORK)
public interface Network extends Module {

    /** A payload the client sends; the server handles it on its main thread, with the player who sent it. */
    <T extends CustomPacketPayload> void toServer(CustomPacketPayload.Type<@org.jetbrains.annotations.NotNull T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, ServerHandler<T> handler);

    /**
     * A payload the server sends. Its handler is given on the client, by the module's client bootstrap through
     * {@code client.ClientNetwork}; a client that starts without one refuses to, naming the payload.
     */
    <T extends CustomPacketPayload> void toClient(CustomPacketPayload.Type<@org.jetbrains.annotations.NotNull T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec);

    /**
     * Registers every declared payload with the game under the version both sides must share, so a client and a
     * server on different versions refuse to connect instead of misreading each other. The root calls it once.
     */
    void register(IEventBus modBus, String version);

    /** What a module does with a payload a player sent; runs on the server's main thread. */
    @FunctionalInterface
    interface ServerHandler<T extends CustomPacketPayload> {

        void handle(T payload, ServerPlayer player);
    }
}
