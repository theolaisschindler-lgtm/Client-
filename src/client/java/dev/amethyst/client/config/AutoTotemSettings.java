package dev.amethyst.client.config;

public final class AutoTotemSettings {
    public boolean enabled = false;
    public boolean healthOnly = false;
    public double healthThreshold = 12.0;
    public int delayTicks = 1;

    public void sanitize() {
        healthThreshold = Double.isFinite(healthThreshold) ? Math.clamp(healthThreshold, 2.0, 20.0) : 12.0;
        delayTicks = Math.clamp(delayTicks, 0, 20);
    }
}
