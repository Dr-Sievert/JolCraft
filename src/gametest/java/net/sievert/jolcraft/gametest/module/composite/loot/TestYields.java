package net.sievert.jolcraft.gametest.module.composite.loot;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.module.composite.loot.Result;
import net.sievert.jolcraft.module.composite.loot.ResultHook;
import net.sievert.jolcraft.module.composite.loot.Yield;
import net.sievert.jolcraft.module.composite.loot.YieldKind;
import net.sievert.jolcraft.module.composite.loot.YieldKinds;
import net.sievert.jolcraft.module.composite.loot.Yielded;
import net.sievert.jolcraft.module.primitive.registration.Ids;

/**
 * The game-test mod's yield registries, a kind of its own, and two hooks: the game-test mod declares them in its root,
 * and its data pack holds a recipe carrying yields and one naming a hook nobody declared.
 */
public final class TestYields {

    public static final YieldKinds KINDS = YieldKinds.of(Ids.of(JolCraftGameTests.MOD_ID, "yield"));
    /** Rewrites every rolled stack into as many gold nuggets. */
    public static final Identifier GILDING = Ids.of(JolCraftGameTests.MOD_ID, "gilding");
    /** Refuses every delivery it is named in. */
    public static final Identifier REFUSING = Ids.of(JolCraftGameTests.MOD_ID, "refusing");

    /** A kind of the game-test mod's own, yielding a number. */
    public record TallyYield(int number, List<Identifier> hooks) implements Yield {
        public static final MapCodec<TallyYield> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                com.mojang.serialization.Codec.INT.fieldOf("number").forGetter(TallyYield::number),
                Identifier.CODEC.listOf().optionalFieldOf("hooks", List.of()).forGetter(TallyYield::hooks)).apply(instance, TallyYield::new));

        @Override
        public MapCodec<TallyYield> codec() {
            return CODEC;
        }

        @Override
        public List<? extends Yielded> roll(LootContext context) {
            return List.of(new Tally(number));
        }
    }

    public record Tally(int number) implements Yielded {
    }

    private TestYields() {
    }

    public static void declare(Result result) {
        result.declare(KINDS);
        result.declare(YieldKind.of(Ids.of(JolCraftGameTests.MOD_ID, "tally"), TallyYield.CODEC));
        result.declare(GILDING, new ResultHook() {
            @Override
            public Optional<Yielded> apply(LootContext context, Yielded rolled) {
                return rolled instanceof Yielded.Stack(ItemStack stack) ? Optional.of(new Yielded.Stack(new ItemStack(Items.GOLD_NUGGET, stack.getCount()))) : Optional.of(rolled);
            }
        });
        result.declare(REFUSING, new ResultHook() {
            @Override
            public boolean canApply(LootContext context) {
                return false;
            }
        });
    }
}
