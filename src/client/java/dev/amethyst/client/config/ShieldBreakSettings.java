package dev.amethyst.client.config;

public final class ShieldBreakSettings {
    public boolean enabled = false;
    public boolean requireAttackKey = true;
    public boolean restoreSlot = true;
    public int delayTicks = 2;

    public void sanitize() {
        delayTicks = Math.clamp(delayTicks, 1, 20);
    }
}
