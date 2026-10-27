package net.sievert.jolcraft.module.primitive.loot;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.IEventBus;

import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.Primitive;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The mod's part in loot: the kinds of loot modifier, condition and function it adds to the data pack's
 * vocabulary, declared as {@link LootKey}s while a module is built and registered once by the root, and rolling a
 * table of the data pack for a level. The tables themselves are data, written by datagen and named by their key.
 * It also builds the loot context a {@link Shape} describes, through {@code context}, so the data a shape validates
 * is fired with a context of the same shape.
 */
@Primitive(Dictionary.LOOT)
public interface Loot extends Module {

    /** Declares a kind. Only while modules are built, once per id. */
    void declare(LootKey<?> key);

    /** Registers every declared kind with the game. The root calls it once. */
    void register(IEventBus modBus);

    /** Rolls a table of the level's data pack with the params, every loaded modifier applied; nothing at all for a table the data pack lacks, not even the modifiers' additions. */
    List<ItemStack> roll(ServerLevel level, ResourceKey<LootTable> table, LootParams params);

    /**
     * A loot context for the level under the shape, rolling with the level's random. What {@code params} provides is
     * checked against the shape: a required key missing is refused naming the shape, so a host whose shape and
     * context disagree learns it at the first call, not from a condition that silently fails. A key beyond the shape
     * is carried: NeoForge lets a context carry more than its set names, so a shape bounds what data may read, not
     * what a caller may give.
     */
    LootContext context(ServerLevel level, Shape shape, Consumer<LootParams.Builder> params);

    /** The same with a random source of the caller's, for a roll that must repeat. */
    LootContext context(ServerLevel level, Shape shape, RandomSource random, Consumer<LootParams.Builder> params);
}
