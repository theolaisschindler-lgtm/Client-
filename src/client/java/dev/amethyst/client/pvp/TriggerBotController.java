package dev.amethyst.client.pvp;

import dev.amethyst.client.config.TriggerBotSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Uses the real crosshair result and vanilla attack cooldown. */
public final class TriggerBotController {
    private LocalPlayer player;
    private int nextAttack;

    public void tick(Minecraft client, TriggerBotSettings settings) {
        if (client.player != player) {
            clear();
            player = client.player;
        }
        if (!settings.enabled || !CombatActions.ready(client)) {
            nextAttack = player == null ? 0 : player.tickCount + settings.delayTicks;
            return;
        }
        if (!CombatActions.isAvailable() || player.tickCount < nextAttack
                || (settings.requireAttackKey && !client.options.keyAttack.isDown())
                || player.getAttackStrengthScale(0) < settings.cooldownThreshold) return;
        var target = WeaponTargeting.crosshair(client, settings.playersOnly, player.entityInteractionRange());
        if (target != null && CombatActions.attack(client, target, false)) {
            nextAttack = player.tickCount + settings.delayTicks;
        }
    }

    public void clear() { player = null; nextAttack = 0; }
}
