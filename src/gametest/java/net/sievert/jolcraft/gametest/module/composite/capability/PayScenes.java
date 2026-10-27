package net.sievert.jolcraft.gametest.module.composite.capability;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestPlayer;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.composite.capability.Inventory;
import net.sievert.jolcraft.module.primitive.recipe.Fate;
import net.sievert.jolcraft.module.primitive.recipe.ItemInput;

/**
 * Matching and paying against real holders: a match claims one resource per input in order and refuses what the
 * holder cannot cover, a pay consumes across slots and rolls a range, wears a tool in its slot and breaks a worn-out
 * one audibly, answers false with nothing changed on a shortfall, costs a creative payer nothing, gives a consumed
 * bucket's empty bucket back to the hand, and pays a chest.
 */
public final class PayScenes {

    private static final Predicate<ItemStack> STICKS = stack -> stack.is(Items.STICK);

    /** A range of one to four that always rolls two, so a roll is told apart from the largest amount a match needs. */
    private static final IntProvider ROLLS_TWO_OF_ONE_TO_FOUR = new IntProvider() {
        @Override
        public int sample(RandomSource random) {
            return 2;
        }

        @Override
        public int minInclusive() {
            return 1;
        }

        @Override
        public int maxInclusive() {
            return 4;
        }

        @Override
        public MapCodec<? extends IntProvider> codec() {
            return ConstantInt.MAP_CODEC;
        }
    };

