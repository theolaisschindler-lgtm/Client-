package dev.amethyst.client.config;

public final class TotemWarningSettings {
    public boolean enabled = false;
    public double healthThreshold = 12.0;
    public double volume = 0.6;
    public int cooldownTicks = 100;

    public void sanitize() {
        healthThreshold = Double.isFinite(healthThreshold) ? Math.clamp(healthThreshold, 2.0, 20.0) : 12.0;
        volume = Double.isFinite(volume) ? Math.clamp(volume, 0.0, 1.0) : 0.6;
        cooldownTicks = Math.clamp(cooldownTicks, 40, 400);
    }
}
