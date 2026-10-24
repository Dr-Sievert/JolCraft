package net.sievert.jolcraft.module.primitive.log;

import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.Primitive;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * Logging. A module tags the log it is handed with itself, {@code log.tagged(this)}, and logs through that,
 * so a line reads {@code [TRADE] [OFFER] message}, tags in capitals in the order they were added. Messages use
 * {@code {}} placeholders. No module takes a raw logger.
 */
@Primitive(Dictionary.LOG)
public interface Log extends Module {

    /** The same log with {@code tag} added after the tags already present. */
    Log tagged(String tag);

    /** {@link #tagged(String)} with the module's id. */
    default Log tagged(Module module) {
        return tagged(module.id());
    }

    void info(String message, Object... arguments);

    void warn(String message, Object... arguments);

    void error(String message, Object... arguments);

    void debug(String message, Object... arguments);
}
