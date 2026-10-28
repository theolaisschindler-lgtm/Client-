package dev.amethyst.client.config;

public final class AutoMaceSettings {
    public boolean enabled = false;
    public double minFallDistance = 2.5;
    public double range = 3.0;
    public boolean restoreSlot = true;
    public boolean requireAttackKey = true;

    public void sanitize() {
        minFallDistance = Double.isFinite(minFallDistance) ? Math.clamp(minFallDistance, 1.5, 10.0) : 2.5;
        range = Double.isFinite(range) ? Math.clamp(range, 1.0, 6.0) : 3.0;
    }
}
