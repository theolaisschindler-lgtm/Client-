package dev.amethyst.client.mixin;

import dev.amethyst.client.freecam.FreecamController;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void amethyst$turnDetachedCamera(LocalPlayer player, double yaw, double pitch) {
        if (!FreecamController.turnCamera(player, yaw, pitch)) player.turn(yaw, pitch);
    }
}
