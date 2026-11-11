package net.sievert.jolcraft.gametest.module.system.rework;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestPlayer;
import net.sievert.jolcraft.gametest.scene.TestScene;

/**
 * The rework kind through the game's own crafting table menu and result slot, against the fixture recipes in
 * {@code scene/data/jolcraft_gametest/recipe/rework} (the scene pack): a leather helmet carried whole onto iron, a
 * chestplate keeping only its name, boots washed of their colour with a water bucket, paper named by the template, a
 * clock alone made a compass, honey made a potion keeping nothing and holding its bottle back, honey giving its bottle
 * back by default, and a stick whose carry the game refuses once the stick is damageable. The clock's recipe also shows
 * the recipe book's side: taking a result awards the recipe, and the limited-crafting rule holds it back until then.
 */
public final class ReworkScenes {

    private static final BlockPos TABLE = new BlockPos(2, 2, 2);
    private static final Component NAME = Component.literal("Old Faithful");
    private static final int RED = 0xB02E26;

    /** The crafting table's own menu, opened for a player standing on a floor beside the table. */
    private static CraftingMenu table(TestScene scene, TestPlayer player) {
        scene.setBlock(0, 0, 0, Blocks.STONE);
        scene.setBlock(TABLE.getX(), TABLE.getY(), TABLE.getZ(), Blocks.CRAFTING_TABLE);
        ServerPlayer live = player.player();
        live.openMenu(Blocks.CRAFTING_TABLE.defaultBlockState().getMenuProvider(scene.level(), scene.helper().absolutePos(TABLE)));
        return (CraftingMenu) live.containerMenu;
    }

    /** Puts the stacks into the grid's first slots in order; each placement asks the recipes again, as a click does. */
    private static void grid(CraftingMenu menu, ItemStack... stacks) {
        for (int index = 0; index < stacks.length; index++) {
            menu.getSlot(1 + index).set(stacks[index]);
        }
    }

    private static ItemStack result(CraftingMenu menu) {
        return menu.getSlot(CraftingMenu.RESULT_SLOT).getItem();
    }

    private static ItemStack slot(CraftingMenu menu, int grid) {
        return menu.getSlot(1 + grid).getItem();
    }

    /** Takes the result with a pickup click, as a player does, and answers what the cursor then held. */
    private static ItemStack take(CraftingMenu menu, ServerPlayer live) {
        menu.clicked(CraftingMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, live);
        ItemStack carried = menu.getCarried();
        menu.setCarried(ItemStack.EMPTY);
        return carried;
    }

    private static ItemStack named(ItemStack stack, Component name) {
        stack.set(DataComponents.CUSTOM_NAME, name);
        return stack;
    }

