package net.sievert.jolcraft.stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A system's handle constants: a final class with a private constructor whose public static final fields are handles, the keys a record must name before the
 * system exists (a {@code RecipeKey} for {@code Recipe.Custom.key()}, a {@code YieldKinds} for a yield codec). A constant is declared where its kind is: a
 * {@code RecipeKey} to {@code Recipe} by the system's constructor, the mod's one {@code YieldKinds} to {@code Result} by the root, which reads it from the set;
 * the root registers once. A handle holds a lazy holder and no module, so the set is a declaration, not a locator. Depends on handles, values, helpers and
 * the game, never on a module or an implementation; sits in a system folder. A value constant (a {@code LanguageKey}, a {@code Shape}) keeps its
 * {@code @Value} set.
 * Checked by the architecture test; see design/module-map.md, "Stereotypes".
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Keys {
}
