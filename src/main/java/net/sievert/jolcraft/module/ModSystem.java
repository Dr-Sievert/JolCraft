package net.sievert.jolcraft.module;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The top tier: a feature, doing something end to end. A system takes primitives, composites and other systems;
 * it is unrestricted. Goes on the system's class, which implements {@link Module}, in its own feature folder.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ModSystem {

    /** The module's id: one {@code Dictionary} word, unique across all tiers. */
    String value();
}
