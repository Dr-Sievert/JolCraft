package gametest;

import java.util.function.Consumer;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Dev-only mod carrying the game tests (see src/gametest/resources/META-INF/neoforge.mods.toml).
 * Test logic is a registered test function ({@link BuiltInRegistries#TEST_FUNCTION}); each test
 * instance is registered in code through {@link RegisterGameTestsEvent}, so no test_instance
 * JSON is needed. runGameTestServer runs every registered instance and exits non-zero on failure.
 */
@Mod(JolCraftGameTests.MOD_ID)
public final class JolCraftGameTests {
    public static final String MOD_ID = "jolcraft_gametest";

    /** Vanilla's empty template, data/minecraft/structure/empty.nbt. */
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");

    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, MOD_ID);

    /** Smoke test: the dev environment boots a server and runs a test. */
    private static final DeferredHolder<Consumer<GameTestHelper>, @org.jetbrains.annotations.NotNull Consumer<GameTestHelper>> SMOKE =
            TEST_FUNCTIONS.register("smoke", () -> GameTestHelper::succeed);

    public JolCraftGameTests(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(JolCraftGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("default"));
        event.registerTest(id("smoke"), new FunctionGameTestInstance(
                SMOKE.getKey(), new TestData<>(environment, EMPTY_STRUCTURE, 20, 0, true)));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
