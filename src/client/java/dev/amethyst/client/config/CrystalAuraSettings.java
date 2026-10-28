package dev.amethyst.client.config;

public final class CrystalAuraSettings {
    public boolean enabled = false;
    public double range = 3.0;
    public int placeDelayTicks = 4;
    public boolean breakCrystals = true;
    public int breakDelayTicks = 4;
    public boolean crosshairOnly = true;
    public boolean requireUseKey = false;
    public boolean restoreSlot = true;

    public void sanitize() {
        range = Double.isFinite(range) ? Math.clamp(range, 1.0, 6.0) : 3.0;
        placeDelayTicks = Math.clamp(placeDelayTicks, 2, 20);
        breakDelayTicks = Math.clamp(breakDelayTicks, 2, 20);
    }
}
