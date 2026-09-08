package net.sievert.jolcraft.world.particle.util;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.sievert.jolcraft.network.JolCraftNetworking;
import net.sievert.jolcraft.network.packet.c2s.ServerboundSpawnParticlePacket;
import net.sievert.jolcraft.network.proxy.JolCraftProxy;
import net.sievert.jolcraft.util.log.JolCraftLogTags;
import net.sievert.jolcraft.util.log.JolCraftLogs;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

/**
 * Particle helper safe to call from common code.
 *
 * IMPORTANT:
 * Vanilla packet semantics:
 * - count == 0: xDist/yDist/zDist are treated as explicit motion for ONE particle; speed is mostly ignored.
 * - count  > 0: spawns COUNT particles with random offsets in xDist/yDist/zDist and velocity scaled by speed.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class JolCraftParticleHelper {

    private JolCraftParticleHelper() {
    }

    // ------------------------------------------------------------
    // WORLD PARTICLES (server-authoritative, called once)
    // ------------------------------------------------------------

    public static void spawn(Level level,
                             ParticleOptions particle,
                             boolean overrideLimiter,
                             boolean alwaysShow,
                             double x, double y, double z,
                             int count,
                             double xDist, double yDist, double zDist,
                             double speed) {

        if (count < 0) return;

        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return;
        if (!Double.isFinite(xDist) || !Double.isFinite(yDist) || !Double.isFinite(zDist)) return;
        if (!Double.isFinite(speed)) return;

        if (!level.isClientSide) {
            if (!(level instanceof ServerLevel serverLevel)) return;

            broadcast(
                    serverLevel,
                    particle,
                    overrideLimiter || alwaysShow,
                    x, y, z,
                    count,
                    xDist, yDist, zDist,
                    speed
            );
            return;
        }

        if (!isRelayable(particle)) {
            JolCraftLogs.warn(
                    JolCraftLogTags.NETWORK,
                    "Particle {} is not in RELAYABLE and cannot be spawned from the client",
                    particle.getType()
            );
            return;
        }

        JolCraftNetworking.sendToServer(
                new ServerboundSpawnParticlePacket(
                        particle,
                        overrideLimiter,
                        alwaysShow,
                        x, y, z,
                        count,
                        xDist, yDist, zDist,
                        speed
                )
        );
    }

    // Particle types a client may ask the server to broadcast on its behalf. Anything spawned
    // through the client branch of spawn() must be listed here.
    private static final Set<ParticleType<?>> RELAYABLE = Set.of(
            ParticleTypes.HAPPY_VILLAGER,
            ParticleTypes.BUBBLE_POP,
            ParticleTypes.CRIT,
            ParticleTypes.HEART,
            ParticleTypes.DUST
    );

    public static boolean isRelayable(ParticleOptions particle) {
        return RELAYABLE.contains(particle.getType());
    }

    // ServerLevel#sendParticles(type, x, ...) hardcodes overrideLimiter to false, so send per
    // player to keep the flag. Vanilla's /particle command does the same.
    public static void broadcast(ServerLevel level,
                                 ParticleOptions particle,
                                 boolean force,
                                 double x, double y, double z,
                                 int count,
                                 double xDist, double yDist, double zDist,
                                 double speed) {

        for (ServerPlayer player : level.players()) {
            level.sendParticles(
                    player,
                    particle,
                    force,
                    x, y, z,
                    count,
                    xDist, yDist, zDist,
                    speed
            );
        }
    }

    public static void spawn(Level level,
                             ParticleOptions particle,
                             double x, double y, double z,
                             int count,
                             double xDist, double yDist, double zDist,
                             double speed) {

        spawn(
                level,
                particle,
                particle.getType().getOverrideLimiter(),
                false,
                x, y, z,
                count,
                xDist, yDist, zDist,
                speed
        );
    }

    public static void spawn(Level level,
                             ParticleOptions particle,
                             BlockPos pos,
                             int count,
                             double xDist, double yDist, double zDist,
                             double speed) {

        spawn(
                level,
                particle,
                particle.getType().getOverrideLimiter(),
                false,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                count,
                xDist, yDist, zDist,
                speed
        );
    }

    // ------------------------------------------------------------
    // LOCAL-ONLY PARTICLES (single player sees it)
    // ------------------------------------------------------------

    public static void spawnLocal(Player player,
                                  ParticleOptions particle,
                                  boolean overrideLimiter,
                                  boolean alwaysShow,
                                  double x, double y, double z,
                                  int count,
                                  double xDist, double yDist, double zDist,
                                  double speed) {

        if (count < 0) return;

        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return;
        if (!Double.isFinite(xDist) || !Double.isFinite(yDist) || !Double.isFinite(zDist)) return;
        if (!Double.isFinite(speed)) return;

        Level level = player.level();

        if (level.isClientSide) {
            Player local = JolCraftProxy.access().getLocalPlayer();
            if (local != player) return;

            boolean forceAlwaysRender = overrideLimiter || alwaysShow;

            if (count == 0) {
                level.addParticle(particle, forceAlwaysRender, x, y, z, xDist, yDist, zDist);
            } else {
                for (int i = 0; i < count; i++) {
                    level.addParticle(particle, forceAlwaysRender, x, y, z, xDist, yDist, zDist);
                }
            }
            return;
        }

        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundLevelParticlesPacket(
                    particle,
                    overrideLimiter || alwaysShow,
                    x, y, z,
                    (float) xDist,
                    (float) yDist,
                    (float) zDist,
                    (float) speed,
                    count
            ));
        }
    }
}