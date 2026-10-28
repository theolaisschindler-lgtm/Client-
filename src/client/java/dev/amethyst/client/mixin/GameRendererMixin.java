package dev.amethyst.client.mixin;

import dev.amethyst.client.freecam.FreecamController;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void amethyst$hideHandsWhenDetached(CallbackInfo ci) {
        if (FreecamController.isActive()) ci.cancel();
    }
}
