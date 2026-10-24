package net.sievert.jolcraft.stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An interface whose every implementor is nested inside its own folder.
 * Checked by the architecture test; see design/module-map.md, "Stereotypes".
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Handle {
}
