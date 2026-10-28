package dev.amethyst.client.freecam;

/** Camera-relative flight math, independent of a Minecraft world or graphics context. */
public final class FreecamMovement {
    private FreecamMovement() { }

    public record Offset(double x, double y, double z) { }

    public static Offset calculate(float yaw, float pitch, int forward, int left, int vertical, double speed) {
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch) || !Double.isFinite(speed) || speed <= 0)
            return new Offset(0, 0, 0);
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(Math.clamp(pitch, -90f, 90f));
        double sinYaw = Math.sin(yawRadians), cosYaw = Math.cos(yawRadians);
        double cosPitch = Math.cos(pitchRadians);
        double x = -sinYaw * cosPitch * forward + cosYaw * left;
        double y = -Math.sin(pitchRadians) * forward + vertical;
        double z = cosYaw * cosPitch * forward + sinYaw * left;
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length < 1.0E-8) return new Offset(0, 0, 0);
        double scale = speed / Math.max(1, length);
        return new Offset(x * scale, y * scale, z * scale);
    }
}
