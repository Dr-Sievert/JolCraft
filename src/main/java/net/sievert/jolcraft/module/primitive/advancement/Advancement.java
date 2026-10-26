package net.sievert.jolcraft.module.primitive.advancement;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;

import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.Primitive;
import net.sievert.jolcraft.util.text.Dictionary;
import net.sievert.jolcraft.util.text.ModString;

/**
 * The mod's part in advancements: the kinds of criterion its features fire, declared as {@link TriggerKey}s while a
 * module is built and registered once by the root, and asking or granting a player's advancements by id. The
 * advancements themselves are data, written by datagen with a title and a description under the keys
 * {@link #title(Identifier)} and {@link #description(Identifier)} give, which datagen demands English for. The
 * game's own class of the same name, {@code net.minecraft.advancements.Advancement}, is datagen's concern.
 */
@Primitive(Dictionary.ADVANCEMENT)
public interface Advancement extends Module {

    /** The translation key of an advancement's title: {@code advancements.<namespace>.<path>.title}. */
    static String title(Identifier id) {
        return ModString.dotted(Dictionary.ADVANCEMENTS, id.getNamespace(), id.getPath(), Dictionary.TITLE);
    }

    /** The translation key of an advancement's description. */
    static String description(Identifier id) {
        return ModString.dotted(Dictionary.ADVANCEMENTS, id.getNamespace(), id.getPath(), Dictionary.DESCRIPTION);
    }

    /** Declares a kind of criterion. Only while modules are built, once per id. */
    void declare(TriggerKey<?> key);

    /** Registers every declared kind with the game. The root calls it once. */
    void register(IEventBus modBus);

    /** Whether the player has completed the advancement; false for one the data pack lacks. */
    boolean has(ServerPlayer player, Identifier advancement);

    /** Completes every criterion the player still lacks on the advancement; true when it is complete afterwards, false for one the data pack lacks or that the player could not be granted. */
    boolean grant(ServerPlayer player, Identifier advancement);
}
