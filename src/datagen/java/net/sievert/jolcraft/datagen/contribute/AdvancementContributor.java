package net.sievert.jolcraft.datagen.contribute;

import net.sievert.jolcraft.datagen.provider.Advancements;

/** A module's advancements; each with a display gets its title and description text from the module's language contributor. */
public interface AdvancementContributor {

    void advancements(Advancements.Writer advancements);
}
