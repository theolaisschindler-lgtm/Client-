package dev.amethyst.client.pvp;

import java.util.Optional;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Geometry shared by visible aim assistance and attack-time server rotations. */
public final class AimGeometry {
    private static final double EPSILON = 1.0e-5;

    private AimGeometry() { }

    public record Angles(float yaw, float pitch) { }

    public static Angles toward(Vec3 eye, Vec3 point) {
        Vec3 delta = point.subtract(eye);
        double horizontal = Math.hypot(delta.x, delta.z);
        return new Angles((float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90),
                (float) -Math.toDegrees(Math.atan2(delta.y, horizontal)));
    }

    public static double wrapDegrees(double angle) {
        double wrapped = angle % 360;
        if (wrapped >= 180) wrapped -= 360;
        if (wrapped < -180) wrapped += 360;
        return wrapped;
    }

    public static double angularDistance(float yaw, float pitch, Angles target) {
        double firstPitch = Math.toRadians(pitch);
        double secondPitch = Math.toRadians(target.pitch());
        double yawDifference = Math.toRadians(wrapDegrees(target.yaw() - yaw));
        double dot = Math.sin(firstPitch) * Math.sin(secondPitch)
                + Math.cos(firstPitch) * Math.cos(secondPitch) * Math.cos(yawDifference);
        return Math.toDegrees(Math.acos(Math.clamp(dot, -1, 1)));
    }

    /** A speed limit applies to the combined yaw/pitch step, including across the +/-180 seam. */
    public static Angles step(float yaw, float pitch, Angles target, double speed) {
        double dy = wrapDegrees(target.yaw() - yaw);
        double dp = Math.clamp(target.pitch(), -90, 90) - pitch;
        double length = Math.hypot(dy, dp);
        double scale = length > 0 ? Math.min(1, Math.max(0, speed) / length) : 0;
        return new Angles((float) (yaw + dy * scale), (float) Math.clamp(pitch + dp * scale, -90, 90));
    }

    /** Prefer the torso; near the reach boundary, use an interior point on the nearest box face. */
    public static Optional<Vec3> reachablePoint(Vec3 eye, AABB box, double reach) {
        if (!Double.isFinite(reach) || reach <= 0 || box.hasNaN()
                || box.distanceToSqr(eye) > reach * reach) return Optional.empty();
        Vec3 center = box.getCenter();
        double padding = Math.min(0.2, box.getYsize() * 0.2);
        Vec3 torso = new Vec3(center.x, Math.clamp(eye.y, box.minY + padding, box.maxY - padding), center.z);
        if (eye.distanceToSqr(torso) <= reach * reach && eye.distanceToSqr(torso) > EPSILON * EPSILON) {
            return Optional.of(torso);
        }
        double insetX = Math.min(EPSILON, box.getXsize() / 2);
        double insetY = Math.min(EPSILON, box.getYsize() / 2);
        double insetZ = Math.min(EPSILON, box.getZsize() / 2);
        Vec3 nearest = new Vec3(Math.clamp(eye.x, box.minX + insetX, box.maxX - insetX),
                Math.clamp(eye.y, box.minY + insetY, box.maxY - insetY),
                Math.clamp(eye.z, box.minZ + insetZ, box.maxZ - insetZ));
        if (eye.distanceToSqr(nearest) <= reach * reach && eye.distanceToSqr(nearest) > EPSILON * EPSILON) {
            return Optional.of(nearest);
        }
        return Optional.empty();
    }
}
