package net.sievert.jolcraft.gametest.module.primitive.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.module.primitive.command.Command;

/** Two branches under the game-test mod's name, declared by two calls so the scene proves they merge. */
public final class TestCommands {

    private TestCommands() {
    }

    public static void declare(Command command) {
        command.declare(_ -> Commands.literal(JolCraftGameTests.MOD_ID)
                .then(Commands.literal("ping").then(Commands.argument("count", IntegerArgumentType.integer(1))
                        .executes(source -> {
                            int count = IntegerArgumentType.getInteger(source, "count");
                            source.getSource().sendSuccess(() -> Component.literal("pong " + count), false);
                            return count;
                        }))));
        command.declare(_ -> Commands.literal(JolCraftGameTests.MOD_ID)
                .then(Commands.literal("echo").then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(source -> {
                            source.getSource().sendSuccess(() -> Component.literal(StringArgumentType.getString(source, "text")), false);
                            return 1;
                        }))));
    }
}
