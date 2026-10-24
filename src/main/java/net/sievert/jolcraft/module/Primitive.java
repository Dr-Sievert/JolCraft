package net.sievert.jolcraft.module;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The bottom tier. A primitive is self-contained: it knows nothing about JolCraft and nothing about the other
 * primitives, and keeps no JolCraft-specific code.
 * Goes on the primitive's interface, which extends {@link Module}; its id is its package name.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Primitive {

    /** The module's id: one {@code Dictionary} word, unique across all tiers. */
    String value();
}
