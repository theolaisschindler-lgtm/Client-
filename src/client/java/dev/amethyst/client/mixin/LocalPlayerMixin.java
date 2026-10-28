package dev.amethyst.client.mixin;

import dev.amethyst.client.freecam.FreecamController;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "isControlledCamera", at = @At("HEAD"), cancellable = true)
    private void amethyst$keepBodySimulation(CallbackInfoReturnable<Boolean> cir) {
        // Vanilla otherwise stops sending real-body position updates when the camera detaches.
        if (FreecamController.controlsBody((LocalPlayer) (Object) this)) cir.setReturnValue(true);
    }
}
