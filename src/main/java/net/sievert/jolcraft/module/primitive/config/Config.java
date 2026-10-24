package net.sievert.jolcraft.module.primitive.config;

import net.neoforged.fml.ModContainer;

import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.Primitive;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The mod's settings, in the config files the game keeps for it. A module declares its {@link ConfigKey}s into a
 * file while it is built; the root calls {@link #register(ModContainer)} once, which writes the files' specs and
 * hands them to the game; from then on a setting is read live with {@link #get(ConfigKey)} and never copied into
 * a field, so an edit or a reload is seen at once. Before a file is loaded, {@code get} answers the key's fallback.
 * On the client the root also calls {@code client.ConfigScreen}, which gives the mod NeoForge's configuration
 * screen; every setting and section is shown under the label datagen demands English for.
 */
@Primitive(Dictionary.CONFIG)
public interface Config extends Module {

    /** The mod's config files, as the game keeps them: one per kind, named {@code <mod id>-<kind>.toml}. */
    enum File {
        /** Loaded on both sides from the game folder after the registry events; not synced, so every side reads its own copy. */
        COMMON,
        /** Loaded on the client only, after the registry events. */
        CLIENT,
        /** Kept per world: loaded when a world opens and sent to clients as they join; at the main menu its settings answer their fallback. */
        SERVER,
        /**
         * Loaded the moment the root registers the files, at the end of its constructor, so it can decide what the
         * mod registers from the registry events on. Reading it earlier, while a module is built, is an error, and
         * its settings keep their first value until the game restarts.
         */
        STARTUP
    }

    /**
     * Declares a setting in a file. Only while modules are built, once per path, and a path may not be a section
     * of another's.
     */
    <T> void declare(File file, ConfigKey<T> key);

    /** Builds every declared file's spec and registers it with the mod. The root calls it once. */
    void register(ModContainer container);

    /**
     * The setting's current value: the file's once loaded, the key's fallback before. A key never declared is an
     * error once the files are built, and a startup setting is an error until the root has registered the files.
     */
    <T> T get(ConfigKey<T> key);
}
