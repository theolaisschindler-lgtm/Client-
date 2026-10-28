package dev.amethyst.client.mixin;

import dev.amethyst.client.pvp.CombatActions;
import dev.amethyst.client.pvp.WTapController;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class PvpAttackMixin {
    @Unique private boolean amethyst$wasSprinting;

    @Inject(method = "attack", at = @At("HEAD"))
    private void amethyst$captureSprint(Player player, Entity target, CallbackInfo ci) {
        amethyst$wasSprinting = player.isSprinting();
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void amethyst$afterAttack(Player player, Entity target, CallbackInfo ci) {
        CombatActions.markAction();
        WTapController.onAttack(player, target, amethyst$wasSprinting);
    }
}
