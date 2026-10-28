package dev.amethyst.client.pvp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class PlayerAimTargets {
    private PlayerAimTargets() { }

    record Target(Player player, Vec3 point, AimGeometry.Angles rotation) { }

    static Optional<Target> find(Minecraft client, double configuredRange, double fov, boolean lineOfSight) {
        var body = client.player;
        if (body == null || client.level == null) return Optional.empty();
        double reach = Math.min(configuredRange, body.entityInteractionRange());
        Vec3 eye = body.getEyePosition();
        var candidates = new ArrayList<AimTargetSelection.Candidate>();
        var targets = new HashMap<Integer, Target>();
        for (Player other : client.level.players()) {
            if (other == body || !other.isAlive() || other.isRemoved() || other.isSpectator()
                    || other.isInvisibleTo(body) || !other.isPickable()) continue;
            var point = AimGeometry.reachablePoint(eye, other.getBoundingBox(), reach);
            if (point.isEmpty()) continue;
            AimGeometry.Angles rotation = AimGeometry.toward(eye, point.get());
            boolean visible = !lineOfSight || client.level.clip(new ClipContext(eye, point.get(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, body)).getType() == HitResult.Type.MISS;
            candidates.add(new AimTargetSelection.Candidate(other.getId(), eye.distanceToSqr(point.get()),
                    AimGeometry.angularDistance(body.getYRot(), body.getXRot(), rotation), true, visible));
            targets.put(other.getId(), new Target(other, point.get(), rotation));
        }
        var chosen = AimTargetSelection.select(candidates, configuredRange, body.entityInteractionRange(), fov, lineOfSight);
        return chosen.isPresent() ? Optional.ofNullable(targets.get(chosen.getAsInt())) : Optional.empty();
    }
}
