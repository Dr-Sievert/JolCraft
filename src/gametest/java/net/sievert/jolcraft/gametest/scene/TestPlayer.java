package net.sievert.jolcraft.gametest.scene;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/**
 * A real {@link ServerPlayer} placed in the level behind a mock connection, the way vanilla's own mock player is
 * made, plus what a scene asks of a player: run a command, and {@link #received()}, every packet the server sent
 * it since it joined or since {@link #clearReceived()}. The scene ticks it, so effects, hunger and regeneration run,
 * and acknowledges its chunk batches, so the arena's chunks and the entities in them are tracked to it. Created
 * through {@link TestScene#addPlayer(String)}.
 */
public final class TestPlayer {

    /** Chunks acknowledged per tick, generous so the arena streams in at once. */
    private static final float CHUNKS_PER_TICK = 64.0F;

    private final ServerPlayer player;
    private final UUID id;

    private TestPlayer(ServerPlayer player) {
        this.player = player;
        this.id = player.getUUID();
    }

    static TestPlayer join(TestScene scene, String name) {
        ServerLevel level = scene.level();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), name), false);
        // An anonymous subclass on purpose: NeoForge's config sync recognises a game-test player by its class not
        // being ServerPlayer itself and skips it, the way it skips vanilla's own mock player.
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        PacketRecorder.track(player.getUUID());
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        // Only after placement: snapTo goes through the connection, which placement creates. The chunk source
        // then tracks the player from the arena rather than from the spawn it was placed at.
        BlockPos origin = scene.origin();
        player.snapTo(origin.getX() + 0.5, origin.getY() + 1, origin.getZ() + 0.5);
        level.getChunkSource().move(player);
        return new TestPlayer(player);
    }

    public ServerPlayer player() {
        return player;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return player.getName().getString();
    }

    /** Dispatches a command as this player, with the player's own permissions; the reply arrives as packets. */
    public void runCommand(String command) {
        player.level().getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack(), command);
    }

    /** Every packet sent to this player since it joined or since the last {@link #clearReceived()}, in order, bundles unpacked. */
    public List<Packet<?>> received() {
        return PacketRecorder.sent(id);
    }

    public <T extends Packet<?>> List<T> received(Class<T> type) {
        return received().stream().filter(type::isInstance).map(type::cast).toList();
    }

    /** The system chat lines this player was shown, command replies included. */
    public List<Component> chat() {
        return received(ClientboundSystemChatPacket.class).stream().map(ClientboundSystemChatPacket::content).toList();
    }

    public void clearReceived() {
        PacketRecorder.clear(id);
    }

    /** One server tick of this player, which nothing else runs for a player without a client. */
    void tick() {
        if (player.level().getServer().getPlayerList().getPlayer(id) == null) {
            return;
        }
        player.doTick();
        player.connection.chunkSender.onChunkBatchReceivedByClient(CHUNKS_PER_TICK);
    }

    void leave() {
        PacketRecorder.untrack(id);
        if (player.level().getServer().getPlayerList().getPlayer(id) != null) {
            player.connection.disconnect(Component.literal("scene ended"));
        }
    }
}
