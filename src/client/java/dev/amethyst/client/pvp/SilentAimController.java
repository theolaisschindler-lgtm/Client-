package dev.amethyst.client.pvp;

import dev.amethyst.client.config.SilentAimSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/** Redirects manual attacks with ordered server rotations while preserving the local view. */
public final class SilentAimController {
    public boolean interceptAttack(Minecraft client, SilentAimSettings settings) {
        if (!settings.enabled || !CombatActions.ready(client)) return false;
        var chosen = PlayerAimTargets.find(client, settings.range, settings.fov, settings.lineOfSight);
        if (chosen.isEmpty()) return false;
        var player = client.player;
        // Consume the click while waiting so it cannot attack a different crosshair entity instead.
        if (!CombatActions.isAvailable() || (settings.requireCooldown && player.getAttackStrengthScale(0.5f) < 0.9f)) {
            return true;
        }
        float originalYaw = player.getYRot();
        float originalPitch = player.getXRot();
        var rotation = chosen.get().rotation();
        player.connection.send(new ServerboundMovePlayerPacket.Rot(rotation.yaw(), rotation.pitch(),
                player.onGround(), player.horizontalCollision));
        try {
            CombatActions.attack(client, chosen.get().player(), settings.requireCooldown);
        } finally {
            player.connection.send(new ServerboundMovePlayerPacket.Rot(originalYaw, originalPitch,
                    player.onGround(), player.horizontalCollision));
        }
        return true;
    }

    public void clear() { }
}
