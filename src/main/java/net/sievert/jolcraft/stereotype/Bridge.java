package net.sievert.jolcraft.stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The only kind of class that may import an optional mod's API (JEI, Jade); every use sits behind a ModList guard.
 * Checked by the architecture test; see design/module-map.md, "Stereotypes".
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Bridge {
}
