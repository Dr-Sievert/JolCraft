package net.sievert.jolcraft.gametest.module.system.hand;

import java.util.List;
import java.util.Optional;

import io.netty.buffer.Unpooled;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.connection.ConnectionType;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestPlayer;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.composite.capability.Inventory;
import net.sievert.jolcraft.module.system.hand.HandKeys;
import net.sievert.jolcraft.module.system.hand.HandRecipe;

/**
 * The hand interaction on a real player, through the server's own use path with its cooldown check, against the
 * fixture recipes in {@code scene/data/jolcraft_gametest/recipe/hand} (the scene pack): shears on wool and its
 * sneaking variant, flint kept on gravel with an empty pool, a water bucket on dirt, a blaze rod on gunpowder with a
 * result of each kind, an apple kept on a stick, and bread on paper naming the game-test mod's refusing hook.
 */
public final class HandScenes {

    private static final Identifier BREAD_ON_PAPER = Identifier.fromNamespaceAndPath(JolCraftGameTests.MOD_ID, "hand/bread_on_paper");

    private static void hold(ServerPlayer player, ItemStack main, ItemStack off) {
        player.getInventory().setSelectedSlot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, main);
        player.setItemInHand(InteractionHand.OFF_HAND, off);
    }

    /** The server's own use of the hand's item, as the packet handler calls it, the cooldown checked first. */
    private static InteractionResult use(ServerPlayer player, InteractionHand hand) {
        return player.gameMode.useItem(player, player.level(), player.getItemInHand(hand), hand);
    }

    /** The items of the kind the player holds, the main slots and the offhand. */
    private static int count(ServerPlayer player, Item item) {
        return JolCraftGameTests.inventory().count(new Inventory.Holder.Player(player), stack -> stack.is(item));
    }

    private static boolean heard(TestPlayer player, String sound) {
        return player.received(ClientboundSoundPacket.class).stream().anyMatch(packet -> packet.getSound().is(Identifier.withDefaultNamespace(sound)));
    }

    /** The swings the player was sent of their own arms, by action. */
    private static List<Integer> swings(TestPlayer player) {
        return player.received(ClientboundAnimatePacket.class).stream().filter(packet -> packet.getId() == player.player().getId())
                .map(ClientboundAnimatePacket::getAction).toList();
    }

    /** Whether the item's cooldown group is cooling down, asked through a fresh stack, since a consumed stack's group is air. */
    private static boolean coolingDown(ServerPlayer player, Item item) {
        return player.getCooldowns().isOnCooldown(new ItemStack(item));
    }

    private static void clearCooldowns(ServerPlayer player, Item... items) {
        for (Item item : items) {
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(new ItemStack(item)));
        }
    }

    @Scene(maxTicks = 20, isolated = true)
    public void eitherOrientationFiresWearingTheToolTakingTheTargetAndSwingingTheToolsHand(TestScene scene) {
        TestPlayer player = scene.addPlayer("Shearer").survival();
        ServerPlayer live = player.player();
        hold(live, new ItemStack(Items.SHEARS), new ItemStack(Items.WHITE_WOOL, 4));
        player.clearReceived();
        InteractionResult fired = use(live, InteractionHand.MAIN_HAND);
        ItemStack shears = live.getMainHandItem();
        scene.check("the click is consumed, the shears worn by one to three, a wool taken and two string given", () -> scene.assertTrue(InteractionResult.CONSUME.equals(fired)
                && shears.is(Items.SHEARS) && shears.getDamageValue() >= 1 && shears.getDamageValue() <= 3 && live.getOffhandItem().getCount() == 3 && count(live, Items.STRING) == 2,
                "result " + fired + ", main " + shears + " damage " + shears.getDamageValue() + ", off " + live.getOffhandItem() + ", string " + count(live, Items.STRING)));
        scene.check("the success sound is heard, the main hand swings and both items cool down", () -> scene.assertTrue(heard(player, "entity.sheep.shear")
                && swings(player).equals(List.of(ClientboundAnimatePacket.SWING_MAIN_HAND)) && coolingDown(live, Items.SHEARS) && coolingDown(live, Items.WHITE_WOOL),
                "swings " + swings(player)));
        clearCooldowns(live, Items.SHEARS, Items.WHITE_WOOL);
        live.getInventory().clearContent();
        hold(live, new ItemStack(Items.WHITE_WOOL, 4), new ItemStack(Items.SHEARS));
        player.clearReceived();
        InteractionResult reversed = use(live, InteractionHand.MAIN_HAND);
        scene.check("with the shears in the offhand it fires too, wearing them there and swinging the offhand", () -> scene.assertTrue(InteractionResult.CONSUME.equals(reversed)
                && live.getOffhandItem().is(Items.SHEARS) && live.getOffhandItem().getDamageValue() >= 1 && live.getMainHandItem().getCount() == 3
                && swings(player).equals(List.of(ClientboundAnimatePacket.SWING_OFF_HAND)), "result " + reversed + ", off " + live.getOffhandItem() + ", swings " + swings(player)));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void withTheAppleKeptInTheOffhandItsUseRightAfterPassesAndThePlayerIsNotEating(TestScene scene) {
        ServerPlayer live = scene.addPlayer("Grafter").survival().player();
        live.getFoodData().setFoodLevel(10);
        hold(live, new ItemStack(Items.STICK, 2), new ItemStack(Items.APPLE));
        InteractionResult fired = use(live, InteractionHand.MAIN_HAND);
        InteractionResult offhand = use(live, InteractionHand.OFF_HAND);
        scene.check("the main hand's click fires: a stick taken, the apple kept, a sapling given", () -> scene.assertTrue(InteractionResult.CONSUME.equals(fired)
                && live.getMainHandItem().is(Items.STICK) && live.getMainHandItem().getCount() == 1 && live.getOffhandItem().is(Items.APPLE) && live.getOffhandItem().getCount() == 1
                && count(live, Items.OAK_SAPLING) == 1, "result " + fired + ", main " + live.getMainHandItem() + ", off " + live.getOffhandItem()));
        scene.check("the offhand's use right after passes on the cooldown, and the hungry player is not eating", () -> scene.assertTrue(InteractionResult.PASS.equals(offhand)
                && !live.isUsingItem() && coolingDown(live, Items.APPLE), "offhand " + offhand + ", using " + live.isUsingItem()));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void anEmptyRollIsPaidPlaysTheFailureSoundAndStillSwings(TestScene scene) {
        TestPlayer player = scene.addPlayer("Knapper").survival();
        ServerPlayer live = player.player();
        hold(live, new ItemStack(Items.FLINT), new ItemStack(Items.GRAVEL, 2));
        player.clearReceived();
        InteractionResult fired = use(live, InteractionHand.MAIN_HAND);
        scene.check("the gravel is paid, the flint kept, the failure sound heard and not the success, the main hand swung", () -> scene.assertTrue(
                InteractionResult.CONSUME.equals(fired) && live.getOffhandItem().getCount() == 1 && live.getMainHandItem().is(Items.FLINT) && live.getMainHandItem().getCount() == 1
                        && heard(player, "block.gravel.break") && !heard(player, "entity.experience_orb.pickup") && swings(player).equals(List.of(ClientboundAnimatePacket.SWING_MAIN_HAND)),
                "result " + fired + ", off " + live.getOffhandItem() + ", swings " + swings(player)));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theSneakingVariantWinsOnlyWhileThePlayerSneaks(TestScene scene) {
        ServerPlayer live = scene.addPlayer("Weaver").survival().player();
        hold(live, new ItemStack(Items.SHEARS), new ItemStack(Items.WHITE_WOOL, 4));
        live.setShiftKeyDown(true);
        use(live, InteractionHand.MAIN_HAND);
        scene.check("sneaking, the carpet recipe fires", () -> scene.assertTrue(count(live, Items.WHITE_CARPET) == 2 && count(live, Items.STRING) == 0,
                "carpet " + count(live, Items.WHITE_CARPET) + ", string " + count(live, Items.STRING)));
        clearCooldowns(live, Items.SHEARS, Items.WHITE_WOOL);
        live.setShiftKeyDown(false);
        use(live, InteractionHand.MAIN_HAND);
        scene.check("standing, the plain one does", () -> scene.assertTrue(count(live, Items.STRING) == 2 && count(live, Items.WHITE_CARPET) == 2,
                "carpet " + count(live, Items.WHITE_CARPET) + ", string " + count(live, Items.STRING)));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aRefusalPaysNothingPlaysTheFailureSoundCoolsBothDownAndNeitherSwingsNorEats(TestScene scene) {
        TestPlayer player = scene.addPlayer("Baker").survival();
        ServerPlayer live = player.player();
        live.getFoodData().setFoodLevel(10);
        hold(live, new ItemStack(Items.BREAD, 2), new ItemStack(Items.PAPER, 3));
        player.clearReceived();
        InteractionResult refused = use(live, InteractionHand.MAIN_HAND);
        scene.check("the click is consumed with both hands untouched and no map given", () -> scene.assertTrue(InteractionResult.CONSUME.equals(refused)
                && live.getMainHandItem().getCount() == 2 && live.getOffhandItem().getCount() == 3 && count(live, Items.MAP) == 0,
                "result " + refused + ", main " + live.getMainHandItem() + ", off " + live.getOffhandItem()));
        scene.check("the failure sound is heard, both items cool down, nothing swings and the hungry player is not eating", () -> scene.assertTrue(
                heard(player, "item.book.page_turn") && !heard(player, "entity.experience_orb.pickup") && swings(player).isEmpty()
                        && coolingDown(live, Items.BREAD) && coolingDown(live, Items.PAPER) && !live.isUsingItem(), "swings " + swings(player) + ", using " + live.isUsingItem()));
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void aRecipeWithATagPredicateAndAHookNamingYieldSurvivesItsStreamCodec(TestScene scene) {
        ServerLevel level = scene.level();
        List<RecipeHolder<HandRecipe>> found = HandKeys.RECIPE.all(level).filter(holder -> holder.id().identifier().equals(BREAD_ON_PAPER)).toList();
        scene.check("the bread recipe is loaded", () -> scene.assertTrue(found.size() == 1, "found " + found.size()));
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.OTHER);
        HandRecipe.STREAM_CODEC.encode(buffer, found.getFirst().value());
        HandRecipe back = HandRecipe.STREAM_CODEC.decode(buffer);
        TagKey<Item> bread = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "foods/bread"));
        scene.check("the tool's tag, the result's hook and the sounds come back", () -> scene.assertTrue(
                back.tool().predicate().items().flatMap(HolderSet::unwrapKey).equals(Optional.of(bread))
                        && back.results().getFirst().hooks().equals(List.of(TestHandHooks.REFUSING)) && back.success().isPresent() && back.failure().isPresent(),
                "back " + back));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void eachOfTheFourKindsIsDelivered(TestScene scene) {
        TestPlayer player = scene.addPlayer("Alchemist").survival();
        ServerPlayer live = player.player();
        hold(live, new ItemStack(Items.BLAZE_ROD), new ItemStack(Items.GUNPOWDER, 2));
        player.clearReceived();
        InteractionResult fired = use(live, InteractionHand.MAIN_HAND);
        List<ArmorStand> stands = scene.level().getEntities(EntityType.ARMOR_STAND, live.getBoundingBox().inflate(2), _ -> true);
        scene.check("blaze powder, an armor stand, haste and the bell all arrive, the rod kept and a gunpowder taken", () -> scene.assertTrue(InteractionResult.CONSUME.equals(fired)
                && count(live, Items.BLAZE_POWDER) == 1 && stands.size() == 1 && live.hasEffect(MobEffects.HASTE) && heard(player, "block.note_block.bell")
                && live.getMainHandItem().is(Items.BLAZE_ROD) && live.getOffhandItem().getCount() == 1,
                "result " + fired + ", powder " + count(live, Items.BLAZE_POWDER) + ", stands " + stands + ", effects " + live.getActiveEffects()));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theBucketsRemainderLandsInItsHand(TestScene scene) {
        ServerPlayer live = scene.addPlayer("Potter").survival().player();
        hold(live, new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.DIRT, 2));
        InteractionResult fired = use(live, InteractionHand.MAIN_HAND);
        scene.check("the water is consumed, its empty bucket is in the main hand, a dirt taken and mud given", () -> scene.assertTrue(InteractionResult.CONSUME.equals(fired)
                && live.getMainHandItem().is(Items.BUCKET) && live.getOffhandItem().getCount() == 1 && count(live, Items.MUD) == 1,
                "result " + fired + ", main " + live.getMainHandItem() + ", off " + live.getOffhandItem() + ", mud " + count(live, Items.MUD)));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aCreativePlayerPaysNothingAndStillGetsTheResult(TestScene scene) {
        ServerPlayer live = scene.addPlayer("Maker").player();
        hold(live, new ItemStack(Items.SHEARS), new ItemStack(Items.WHITE_WOOL, 4));
        InteractionResult fired = use(live, InteractionHand.MAIN_HAND);
        scene.check("the shears are unworn, the wool whole and the string given", () -> scene.assertTrue(InteractionResult.CONSUME.equals(fired)
                && live.getMainHandItem().getDamageValue() == 0 && live.getOffhandItem().getCount() == 4 && count(live, Items.STRING) == 2,
                "result " + fired + ", main " + live.getMainHandItem() + ", off " + live.getOffhandItem() + ", string " + count(live, Items.STRING)));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void anEmptyHandAnOffhandCoolingDownOrAPairNoRecipeHoldsLeavesTheClickToVanilla(TestScene scene) {
        ServerPlayer live = scene.addPlayer("Idler").survival().player();
        hold(live, new ItemStack(Items.SHEARS), ItemStack.EMPTY);
        InteractionResult empty = use(live, InteractionHand.MAIN_HAND);
        scene.check("with an empty offhand the shears' own use answers", () -> scene.assertTrue(InteractionResult.PASS.equals(empty) && !coolingDown(live, Items.SHEARS),
                "result " + empty));
        hold(live, new ItemStack(Items.SHEARS), new ItemStack(Items.WHITE_WOOL, 4));
        live.getCooldowns().addCooldown(new ItemStack(Items.WHITE_WOOL), 100);
        InteractionResult cooling = use(live, InteractionHand.MAIN_HAND);
        scene.check("with the wool cooling down nothing fires", () -> scene.assertTrue(InteractionResult.PASS.equals(cooling) && live.getOffhandItem().getCount() == 4
                && count(live, Items.STRING) == 0 && !coolingDown(live, Items.SHEARS), "result " + cooling));
        hold(live, new ItemStack(Items.SHEARS), new ItemStack(Items.STONE));
        InteractionResult unknown = use(live, InteractionHand.MAIN_HAND);
        scene.check("and a pair no recipe holds is left alone", () -> scene.assertTrue(InteractionResult.PASS.equals(unknown) && live.getOffhandItem().getCount() == 1
                && count(live, Items.STRING) == 0 && !coolingDown(live, Items.STONE), "result " + unknown));
        scene.succeed();
    }
}
