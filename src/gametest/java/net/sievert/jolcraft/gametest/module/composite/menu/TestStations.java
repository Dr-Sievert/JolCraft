package net.sievert.jolcraft.gametest.module.composite.menu;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.module.composite.capability.Inventory;
import net.sievert.jolcraft.module.composite.menu.Station;
import net.sievert.jolcraft.module.composite.menu.StationBlock;
import net.sievert.jolcraft.module.composite.menu.StationBlockEntity;
import net.sievert.jolcraft.module.composite.menu.StationEvent;
import net.sievert.jolcraft.module.composite.menu.StationKind;
import net.sievert.jolcraft.module.composite.menu.StationLayout;
import net.sievert.jolcraft.module.composite.menu.StationMenu;
import net.sievert.jolcraft.module.composite.menu.Tick;
import net.sievert.jolcraft.module.primitive.language.LanguageKey;
import net.sievert.jolcraft.module.primitive.registration.Ids;

/**
 * The game-test mod's two stations: a bench (two inputs, a tool cell taking sticks, an output, one data value that depends
 * on the viewer, a button that moves one item from an input to the output and toggles a block property, synced items and a
 * tick counter) and a crate (a 9 by 2 storage grid whose contents are not dropped). The game-test mod declares the kinds
 * and exposes the block entities in its root, which also listens to the station events and keeps them for the scenes.
 */
public final class TestStations {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(JolCraftGameTests.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, JolCraftGameTests.MOD_ID);

    /** Cells 0 and 1 are inputs, 2 the tool, 3 the output. */
    public static final StationKind<StationMenu> BENCH = StationKind.of(Ids.of(JolCraftGameTests.MOD_ID, "bench"),
            StationLayout.of(4).input(2, 3).input(4, 3).tool(6, 3, stack -> stack.is(Items.STICK), Ids.path(JolCraftGameTests.MOD_ID, "station/stick")).output(9, 3),
            LanguageKey.of("container", JolCraftGameTests.MOD_ID, "bench")).data(1).syncedItems();
    public static final StationKind<StationMenu> CRATE = StationKind.of(Ids.of(JolCraftGameTests.MOD_ID, "crate"),
            StationLayout.of(4).grid(2, 9, 8, 18), LanguageKey.of("container", JolCraftGameTests.MOD_ID, "crate"));

    public static final DeferredBlock<BenchBlock> BENCH_BLOCK = BLOCKS.registerBlock("bench", BenchBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BenchBlockEntity>> BENCH_ENTITY = BLOCK_ENTITIES.register("bench",
            () -> new BlockEntityType<>(BenchBlockEntity::new, BENCH_BLOCK.get()));
    public static final DeferredBlock<CrateBlock> CRATE_BLOCK = BLOCKS.registerBlock("crate", CrateBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrateBlockEntity>> CRATE_ENTITY = BLOCK_ENTITIES.register("crate",
            () -> new BlockEntityType<>(CrateBlockEntity::new, CRATE_BLOCK.get()));

    private static final List<StationEvent> EVENTS = new ArrayList<>();
    private static boolean deliverOnce;

    private TestStations() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

    public static void declare(Station station, Inventory inventory) {
        station.declare(BENCH);
        station.declare(CRATE);
        inventory.exposeBlockEntity(BENCH_ENTITY);
        inventory.exposeBlockEntity(CRATE_ENTITY);
    }

    /** The station events posted since the last {@link #clearEvents()}, in order, as the root's listeners saw them. */
    public static List<StationEvent> events() {
        return EVENTS;
    }

    public static void clearEvents() {
        EVENTS.clear();
        deliverOnce = false;
    }

    /**
     * The dev root's listener for the bench's button 0: moves one item from the first non-empty input into the output,
     * drops what the output could not take, toggles the block's {@code LIT} property and handles the event.
     */
    public static void onButton(StationEvent.Button event) {
        EVENTS.add(event);
        StationBlockEntity station = event.station();
        if (station.kind() != BENCH || event.id() != 0 || !(station.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = station.getBlockPos();
        for (int cell = 0; cell < 2; cell++) {
            ItemStack taken = station.removeItem(cell, 1);
            if (!taken.isEmpty()) {
                ItemStack left = station.deliver(3, taken);
                if (!left.isEmpty()) {
                    Block.popResource(level, pos, left);
                }
                break;
            }
        }
        BlockState state = level.getBlockState(pos);
        level.setBlock(pos, state.setValue(BlockStateProperties.LIT, !state.getValue(BlockStateProperties.LIT)), Block.UPDATE_ALL);
        event.setHandled();
    }

    /** The dev root's listener for changes: keeps the event for the scenes. */
    public static void onChanged(StationEvent.Changed event) {
        EVENTS.add(event);
        if (deliverOnce && event.station().kind() == BENCH) {
            deliverOnce = false;
            event.station().deliver(3, new ItemStack(Items.STONE));
        }
    }

    /** Makes the next change of a bench write a stone into its output from inside the event, once. */
    public static void deliverOnNextChange() {
        deliverOnce = true;
    }

    public static final class BenchBlock extends StationBlock {

        private static final MapCodec<BenchBlock> CODEC = simpleCodec(BenchBlock::new);

        BenchBlock(BlockBehaviour.Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.LIT, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(BlockStateProperties.LIT);
        }

        @Override
        protected @NotNull MapCodec<BenchBlock> codec() {
            return CODEC;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BenchBlockEntity(pos, state);
        }

        @Override
        protected BlockEntityType<? extends StationBlockEntity> type() {
            return BENCH_ENTITY.get();
        }
    }

    public static final class BenchBlockEntity extends StationBlockEntity implements Tick {

        private int ticks;

        public BenchBlockEntity(BlockPos pos, BlockState state) {
            super(BENCH_ENTITY.get(), pos, state, BENCH);
        }

        @Override
        public void tickServer(ServerLevel level) {
            ticks++;
        }

        public int ticks() {
            return ticks;
        }

        @Override
        protected int readData(int index, Player viewer) {
            return viewer.getName().getString().length();
        }
    }

    public static final class CrateBlock extends StationBlock {

        private static final MapCodec<CrateBlock> CODEC = simpleCodec(CrateBlock::new);

        CrateBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected @NotNull MapCodec<CrateBlock> codec() {
            return CODEC;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CrateBlockEntity(pos, state);
        }

        @Override
        protected BlockEntityType<? extends StationBlockEntity> type() {
            return CRATE_ENTITY.get();
        }
    }

    public static final class CrateBlockEntity extends StationBlockEntity {

        public CrateBlockEntity(BlockPos pos, BlockState state) {
            super(CRATE_ENTITY.get(), pos, state, CRATE);
        }

        @Override
        protected boolean dropsContents() {
            return false;
        }
    }
}
