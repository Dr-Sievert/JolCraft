package net.sievert.jolcraft.module.primitive.loot;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

/** The bookkeeping; the game's registries are frozen here, so scenes in the game-test mod roll real tables with a real modifier. */
class ModLootTest {

    record Always() implements LootItemCondition {
        static final MapCodec<Always> CODEC = MapCodec.unit(Always::new);

        @Override
        public MapCodec<Always> codec() {
            return CODEC;
        }

        @Override
        public boolean test(LootContext context) {
            return true;
        }
    }

    private final LootKey<Always> always = LootKey.condition(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "always"), Always.CODEC);
    private final LootKey<Always> never = LootKey.condition(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "never"), MapCodec.unit(Always::new));
    private final IEventBus bus = BusBuilder.builder().build();

    @Test
    void kindsAreCountedOncePerIdAndCarryTheirCodec() {
        ModLoot loot = new ModLoot();
        loot.declare(always);
        loot.declare(never);
        Assertions.assertEquals(2, loot.declared());
        Assertions.assertTrue(Assertions.assertThrows(IllegalStateException.class, () -> loot.declare(always)).getMessage().contains("declared twice"), "the same key twice");
        Assertions.assertTrue(Assertions.assertThrows(IllegalStateException.class, () -> loot.declare(LootKey.condition(always.id(), MapCodec.unit(Always::new)))).getMessage().contains("declared twice"), "one id twice, whatever the codec");
        Assertions.assertSame(Always.CODEC, always.codec());
        Assertions.assertEquals(LootKey.Kind.CONDITION, always.kind());
        Assertions.assertFalse(always.isRegistered(), "nothing registered it");
    }

    @Test
    void oneIdMayNameAKindInEachRegistryButOneCodecMayNotServeTwoKeysOfAKind() {
        ModLoot loot = new ModLoot();
        MapCodec<Always> other = MapCodec.unit(Always::new);
        loot.declare(always);
        loot.declare(LootKey.function(always.id(), MapCodec.unit(() -> new Seven())));
        Assertions.assertEquals(2, loot.declared(), "a condition and a function may share an id, they live in different registries");
        Assertions.assertTrue(Assertions.assertThrows(IllegalStateException.class, () -> loot.declare(LootKey.condition(never.id(), Always.CODEC))).getMessage().contains("shares its codec"), "the same codec instance twice");
        Assertions.assertDoesNotThrow(() -> loot.declare(LootKey.condition(never.id(), other)));
        Assertions.assertEquals(LootKey.Kind.MODIFIER, LootKey.modifier(always.id(), MapCodec.unit(() -> null)).kind());
        Assertions.assertEquals(LootKey.Kind.FUNCTION, LootKey.function(always.id(), MapCodec.unit(() -> new Seven())).kind());
    }

    record Seven() implements net.minecraft.world.level.storage.loot.functions.LootItemFunction {
        @Override
        public MapCodec<Seven> codec() {
            return MapCodec.unit(Seven::new);
        }

        @Override
        public net.minecraft.world.item.ItemStack apply(net.minecraft.world.item.ItemStack stack, LootContext context) {
            stack.setCount(7);
            return stack;
        }
    }

    @Test
    void theGamesNamespaceIsRefused() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ModLoot().declare(LootKey.condition(Identifier.withDefaultNamespace("always"), Always.CODEC)));
    }

    @Test
    void registeringTwiceOrDeclaringAfterItIsRefused() {
        ModLoot loot = new ModLoot();
        loot.declare(always);
        loot.register(bus);
        Assertions.assertThrows(IllegalStateException.class, () -> loot.register(bus));
        Assertions.assertThrows(IllegalStateException.class, () -> loot.declare(never));
    }

    @Test
    void theIdIsTheFolderName() {
        Assertions.assertEquals(Dictionary.LOOT, new ModLoot().id());
    }
}
