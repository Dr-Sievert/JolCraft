package net.sievert.jolcraft.module.system.flora;

import java.util.List;

import net.sievert.jolcraft.stereotype.Value;

/**
 * The shape of a huge mushroom, in cells around the block it grows from: a stem {@code stem} blocks high, one tip block
 * on top of it, and rings of cap. Pure arithmetic, so the shape can be pinned without a world.
 */
@Value
public record MushroomShape(int stem, List<Ring> rings) {

    /** One ring of cap: the outline of the square of {@code radius} around the stem's column, {@code height} above the origin, with or without its four corners. */
    public record Ring(int height, int radius, boolean corners) {

        /** Whether the cell at this offset from the stem's column is a block of the ring. */
        public boolean contains(int dx, int dz) {
            int x = Math.abs(dx);
            int z = Math.abs(dz);
            if (Math.max(x, z) != radius) {
                return false;
            }
            return corners || x != radius || z != radius;
        }

        /** How many blocks the ring has. */
        public int count() {
            return corners ? 8 * radius : 8 * radius - 4;
        }
    }

    /** Whether the cell at this offset from the origin belongs to the mushroom: its stem, its tip, or a block of a ring. */
    public boolean contains(int dx, int dy, int dz) {
        if (dx == 0 && dz == 0) {
            return dy >= 0 && dy <= stem;
        }
        for (Ring ring : rings) {
            if (ring.height() == dy && ring.contains(dx, dz)) {
                return true;
            }
        }
        return false;
    }

    /** The largest radius of any ring at this height above the origin; 0 where there is none, the stem's column alone. */
    public int radiusAt(int dy) {
        int radius = 0;
        for (Ring ring : rings) {
            if (ring.height() == dy) {
                radius = Math.max(radius, ring.radius());
            }
        }
        return radius;
    }

    /** The whole height: the stem and the tip above it. */
    public int height() {
        return stem + 1;
    }
}
