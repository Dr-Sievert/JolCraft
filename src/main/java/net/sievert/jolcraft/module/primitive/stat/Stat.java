package net.sievert.jolcraft.module.primitive.stat;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;

import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.Primitive;
import net.sievert.jolcraft.util.text.Dictionary;
import net.sievert.jolcraft.util.text.ModString;

/**
 * The mod's statistics: the counts the game keeps per player and shows on its statistics screen. A module declares
 * its {@link StatKey}s while it is built; the root calls {@link #register(IEventBus)} once; from then on a module
 * awards and reads a player's count here, with the key. Every statistic is named under {@link #name(Identifier)},
 * which datagen demands English for. The game's own class of the same name, {@code net.minecraft.stats.Stat}, is
 * what the key hands the game.
 */
@Primitive(Dictionary.STAT)
public interface Stat extends Module {

    /** The translation key of a statistic's name: {@code stat.<namespace>.<path>}, as the game names its own. */
    static String name(Identifier id) {
        return ModString.dotted(Dictionary.STAT, id.getNamespace(), id.getPath());
    }

    /** Declares a statistic. Only while modules are built, once per id. */
    void declare(StatKey key);

    /** Registers every declared statistic with the game. The root calls it once. */
    void register(IEventBus modBus);

    /** Adds to the player's count. */
    void award(ServerPlayer player, StatKey key, int amount);

    /** The player's count. */
    int value(ServerPlayer player, StatKey key);
}
