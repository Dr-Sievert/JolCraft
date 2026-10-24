package net.sievert.jolcraft.datagen;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a datagen contributor: a public class with a public no-argument constructor implementing one or more of
 * the interfaces in {@code datagen.contribute}. Its value is the folder of the module it generates for (a
 * primitive's or system's id, or the primitive folder a composite sits in), and the class sits in that module's
 * mirrored place, {@code datagen.module.<tier>.<folder>}; the run fails otherwise, and the architecture test
 * checks the module exists.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Contributes {

    /** The module's folder, one {@code Dictionary} word. */
    String value();
}
