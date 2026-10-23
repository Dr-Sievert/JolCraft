package net.sievert.jolcraft.network.packet.s2c;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.config.custom.brewing.CorruptionEffectsConfig;
import net.sievert.jolcraft.config.custom.brewing.CorruptionEffectsConfigManager;
import net.sievert.jolcraft.config.custom.dwarf.DwarfProfessionConfig;
import net.sievert.jolcraft.config.custom.dwarf.DwarfProfessionConfigManager;
import net.sievert.jolcraft.data.id.network.JolCraftNetworkIds;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pushes the data-driven configs to the client on datapack sync.
 *
 * Both managers are server-data reload listeners, so on a dedicated server the client's copies stay
 * empty and JEI silently renders nothing. In single player these are the same JVM singletons the
 * server just filled, so applying this there is a no-op with identical content.
 */
public record ClientboundConfigSyncPacket(
        @NotNull Map<ResourceLocation, DwarfProfessionConfig> dwarfProfessions,
        @NotNull CorruptionEffectsConfig corruptionEffects
) implements CustomPacketPayload {

    private static final int MAX_PROFESSIONS = 256;

    private static final StreamCodec<RegistryFriendlyByteBuf, Map<ResourceLocation, DwarfProfessionConfig>> DWARF_PROFESSIONS_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(
                    DwarfProfessionConfigManager.INSTANCE.syncCodec()
            );

    private static final StreamCodec<RegistryFriendlyByteBuf, CorruptionEffectsConfig> CORRUPTION_EFFECTS_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(
                    CorruptionEffectsConfig.CODEC
            );

    public static final Type<ClientboundConfigSyncPacket> TYPE =
            new Type<>(
                    JolCraft.location(
                            JolCraftNetworkIds.CONFIG_SYNC
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundConfigSyncPacket> CODEC =
            StreamCodec.of(
                    ClientboundConfigSyncPacket::write,
                    ClientboundConfigSyncPacket::read
            );

    public ClientboundConfigSyncPacket {
        // LinkedHashMap copy, not Map.copyOf, so the wire order stays deterministic.
        dwarfProfessions = Collections.unmodifiableMap(
                new LinkedHashMap<>(dwarfProfessions)
        );

        if (dwarfProfessions.size() > MAX_PROFESSIONS) {
            throw new IllegalArgumentException(
                    "dwarf profession config payload exceeds "
                            + MAX_PROFESSIONS
                            + " entries"
            );
        }
    }

    public static @NotNull ClientboundConfigSyncPacket current() {
        return new ClientboundConfigSyncPacket(
                DwarfProfessionConfigManager.INSTANCE.syncPayload(),
                CorruptionEffectsConfigManager.INSTANCE.snapshot()
        );
    }

    public void apply() {
        DwarfProfessionConfigManager.INSTANCE.applySynced(this.dwarfProfessions);
        CorruptionEffectsConfigManager.INSTANCE.applySynced(this.corruptionEffects);
    }

    public static void clear() {
        DwarfProfessionConfigManager.INSTANCE.reset();
        CorruptionEffectsConfigManager.INSTANCE.reset();
    }

    private static @NotNull ClientboundConfigSyncPacket read(
            @NotNull RegistryFriendlyByteBuf buffer
    ) {
        Map<ResourceLocation, DwarfProfessionConfig> professions =
                DWARF_PROFESSIONS_CODEC.decode(buffer);

        if (professions.size() > MAX_PROFESSIONS) {
            throw new IllegalArgumentException(
                    "invalid dwarf profession config payload size: "
                            + professions.size()
            );
        }

        CorruptionEffectsConfig corruption =
                CORRUPTION_EFFECTS_CODEC.decode(buffer);

        return new ClientboundConfigSyncPacket(
                professions,
                corruption
        );
    }

    private static void write(
            @NotNull RegistryFriendlyByteBuf buffer,
            @NotNull ClientboundConfigSyncPacket packet
    ) {
        DWARF_PROFESSIONS_CODEC.encode(
                buffer,
                packet.dwarfProfessions()
        );

        CORRUPTION_EFFECTS_CODEC.encode(
                buffer,
                packet.corruptionEffects()
        );
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
