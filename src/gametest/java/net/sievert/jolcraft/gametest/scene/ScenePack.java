package net.sievert.jolcraft.gametest.scene;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * The scene fixtures as a built-in data pack, {@code src/gametest/resources/scene}: data maps, tags, placed features, affinity rows, a
 * cure tag and loot modifiers that act on vanilla content or on the real mod's rules, which a dev run must not feel.
 * The pack is always listed. It is required (forced on in every world, however the world's pack list reads) only
 * when the system property {@value #PROPERTY} is {@code true}, which build.gradle sets on the game-test server run
 * alone; elsewhere its source does not add it to a world by itself, so it stays off until
 * {@code /datapack enable "mod/jolcraft_gametest:scene"}.
 */
public final class ScenePack {

    /** Set to {@code true} by build.gradle on runGameTestServer only. */
    public static final String PROPERTY = "jolcraft.gametest.scene";

    private static final PackSource LISTED = PackSource.create(PackSource.NO_DECORATION, false);

    private ScenePack() {
    }

    public static void register(IEventBus modBus, String modId) {
        boolean required = Boolean.getBoolean(PROPERTY);
        modBus.addListener(AddPackFindersEvent.class, event -> event.addPackFinders(
                Identifier.fromNamespaceAndPath(modId, "scene"),
                PackType.SERVER_DATA,
                Component.literal("JolCraft scene fixtures"),
                LISTED,
                required,
                Pack.Position.TOP));
    }
}
