package net.sievert.jolcraft.gametest.module.composite.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.AdvancedContainerSetDataPayload;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestPlayer;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.composite.menu.StationBlockEntity;
import net.sievert.jolcraft.module.composite.menu.StationEvent;
import net.sievert.jolcraft.module.composite.menu.StationMenu;
import net.sievert.jolcraft.module.composite.menu.StationSlot;

/**
 * The station composite against the game-test mod's bench and crate: the menu a player is given, the slot rules and the
 * quick-move routes, the pick-all, per-viewer data, the button and change events, removal and drops, hoppers and the
 * exposed capability, save and load, the sync packet, the ticker and the comparator. Scenes with a player are isolated.
 */
public final class StationScenes {

    private static final BlockPos AT = new BlockPos(2, 2, 2);

    /** A floor under the player the scene adds, since a mock player falls like any other and its menu would close out of reach. */
    private static void floor(TestScene scene) {
        scene.setBlock(0, 0, 0, Blocks.STONE);
    }

    private static TestStations.BenchBlockEntity bench(TestScene scene, BlockPos at) {
        scene.level().setBlock(scene.helper().absolutePos(at), TestStations.BENCH_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        return scene.helper().getBlockEntity(at, TestStations.BenchBlockEntity.class);
    }

    private static TestStations.CrateBlockEntity crate(TestScene scene, BlockPos at) {
        scene.level().setBlock(scene.helper().absolutePos(at), TestStations.CRATE_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        return scene.helper().getBlockEntity(at, TestStations.CrateBlockEntity.class);
    }

    private static StationMenu open(TestPlayer player, StationBlockEntity station) {
        station.open(player.player());
        return (StationMenu) player.player().containerMenu;
    }

    private static long changedFor(StationBlockEntity station) {
        return TestStations.events().stream().filter(event -> event instanceof StationEvent.Changed && event.station() == station).count();
    }

    private static int count(TestScene scene, BlockPos at, net.minecraft.world.item.Item item) {
        return scene.helper().getEntities(EntityType.ITEM, at, 2).stream().map(ItemEntity::getItem).filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void openingTheBenchGivesTheStationSlotsThenThePlayersAndTellsTheClient(TestScene scene) {
        floor(scene);
        TestStations.BenchBlockEntity bench = bench(scene, AT);
        TestPlayer player = scene.addPlayer("Opener");
        player.clearReceived();
        StationMenu menu = open(player, bench);
        scene.check("the player holds a station menu of the bench's kind", () -> scene.assertTrue(menu.kind() == TestStations.BENCH && menu.getType() == TestStations.BENCH.menu().type(), "menu " + menu));
        scene.check("four station slots, then 36 player slots, and the ranges", () -> scene.assertTrue(menu.slots.size() == 40
                && menu.stationEnd() == 4 && menu.playerStart() == 4 && menu.hotbarStart() == 31 && menu.playerEnd() == 40, "slots " + menu.slots.size()));
        scene.check("the station slots are over the bench and the rest over the player's inventory", () -> {
            for (int index = 0; index < 4; index++) {
                scene.assertTrue(menu.slots.get(index) instanceof StationSlot && menu.slots.get(index).container == bench, "slot " + index);
            }
            for (int index = 4; index < 40; index++) {
                scene.assertTrue(menu.slots.get(index).container == player.player().getInventory(), "player slot " + index);
            }
        });
        List<ClientboundOpenScreenPacket> opened = player.received(ClientboundOpenScreenPacket.class);
        scene.check("the open packet names the bench's menu type and the title, with no data payload", () -> scene.assertTrue(opened.size() == 1
                && opened.getFirst().getType() == TestStations.BENCH.menu().type() && opened.getFirst().getContainerId() == menu.containerId
                && opened.getFirst().getTitle().getString().equals(TestStations.BENCH.title().text().getString()), "open packets " + opened.size()));
        scene.check("the client copy built from an empty buffer has 4 + 36 slots and one data slot", () -> {
            RegistryFriendlyByteBuf empty = new RegistryFriendlyByteBuf(Unpooled.buffer(), scene.level().registryAccess(), ConnectionType.OTHER);
            scene.assertTrue(opened.getFirst().getType().create(opened.getFirst().getContainerId(), player.player().getInventory(), empty) instanceof StationMenu copy
                    && copy.slots.size() == 40 && copy.stationEnd() == 4 && copy.kind().dataCount() == 1 && copy.data(0) == 0, "client copy");
        });
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theSlotRulesRefuseTheOutputAndAWrongToolAndAgreeWithTheBlockEntity(TestScene scene) {
        floor(scene);
        TestStations.BenchBlockEntity bench = bench(scene, AT);
        TestPlayer player = scene.addPlayer("Placer").survival();
        ServerPlayer live = player.player();
        StationMenu menu = open(player, bench);
        menu.setCarried(new ItemStack(Items.STONE, 3));
        menu.clicked(3, 0, ContainerInput.PICKUP, live);
        scene.check("the output refuses a placed stack", () -> scene.assertTrue(bench.getItem(3).isEmpty() && menu.getCarried().getCount() == 3, "output " + bench.getItem(3)));
        menu.clicked(2, 0, ContainerInput.PICKUP, live);
        scene.check("the tool cell refuses a non-stick", () -> scene.assertTrue(bench.getItem(2).isEmpty() && menu.getCarried().getCount() == 3, "tool " + bench.getItem(2)));
        menu.clicked(0, 0, ContainerInput.PICKUP, live);
        scene.check("an input accepts anything", () -> scene.assertTrue(bench.getItem(0).is(Items.STONE) && bench.getItem(0).getCount() == 3 && menu.getCarried().isEmpty(), "input " + bench.getItem(0)));
        menu.setCarried(new ItemStack(Items.STICK));
        menu.clicked(2, 0, ContainerInput.PICKUP, live);
        scene.check("the tool cell accepts a stick", () -> scene.assertTrue(bench.getItem(2).is(Items.STICK) && menu.getCarried().isEmpty(), "tool " + bench.getItem(2)));
        scene.check("the block entity's rule agrees with every slot's for a stick and a stone", () -> {
            for (ItemStack stack : List.of(new ItemStack(Items.STICK), new ItemStack(Items.STONE))) {
                for (int index = 0; index < 4; index++) {
                    scene.assertTrue(bench.canPlaceItem(index, stack) == menu.slots.get(index).mayPlace(stack), "slot " + index + " for " + stack);
                }
            }
        });
        scene.succeed();
    }

    @Scene(maxTicks = 30, isolated = true)
    public void shiftClickFollowsTheRolesAndTheOutputCountsAsCrafted(TestScene scene) {
        floor(scene);
        TestStations.BenchBlockEntity bench = bench(scene, AT);
        TestPlayer player = scene.addPlayer("Mover").survival();
        ServerPlayer live = player.player();
        StationMenu menu = open(player, bench);
        net.minecraft.world.entity.player.Inventory items = live.getInventory();
        List<PlayerEvent.ItemCraftedEvent> crafted = new ArrayList<>();
        Consumer<PlayerEvent.ItemCraftedEvent> listener = crafted::add;
        NeoForge.EVENT_BUS.addListener(PlayerEvent.ItemCraftedEvent.class, listener);
        scene.onEnd(() -> NeoForge.EVENT_BUS.unregister(listener));
        int hotbar = menu.hotbarStart();

        items.setItem(0, new ItemStack(Items.STICK));
        menu.clicked(hotbar, 0, ContainerInput.QUICK_MOVE, live);
        scene.check("a stick from the hotbar lands in the tool cell", () -> scene.assertTrue(bench.getItem(2).is(Items.STICK) && items.getItem(0).isEmpty() && bench.getItem(0).isEmpty(), "tool " + bench.getItem(2)));

        bench.setItem(2, new ItemStack(Items.STICK, 64));
        items.setItem(0, new ItemStack(Items.STICK));
        menu.clicked(hotbar, 0, ContainerInput.QUICK_MOVE, live);
        scene.check("a second stick with the tool cell full lands in the first input", () -> scene.assertTrue(bench.getItem(0).is(Items.STICK) && bench.getItem(0).getCount() == 1 && items.getItem(0).isEmpty(), "input " + bench.getItem(0)));

        bench.clearContent();
        items.setItem(0, new ItemStack(Items.STONE));
        menu.clicked(hotbar, 0, ContainerInput.QUICK_MOVE, live);
        scene.check("a stone from the hotbar lands in the first input", () -> scene.assertTrue(bench.getItem(0).is(Items.STONE) && bench.getItem(1).isEmpty() && bench.getItem(2).isEmpty(), "input " + bench.getItem(0)));

        menu.clicked(0, 0, ContainerInput.QUICK_MOVE, live);
        scene.check("an input's stack lands in the player strip forwards, in the first main slot", () -> scene.assertTrue(items.getItem(9).is(Items.STONE) && bench.getItem(0).isEmpty(), "main slot " + items.getItem(9)));

        items.clearContent();
        bench.clearContent();
        bench.setItem(3, new ItemStack(Items.STONE, 5));
        int before = live.getStats().getValue(Stats.ITEM_CRAFTED.get(Items.STONE));
        menu.clicked(3, 0, ContainerInput.QUICK_MOVE, live);
        scene.check("the output's stack lands in the strip backwards, in the last hotbar slot", () -> scene.assertTrue(items.getItem(8).is(Items.STONE) && items.getItem(8).getCount() == 5 && bench.getItem(3).isEmpty(), "last hotbar " + items.getItem(8)));
        scene.check("exactly one crafting event with the taken count", () -> scene.assertTrue(crafted.size() == 1 && crafted.getFirst().getCrafting().is(Items.STONE)
                && crafted.getFirst().getCrafting().getCount() == 5 && crafted.getFirst().getEntity() == live, "events " + crafted.size()));
        scene.check("and the crafted statistic grew by the count", () -> scene.assertEquals(before + 5, live.getStats().getValue(Stats.ITEM_CRAFTED.get(Items.STONE)), "crafted stones"));

        items.clearContent();
        bench.clearContent();
        bench.setItem(0, new ItemStack(Items.STONE, 64));
        bench.setItem(1, new ItemStack(Items.STONE, 64));
        bench.setItem(2, new ItemStack(Items.STICK, 64));
        items.setItem(0, new ItemStack(Items.DIRT));
        menu.clicked(hotbar, 0, ContainerInput.QUICK_MOVE, live);
        scene.check("with every cell full a stack nothing accepts swaps from the hotbar into main", () -> scene.assertTrue(items.getItem(0).isEmpty() && items.getItem(9).is(Items.DIRT), "main slot " + items.getItem(9)));
        scene.check("quick-moving an empty slot answers empty", () -> scene.assertTrue(menu.quickMoveStack(live, 5).isEmpty() && menu.quickMoveStack(live, 3).isEmpty(), "empty slots"));

        for (int slot = 0; slot < 36; slot++) {
            items.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        bench.setItem(3, new ItemStack(Items.STONE, 5));
        ItemStack output = menu.quickMoveStack(live, 3);
        scene.check("a filled output with no room in the player's slots answers empty and moves nothing", () -> scene.assertTrue(output.isEmpty() && bench.getItem(3).getCount() == 5, "output " + bench.getItem(3)));
        ItemStack mainSlot = menu.quickMoveStack(live, menu.stationEnd());
        scene.check("a player slot nothing accepts and with no room to swap answers empty and moves nothing", () -> scene.assertTrue(mainSlot.isEmpty() && items.getItem(9).getCount() == 64
                && bench.getItem(0).getCount() == 64, "main slot " + items.getItem(9)));

        items.setItem(0, ItemStack.EMPTY);
        ItemStack swapped = menu.quickMoveStack(live, menu.stationEnd());
        scene.check("a main slot swaps into an empty hotbar slot", () -> scene.assertTrue(!swapped.isEmpty() && items.getItem(0).is(Items.COBBLESTONE) && items.getItem(9).isEmpty(), "hotbar " + items.getItem(0)));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theCrateMovesBackwardsOutAndForwardsIn(TestScene scene) {
        floor(scene);
        TestStations.CrateBlockEntity crate = crate(scene, AT);
        TestPlayer player = scene.addPlayer("Packer").survival();
        ServerPlayer live = player.player();
        StationMenu menu = open(player, crate);
        net.minecraft.world.entity.player.Inventory items = live.getInventory();
        scene.check("the crate has 18 storage cells before the player's slots", () -> scene.assertTrue(menu.stationEnd() == 18 && menu.hotbarStart() == 45 && menu.slots.size() == 54, "slots " + menu.slots.size()));
        crate.setItem(0, new ItemStack(Items.STONE, 4));
        menu.clicked(0, 0, ContainerInput.QUICK_MOVE, live);
        scene.check("a stack in a storage cell lands in the last hotbar slot", () -> scene.assertTrue(items.getItem(8).is(Items.STONE) && items.getItem(8).getCount() == 4 && crate.getItem(0).isEmpty(), "last hotbar " + items.getItem(8)));
        items.clearContent();
        items.setItem(0, new ItemStack(Items.DIRT, 2));
        menu.clicked(menu.hotbarStart(), 0, ContainerInput.QUICK_MOVE, live);
        scene.check("a hotbar stack lands in the first storage cell", () -> scene.assertTrue(crate.getItem(0).is(Items.DIRT) && crate.getItem(0).getCount() == 2 && items.getItem(0).isEmpty(), "cell 0 " + crate.getItem(0)));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aDoubleClickGathersFromTheInputAndLeavesTheOutputAlone(TestScene scene) {
        floor(scene);
        TestStations.BenchBlockEntity bench = bench(scene, AT);
        TestPlayer player = scene.addPlayer("Gatherer").survival();
        ServerPlayer live = player.player();
        StationMenu menu = open(player, bench);
        bench.setItem(0, new ItemStack(Items.STONE, 5));
        bench.setItem(3, new ItemStack(Items.STONE, 3));
        menu.setCarried(new ItemStack(Items.STONE));
        menu.clicked(10, 0, ContainerInput.PICKUP_ALL, live);
        scene.check("the carried stack gathered from the input", () -> scene.assertTrue(menu.getCarried().getCount() == 6 && bench.getItem(0).isEmpty(), "carried " + menu.getCarried()));
        scene.check("and took nothing from the output", () -> scene.assertTrue(bench.getItem(3).getCount() == 3, "output " + bench.getItem(3)));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void twoViewersEachReadTheirOwnDataAndEachClientReceivesIt(TestScene scene) {
        floor(scene);
        TestStations.BenchBlockEntity bench = bench(scene, AT);
        TestPlayer shorter = scene.addPlayer("Al");
        TestPlayer longer = scene.addPlayer("Barnabas");
        shorter.clearReceived();
        longer.clearReceived();
        StationMenu first = open(shorter, bench);
        StationMenu second = open(longer, bench);
        scene.check("each menu reads its viewer's name length", () -> scene.assertTrue(first.data(0) == 2 && second.data(0) == 8, "data " + first.data(0) + ", " + second.data(0)));
        scene.runAtTick(2, () -> {
            scene.check("and each client received its own value as the full int payload", () -> {
                scene.assertTrue(values(shorter, first.containerId).contains(2) && !values(shorter, first.containerId).contains(8), "first " + values(shorter, first.containerId));
                scene.assertTrue(values(longer, second.containerId).contains(8) && !values(longer, second.containerId).contains(2), "second " + values(longer, second.containerId));
            });
            scene.succeed();
        });
    }

    private static List<Integer> values(TestPlayer player, int containerId) {
        return player.received(ClientboundCustomPayloadPacket.class).stream().map(ClientboundCustomPayloadPacket::payload)
                .filter(AdvancedContainerSetDataPayload.class::isInstance).map(AdvancedContainerSetDataPayload.class::cast)
                .filter(payload -> payload.containerId() == (byte) containerId).map(AdvancedContainerSetDataPayload::value).toList();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theButtonPostsAnEventTheRootHandlesAndTheChangeReachesTheSlots(TestScene scene) {
        floor(scene);
        TestStations.BenchBlockEntity bench = bench(scene, AT);
        TestPlayer player = scene.addPlayer("Presser");
        ServerPlayer live = player.player();
        StationMenu menu = open(player, bench);
        bench.setItem(0, new ItemStack(Items.STONE, 2));
        menu.broadcastChanges();
        TestStations.clearEvents();
        player.clearReceived();
        boolean pressed = menu.clickMenuButton(live, 0);
        List<StationEvent.Button> buttons = TestStations.events().stream().filter(StationEvent.Button.class::isInstance).map(StationEvent.Button.class::cast).toList();
        scene.check("button 0 answers true after exactly one handled event", () -> scene.assertTrue(pressed && buttons.size() == 1 && buttons.getFirst().handled()
                && buttons.getFirst().id() == 0 && buttons.getFirst().player() == live && buttons.getFirst().station() == bench, "buttons " + buttons.size()));
        scene.check("the root's listener moved one item from the input to the output", () -> scene.assertTrue(bench.getItem(0).getCount() == 1 && bench.getItem(3).is(Items.STONE) && bench.getItem(3).getCount() == 1, "cells " + bench.getItem(0) + ", " + bench.getItem(3)));
        scene.check("and toggled LIT without losing the block entity", () -> scene.assertTrue(scene.level().getBlockState(scene.helper().absolutePos(AT)).getValue(BlockStateProperties.LIT)
                && scene.helper().getBlockEntity(AT, TestStations.BenchBlockEntity.class) == bench, "lit"));
        menu.broadcastChanges();
        scene.check("the slot change was sent to the player", () -> scene.assertTrue(player.received(ClientboundContainerSetSlotPacket.class).stream()
                .anyMatch(packet -> packet.getSlot() == 3 && packet.getItem().is(Items.STONE)), "slot packets " + player.received(ClientboundContainerSetSlotPacket.class).size()));
        TestStations.clearEvents();
        boolean unknown = menu.clickMenuButton(live, 7);
        List<StationEvent.Button> unhandled = TestStations.events().stream().filter(StationEvent.Button.class::isInstance).map(StationEvent.Button.class::cast).toList();
        scene.check("an unknown id answers false and posts an event nobody handled", () -> scene.assertTrue(!unknown && unhandled.size() == 1 && !unhandled.getFirst().handled() && unhandled.getFirst().id() == 7, "unhandled " + unhandled.size()));
        scene.succeed();
    }

    @Scene(maxTicks = 50, isolated = true)
    public void aChangeIsOneEventWhateverTouchedTheCellsAndASaveIsNone(TestScene scene) {
        floor(scene);
        TestStations.BenchBlockEntity bench = bench(scene, AT);
        TestPlayer player = scene.addPlayer("Changer").survival();
        ServerPlayer live = player.player();
        StationMenu menu = open(player, bench);
        scene.runAtTick(2, () -> {
            TestStations.clearEvents();
            menu.setCarried(new ItemStack(Items.STONE));
            menu.clicked(0, 0, ContainerInput.PICKUP, live);
            scene.check("one menu click moving an item posts one change", () -> scene.assertEquals(1L, changedFor(bench), "changes after a click"));
            TestStations.clearEvents();
            BlockPos above = AT.above();
            scene.level().setBlock(scene.helper().absolutePos(above), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN), Block.UPDATE_ALL);
            scene.helper().getBlockEntity(above, HopperBlockEntity.class).setItem(0, new ItemStack(Items.STONE));
            scene.runAtTick(30, () -> {
                scene.check("one hopper insert posts one change", () -> scene.assertTrue(bench.getItem(0).getCount() == 2 && changedFor(bench) == 1, "input " + bench.getItem(0) + ", changes " + changedFor(bench)));
                TestStations.clearEvents();
                ResourceHandler<ItemResource> handler = scene.level().getCapability(Capabilities.Item.BLOCK, scene.helper().absolutePos(AT), null);
                try (Transaction transaction = Transaction.openRoot()) {
                    handler.insert(ItemResource.of(Items.STONE), 1, transaction);
                    transaction.commit();
                }
                scene.check("one insert through the exposed capability posts one change", () -> scene.assertTrue(bench.getItem(0).getCount() == 3 && changedFor(bench) == 1, "input " + bench.getItem(0) + ", changes " + changedFor(bench)));
                TestStations.clearEvents();
                bench.saveWithoutMetadata(scene.level().registryAccess());
                scene.check("a save posts none", () -> scene.assertEquals(0L, changedFor(bench), "changes after a save"));
                scene.succeed();
            });
        });
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aPropertyKeepsTheContentsAndRemovalClosesTheMenuAndDropsThemButTheCrateDropsNothing(TestScene scene) {
        floor(scene);
        TestStations.BenchBlockEntity bench = bench(scene, AT);
        BlockPos crateAt = new BlockPos(7, 2, 7);
        TestStations.CrateBlockEntity crate = crate(scene, crateAt);
        TestPlayer player = scene.addPlayer("Breaker");
        ServerPlayer live = player.player();
        StationMenu menu = open(player, bench);
        bench.setItem(0, new ItemStack(Items.STONE, 3));
        bench.setItem(2, new ItemStack(Items.STICK));
        crate.setItem(0, new ItemStack(Items.DIRT, 4));
        BlockPos at = scene.helper().absolutePos(AT);
        BlockState lit = scene.level().getBlockState(at).setValue(BlockStateProperties.LIT, true);
        scene.level().setBlock(at, lit, Block.UPDATE_ALL);
        scene.check("toggling LIT keeps the block entity and its contents", () -> scene.assertTrue(scene.helper().getBlockEntity(AT, TestStations.BenchBlockEntity.class) == bench
                && bench.getItem(0).getCount() == 3 && bench.getItem(2).is(Items.STICK) && live.containerMenu == menu, "bench " + bench.getItem(0)));
        scene.level().setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        scene.level().setBlock(scene.helper().absolutePos(crateAt), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        scene.check("replacing the bench drops its contents as item entities", () -> scene.assertTrue(count(scene, AT, Items.STONE) == 3 && count(scene, AT, Items.STICK) == 1, "stones " + count(scene, AT, Items.STONE)));
        scene.check("and the crate, which does not drop, drops nothing", () -> scene.assertEquals(0, count(scene, crateAt, Items.DIRT), "dirt"));
        scene.runAtTick(3, () -> {
            scene.check("the open menu closed on the next tick", () -> scene.assertTrue(live.containerMenu == live.inventoryMenu, "menu " + live.containerMenu));
            scene.succeed();
        });
    }

    @Scene(maxTicks = 70)
    public void aHopperAboveInsertsIntoAnInputAndNeverTheOutput(TestScene scene) {
        TestStations.BenchBlockEntity bench = bench(scene, AT);
        BlockPos above = AT.above();
        scene.level().setBlock(scene.helper().absolutePos(above), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN), Block.UPDATE_ALL);
        HopperBlockEntity hopper = scene.helper().getBlockEntity(above, HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.STONE));
        scene.runAtTick(25, () -> {
            scene.check("the hopper put its stone into input 0", () -> scene.assertTrue(bench.getItem(0).is(Items.STONE) && bench.getItem(0).getCount() == 1 && hopper.isEmpty(), "input " + bench.getItem(0)));
            bench.setItem(0, new ItemStack(Items.STONE, 64));
            bench.setItem(1, new ItemStack(Items.STONE, 64));
            hopper.setItem(0, new ItemStack(Items.STONE));
        });
        scene.runAtTick(60, () -> {
            scene.check("with the inputs full it keeps the stone and the output stays empty", () -> scene.assertTrue(bench.getItem(3).isEmpty() && bench.getItem(2).isEmpty()
                    && hopper.getItem(0).getCount() == 1, "output " + bench.getItem(3) + ", hopper " + hopper.getItem(0)));
            scene.succeed();
        });
    }

    @Scene(maxTicks = 40)
    public void aHopperBelowTakesTheOutputAndNothingElse(TestScene scene) {
        TestStations.BenchBlockEntity bench = bench(scene, AT);
        BlockPos below = AT.below();
        scene.level().setBlock(scene.helper().absolutePos(below), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN), Block.UPDATE_ALL);
        HopperBlockEntity hopper = scene.helper().getBlockEntity(below, HopperBlockEntity.class);
        bench.setItem(0, new ItemStack(Items.STONE, 3));
        bench.setItem(3, new ItemStack(Items.STONE));
        scene.runAtTick(30, () -> {
            int held = 0;
            for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
                held += hopper.getItem(slot).getCount();
            }
            int taken = held;
            scene.check("the hopper holds only the output's stone and the input is untouched", () -> scene.assertTrue(taken == 1 && bench.getItem(3).isEmpty() && bench.getItem(0).getCount() == 3, "hopper " + taken + ", input " + bench.getItem(0)));
            scene.succeed();
        });
    }

    @Scene(maxTicks = 20)
    public void theExposedCapabilityShowsTheSameRulesPerSideAndTakesFromAnyCellWithNoSide(TestScene scene) {
        TestStations.BenchBlockEntity bench = bench(scene, AT);
        BlockPos at = scene.helper().absolutePos(AT);
        ServerLevel level = scene.level();
        bench.setItem(0, new ItemStack(Items.DIRT, 5));
        bench.setItem(3, new ItemStack(Items.STONE, 3));
        ResourceHandler<ItemResource> down = level.getCapability(Capabilities.Item.BLOCK, at, Direction.DOWN);
        int inputTaken;
        int outputTaken;
        try (Transaction transaction = Transaction.openRoot()) {
            inputTaken = down.extract(ItemResource.of(Items.DIRT), 64, transaction);
            outputTaken = down.extract(ItemResource.of(Items.STONE), 64, transaction);
            transaction.commit();
        }
        scene.check("from below only the output can be extracted", () -> scene.assertTrue(inputTaken == 0 && outputTaken == 3 && bench.getItem(3).isEmpty() && bench.getItem(0).getCount() == 5, "input " + inputTaken + ", output " + outputTaken));
        bench.clearContent();
        ResourceHandler<ItemResource> up = level.getCapability(Capabilities.Item.BLOCK, at, Direction.UP);
        int inserted;
        try (Transaction transaction = Transaction.openRoot()) {
            inserted = up.insert(ItemResource.of(Items.STONE), 200, transaction);
            transaction.commit();
        }
        scene.check("from above stones go only into the two inputs", () -> scene.assertTrue(inserted == 128 && bench.getItem(2).isEmpty() && bench.getItem(3).isEmpty(), "inserted " + inserted));
        bench.clearContent();
        bench.setItem(1, new ItemStack(Items.DIRT, 5));
        ResourceHandler<ItemResource> within = level.getCapability(Capabilities.Item.BLOCK, at, null);
        int fromInput;
        try (Transaction transaction = Transaction.openRoot()) {
            fromInput = within.extract(ItemResource.of(Items.DIRT), 64, transaction);
            transaction.commit();
        }
        scene.check("with no side an input can be extracted too", () -> scene.assertTrue(fromInput == 5 && bench.getItem(1).isEmpty(), "extracted " + fromInput));
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void aSaveAndLoadKeepsTheItemsAndRecreatesTheListAndLoadingRefreshesOnce(TestScene scene) {
        TestStations.BenchBlockEntity bench = bench(scene, AT);
        ServerLevel level = scene.level();
        BlockPos pos = bench.getBlockPos();
        BlockState state = bench.getBlockState();
        bench.setItem(0, new ItemStack(Items.STONE, 3));
        bench.setItem(1, new ItemStack(Items.DIRT, 2));
        bench.setItem(2, new ItemStack(Items.STICK));
        CompoundTag tag = bench.saveWithoutMetadata(level.registryAccess());
        bench.setItem(1, ItemStack.EMPTY);
        TestStations.BenchBlockEntity fresh = new TestStations.BenchBlockEntity(pos, state);
        fresh.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        scene.check("a fresh block entity of the kind has the saved items", () -> scene.assertTrue(fresh.getItem(0).is(Items.STONE) && fresh.getItem(0).getCount() == 3
                && fresh.getItem(1).is(Items.DIRT) && fresh.getItem(2).is(Items.STICK) && fresh.getItem(3).isEmpty(), "fresh " + fresh.getItem(0) + ", " + fresh.getItem(1)));
        TestStations.BenchBlockEntity filled = new TestStations.BenchBlockEntity(pos, state);
        CompoundTag emptied = bench.saveWithoutMetadata(level.registryAccess());
        filled.setItem(1, new ItemStack(Items.STONE, 9));
        filled.setItem(3, new ItemStack(Items.STONE, 9));
        filled.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), emptied));
        scene.check("a slot emptied before saving is empty after a load that follows a fill", () -> scene.assertTrue(filled.getItem(1).isEmpty() && filled.getItem(3).isEmpty()
                && filled.getItem(0).getCount() == 3, "filled " + filled.getItem(1) + ", " + filled.getItem(3)));
        TestStations.BenchBlockEntity loaded = new TestStations.BenchBlockEntity(pos, state);
        loaded.setLevel(level);
        TestStations.clearEvents();
        loaded.onLoad();
        scene.check("loading on the server posts one change", () -> scene.assertEquals(1L, changedFor(loaded), "changes on load"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void syncSendsTheBlockEntityPacketWithTheCellsAndNoLockAndTheCrateSendsNone(TestScene scene) {
        floor(scene);
        TestStations.BenchBlockEntity bench = bench(scene, AT);
        BlockPos cratePos = new BlockPos(4, 2, 4);
        TestStations.CrateBlockEntity crate = crate(scene, cratePos);
        TestPlayer player = scene.addPlayer("Tracker");
        CompoundTag lock = new CompoundTag();
        lock.putString("items", "minecraft:stick");
        CompoundTag locked = new CompoundTag();
        locked.put("lock", lock);
        bench.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, scene.level().registryAccess(), locked));
        scene.check("the bench carries a lock before it is synced", () -> scene.assertTrue(bench.isLocked(), "locked"));
        bench.setItem(0, new ItemStack(Items.STONE, 3));
        crate.setItem(0, new ItemStack(Items.STONE, 3));
        player.clearReceived();
        bench.sync();
        crate.sync();
        scene.runAtTick(3, () -> {
            List<ClientboundBlockEntityDataPacket> packets = player.received(ClientboundBlockEntityDataPacket.class);
            BlockPos benchAt = scene.helper().absolutePos(AT);
            BlockPos crateAt = scene.helper().absolutePos(cratePos);
            scene.check("the tracking player received the bench's packet with the items and no lock key", () -> scene.assertTrue(packets.stream().anyMatch(packet -> packet.getPos().equals(benchAt)
                    && !packet.getTag().contains("lock") && packet.getTag().getListOrEmpty("Items").size() == 1
                    && packet.getTag().getListOrEmpty("Items").getCompoundOrEmpty(0).getIntOr("Slot", -1) == 0
                    && packet.getTag().getListOrEmpty("Items").getCompoundOrEmpty(0).getIntOr("count", 0) == 3), "packets " + packets.size()));
            scene.check("and none for the crate", () -> scene.assertTrue(packets.stream().noneMatch(packet -> packet.getPos().equals(crateAt)), "packets " + packets.size()));
            scene.succeed();
        });
    }

    @Scene(maxTicks = 20)
    public void aListenerWritingIntoTheCellsDuringTheEventDoesNotRepeatTheChange(TestScene scene) {
        TestStations.BenchBlockEntity bench = bench(scene, AT);
        TestStations.clearEvents();
        TestStations.deliverOnNextChange();
        bench.setItem(0, new ItemStack(Items.DIRT));
        scene.check("a click whose listener delivers into the output posts one change", () -> scene.assertTrue(bench.getItem(3).is(Items.STONE) && changedFor(bench) == 1, "changes " + changedFor(bench)));
        bench.setChanged();
        scene.check("a second setChanged of the same click posts none", () -> scene.assertEquals(1L, changedFor(bench), "changes"));
        bench.removeItem(3, 1);
        scene.check("taking the delivered output posts one more", () -> scene.assertTrue(bench.getItem(3).isEmpty() && changedFor(bench) == 2, "changes " + changedFor(bench)));
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void theBenchTicksOnEveryServerTick(TestScene scene) {
        TestStations.BenchBlockEntity bench = bench(scene, AT);
        int[] start = new int[1];
        scene.runAtTick(2, () -> start[0] = bench.ticks());
        scene.runAtTick(7, () -> {
            scene.check("the counter advanced once per tick for five ticks", () -> scene.assertEquals(5, bench.ticks() - start[0], "ticks"));
            scene.succeed();
        });
    }

    @Scene(maxTicks = 20)
    public void theComparatorReadsTheFill(TestScene scene) {
        TestStations.BenchBlockEntity bench = bench(scene, AT);
        BlockPos at = scene.helper().absolutePos(AT);
        int empty = scene.level().getBlockState(at).getAnalogOutputSignal(scene.level(), at, Direction.NORTH);
        bench.setItem(0, new ItemStack(Items.STONE, 64));
        int full = scene.level().getBlockState(at).getAnalogOutputSignal(scene.level(), at, Direction.NORTH);
        scene.check("the signal grows when a stack is placed", () -> scene.assertTrue(empty == 0 && full > empty, "empty " + empty + ", full " + full));
        scene.succeed();
    }
}
