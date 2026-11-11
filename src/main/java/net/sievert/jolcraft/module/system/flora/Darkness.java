package net.sievert.jolcraft.module.system.flora;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;

import net.sievert.jolcraft.stereotype.Helper;

/** The one place that knows what "dark" means for the plants that grow in the dark. */
@Helper
public final class Darkness {

    /** The highest raw brightness at which a block is still dark. */
    public static final int LIMIT = 8;

    private Darkness() {
    }

    /**
     * Whether the block at {@code pos} is dark: raw brightness {@link #LIMIT} or less. While a chunk is generated it is
     * not lit yet and brightness reads 15 everywhere, so in a {@link WorldGenRegion} being below the surface
     * heightmap stands in for darkness.
     */
    public static boolean at(LevelReader level, BlockPos pos) {
        if (level instanceof WorldGenRegion) {
            return pos.getY() < level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        }
        return level.getRawBrightness(pos, 0) <= LIMIT;
    }
}
