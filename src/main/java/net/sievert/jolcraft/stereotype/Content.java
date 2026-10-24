package net.sievert.jolcraft.stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A module folder's registry declarations: a final class with a private constructor whose public static final fields are DeferredRegisters and DeferredHolders, plus one register(IEventBus). Depends on vanilla, NeoForge and its own folder's values, never on a module.
 * Checked by the architecture test; see design/module-map.md, "Stereotypes".
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Content {
}
