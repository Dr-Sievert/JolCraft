package net.sievert.jolcraft.module.composite.menu;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import net.sievert.jolcraft.stereotype.Value;

/**
 * The shape of a station: a panel {@value #WIDTH} pixels wide and {@code height} tiles of {@value #TILE} pixels high, and
 * an ordered list of {@link Cell}s, each one slot. A cell's place in the list is its container slot index and its saved
 * slot number, so the block entity, the menu and the screen agree by construction; cells are append-only once a station
 * has shipped. Built by {@link #of} and the cell builders, each returning a new layout, and checked by
 * {@link #check()} when the station is declared. The {@link Role} of a cell decides every rule written once for all
 * stations: what may be placed, what a hopper may take, where a shift-click goes.
 */
@Value
public record StationLayout(int height, List<Cell> cells) {

    /** The panel's width in pixels: eleven tiles, the vanilla inventory strip's width. */
    public static final int WIDTH = 176;
    /** One tile in pixels. */
    public static final int TILE = 16;
    /** A slot's frame in pixels: the 16 pixel slot and a pixel around it. */
    public static final int FRAME = 18;
    /** The least height of a panel in tiles. */
    public static final int MIN_HEIGHT = 3;

    public StationLayout {
        cells = List.copyOf(cells);
    }

    /** The pixel position of the 1-based tile {@code n}; refuses a tile below the first. */
    public static int tile(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("tiles count from 1, not " + n);
        }
        return (n - 1) * TILE;
    }

    /** A layout of the given height in tiles with no cells. */
    public static StationLayout of(int height) {
        return new StationLayout(height, List.of());
    }

    /** One more input cell at the tiles, taking anything. */
    public StationLayout input(int tileX, int tileY) {
        return input(tileX, tileY, _ -> true);
    }

    /** One more input cell at the tiles, taking what {@code accepts} says. */
    public StationLayout input(int tileX, int tileY, Predicate<ItemStack> accepts) {
        return with(new Cell(tile(tileX), tile(tileY), Role.INPUT, accepts, Optional.empty()));
    }

    /** One more tool cell at the tiles, taking what {@code accepts} says, its empty slot showing the hint sprite when there is one. */
    public StationLayout tool(int tileX, int tileY, Predicate<ItemStack> accepts, @Nullable Identifier hint) {
        return with(new Cell(tile(tileX), tile(tileY), Role.TOOL, accepts, Optional.ofNullable(hint)));
    }

    /** One more output cell at the tiles; it takes nothing from a player or a hopper. */
    public StationLayout output(int tileX, int tileY) {
        return with(new Cell(tile(tileX), tile(tileY), Role.OUTPUT, _ -> false, Optional.empty()));
    }

    /** One more storage cell at the tiles, taking anything. */
    public StationLayout storage(int tileX, int tileY) {
        return with(new Cell(tile(tileX), tile(tileY), Role.STORAGE, _ -> true, Optional.empty()));
    }

    /** A grid of storage cells, row by row, the first at the pixel {@code left}, {@code top} and the next every {@value #FRAME} pixels. */
    public StationLayout grid(int rows, int columns, int left, int top) {
        StationLayout layout = this;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                layout = layout.with(new Cell(left + FRAME * column, top + FRAME * row, Role.STORAGE, _ -> true, Optional.empty()));
            }
        }
        return layout;
    }

    private StationLayout with(Cell cell) {
        List<Cell> grown = new ArrayList<>(cells);
        grown.add(cell);
        return new StationLayout(height, grown);
    }

    /** The number of cells, which is the container's size. */
    public int size() {
        return cells.size();
    }

    /** The cell at the container slot index. */
    public Cell cell(int index) {
        return cells.get(index);
    }

    /** The panel's height in pixels. */
    public int panelHeight() {
        return height * TILE;
    }

    /**
     * Refuses a layout no station can have, naming the fault: a height under {@value #MIN_HEIGHT}, no cells, a cell whose
     * frame leaves the panel, two frames overlapping, or the cells of one role not next to each other.
     */
    public void check() {
        if (height < MIN_HEIGHT) {
            throw new IllegalArgumentException("a panel is at least " + MIN_HEIGHT + " tiles high, not " + height);
        }
        if (cells.isEmpty()) {
            throw new IllegalArgumentException("a station has at least one cell");
        }
        for (int index = 0; index < cells.size(); index++) {
            Cell cell = cells.get(index);
            if (cell.x() - 1 < 0 || cell.x() + FRAME - 1 > WIDTH || cell.y() - 1 < 0 || cell.y() + FRAME - 1 > panelHeight()) {
                throw new IllegalArgumentException("cell " + index + " at " + cell.x() + ", " + cell.y() + " has its frame outside the panel of "
                        + WIDTH + " by " + panelHeight() + " pixels");
            }
            for (int other = 0; other < index; other++) {
                Cell before = cells.get(other);
                if (Math.abs(cell.x() - before.x()) < FRAME && Math.abs(cell.y() - before.y()) < FRAME) {
                    throw new IllegalArgumentException("cells " + other + " and " + index + " overlap");
                }
            }
        }
        runs();
    }

    /**
     * The contiguous index ranges {@code [start, end)} of each role present, ordered tool, input, storage and never output:
     * the order a shift-click from a player slot tries them. Refuses a role whose cells are not next to each other.
     */
    public List<Run> runs() {
        Map<Role, Run> found = new EnumMap<>(Role.class);
        int index = 0;
        while (index < cells.size()) {
            Role role = cells.get(index).role();
            int end = index;
            while (end < cells.size() && cells.get(end).role() == role) {
                end++;
            }
            if (found.put(role, new Run(role, index, end)) != null) {
                throw new IllegalArgumentException("the " + role + " cells are not contiguous: a second run starts at " + index);
            }
            index = end;
        }
        List<Run> ordered = new ArrayList<>();
        for (Role role : List.of(Role.TOOL, Role.INPUT, Role.STORAGE)) {
            if (found.containsKey(role)) {
                ordered.add(found.get(role));
            }
        }
        return ordered;
    }

    /** The cells of one role from {@code start} up to, not including, {@code end}. */
    public record Run(Role role, int start, int end) {
    }

    /** One slot: its pixel position inside the panel, its role, what it accepts and the sprite of its empty slot. */
    public record Cell(int x, int y, Role role, Predicate<ItemStack> accepts, Optional<Identifier> hint) {

        /** Whether the stack may be placed here: the role allows placing and the cell's predicate agrees. */
        public boolean accepts(ItemStack stack) {
            return role.placeable() && accepts.test(stack);
        }
    }

    /** What a cell is for; the rules that follow from it are methods here, never a switch elsewhere. */
    public enum Role {
        INPUT(true, false, false, false),
        TOOL(true, false, false, false),
        OUTPUT(false, true, true, true),
        STORAGE(true, true, true, false);

        private final boolean placeable;
        private final boolean extractable;
        private final boolean backwards;
        private final boolean result;

        Role(boolean placeable, boolean extractable, boolean backwards, boolean result) {
            this.placeable = placeable;
            this.extractable = extractable;
            this.backwards = backwards;
            this.result = result;
        }

        /** Whether a player or a hopper may put stacks here. */
        public boolean placeable() {
            return placeable;
        }

        /** Whether a hopper may take from here through a face. */
        public boolean extractable() {
            return extractable;
        }

        /** Whether a shift-click from here moves to the player from the end of the strip. */
        public boolean backwards() {
            return backwards;
        }

        /** Whether taking from here is crafting: counted and announced, and left out of a pick-all. */
        public boolean result() {
            return result;
        }
    }
}
