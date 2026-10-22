package net.sievert.jolcraft.world.worldgen.structure.util;

import net.minecraft.world.level.block.Rotation;

import java.util.List;

public final class JolCraftStructureContext {

    private static final ThreadLocal<Boolean> ACTIVE =
            ThreadLocal.withInitial(() -> false);

    private static final ThreadLocal<Rotation> ROTATION =
            new ThreadLocal<>();

    private static final ThreadLocal<List<SinglePlacementPart>> SINGLE_PLACEMENT_PARTS =
            ThreadLocal.withInitial(List::of);

    private static final ThreadLocal<Integer> MIN_TERRAIN_COVER =
            ThreadLocal.withInitial(() -> 0);

    private JolCraftStructureContext() {}

    public static void activate(List<SinglePlacementPart> singlePlacementParts) {
        ACTIVE.set(true);
        SINGLE_PLACEMENT_PARTS.set(List.copyOf(singlePlacementParts));
    }

    public static boolean isActive() {
        return ACTIVE.get();
    }

    public static void setRotation(Rotation rotation) {
        ROTATION.set(rotation);
    }

    public static Rotation getRotation() {
        return ROTATION.get();
    }

    public static List<SinglePlacementPart> getSinglePlacementParts() {
        return SINGLE_PLACEMENT_PARTS.get();
    }

    public static void setMinTerrainCover(int blocks) {
        MIN_TERRAIN_COVER.set(blocks);
    }

    /**
     * Blocks of terrain required above the roof of a buried jigsaw piece before it may be placed.
     * Zero disables the check.
     */
    public static int getMinTerrainCover() {
        return MIN_TERRAIN_COVER.get();
    }

    public static void clear() {
        ACTIVE.remove();
        ROTATION.remove();
        SINGLE_PLACEMENT_PARTS.remove();
        MIN_TERRAIN_COVER.remove();
    }
}