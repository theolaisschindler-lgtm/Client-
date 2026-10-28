package dev.amethyst.client.mixin;

import dev.amethyst.client.freecam.FreecamController;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void amethyst$disableAttack(CallbackInfoReturnable<Boolean> cir) {
        if (FreecamController.isActive()) cir.setReturnValue(false);
    }

    @Inject(method = {"continueAttack", "startUseItem", "pickBlock"}, at = @At("HEAD"), cancellable = true)
    private void amethyst$disableWorldInteraction(CallbackInfo ci) {
        if (FreecamController.isActive()) ci.cancel();
    }
}
