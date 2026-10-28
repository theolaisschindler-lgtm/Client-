package dev.amethyst.client.mixin;

import dev.amethyst.client.pvp.WTapController;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class WTapInputMixin extends ClientInput {
    @Inject(method = "tick", at = @At("TAIL"))
    private void amethyst$releaseForward(CallbackInfo ci) {
        if (!WTapController.releaseForward()) return;
        keyPresses = new Input(false, keyPresses.backward(), keyPresses.left(), keyPresses.right(),
                keyPresses.jump(), keyPresses.shift(), false);
        float sideways = keyPresses.left() == keyPresses.right() ? 0 : keyPresses.left() ? 1 : -1;
        moveVector = new Vec2(sideways, keyPresses.backward() ? -1 : 0).normalized();
    }
}
