package net.sievert.jolcraft.datagen.contribute;

import net.sievert.jolcraft.datagen.provider.Loot;
import net.sievert.jolcraft.datagen.provider.LootModifiers;

/**
 * A module's loot tables. Every block of the mod needs one unless it is built with {@code noLootTable()} (then adding one is refused) or Handmade exempts it; so does every entity type (an empty table is fine) unless it is built with {@code noLootTable()} or Handmade exempts it.
 * The tables that are neither a block's nor an entity type's, a chest, a fishing table or a table a loot modifier adds, go through {@link #tables}, each under its own context.
 */
public interface LootContributor {

    default void blockLoot(Loot.Blocks loot) {
    }

    default void entityLoot(Loot.Entities loot) {
    }

    /** Tables under any context: a chest, a fishing table, a table a loot modifier adds. */
    default void tables(Loot.Tables tables) {
    }

    /** The module's global loot modifiers, each under its name with the conditions it applies under. */
    default void modifiers(LootModifiers modifiers) {
    }
}
