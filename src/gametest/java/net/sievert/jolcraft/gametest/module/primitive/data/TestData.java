package net.sievert.jolcraft.gametest.module.primitive.data;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.saveddata.SavedData;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.module.primitive.data.Data;
import net.sievert.jolcraft.module.primitive.data.DataKey;
import net.sievert.jolcraft.module.primitive.registration.Ids;

/** The saved data the data scenes use: a counter kept per level and one kept for the server; the game-test mod declares them in its root. */
public final class TestData {

    /** A count; changing it does not mark the data dirty on purpose, {@code DataKey.update} does. */
    public static final class Counter extends SavedData {
        public static final Codec<Counter> CODEC = Codec.INT.fieldOf("count").codec().xmap(Counter::new, Counter::count);
        private int count;

        public Counter() {
        }

        public Counter(int count) {
            this.count = count;
        }

        public int count() {
            return count;
        }

        public void add(int amount) {
            count += amount;
        }
    }

    public static final DataKey<Counter> LOCAL = DataKey.of(Ids.of(JolCraftGameTests.MOD_ID, "local"), Counter::new, Counter.CODEC);
    public static final DataKey<Counter> SHARED = DataKey.of(Ids.of(JolCraftGameTests.MOD_ID, "shared"), Counter::new, Counter.CODEC).serverWide();

    private TestData() {
    }

    public static void declare(Data data) {
        data.declare(LOCAL);
        data.declare(SHARED);
    }
}
