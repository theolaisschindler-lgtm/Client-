package dev.amethyst.client.config;

public final class AutoCrystalSettings {
    public boolean enabled = false;
    public double range = 3.0;
    public int delayTicks = 4;
    public boolean crosshairOnly = true;
    public boolean requireAttackKey = false;
    public boolean lineOfSight = true;

    public void sanitize() {
        range = Double.isFinite(range) ? Math.clamp(range, 1.0, 6.0) : 3.0;
        delayTicks = Math.clamp(delayTicks, 2, 20);
    }
}
