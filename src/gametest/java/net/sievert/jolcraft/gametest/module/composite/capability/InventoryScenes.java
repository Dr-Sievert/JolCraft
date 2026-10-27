package net.sievert.jolcraft.gametest.module.composite.capability;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestPlayer;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.composite.capability.Inventory;

/**
 * The inventory composite against real holders: a player given to, counted and refused the worn armor, a hand given
 * into first, a chest and a bare block, a living entity that equips, and the game-test mod's own bin reached through
 * the block capability it was exposed under.
 */
public final class InventoryScenes {

    private static final Predicate<ItemStack> STICKS = stack -> stack.is(Items.STICK);
    private static final Predicate<ItemStack> BOOTS = stack -> stack.is(Items.LEATHER_BOOTS);

    @Scene(maxTicks = 20, isolated = true)
    public void aGiveFillsTheHeldStackThenTheMainSlotsAndDropsTheRestTargetedAtThePlayer(TestScene scene) {
        Inventory inventory = JolCraftGameTests.inventory();
        TestPlayer player = scene.addPlayer("Receiver").survival();
        ServerPlayer live = player.player();
        net.minecraft.world.entity.player.Inventory items = live.getInventory();
        items.setSelectedSlot(0);
        items.setItem(0, new ItemStack(Items.STICK, 10));
        for (int slot = 1; slot < 36; slot++) {
            items.setItem(slot, new ItemStack(Items.STONE, 64));
        }
        live.containerMenu.broadcastChanges(); // the setup's own slot packets go out now, so what follows is the give's
        player.clearReceived();
        ItemStack given = new ItemStack(Items.STICK, 100);
        List<ItemStack> dropped = inventory.give(new Inventory.Holder.Player(live), List.of(ItemStack.EMPTY, given));
        scene.check("the held stack fills to its max and the given stack is untouched", () -> scene.assertTrue(items.getItem(0).getCount() == 64 && given.getCount() == 100, "held " + items.getItem(0)));
        scene.check("the stone slots, the empty offhand and the armor are left alone", () -> scene.assertTrue(items.getItem(1).is(Items.STONE) && items.getItem(1).getCount() == 64
                && live.getOffhandItem().isEmpty() && live.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "other slots"));
        scene.check("the rest is answered as overflow, the empty stack skipped", () -> scene.assertTrue(dropped.size() == 1 && dropped.getFirst().is(Items.STICK) && dropped.getFirst().getCount() == 46, "dropped " + dropped));
        // The toss leaves the entity at the player's eye height this tick; a small box around the player, so a neighbouring arena's leftovers stay out.
        List<ItemEntity> entities = scene.helper().getEntities(EntityType.ITEM, new BlockPos(0, 1, 0), 2);
        scene.check("and tossed as one item entity with no pickup delay, targeted at the player, with no thrower", () -> scene.assertTrue(entities.size() == 1
                && entities.getFirst().getItem().getCount() == 46 && !entities.getFirst().hasPickUpDelay() && live.getUUID().equals(entities.getFirst().getTarget())
                && entities.getFirst().getOwner() == null, "entities " + entities));
        scene.check("the filled slot reaches the player at the commit, before any tick", () -> scene.assertTrue(player.received(ClientboundContainerSetSlotPacket.class).stream()
                .anyMatch(packet -> packet.getItem().is(Items.STICK) && packet.getItem().getCount() == 64), "slot packets " + player.received(ClientboundContainerSetSlotPacket.class).size()));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aCountSpansTheMainSlotsAndTheOffhandAndNotTheWornArmorAndAHandIsItsSlotAlone(TestScene scene) {
        Inventory inventory = JolCraftGameTests.inventory();
        ServerPlayer live = scene.addPlayer("Counter").survival().player();
        net.minecraft.world.entity.player.Inventory items = live.getInventory();
        items.setItem(3, new ItemStack(Items.STICK, 2));
        items.setItem(20, new ItemStack(Items.STICK, 3));
        live.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.STICK, 4));
        items.setItem(5, new ItemStack(Items.LEATHER_BOOTS));
        live.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        items.setSelectedSlot(3);
        Inventory.Holder whole = new Inventory.Holder.Player(live);
        scene.check("the sticks of three slots and the offhand are summed", () -> scene.assertEquals(9, inventory.count(whole, STICKS), "sticks"));
        scene.check("has asks the same", () -> scene.assertTrue(inventory.has(whole, STICKS, 9) && !inventory.has(whole, STICKS, 10), "has"));
        scene.check("the worn boots are not counted", () -> scene.assertEquals(1, inventory.count(whole, BOOTS), "boots"));
        scene.check("a hand counts its slot alone", () -> scene.assertTrue(inventory.count(new Inventory.Holder.Hand(live, InteractionHand.MAIN_HAND), STICKS) == 2
                && inventory.count(new Inventory.Holder.Hand(live, InteractionHand.OFF_HAND), STICKS) == 4, "hands"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aHandHoldersGiveLandsInTheHandBeforeTheMainSlots(TestScene scene) {
        Inventory inventory = JolCraftGameTests.inventory();
        ServerPlayer live = scene.addPlayer("Handed").survival().player();
        net.minecraft.world.entity.player.Inventory items = live.getInventory();
        items.setSelectedSlot(4);
        Inventory.Holder hand = new Inventory.Holder.Hand(live, InteractionHand.MAIN_HAND);
        List<ItemStack> none = inventory.give(hand, new ItemStack(Items.BUCKET));
        scene.check("an empty hand receives the bucket, not the first main slot", () -> scene.assertTrue(items.getItem(4).is(Items.BUCKET) && items.getItem(0).isEmpty() && none.isEmpty(), "bucket at " + items.getItem(4)));
        inventory.give(hand, new ItemStack(Items.STONE, 5));
        scene.check("what the hand refuses goes on as the player's give: the first empty main slot", () -> scene.assertTrue(items.getItem(4).is(Items.BUCKET) && items.getItem(0).is(Items.STONE) && items.getItem(0).getCount() == 5, "stone at " + items.getItem(0)));
        inventory.give(new Inventory.Holder.Player(live), new ItemStack(Items.BUCKET));
        scene.check("a player's give fills a hand already holding the resource first", () -> scene.assertEquals(2, items.getItem(4).getCount(), "buckets in hand"));
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void aChestHolderIsGivenAndCountedAndABlockWithoutAHandlerDropsAtItsCentre(TestScene scene) {
        Inventory inventory = JolCraftGameTests.inventory();
        ServerLevel level = scene.level();
        BlockPos chest = scene.helper().absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
        Inventory.Holder holder = new Inventory.Holder.Block(level, chest, null);
        List<ItemStack> none = inventory.give(holder, new ItemStack(Items.STICK, 10));
        ChestBlockEntity entity = scene.helper().getBlockEntity(new BlockPos(1, 1, 1), ChestBlockEntity.class);
        scene.check("the chest's first slot holds the sticks", () -> scene.assertTrue(entity.getItem(0).is(Items.STICK) && entity.getItem(0).getCount() == 10 && none.isEmpty(), "slot 0 " + entity.getItem(0)));
        entity.setItem(0, ItemStack.EMPTY);
        entity.setItem(1, new ItemStack(Items.STICK, 5)); // the filled slot is no longer the first, so stacking shows against index order
        inventory.give(holder, new ItemStack(Items.STICK, 3));
        scene.check("a give stacks into the filled slot before an empty one that comes first", () -> scene.assertTrue(entity.getItem(0).isEmpty() && entity.getItem(1).getCount() == 8, "slots " + entity.getItem(0) + ", " + entity.getItem(1)));
        inventory.give(holder, new ItemStack(Items.STICK, 70));
        scene.check("then fills it and goes on to the empty one", () -> scene.assertTrue(entity.getItem(1).getCount() == 64 && entity.getItem(0).getCount() == 14, "slots " + entity.getItem(0) + ", " + entity.getItem(1)));
        scene.check("and the count sums the slots", () -> scene.assertEquals(78, inventory.count(holder, STICKS), "sticks"));
        BlockPos stone = scene.helper().absolutePos(new BlockPos(3, 1, 3));
        level.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
        Inventory.Holder bare = new Inventory.Holder.Block(level, stone, null);
        List<ItemStack> dropped = inventory.give(bare, List.of(new ItemStack(Items.STICK, 150), new ItemStack(Items.STONE, 5)));
        List<ItemEntity> entities = scene.helper().getEntities(EntityType.ITEM, new BlockPos(3, 1, 3), 1);
        scene.check("a block without a handler counts nothing and drops every given stack at its centre with the default delay, the overflow split by max stack size", () -> scene.assertTrue(
                inventory.count(bare, STICKS) == 0
                        && dropped.stream().map(ItemStack::getCount).toList().equals(List.of(64, 64, 22, 5)) && dropped.get(3).is(Items.STONE)
                        && entities.size() == 4 && entities.stream().allMatch(ItemEntity::hasPickUpDelay)
                        && entities.stream().allMatch(item -> item.position().equals(stone.getCenter())),
                "dropped " + dropped + ", entities " + entities));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aLivingEntityHolderEquipsAGiveAndTheGuardsHold(TestScene scene) {
        Inventory inventory = JolCraftGameTests.inventory();
        ArmorStand stand = scene.helper().spawn(EntityType.ARMOR_STAND, new BlockPos(2, 1, 2));
        stand.setShowArms(true); // a stand without arms may hold nothing in its hands
        Inventory.Holder holder = new Inventory.Holder.Entity(stand);
        List<ItemStack> none = inventory.give(holder, new ItemStack(Items.IRON_SWORD));
        scene.check("the sword goes into the main hand", () -> scene.assertTrue(stand.getMainHandItem().is(Items.IRON_SWORD) && none.isEmpty(), "main hand " + stand.getMainHandItem()));
        scene.check("and is counted there", () -> scene.assertEquals(1, inventory.count(holder, stack -> stack.is(Items.IRON_SWORD)), "swords"));
        List<ItemStack> dropped = inventory.give(holder, new ItemStack(Items.IRON_SWORD));
        scene.check("a second sword fits no slot the stand may hold it in and spawns at the stand", () -> scene.assertTrue(dropped.size() == 1 && stand.getOffhandItem().isEmpty()
                && scene.helper().getEntities(EntityType.ITEM, new BlockPos(2, 1, 2), 2).size() == 1, "dropped " + dropped));
        ServerPlayer player = scene.addPlayer("Guarded").player();
        scene.check("a player is refused as an entity holder", () -> {
            try {
                new Inventory.Holder.Entity(player);
                scene.fail("a player was accepted as an entity holder");
            } catch (IllegalArgumentException expected) {
                scene.assertTrue(expected.getMessage().contains("Inventory.Holder.Player"), "message " + expected.getMessage());
            }
        });
        Throwable offThread = CompletableFuture.supplyAsync(() -> inventory.count(holder, STICKS)).handle((_, thrown) -> thrown).join();
        scene.check("an operation off the server thread is refused", () -> scene.assertTrue(offThread instanceof CompletionException && offThread.getCause() instanceof IllegalStateException, "thrown " + offThread));
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void anExposedBlockEntityIsReachedThroughTheBlockCapabilityAndByTheComposite(TestScene scene) {
        Inventory inventory = JolCraftGameTests.inventory();
        ServerLevel level = scene.level();
        BlockPos bin = scene.helper().absolutePos(new BlockPos(1, 1, 3));
        level.setBlock(bin, TestBins.BIN_BLOCK.get().defaultBlockState(), 3);
        scene.check("the bin answers the block capability from within and from a side", () -> scene.assertTrue(
                level.getCapability(Capabilities.Item.BLOCK, bin, null) != null && level.getCapability(Capabilities.Item.BLOCK, bin, Direction.UP) != null, "capability"));
        Inventory.Holder holder = new Inventory.Holder.Block(level, bin, Direction.UP);
        inventory.give(holder, new ItemStack(Items.STICK, 5));
        TestBins.BinBlockEntity entity = scene.helper().getBlockEntity(new BlockPos(1, 1, 3), TestBins.BinBlockEntity.class);
        scene.check("a give lands in the bin's slot and is counted", () -> scene.assertTrue(entity.getItem(0).is(Items.STICK) && entity.getItem(0).getCount() == 5 && inventory.count(holder, STICKS) == 5, "slot 0 " + entity.getItem(0)));
        List<ItemStack> dropped = inventory.give(holder, new ItemStack(Items.STICK, 200));
        scene.check("the three slots fill and the rest is overflow", () -> scene.assertTrue(inventory.count(holder, STICKS) == 3 * 64 && dropped.size() == 1 && dropped.getFirst().getCount() == 13, "count " + inventory.count(holder, STICKS) + ", dropped " + dropped));
        scene.succeed();
    }
}
