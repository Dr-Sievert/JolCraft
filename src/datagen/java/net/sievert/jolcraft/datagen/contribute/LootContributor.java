package net.sievert.jolcraft.datagen.contribute;

import net.sievert.jolcraft.datagen.provider.Loot;

/** A module's loot tables. Every block of the mod needs one unless Handmade exempts it; so does every entity that drops. */
public interface LootContributor {

    default void blockLoot(Loot.Blocks loot) {
    }

    default void entityLoot(Loot.Entities loot) {
    }
}