    private static ItemInput input(Fate fate, ItemLike... items) {
        return ItemInput.of(ItemPredicate.Builder.item().of(BuiltInRegistries.ITEM, items), fate);
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aMatchClaimsOneResourcePerInputInOrderAndRefusesWhatTheHolderCannotCover(TestScene scene) {
        Inventory inventory = JolCraftGameTests.inventory();
        ServerPlayer live = scene.addPlayer("Matcher").survival().player();
        net.minecraft.world.entity.player.Inventory items = live.getInventory();
        items.setItem(0, new ItemStack(Items.STICK, 2));
        items.setItem(1, new ItemStack(Items.STICK, 2));
        Inventory.Holder holder = new Inventory.Holder.Player(live);
        List<ItemInput> threeAndTwo = List.of(input(Fate.consume(3), Items.STICK), input(Fate.consume(2), Items.STICK));
        scene.check("three plus two sticks of four are refused", () -> scene.assertTrue(inventory.match(holder, threeAndTwo).isEmpty(), "match"));
        items.setItem(2, new ItemStack(Items.STICK));
        Optional<Inventory.Match> match = inventory.match(holder, threeAndTwo);
        scene.check("and accepted of five, each input seeing what was left for it", () -> scene.assertTrue(match.isPresent()
                && match.get().stacks().stream().map(ItemStack::getCount).toList().equals(List.of(5, 2)) && match.get().stacks().stream().allMatch(STICKS)
                && items.getItem(0).getCount() == 2, "match " + match.map(Inventory.Match::stacks)));
        scene.check("a damage input needs a damageable item, a keep input anything present, and no inputs match trivially", () -> scene.assertTrue(
                inventory.match(holder, List.of(input(Fate.damage(1), Items.STICK))).isEmpty()
                        && inventory.match(holder, List.of(input(Fate.keep(), Items.STICK))).isPresent()
                        && inventory.match(holder, List.of()).map(trivial -> trivial.found().isEmpty()).orElse(false), "kinds"));
        items.clearContent();
        items.setItem(0, new ItemStack(Items.STONE));
        items.setItem(1, new ItemStack(Items.STICK));
        ItemInput either = input(Fate.consume(1), Items.STICK, Items.STONE);
        ItemInput stone = input(Fate.consume(1), Items.STONE);
        scene.check("the wider input first takes the stone in index order and leaves the narrower one nothing; the narrower first matches", () -> scene.assertTrue(
                inventory.match(holder, List.of(either, stone)).isEmpty() && inventory.match(holder, List.of(stone, either)).isPresent(), "overlap"));
        items.setItem(0, new ItemStack(Items.STONE, 2));
        items.setItem(1, new ItemStack(Items.STICK, 2));
        scene.check("two kinds are never summed under one input, and a keep needs its item present", () -> scene.assertTrue(
                inventory.match(holder, List.of(input(Fate.consume(3), Items.STICK, Items.STONE))).isEmpty()
                        && inventory.match(holder, List.of(input(Fate.keep(), Items.DIAMOND))).isEmpty(), "mixing"));
        BlockPos bare = scene.helper().absolutePos(new BlockPos(3, 1, 3));
        scene.level().setBlock(bare, Blocks.STONE.defaultBlockState(), 3);
        scene.check("a holder without a handler matches nothing", () -> scene.assertTrue(
                inventory.match(new Inventory.Holder.Block(scene.level(), bare, null), List.of(input(Fate.keep(), Items.STICK))).isEmpty(), "bare"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aPayConsumesAcrossSlotsRollsARangeKeepsAKeptItemAndAShortfallChangesNothing(TestScene scene) {
        Inventory inventory = JolCraftGameTests.inventory();
        ServerPlayer live = scene.addPlayer("Payer").survival().player();
        net.minecraft.world.entity.player.Inventory items = live.getInventory();
        items.setItem(3, new ItemStack(Items.STICK, 2));
        items.setItem(20, new ItemStack(Items.STICK, 2));
        live.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.STICK));
        items.setItem(7, new ItemStack(Items.STONE));
        Inventory.Holder holder = new Inventory.Holder.Player(live);
        Optional<Inventory.Match> taken = inventory.take(holder, List.of(input(Fate.consume(5), Items.STICK), input(Fate.keep(), Items.STONE)));
        scene.check("a consume is taken across the slots and the offhand; the kept stone stays", () -> scene.assertTrue(taken.isPresent()
                && items.getItem(3).isEmpty() && items.getItem(20).isEmpty() && live.getOffhandItem().isEmpty() && items.getItem(7).is(Items.STONE), "slots " + items.getItem(3) + ", " + items.getItem(20) + ", " + live.getOffhandItem()));
        items.setItem(3, new ItemStack(Items.STICK, 2));
        items.setItem(20, new ItemStack(Items.STICK, 2));
        live.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.STICK));
        Optional<Inventory.Match> rolled = inventory.take(holder, List.of(new ItemInput(ItemPredicate.Builder.item().of(BuiltInRegistries.ITEM, Items.STICK).build(), new Fate(Fate.Kind.CONSUME, ROLLS_TWO_OF_ONE_TO_FOUR))));
        scene.check("a ranged consume needs its largest amount to match and takes what it rolls", () -> scene.assertTrue(rolled.isPresent() && inventory.count(holder, STICKS) == 3, "left " + inventory.count(holder, STICKS)));
        items.setSelectedSlot(8);
        items.setItem(8, new ItemStack(Items.STICK, 5));
        items.setItem(4, new ItemStack(Items.STICK, 10));
        Inventory.Holder hand = new Inventory.Holder.Hand(live, InteractionHand.MAIN_HAND);
        scene.check("a hand holder is refused what its slot alone cannot cover though the player holds it", () -> scene.assertTrue(inventory.take(hand, List.of(input(Fate.consume(6), Items.STICK))).isEmpty()
                && items.getItem(8).getCount() == 5 && items.getItem(4).getCount() == 10, "hand " + items.getItem(8) + ", slot 4 " + items.getItem(4)));
        Optional<Inventory.Match> fromHand = inventory.take(hand, List.of(input(Fate.consume(3), Items.STICK)));
        scene.check("and pays from that slot alone, not from the stack before it", () -> scene.assertTrue(fromHand.isPresent() && items.getItem(8).getCount() == 2 && items.getItem(4).getCount() == 10,
                "hand " + items.getItem(8) + ", slot 4 " + items.getItem(4)));
        Optional<Inventory.Match> match = inventory.match(holder, List.of(input(Fate.consume(2), Items.STICK)));
        items.clearContent();
        live.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        items.setItem(9, new ItemStack(Items.STICK));
        boolean paid = match.isPresent() && inventory.pay(match.get());
        scene.check("a coverage lost between match and pay answers false with nothing changed", () -> scene.assertTrue(match.isPresent() && !paid && items.getItem(9).getCount() == 1, "paid " + paid + ", slot 9 " + items.getItem(9)));
        scene.succeed();
    }

