package net.sievert.jolcraft.stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Extends the event bus's base type: one of JolCraft's own events, posted by an actor or a mixin and listened to by a system. Sits in a composite, a system or the event primitive.
 * Checked by the architecture test; see design/module-map.md, "Stereotypes".
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Event {
}
