package net.sievert.jolcraft.gametest.module.primitive.network;

import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.module.primitive.network.Network;
import net.sievert.jolcraft.module.primitive.registration.Ids;

/** The payloads the network scenes exchange: the game-test mod declares them in its root. */
public final class TestPayloads {

    private static final AtomicReference<Thread> HANDLER_THREAD = new AtomicReference<>();

    private TestPayloads() {
    }

    /** Sent to a player. */
    public record Ping(int number) implements CustomPacketPayload {
        public static final Type<Ping> TYPE = new Type<>(Ids.of(JolCraftGameTests.MOD_ID, "ping"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Ping> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Ping::number, Ping::new);

        @Override
        public Type<Ping> type() {
            return TYPE;
        }
    }

    /** Sent by a player; the server answers it with a {@link Ping} carrying the same value. */
    public record Pong(int number) implements CustomPacketPayload {
        public static final Type<Pong> TYPE = new Type<>(Ids.of(JolCraftGameTests.MOD_ID, "pong"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Pong> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Pong::number, Pong::new);

        @Override
        public Type<Pong> type() {
            return TYPE;
        }
    }

    public static void declare(Network network) {
        network.toClient(Ping.TYPE, Ping.CODEC);
        network.toServer(Pong.TYPE, Pong.CODEC, (pong, player) -> {
            HANDLER_THREAD.set(Thread.currentThread());
            PacketDistributor.sendToPlayer(player, new Ping(pong.number()));
        });
    }

    /** The thread the last pong was handled on, or null since {@link #forgetHandlerThread()}. */
    public static Thread handlerThread() {
        return HANDLER_THREAD.get();
    }

    public static void forgetHandlerThread() {
        HANDLER_THREAD.set(null);
    }
}
