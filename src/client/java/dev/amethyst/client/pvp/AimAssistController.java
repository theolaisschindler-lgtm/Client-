package dev.amethyst.client.pvp;

import dev.amethyst.client.config.AimAssistSettings;
import net.minecraft.client.Minecraft;

/** Visible, rate-limited aim movement; never changes reach or performs an attack. */
public final class AimAssistController {
    public void tick(Minecraft client, AimAssistSettings settings) {
        if (!settings.enabled || !CombatActions.ready(client)
                || (settings.requireAttackKey && !client.options.keyAttack.isDown())) return;
        var chosen = PlayerAimTargets.find(client, settings.range, settings.fov, settings.lineOfSight);
        if (chosen.isEmpty()) return;
        var player = client.player;
        var step = AimGeometry.step(player.getYRot(), player.getXRot(), chosen.get().rotation(), settings.turnSpeed);
        player.setYRot(step.yaw());
        player.setXRot(step.pitch());
        player.setYHeadRot(step.yaw());
    }

    public void clear() { }
}
