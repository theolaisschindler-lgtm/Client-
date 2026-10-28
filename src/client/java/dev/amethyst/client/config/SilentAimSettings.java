package dev.amethyst.client.config;

public final class SilentAimSettings {
    public boolean enabled = false;
    public double range = 3.0;
    public double fov = 45.0;
    public boolean lineOfSight = true;
    public boolean requireCooldown = true;

    public void sanitize() {
        range = Double.isFinite(range) ? Math.clamp(range, 1.0, 6.0) : 3.0;
        fov = Double.isFinite(fov) ? Math.clamp(fov, 5.0, 120.0) : 45.0;
    }
}
