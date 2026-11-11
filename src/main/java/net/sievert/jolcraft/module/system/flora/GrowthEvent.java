package net.sievert.jolcraft.module.system.flora;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import net.sievert.jolcraft.stereotype.Event;

/**
 * A question the game asks about a growing plant, with the number vanilla would use, which a listener may change.
 * Posted by the crop and mushroom mixins on the game bus. It may be posted on a worldgen thread and on the client,
 * where placement is predicted, so a listener reads only the level the event hands it.
 */
@Event
public abstract class GrowthEvent extends net.neoforged.bus.api.Event {

    private final BlockGetter level;
    private final BlockPos pos;

    GrowthEvent(BlockGetter level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    /** The level the question was asked in. */
    public BlockGetter level() {
        return level;
    }

    /** The plant's position. */
    public BlockPos pos() {
        return pos;
    }

    /** The block state under the plant. */
    public BlockState below() {
        return level.getBlockState(pos.below());
    }

    /** The raw brightness a crop reads, the light it grows and survives by. */
    public static final class Light extends GrowthEvent {

        private int brightness;

        public Light(BlockGetter level, BlockPos pos, int brightness) {
            super(level, pos);
            this.brightness = brightness;
        }

        public int brightness() {
            return brightness;
        }

        /** Raises the brightness to at least {@code minimum}; never lowers it. */
        public void atLeast(int minimum) {
            brightness = Math.max(brightness, minimum);
        }
    }

    /** A crop's growth speed, which a listener may scale. */
    public static final class Speed extends GrowthEvent {

        private float speed;

        public Speed(BlockGetter level, BlockPos pos, float speed) {
            super(level, pos);
            this.speed = speed;
        }

        public float speed() {
            return speed;
        }

        public void scale(float factor) {
            speed *= factor;
        }
    }

    /** A mushroom's chance to spread on a random tick; the game's is 1 in 25. */
    public static final class Spread extends GrowthEvent {

        /** The game's own chance, one in 25. */
        public static final float VANILLA = 1.0F / 25.0F;

        private float chance = VANILLA;

        public Spread(BlockGetter level, BlockPos pos) {
            super(level, pos);
        }

        public float chance() {
            return chance;
        }

        public void setChance(float chance) {
            this.chance = chance;
        }

        /** Whether a listener moved the chance from the game's own. */
        public boolean changed() {
            return chance != VANILLA;
        }
    }

    /** A mushroom's chance to grow huge from bonemeal; the game's is 0.4. */
    public static final class Bonemeal extends GrowthEvent {

        /** The game's own chance. */
        public static final float VANILLA = 0.4F;

        private float chance = VANILLA;

        public Bonemeal(BlockGetter level, BlockPos pos) {
            super(level, pos);
        }

        public float chance() {
            return chance;
        }

        public void setChance(float chance) {
            this.chance = chance;
        }
    }
}
