package net.sievert.jolcraft.stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A public class the game creates and calls, extending a game type: an entity, a block entity, a menu, a block, an item. Holds no module and no mutable static state; reads data (recipes, datapack registries, attachments, saved data) and posts events, and never asks a system anything.
 * Checked by the architecture test; see design/module-map.md, "Stereotypes".
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Actor {
}
