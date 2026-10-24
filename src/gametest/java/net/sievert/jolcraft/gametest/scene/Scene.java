package net.sievert.jolcraft.gametest.scene;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.sievert.jolcraft.util.text.Dictionary;

/**
 * A game test. Goes on a {@code public void method(TestScene scene)} of a public class in the gametest source set;
 * the class needs a public no-argument constructor unless the method is static. The scene exists once it is
 * written, and its id is its place: {@code system/trade/trade_scenes/offers_match} for a scene class under
 * {@code gametest.module.system.trade}, or {@code example_scenes/...} for the adapter's own.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Scene {

    /** A path of its own below the test mod's namespace, instead of the one derived from the class and method. */
    String name() default "";

    /** The structure template placed for the scene: vanilla's empty one-block template unless the scene names its own. */
    String structure() default Dictionary.MINECRAFT + ":" + Dictionary.EMPTY;

    /** Ticks before the scene fails for not having succeeded; at least 1. */
    int maxTicks() default 100;

    int setupTicks() default 0;

    /** A required scene failing fails the run; an optional one is only reported. */
    boolean required() default true;

    /** How many times the scene is run; {@link #requiredSuccesses()} of them must pass. */
    int attempts() default 1;

    int requiredSuccesses() default 1;

    boolean skyAccess() default false;

    /**
     * Runs the scene in a batch of its own, so no other scene shares its level ticks, its player list or the
     * packets its players receive. Set it on every scene that adds a player.
     */
    boolean isolated() default false;
}
