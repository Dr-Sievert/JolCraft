package net.sievert.jolcraft.module.primitive.log;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;

import net.sievert.jolcraft.stereotype.Implementation;
import net.sievert.jolcraft.util.text.ModString;

/** {@link Log} over an SLF4J logger; each tagged log is a new immutable view with one more tag. */
@Implementation
public final class ModLog implements Log {

    private final Logger logger;
    private final List<String> tags;
    private final String prefix;

    public ModLog(Logger logger) {
        this(logger, List.of());
    }

    private ModLog(Logger logger, List<String> tags) {
        this.logger = logger;
        this.tags = tags;
        this.prefix = tags.isEmpty() ? "" : tags.stream().map(ModString::tag).collect(Collectors.joining(" ")) + " ";
    }

    @Override
    public Log tagged(String tag) {
        return new ModLog(logger, Stream.concat(tags.stream(), Stream.of(tag)).toList());
    }

    @Override
    public void info(String message, Object... arguments) {
        logger.info(prefix + message, arguments);
    }

    @Override
    public void warn(String message, Object... arguments) {
        logger.warn(prefix + message, arguments);
    }

    @Override
    public void error(String message, Object... arguments) {
        logger.error(prefix + message, arguments);
    }

    @Override
    public void debug(String message, Object... arguments) {
        logger.debug(prefix + message, arguments);
    }
}
