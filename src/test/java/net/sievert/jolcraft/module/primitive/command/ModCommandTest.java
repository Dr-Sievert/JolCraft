package net.sievert.jolcraft.module.primitive.command;

import com.mojang.brigadier.CommandDispatcher;
import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

class ModCommandTest {

    private final IEventBus bus = BusBuilder.builder().build();

    @Test
    void declaredTreesAreRegisteredWhenTheGameAsksAndSharedRootsMerge() {
        ModCommand command = new ModCommand();
        List<CommandBuildContext> seen = new ArrayList<>();
        command.declare(context -> {
            seen.add(context);
            return Commands.literal(Dictionary.JOLCRAFT).then(Commands.literal("one").executes(_ -> 1));
        });
        command.declare(_ -> Commands.literal(Dictionary.JOLCRAFT).then(Commands.literal("two").executes(_ -> 2)));
        Assertions.assertEquals(2, command.declared());
        command.register(bus);
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        CommandBuildContext context = CommandBuildContext.simple(RegistryAccess.EMPTY, FeatureFlags.DEFAULT_FLAGS);
        bus.post(new RegisterCommandsEvent(dispatcher, Commands.CommandSelection.ALL, context));
        CommandNode<CommandSourceStack> root = dispatcher.getRoot().getChild(Dictionary.JOLCRAFT);
        Assertions.assertNotNull(root, "the shared root");
        Assertions.assertNotNull(root.getChild("one"));
        Assertions.assertNotNull(root.getChild("two"));
        Assertions.assertEquals(List.of(context), seen, "the tree was built with the event's context");

        // The game asks again on a reload: a fresh dispatcher, a fresh context, every tree built again.
        CommandDispatcher<CommandSourceStack> reloaded = new CommandDispatcher<>();
        CommandBuildContext again = CommandBuildContext.simple(RegistryAccess.EMPTY, FeatureFlags.DEFAULT_FLAGS);
        bus.post(new RegisterCommandsEvent(reloaded, Commands.CommandSelection.ALL, again));
        Assertions.assertNotNull(reloaded.getRoot().getChild(Dictionary.JOLCRAFT).getChild("two"));
        Assertions.assertEquals(List.of(context, again), seen, "built once per event, each with its own context");
    }

    @Test
    void attachingTwiceOrDeclaringAfterItIsRefused() {
        ModCommand command = new ModCommand();
        command.register(bus);
        Assertions.assertThrows(IllegalStateException.class, () -> command.register(bus));
        Assertions.assertThrows(IllegalStateException.class, () -> command.declare(_ -> Commands.literal("late")));
    }

    @Test
    void theIdIsTheFolderName() {
        Assertions.assertEquals(Dictionary.COMMAND, new ModCommand().id());
    }
}
