package net.sievert.jolcraft.module;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The middle tier. A composite is built from primitives only, never from other composites: anything that makes
 * two primitives work together is a composite. Its constructor takes exactly the primitives it uses.
 * Goes on the composite's class, which implements {@link Module}, in the folder of the primitive it is organised under.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Composite {

    /** The module's id: one {@code Dictionary} word, unique across all tiers. */
    String value();
}
