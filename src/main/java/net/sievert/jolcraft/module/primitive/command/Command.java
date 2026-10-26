package net.sievert.jolcraft.module.primitive.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.IEventBus;

import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.Primitive;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The mod's commands. A module declares its command {@link Tree}s while it is built, each a Brigadier literal with
 * what hangs under it, built with the context the game offers when it registers commands; the root calls
 * {@link #register(IEventBus)} once. Trees that share a root literal merge the way the game merges them: the
 * branches join under the root that was declared first, which keeps its own requirement, while a command on a later root replaces its command; so a shared
 * root carries no requirement or command of its own, and each branch carries what it requires. The game asks again on every
 * reload, and every tree is built again.
 */
@Primitive(Dictionary.COMMAND)
public interface Command extends Module {

    /** Declares a tree. Only while modules are built. */
    void declare(Tree tree);

    /** Attaches to the game bus: when the game registers commands, every declared tree is built and registered. The root calls it once. */
    void register(IEventBus gameBus);

    /** A command tree, built when the game registers commands. */
    @FunctionalInterface
    interface Tree {

        LiteralArgumentBuilder<CommandSourceStack> build(CommandBuildContext context);
    }
}
