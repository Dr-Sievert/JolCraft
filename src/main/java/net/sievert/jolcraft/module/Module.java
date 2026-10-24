package net.sievert.jolcraft.module;

/**
 * Implemented at every tier. Exposes the module's id, read from its tier annotation
 * ({@link Primitive}, {@link Composite} or {@link ModSystem}), used for logging and diagnostics.
 * Nothing else: there is no shared behaviour to put here, and no module declares id() itself.
 */
public interface Module {

    default String id() {
        return ModuleId.of(getClass());
    }
}
