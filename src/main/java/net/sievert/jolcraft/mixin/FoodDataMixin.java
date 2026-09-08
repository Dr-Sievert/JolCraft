package net.sievert.jolcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.sievert.jolcraft.world.entity.JolCraftAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    // @WrapOperation, not @Redirect, so other mods can wrap addExhaustion here too. The trailing
    // player parameter captures FoodData#tick's argument; captured args come after the Operation.
    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V"
            )
    )
    private void jolcraft$modifyRegenerationExhaustion(
            FoodData foodData,
            float exhaustion,
            Operation<Void> original,
            Player player
    ) {
        double exhaustionModifier = player.getAttributeValue(JolCraftAttributes.EXHAUSTION);

        float modifiedExhaustion = exhaustion * (1.0F + (float) exhaustionModifier);

        original.call(foodData, modifiedExhaustion);
    }
}
