package net.sievert.jolcraft.module.composite.loot;

import java.util.List;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextUser;

import net.sievert.jolcraft.stereotype.Contract;

/**
 * One data-described thing a result yields: an item pool, an entity, a sound, an effect, or a kind of a module's own.
 * A yield is a value read by the codec its kind registered; it validates itself against the shape of the context it
 * will be rolled with, as the game's loot does, and {@link #roll} answers values without acting on the world: the
 * system that asked spawns, plays, applies or hands over. The hooks it names by id run on each rolled value.
 */
@Contract
public interface Yield extends LootContextUser {

    /** The codec of this yield's kind, the one its kind registered. */
    MapCodec<? extends Yield> codec();

    /** The hooks run on each rolled value, by the ids a module declared them under; a kind that names none leaves this. */
    default List<Identifier> hooks() {
        return List.of();
    }

    /** What this yield yields against the context: nothing, one value or several, never acting on the world. */
    List<? extends Yielded> roll(LootContext context);
}
