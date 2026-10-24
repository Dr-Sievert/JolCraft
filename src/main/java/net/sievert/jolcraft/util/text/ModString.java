package net.sievert.jolcraft.util.text;

import java.util.Locale;

/**
 * Joins {@link Dictionary} words into the longer strings ids, keys and paths are made of. Named with the
 * {@code Mod} prefix because its noun is a JDK type; see design/module-map.md, "Naming".
 */
public final class ModString {

    private ModString() {
    }

    public static String dotted(String... words) {
        return String.join(".", words);
    }

    public static String underscored(String... words) {
        return String.join("_", words);
    }

    public static String slashed(String... words) {
        return String.join("/", words);
    }

    public static String dashed(String... words) {
        return String.join("-", words);
    }

    public static String spaced(String... words) {
        return String.join(" ", words);
    }

    /** {@code [LOG]}: how a tag is written in a log line. */
    public static String tag(String word) {
        return "[" + word.toUpperCase(Locale.ROOT) + "]";
    }
}
