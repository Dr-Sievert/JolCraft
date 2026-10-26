package net.sievert.jolcraft.gametest.module.primitive.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.module.primitive.loot.Loot;
import net.sievert.jolcraft.module.primitive.loot.LootKey;
import net.sievert.jolcraft.module.primitive.registration.Ids;

/** The loot kinds the loot scenes use; the game-test mod declares them in its root, and its data pack applies the modifier to the dungeon chest and, through a second file, to a table that does not exist. */
public final class TestLoot {

    /** A condition that always holds. */
    public record Always() implements LootItemCondition {
        public static final MapCodec<Always> CODEC = MapCodec.unit(Always::new);

        @Override
        public MapCodec<Always> codec() {
            return CODEC;
        }

        @Override
        public boolean test(LootContext context) {
            return true;
        }
    }

    /** Adds a nether star to whatever was rolled. */
    public static final class AddStar extends LootModifier {
        public static final MapCodec<AddStar> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, AddStar::new));

        public AddStar(LootItemCondition[] conditions, int priority) {
            super(conditions, priority);
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
            generatedLoot.add(new ItemStack(Items.NETHER_STAR));
            return generatedLoot;
        }

        @Override
        public MapCodec<? extends IGlobalLootModifier> codec() {
            return ADD_STAR.codec();
        }
    }

    /** A function that sets the count to seven. */
    public record Seven() implements LootItemFunction {
        public static final MapCodec<Seven> CODEC = MapCodec.unit(Seven::new);

        @Override
        public MapCodec<Seven> codec() {
            return CODEC;
        }

        @Override
        public ItemStack apply(ItemStack stack, LootContext context) {
            stack.setCount(7);
            return stack;
        }
    }

    public static final LootKey<Seven> SEVEN = LootKey.function(Ids.of(JolCraftGameTests.MOD_ID, "seven"), Seven.CODEC);
    public static final LootKey<Always> ALWAYS = LootKey.condition(Ids.of(JolCraftGameTests.MOD_ID, "always"), Always.CODEC);
    public static final LootKey<AddStar> ADD_STAR = LootKey.modifier(Ids.of(JolCraftGameTests.MOD_ID, "add_star"), AddStar.CODEC);

    private TestLoot() {
    }

    public static void declare(Loot loot) {
        loot.declare(ALWAYS);
        loot.declare(SEVEN);
        loot.declare(ADD_STAR);
    }
}
