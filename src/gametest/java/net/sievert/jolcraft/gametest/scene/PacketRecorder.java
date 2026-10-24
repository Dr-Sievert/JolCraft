package net.sievert.jolcraft.gametest.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;

/**
 * The packets the server sends to tracked players, fed by the mixin on the packet listener's send. Only players
 * a scene added are tracked, and a scene drops its players' recordings when it ends, so nothing leaks between
 * scenes. A bundle is recorded as the packets inside it, which is what entity spawns and updates arrive as.
 */
public final class PacketRecorder {

    private static final Map<UUID, List<Packet<?>>> TRACKED = new ConcurrentHashMap<>();

    private PacketRecorder() {
    }

    /** Called by the mixin for every packet sent to a player. */
    public static void record(ServerPlayer player, Packet<?> packet) {
        if (player == null) {
            return;
        }
        List<Packet<?>> sent = TRACKED.get(player.getUUID());
        if (sent == null) {
            return;
        }
        if (packet instanceof BundlePacket<?> bundle) {
            for (Packet<?> inner : bundle.subPackets()) {
                sent.add(inner);
            }
        } else {
            sent.add(packet);
        }
    }

    static void track(UUID id) {
        TRACKED.put(id, Collections.synchronizedList(new ArrayList<>()));
    }

    static void untrack(UUID id) {
        TRACKED.remove(id);
    }

    static List<Packet<?>> sent(UUID id) {
        List<Packet<?>> sent = TRACKED.get(id);
        if (sent == null) {
            return List.of();
        }
        synchronized (sent) {
            return List.copyOf(sent);
        }
    }

    static void clear(UUID id) {
        List<Packet<?>> sent = TRACKED.get(id);
        if (sent != null) {
            sent.clear();
        }
    }
}
