package net.sievert.jolcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.sievert.jolcraft.world.entity.JolCraftAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerMixin {

    // @WrapOperation, not @Redirect, so other mods can wrap addExhaustion here too.
    @WrapOperation(
            method = "causeFoodExhaustion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V"
            )
    )
    private void jolcraft$modifyFoodExhaustion(
            FoodData foodData,
            float exhaustion,
            Operation<Void> original
    ) {
        Player player = (Player) (Object) this;

        double exhaustionModifier = player.getAttributeValue(JolCraftAttributes.EXHAUSTION);

        float modifiedExhaustion = exhaustion * (1.0F + (float) exhaustionModifier);

        original.call(foodData, modifiedExhaustion);
    }
}
