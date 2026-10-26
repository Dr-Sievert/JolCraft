package net.sievert.jolcraft.module.primitive.config;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

import net.sievert.jolcraft.stereotype.Handle;
import net.sievert.jolcraft.util.text.Dictionary;
import net.sievert.jolcraft.util.text.ModString;

/**
 * One setting: its path in the file, built from {@code Dictionary} words (the last word is the name, the ones
 * before it the sections), its fallback, the owner's comment in English, and the kind that decides how the game
 * validates it. A key is a constant where it is used and is declared into a file through {@link Config}. The
 * setting and each of its sections carry the translation key {@code <mod id>.configuration.<path>}, which the
 * config screen shows and datagen demands English for; the comment is the screen's tooltip.
 */
@Handle
public sealed interface ConfigKey<T> {

    Pattern WORD = Pattern.compile("[a-z0-9_]+");

    /** Words the config screen uses in keys of its own, so a path may not carry them. */
    Set<String> RESERVED = Set.of("title", "button", "tooltip", "warning", "section");

    List<String> path();

    T fallback();

    /** The comment written above the setting in the file and shown as its tooltip; English owner documentation, outside the Dictionary rule. */
    String comment();

    /** The setting's name: the last word of its path. */
    default String name() {
        return path().getLast();
    }

    /** The sections the setting sits in: every word of its path but the last. */
    default List<String> sections() {
        return path().subList(0, path().size() - 1);
    }

    /** The translation key of this setting's label on the config screen, under the mod's namespace. */
    default String translation(String namespace) {
        return translation(namespace, path().toArray(String[]::new));
    }

    /** The translation key of the setting or section at these words, {@code <namespace>.configuration.<words>}: the one formula the mod and datagen share. */
    static String translation(String namespace, String... words) {
        return ModString.dotted(namespace, Dictionary.CONFIGURATION, ModString.dotted(words));
    }

    /**
     * Defines the value under its name in a builder that stands in the setting's section with the comment,
     * translation key and restart kind set; called by {@link Config} once per key.
     */
    ConfigValue<T> defineValue(ModConfigSpec.Builder builder, String name);

    static ConfigKey<Boolean> flag(boolean fallback, String comment, String... path) {
        return new Flag(words(path), fallback, comment);
    }

    static ConfigKey<Integer> whole(int fallback, int min, int max, String comment, String... path) {
        return new Whole(words(path), fallback, min, max, comment);
    }

    static ConfigKey<Double> real(double fallback, double min, double max, String comment, String... path) {
        return new Real(words(path), fallback, min, max, comment);
    }

    static ConfigKey<String> text(String fallback, String comment, String... path) {
        return new Text(words(path), fallback, comment);
    }

    static <E extends Enum<E>> ConfigKey<E> choice(E fallback, String comment, String... path) {
        return new Choice<>(words(path), fallback, comment);
    }

    private static List<String> words(String... path) {
        if (path.length == 0) {
            throw new IllegalArgumentException("a config key needs at least one word");
        }
        for (String word : path) {
            if (!WORD.matcher(word).matches()) {
                throw new IllegalArgumentException("\"" + word + "\" is no config-key word: lowercase letters, digits and underscores only");
            }
            if (RESERVED.contains(word)) {
                throw new IllegalArgumentException("\"" + word + "\" is a word the config screen uses for keys of its own");
            }
        }
        return List.of(path);
    }

    /**
     * A boolean, as the game's own boolean setting so the config screen can toggle it; the game also accepts the quoted
     * spelling in the file and hands it back as text, which {@code ModConfig.get} reads as the boolean it names.
     */
    record Flag(List<String> path, Boolean fallback, String comment) implements ConfigKey<Boolean> {
        @Override
        public ConfigValue<Boolean> defineValue(ModConfigSpec.Builder builder, String name) {
            return builder.define(name, fallback.booleanValue());
        }
    }

    record Whole(List<String> path, Integer fallback, int min, int max, String comment) implements ConfigKey<Integer> {
        @Override
        public ConfigValue<Integer> defineValue(ModConfigSpec.Builder builder, String name) {
            return builder.defineInRange(name, fallback, min, max);
        }
    }

    record Real(List<String> path, Double fallback, double min, double max, String comment) implements ConfigKey<Double> {
        @Override
        public ConfigValue<Double> defineValue(ModConfigSpec.Builder builder, String name) {
            return builder.defineInRange(name, fallback, min, max);
        }
    }

    record Text(List<String> path, String fallback, String comment) implements ConfigKey<String> {
        @Override
        public ConfigValue<String> defineValue(ModConfigSpec.Builder builder, String name) {
            return builder.define(name, fallback);
        }
    }

    record Choice<E extends Enum<E>>(List<String> path, E fallback, String comment) implements ConfigKey<E> {
        @Override
        public ConfigValue<E> defineValue(ModConfigSpec.Builder builder, String name) {
            return builder.defineEnum(name, fallback);
        }
    }
}
