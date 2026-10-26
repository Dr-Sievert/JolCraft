package net.sievert.jolcraft.module.primitive.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The bookkeeping and the replay. The game's own registry is already closed when these run, so the replay goes
 * into a registrar that only records, and a scene in the game-test mod carries a payload each way through a real
 * player.
 */
class ModNetworkTest {

    record Ping() implements CustomPacketPayload {
        static final Type<Ping> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "ping"));
        static final StreamCodec<RegistryFriendlyByteBuf, Ping> CODEC = StreamCodec.unit(new Ping());

        @Override
        public Type<Ping> type() {
            return TYPE;
        }
    }

    record Pong() implements CustomPacketPayload {
        static final Type<Pong> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "pong"));
        static final Type<Pong> VANILLA = new Type<>(Identifier.withDefaultNamespace("pong"));
        static final StreamCodec<RegistryFriendlyByteBuf, Pong> CODEC = StreamCodec.unit(new Pong());

        @Override
        public Type<Pong> type() {
            return TYPE;
        }
    }

    /** NeoForge's registrar with its registering replaced by recording, since the game's registry is closed here. */
    static final class RecordingRegistrar extends PayloadRegistrar {
        final List<String> calls = new ArrayList<>();

        RecordingRegistrar(String version) {
            super(version);
        }

        @Override
        public <T extends CustomPacketPayload> PayloadRegistrar playToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
            calls.add("to client " + type.id());
            return this;
        }

        @Override
        public <T extends CustomPacketPayload> PayloadRegistrar playToServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler) {
            calls.add("to server " + type.id());
            return this;
        }
    }

    static final class RecordingEvent extends RegisterPayloadHandlersEvent {
        String version;
        RecordingRegistrar registrar;

        @Override
        public PayloadRegistrar registrar(String version) {
            this.version = version;
            registrar = new RecordingRegistrar(version);
            return registrar;
        }
    }

    private final IEventBus bus = BusBuilder.builder().build();

    @Test
    void payloadsAreCountedOncePerId() {
        ModNetwork network = new ModNetwork();
        network.toClient(Ping.TYPE, Ping.CODEC);
        network.toServer(Pong.TYPE, Pong.CODEC, (_, _) -> { });
        Assertions.assertEquals(2, network.declared());
        Assertions.assertThrows(IllegalStateException.class, () -> network.toClient(Ping.TYPE, Ping.CODEC));
        Assertions.assertThrows(IllegalStateException.class, () -> network.toServer(Ping.TYPE, Ping.CODEC, (_, _) -> { }), "one direction per id");
    }

    @Test
    void theGamesNamespaceIsRefused() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ModNetwork().toServer(Pong.VANILLA, Pong.CODEC, (_, _) -> { }));
    }

    @Test
    void registeringReplaysEveryDeclarationInOrderUnderTheVersion() {
        ModNetwork network = new ModNetwork();
        network.toClient(Ping.TYPE, Ping.CODEC);
        network.toServer(Pong.TYPE, Pong.CODEC, (_, _) -> { });
        network.register(bus, "7");
        RecordingEvent event = new RecordingEvent();
        network.onRegister(event);
        Assertions.assertEquals("7", event.version);
        Assertions.assertEquals(List.of("to client jolcraft:ping", "to server jolcraft:pong"), event.registrar.calls);
    }

    @Test
    void registeringTwiceOrDeclaringAfterItIsRefused() {
        ModNetwork network = new ModNetwork();
        network.toClient(Ping.TYPE, Ping.CODEC);
        network.register(bus, "1");
        Assertions.assertThrows(IllegalStateException.class, () -> network.register(bus, "1"));
        Assertions.assertThrows(IllegalStateException.class, () -> network.toServer(Pong.TYPE, Pong.CODEC, (_, _) -> { }));
    }

    @Test
    void aBlankVersionIsRefused() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ModNetwork().register(bus, " "));
    }

    @Test
    void theIdIsTheFolderName() {
        Assertions.assertEquals(Dictionary.NETWORK, new ModNetwork().id());
    }
}
