package net.sievert.jolcraft.module.primitive.network;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import net.sievert.jolcraft.stereotype.Implementation;
import net.sievert.jolcraft.util.text.Dictionary;

/** {@link Network} over NeoForge's payload registrar: declarations are kept until the game asks for them. */
@Implementation
public final class ModNetwork implements Network {

    private final Map<Identifier, Consumer<PayloadRegistrar>> declared = new LinkedHashMap<>();
    private String version;

    @Override
    public <T extends CustomPacketPayload> void toServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, ServerHandler<T> handler) {
        declare(type, registrar -> registrar.playToServer(type, codec, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                throw new IllegalStateException(type.id() + " was handled away from the server");
            }
            if (player.hasDisconnected()) {
                return; // the player left between the network thread receiving the payload and this tick
            }
            handler.handle(payload, player);
        }));
    }

    @Override
    public <T extends CustomPacketPayload> void toClient(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        declare(type, registrar -> registrar.playToClient(type, codec));
    }

    private void declare(CustomPacketPayload.Type<?> type, Consumer<PayloadRegistrar> declaration) {
        if (version != null) {
            throw new IllegalStateException(type.id() + " was declared after the network was registered");
        }
        if (type.id().getNamespace().equals(Dictionary.MINECRAFT)) {
            throw new IllegalArgumentException(type.id() + " uses the game's namespace; a payload id is the mod's own");
        }
        if (declared.putIfAbsent(type.id(), declaration) != null) {
            throw new IllegalStateException(type.id() + " was declared twice");
        }
    }

    @Override
    public void register(IEventBus modBus, String version) {
        if (this.version != null) {
            throw new IllegalStateException("the network was registered twice");
        }
        if (version.isBlank()) {
            throw new IllegalArgumentException("the network needs a version both sides share");
        }
        this.version = version;
        modBus.addListener(RegisterPayloadHandlersEvent.class, this::onRegister);
    }

    /** Replays every declaration into the game's registrar under the version; package-private so a test can hand it an event of its own. */
    void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(version);
        declared.values().forEach(declaration -> declaration.accept(registrar));
    }

    /** How many payloads are declared, for the root's log line. */
    public int declared() {
        return declared.size();
    }
}
