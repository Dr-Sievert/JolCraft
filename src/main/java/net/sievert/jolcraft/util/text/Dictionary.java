package net.sievert.jolcraft.util.text;

/**
 * Every word used in an id, a key, a path or a tag, one lowercase word per constant under the same name in
 * capitals, in one alphabetical list. Longer strings are built from these with {@link ModString}. A word is never
 * written as a literal in module code; test code and log, assertion and exception messages are outside the rule.
 */
public final class Dictionary {

    public static final String ACTION = "action";
    public static final String ADVANCEMENT = "advancement";
    public static final String ADVANCEMENTS = "advancements";
    public static final String AMOUNT = "amount";
    public static final String ATTACHMENT = "attachment";
    public static final String AWARDS = "awards";
    public static final String BOOK = "book";
    /** The namespace of the tags mods share. */
    public static final String C = "c";
    public static final String CAPABILITY = "capability";
    public static final String COMMAND = "command";
    public static final String CONFIG = "config";
    public static final String CONFIGURATION = "configuration";
    public static final String CONSUME = "consume";
    public static final String COOLDOWN = "cooldown";
    public static final String COUNT = "count";
    public static final String DAMAGE = "damage";
    public static final String DATA = "data";
    public static final String DATAGEN = "datagen";
    public static final String DEFAULT = "default";
    public static final String DESCRIPTION = "description";
    public static final String EFFECT = "effect";
    public static final String EMPTY = "empty";
    public static final String ENTITY = "entity";
    public static final String FATE = "fate";
    public static final String FEEDBACK = "feedback";
    public static final String GAMETEST = "gametest";
    public static final String HOOK = "hook";
    public static final String HOOKS = "hooks";
    public static final String INVENTORY = "inventory";
    public static final String ITEM = "item";
    public static final String JOLCRAFT = "jolcraft";
    public static final String KEEP = "keep";
    public static final String KIND = "kind";
    public static final String LANGUAGE = "language";
    public static final String LEDGER = "ledger";
    public static final String LOG = "log";
    public static final String LOOT = "loot";
    public static final String MENU = "menu";
    public static final String MINECRAFT = "minecraft";
    public static final String NETWORK = "network";
    public static final String PITCH = "pitch";
    public static final String PLAYER = "player";
    public static final String POOL = "pool";
    public static final String PREDICATE = "predicate";
    public static final String RECIPE = "recipe";
    public static final String REGISTRATION = "registration";
    public static final String RESULT = "result";
    public static final String SCHEDULE = "schedule";
    public static final String SOUND = "sound";
    public static final String SOURCE = "source";
    public static final String STAT = "stat";
    public static final String SUBTITLES = "subtitles";
    public static final String TITLE = "title";
    public static final String TYPE = "type";
    public static final String UNTIL = "until";
    public static final String VOLUME = "volume";
    public static final String YIELD = "yield";

    private Dictionary() {
    }
}
