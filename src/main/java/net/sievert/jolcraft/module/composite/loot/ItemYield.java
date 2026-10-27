package net.sievert.jolcraft.module.composite.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;

import net.sievert.jolcraft.stereotype.Value;
import net.sievert.jolcraft.util.text.Dictionary;

/** Stacks rolled from a loot pool of the game's own form: rolls, entries with weights and functions, conditions. */
@Value
public record ItemYield(LootPool pool, List<Identifier> hooks) implements Yield {

    public static final MapCodec<ItemYield> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LootPool.CODEC.fieldOf(Dictionary.POOL).forGetter(ItemYield::pool),
            Identifier.CODEC.listOf().optionalFieldOf(Dictionary.HOOKS, List.of()).forGetter(ItemYield::hooks)).apply(instance, ItemYield::new));

    public ItemYield {
        Objects.requireNonNull(pool, "a pool");
        hooks = List.copyOf(hooks);
    }

    public static ItemYield of(LootPool.Builder pool, Identifier... hooks) {
        return new ItemYield(pool.build(), List.of(hooks));
    }

    @Override
    public MapCodec<ItemYield> codec() {
        return CODEC;
    }

    @Override
    public void validate(ValidationContext context) {
        Yield.super.validate(context);
        pool.validate(context.forField(Dictionary.POOL));
    }

    @Override
    public List<? extends Yielded> roll(LootContext context) {
        List<Yielded> rolled = new ArrayList<>();
        // The game splits what a table rolls to the item's stack size and drops what the level's features disable; so does this.
        pool.addRandomItems(LootTable.createStackSplitter(context.getLevel(), stack -> {
            if (!stack.isEmpty()) {
                rolled.add(new Yielded.Stack(stack));
            }
        }), context);
        return rolled;
    }
}