    /** A sided furnace, seen from below, shows its result and fuel slots but gives up only buckets from the fuel slot and takes nothing into the result slot. */
    @Scene(maxTicks = 20)
    public void aSidedHolderRollsBackAShortfallMidPayAndDropsAWornToolItsSlotRefuses(TestScene scene) {
        Inventory inventory = JolCraftGameTests.inventory();
        ServerLevel level = scene.level();
        BlockPos furnace = scene.helper().absolutePos(new BlockPos(2, 1, 2));
        level.setBlock(furnace, Blocks.FURNACE.defaultBlockState(), 3);
        FurnaceBlockEntity entity = scene.helper().getBlockEntity(new BlockPos(2, 1, 2), FurnaceBlockEntity.class);
        entity.setItem(2, new ItemStack(Items.IRON_INGOT));
        entity.setItem(1, new ItemStack(Items.STICK));
        Inventory.Holder below = new Inventory.Holder.Block(level, furnace, Direction.DOWN);
        Optional<Inventory.Match> match = inventory.match(below, List.of(input(Fate.consume(1), Items.IRON_INGOT), input(Fate.consume(1), Items.STICK)));
        boolean paid = match.isPresent() && inventory.pay(match.get());
        scene.check("both slots are seen and matched, the ingot is extracted, the stick refused through the face, and the pay rolls back whole", () -> scene.assertTrue(
                match.isPresent() && !paid && entity.getItem(2).is(Items.IRON_INGOT) && entity.getItem(2).getCount() == 1 && entity.getItem(1).is(Items.STICK),
                "paid " + paid + ", slots " + entity.getItem(2) + ", " + entity.getItem(1)));
        entity.setItem(1, ItemStack.EMPTY);
        entity.setItem(2, new ItemStack(Items.IRON_PICKAXE));
        Optional<Inventory.Match> worn = inventory.take(below, List.of(input(Fate.damage(1), Items.IRON_PICKAXE)));
        List<ItemEntity> entities = scene.helper().getEntities(EntityType.ITEM, new BlockPos(2, 1, 2), 1);
        scene.check("a worn tool its slot refuses back, and no other slot takes, drops at the block's centre", () -> scene.assertTrue(worn.isPresent() && entity.getItem(2).isEmpty()
                && entities.size() == 1 && entities.getFirst().getItem().is(Items.IRON_PICKAXE) && entities.getFirst().getItem().getDamageValue() == 1
                && entities.getFirst().position().equals(furnace.getCenter()), "slot 2 " + entity.getItem(2) + ", entities " + entities));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aDamageWearsTheToolInItsSlotAWornOutOneBreaksAudiblyAndACreativePayerPaysNothing(TestScene scene) {
        Inventory inventory = JolCraftGameTests.inventory();
        TestPlayer player = scene.addPlayer("Smith").survival();
        ServerPlayer live = player.player();
        net.minecraft.world.entity.player.Inventory items = live.getInventory();
        items.setItem(7, new ItemStack(Items.IRON_PICKAXE));
        Inventory.Holder holder = new Inventory.Holder.Player(live);
        ItemInput wear = input(Fate.damage(5), Items.IRON_PICKAXE);
        Optional<Inventory.Match> worn = inventory.take(holder, List.of(wear));
        scene.check("the tool is worn by the amount in its own slot", () -> scene.assertTrue(worn.isPresent() && items.getItem(7).is(Items.IRON_PICKAXE)
                && items.getItem(7).getDamageValue() == 5 && items.getItem(7).getCount() == 1, "slot 7 " + items.getItem(7) + " damage " + items.getItem(7).getDamageValue()));
        ItemStack nearlyWorn = new ItemStack(Items.IRON_PICKAXE);
        nearlyWorn.setDamageValue(nearlyWorn.getMaxDamage() - 3);
        items.setItem(7, ItemStack.EMPTY);
        items.setItem(8, nearlyWorn);
        player.clearReceived();
        Optional<Inventory.Match> broken = inventory.take(holder, List.of(wear));
        scene.check("a worn-out tool breaks: the slot empties and the player hears it", () -> scene.assertTrue(broken.isPresent() && items.getItem(8).isEmpty()
                && player.received(ClientboundSoundPacket.class).stream().anyMatch(packet -> packet.getSound().value() == SoundEvents.ITEM_BREAK.value()),
                "slot 8 " + items.getItem(8) + ", sounds " + player.received(ClientboundSoundPacket.class).size()));
        live.setGameMode(GameType.CREATIVE);
        items.setItem(8, new ItemStack(Items.IRON_PICKAXE));
        items.setItem(9, new ItemStack(Items.STICK, 3));
        items.setItem(10, new ItemStack(Items.WATER_BUCKET));
        Optional<Inventory.Match> free = inventory.take(holder, List.of(wear, input(Fate.consume(2), Items.STICK), input(Fate.consume(1), Items.WATER_BUCKET)));
        scene.check("a creative payer still needs the items but pays nothing and gets no remainder", () -> scene.assertTrue(free.isPresent()
                && items.getItem(8).is(Items.IRON_PICKAXE) && items.getItem(8).getCount() == 1 && items.getItem(8).getDamageValue() == 0
                && items.getItem(9).getCount() == 3 && items.getItem(10).is(Items.WATER_BUCKET)
                && inventory.count(holder, stack -> stack.is(Items.BUCKET)) == 0, "slots " + items.getItem(8) + ", " + items.getItem(9) + ", " + items.getItem(10)));
        items.setItem(9, ItemStack.EMPTY);
        scene.check("and is refused what they do not hold", () -> scene.assertTrue(inventory.take(holder, List.of(input(Fate.consume(2), Items.STICK))).isEmpty(), "without sticks"));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aConsumedBucketsEmptyBucketComesBackToTheHandAndAChestHolderIsPaid(TestScene scene) {
        Inventory inventory = JolCraftGameTests.inventory();
        ServerPlayer live = scene.addPlayer("Carrier").survival().player();
        ServerLevel level = scene.level();
        net.minecraft.world.entity.player.Inventory items = live.getInventory();
        items.setSelectedSlot(2);
        items.setItem(2, new ItemStack(Items.WATER_BUCKET));
        Inventory.Holder hand = new Inventory.Holder.Hand(live, InteractionHand.MAIN_HAND);
        Optional<Inventory.Match> poured = inventory.take(hand, List.of(input(Fate.consume(1), Items.WATER_BUCKET)));
        scene.check("the water is consumed and the empty bucket lands in the hand, not the first main slot", () -> scene.assertTrue(poured.isPresent()
                && items.getItem(2).is(Items.BUCKET) && items.getItem(2).getCount() == 1 && items.getItem(0).isEmpty(), "hand " + items.getItem(2) + ", slot 0 " + items.getItem(0)));
        items.setSelectedSlot(4);
        for (int slot = 0; slot < 36; slot++) {
            items.setItem(slot, slot == 4 ? new ItemStack(Items.HONEY_BOTTLE, 5) : new ItemStack(Items.STONE, 64));
        }
        Optional<Inventory.Match> drunk = inventory.take(new Inventory.Holder.Hand(live, InteractionHand.MAIN_HAND), List.of(input(Fate.consume(2), Items.HONEY_BOTTLE)));
        List<ItemEntity> entities = scene.helper().getEntities(EntityType.ITEM, new BlockPos(0, 1, 0), 2);
        scene.check("a remainder comes back one per item consumed, and with no room it is tossed at the player", () -> scene.assertTrue(drunk.isPresent()
                && items.getItem(4).is(Items.HONEY_BOTTLE) && items.getItem(4).getCount() == 3 && entities.size() == 1 && entities.getFirst().getItem().is(Items.GLASS_BOTTLE)
                && entities.getFirst().getItem().getCount() == 2 && live.getUUID().equals(entities.getFirst().getTarget()), "hand " + items.getItem(4) + ", entities " + entities));
        BlockPos chest = scene.helper().absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
        ChestBlockEntity entity = scene.helper().getBlockEntity(new BlockPos(1, 1, 1), ChestBlockEntity.class);
        entity.setItem(0, new ItemStack(Items.STICK, 10));
        entity.setItem(5, new ItemStack(Items.IRON_PICKAXE));
        Inventory.Holder block = new Inventory.Holder.Block(level, chest, null);
        Optional<Inventory.Match> paid = inventory.take(block, List.of(input(Fate.consume(4), Items.STICK), input(Fate.damage(3), Items.IRON_PICKAXE)));
        scene.check("a chest pays in full, the tool worn in its slot, nobody paying creatively", () -> scene.assertTrue(paid.isPresent()
                && entity.getItem(0).getCount() == 6 && entity.getItem(5).is(Items.IRON_PICKAXE) && entity.getItem(5).getDamageValue() == 3, "slots " + entity.getItem(0) + ", " + entity.getItem(5)));
        scene.check("and what it cannot cover is refused whole", () -> scene.assertTrue(inventory.take(block, List.of(input(Fate.consume(4), Items.STICK), input(Fate.consume(1), Items.STONE))).isEmpty()
                && entity.getItem(0).getCount() == 6, "refused, slot 0 " + entity.getItem(0)));
        scene.succeed();
    }
}