    private static ItemStack dyed(ItemStack stack) {
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(RED));
        return stack;
    }

    private static boolean red(ItemStack stack) {
        DyedItemColor color = stack.get(DataComponents.DYED_COLOR);
        return color != null && color.rgb() == RED;
    }

    private static ResourceKey<Recipe<?>> fixture(String name) {
        return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(JolCraftGameTests.MOD_ID, "rework/" + name));
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aDyedNamedHelmetAndAnIngotMakeAnIronHelmetCarryingBoth(TestScene scene) {
        TestPlayer player = scene.addPlayer("Smith").survival();
        ServerPlayer live = player.player();
        CraftingMenu menu = table(scene, player);
        grid(menu, named(dyed(new ItemStack(Items.LEATHER_HELMET)), NAME), new ItemStack(Items.IRON_INGOT));
        ItemStack made = take(menu, live);
        scene.check("an iron helmet carrying the name and the colour", () -> scene.assertTrue(made.is(Items.IRON_HELMET)
                && NAME.equals(made.get(DataComponents.CUSTOM_NAME)) && red(made), "made " + made + " " + made.getComponentsPatch()));
        scene.check("the helmet and the ingot are used up", () -> scene.assertTrue(slot(menu, 0).isEmpty() && slot(menu, 1).isEmpty(),
                "grid " + slot(menu, 0) + ", " + slot(menu, 1)));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void keepCarriesOnlyTheTypesItNames(TestScene scene) {
        TestPlayer player = scene.addPlayer("Armourer").survival();
        CraftingMenu menu = table(scene, player);
        grid(menu, named(dyed(new ItemStack(Items.LEATHER_CHESTPLATE)), NAME), new ItemStack(Items.IRON_INGOT));
        ItemStack made = take(menu, player.player());
        scene.check("an iron chestplate with the name and without the colour", () -> scene.assertTrue(made.is(Items.IRON_CHESTPLATE)
                && NAME.equals(made.get(DataComponents.CUSTOM_NAME)) && !made.has(DataComponents.DYED_COLOR), "made " + made + " " + made.getComponentsPatch()));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void removeWashesTheColourOffAndTheEmptyBucketComesBack(TestScene scene) {
        TestPlayer player = scene.addPlayer("Washer").survival();
        CraftingMenu menu = table(scene, player);
        grid(menu, named(dyed(new ItemStack(Items.LEATHER_BOOTS)), NAME), new ItemStack(Items.WATER_BUCKET));
        ItemStack made = take(menu, player.player());
        scene.check("the boots keep their name and lose their colour", () -> scene.assertTrue(made.is(Items.LEATHER_BOOTS)
                && NAME.equals(made.get(DataComponents.CUSTOM_NAME)) && !made.has(DataComponents.DYED_COLOR), "made " + made + " " + made.getComponentsPatch()));
        scene.check("the boots' slot is empty and the bucket's holds the empty bucket", () -> scene.assertTrue(slot(menu, 0).isEmpty() && slot(menu, 1).is(Items.BUCKET),
                "grid " + slot(menu, 0) + ", " + slot(menu, 1)));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aCraftThatChangesNothingGivesNoResultAndTakesNothing(TestScene scene) {
        TestPlayer player = scene.addPlayer("Idler").survival();
        CraftingMenu menu = table(scene, player);
        grid(menu, new ItemStack(Items.LEATHER_BOOTS), new ItemStack(Items.WATER_BUCKET));
        scene.check("undyed boots and a water bucket give no result", () -> scene.assertTrue(result(menu).isEmpty(), "result " + result(menu)));
        ItemStack taken = take(menu, player.player());
        scene.check("a click on the empty result takes nothing", () -> scene.assertTrue(taken.isEmpty() && slot(menu, 0).is(Items.LEATHER_BOOTS)
                && slot(menu, 1).is(Items.WATER_BUCKET), "taken " + taken + ", grid " + slot(menu, 0) + ", " + slot(menu, 1)));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void theTemplatesNameWinsOverTheCarriedOneAndPaperAlreadyNamedSoGivesNoResult(TestScene scene) {
        TestPlayer player = scene.addPlayer("Scribe").survival();
        CraftingMenu menu = table(scene, player);
        grid(menu, named(new ItemStack(Items.PAPER), Component.literal("Plain")), new ItemStack(Items.INK_SAC));
        ItemStack made = take(menu, player.player());
        scene.check("paper named by the template", () -> scene.assertTrue(made.is(Items.PAPER) && Component.literal("Written").equals(made.get(DataComponents.CUSTOM_NAME)),
                "made " + made + " " + made.getComponentsPatch()));
        grid(menu, named(new ItemStack(Items.PAPER), Component.literal("Written")), new ItemStack(Items.INK_SAC));
        scene.check("paper already named so gives no result", () -> scene.assertTrue(result(menu).isEmpty(), "result " + result(menu)));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aClockAloneMakesACompassCarryingItsName(TestScene scene) {
        TestPlayer player = scene.addPlayer("Navigator").survival();
        CraftingMenu menu = table(scene, player);
        grid(menu, named(new ItemStack(Items.CLOCK), NAME));
        ItemStack made = take(menu, player.player());
        scene.check("a compass with the clock's name", () -> scene.assertTrue(made.is(Items.COMPASS) && NAME.equals(made.get(DataComponents.CUSTOM_NAME)),
                "made " + made + " " + made.getComponentsPatch()));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void honeyKeepingNothingHoldsItsBottleBackAndTheDefaultGivesItBack(TestScene scene) {
        TestPlayer player = scene.addPlayer("Brewer").survival();
        ServerPlayer live = player.player();
        CraftingMenu menu = table(scene, player);
        grid(menu, named(new ItemStack(Items.HONEY_BOTTLE), NAME), new ItemStack(Items.SUGAR));
        ItemStack potion = take(menu, live);
        PotionContents contents = potion.get(DataComponents.POTION_CONTENTS);
        scene.check("the template's potion, without the honey's name", () -> scene.assertTrue(potion.is(Items.POTION) && contents != null && contents.is(Potions.SWIFTNESS)
                && !potion.has(DataComponents.CUSTOM_NAME), "made " + potion + " " + potion.getComponentsPatch()));
        scene.check("no glass bottle comes back in the honey's slot", () -> scene.assertTrue(slot(menu, 0).isEmpty() && slot(menu, 1).isEmpty(),
                "grid " + slot(menu, 0) + ", " + slot(menu, 1)));
        grid(menu, new ItemStack(Items.HONEY_BOTTLE), new ItemStack(Items.GLOWSTONE_DUST));
        ItemStack dye = take(menu, live);
        scene.check("by default the honey's glass bottle comes back in its slot", () -> scene.assertTrue(dye.is(Items.YELLOW_DYE) && slot(menu, 0).is(Items.GLASS_BOTTLE),
                "made " + dye + ", grid " + slot(menu, 0)));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void aCarryTheGameRefusesGivesNoResult(TestScene scene) {
        TestPlayer player = scene.addPlayer("Fletcher").survival();
        CraftingMenu menu = table(scene, player);
        grid(menu, new ItemStack(Items.STICK), new ItemStack(Items.FLINT));
        ItemStack made = take(menu, player.player());
        scene.check("a plain stick and flint make paper", () -> scene.assertTrue(made.is(Items.PAPER), "made " + made));
        ItemStack damageable = new ItemStack(Items.STICK);
        damageable.set(DataComponents.MAX_STACK_SIZE, 1);
        damageable.set(DataComponents.MAX_DAMAGE, 5);
        grid(menu, damageable, new ItemStack(Items.FLINT));
        scene.check("a damageable stick would make damageable paper that stacks, so there is no result", () -> scene.assertTrue(result(menu).isEmpty(), "result " + result(menu)));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void takingAResultAwardsTheRecipeToThePlayersBook(TestScene scene) {
        TestPlayer player = scene.addPlayer("Learner").survival();
        ServerPlayer live = player.player();
        CraftingMenu menu = table(scene, player);
        scene.check("the recipe is not in the book before", () -> scene.assertFalse(live.getRecipeBook().contains(fixture("clock")), "known before"));
        grid(menu, new ItemStack(Items.CLOCK));
        ItemStack made = take(menu, live);
        scene.check("a compass is taken and the recipe is in the book after", () -> scene.assertTrue(made.is(Items.COMPASS) && live.getRecipeBook().contains(fixture("clock")),
                "made " + made));
        scene.succeed();
    }

    @Scene(maxTicks = 20, isolated = true)
    public void underLimitedCraftingAnUnknownRecipeGivesNoResultUntilItIsAwarded(TestScene scene) {
        GameRules rules = scene.level().getGameRules();
        boolean before = rules.get(GameRules.LIMITED_CRAFTING);
        scene.onEnd(() -> rules.set(GameRules.LIMITED_CRAFTING, before, scene.level().getServer()));
        rules.set(GameRules.LIMITED_CRAFTING, true, scene.level().getServer());
        TestPlayer player = scene.addPlayer("Apprentice").survival();
        ServerPlayer live = player.player();
        CraftingMenu menu = table(scene, player);
        grid(menu, new ItemStack(Items.CLOCK));
        scene.check("no result while the recipe is unknown", () -> scene.assertTrue(result(menu).isEmpty(), "result " + result(menu)));
        RecipeHolder<?> clock = scene.level().recipeAccess().byKey(fixture("clock")).orElseThrow();
        live.awardRecipes(List.of(clock));
        grid(menu, new ItemStack(Items.CLOCK));
        scene.check("the compass once the recipe is awarded", () -> scene.assertTrue(result(menu).is(Items.COMPASS), "result " + result(menu)));
        scene.succeed();
    }
}
