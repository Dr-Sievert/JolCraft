package net.sievert.jolcraft.util.text;

/**
 * Every word used in an id, a key, a path or a tag, one lowercase word per constant under the same name in
 * capitals, in one alphabetical list. Longer strings are built from these with {@link ModString}. A word is never
 * written as a literal in module code; test code and log, assertion and exception messages are outside the rule.
 */
public final class Dictionary {

    public static final String ATTACHMENT = "attachment";
    /** The namespace of the tags mods share. */
    public static final String C = "c";
    public static final String CONFIG = "config";
    public static final String CONFIGURATION = "configuration";
    public static final String DATA = "data";
    public static final String DATAGEN = "datagen";
    public static final String DEFAULT = "default";
    public static final String EMPTY = "empty";
    public static final String GAMETEST = "gametest";
    public static final String JOLCRAFT = "jolcraft";
    public static final String LANGUAGE = "language";
    public static final String LOG = "log";
    public static final String MINECRAFT = "minecraft";
    public static final String NETWORK = "network";
    public static final String REGISTRATION = "registration";
    public static final String SCENE = "scene";

    private Dictionary() {
    }
}
