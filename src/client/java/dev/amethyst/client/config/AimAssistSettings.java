package dev.amethyst.client.config;

public final class AimAssistSettings {
    public boolean enabled = false;
    public double range = 3.0;
    public double fov = 70.0;
    public double turnSpeed = 5.0;
    public boolean requireAttackKey = true;
    public boolean lineOfSight = true;

    public void sanitize() {
        range = Double.isFinite(range) ? Math.clamp(range, 1.0, 6.0) : 3.0;
        fov = Double.isFinite(fov) ? Math.clamp(fov, 10.0, 180.0) : 70.0;
        turnSpeed = Double.isFinite(turnSpeed) ? Math.clamp(turnSpeed, 1.0, 30.0) : 5.0;
    }
}
