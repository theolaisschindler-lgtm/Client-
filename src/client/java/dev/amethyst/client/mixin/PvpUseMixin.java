package dev.amethyst.client.mixin;

import dev.amethyst.client.pvp.CombatActions;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Manual use takes priority over end-of-tick combat automation. */
@Mixin(MultiPlayerGameMode.class)
public abstract class PvpUseMixin {
    @Inject(method = {"useItemOn", "useItem"}, at = @At("RETURN"))
    private void amethyst$trackUse(CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction()) CombatActions.markAction();
    }
}
