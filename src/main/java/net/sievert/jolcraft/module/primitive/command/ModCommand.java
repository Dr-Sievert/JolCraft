package net.sievert.jolcraft.module.primitive.command;

import java.util.ArrayList;
import java.util.List;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import net.sievert.jolcraft.stereotype.Implementation;

/** {@link Command} over the game's command registration: the trees are kept until the game asks for them. */
@Implementation
public final class ModCommand implements Command {

    private final List<Tree> declared = new ArrayList<>();
    private boolean registered;

    @Override
    public void declare(Tree tree) {
        if (registered) {
            throw new IllegalStateException("a command tree was declared after the commands were attached to the game bus");
        }
        declared.add(tree);
    }

    @Override
    public void register(IEventBus gameBus) {
        if (registered) {
            throw new IllegalStateException("the commands were attached to the game bus twice");
        }
        registered = true;
        gameBus.addListener(RegisterCommandsEvent.class, event -> declared.forEach(tree -> event.getDispatcher().register(tree.build(event.getBuildContext()))));
    }

    /** How many trees are declared, for the root's log line. */
    public int declared() {
        return declared.size();
    }
}
