package net.sievert.jolcraft.gametest.module.primitive.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestScene;

/** The capability primitive against real holders: the declared providers answer for the entity, item and block-entity kinds they were given to and nothing else. */
public final class CapabilityScenes {

    @Scene(maxTicks = 20)
    public void declaredProvidersAnswerForTheirKindsOnly(TestScene scene) {
        ArmorStand stand = scene.helper().spawn(EntityType.ARMOR_STAND, new BlockPos(0, 1, 0));
        Pig pig = scene.helper().spawn(EntityType.PIG, new BlockPos(1, 1, 1));
        scene.check("the armor stand greets", () -> scene.assertEquals("hello from armor_stand", stand.getCapability(TestCapabilities.GREETING), "stand greeting"));
        scene.check("the pig does not", () -> scene.assertTrue(pig.getCapability(TestCapabilities.GREETING) == null, "pig greeting"));
        scene.check("a stick is labelled", () -> scene.assertEquals("a stick", new ItemStack(Items.STICK).getCapability(TestCapabilities.LABEL), "stick label"));
        scene.check("a stone is not", () -> scene.assertTrue(new ItemStack(Items.STONE).getCapability(TestCapabilities.LABEL) == null, "stone label"));
        BlockPos chest = scene.helper().absolutePos(new BlockPos(2, 1, 2));
        scene.level().setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
        scene.check("a chest's block entity answers from a side", () -> scene.assertEquals("a chest seen from up", scene.level().getCapability(TestCapabilities.SIGN, chest, Direction.UP), "chest sign"));
        scene.check("and an empty spot does not", () -> scene.assertTrue(scene.level().getCapability(TestCapabilities.SIGN, chest.above(), Direction.UP) == null, "air sign"));
        scene.succeed();
    }
}
