package net.sievert.jolcraft.datagen.contribute;

import net.sievert.jolcraft.datagen.provider.Sounds;

/** A module's sound definitions: one per sound event it registers. */
public interface SoundContributor {

    void sounds(Sounds sounds);
}
