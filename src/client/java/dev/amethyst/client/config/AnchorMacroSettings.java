package dev.amethyst.client.config;

public final class AnchorMacroSettings {
    public boolean enabled = false;
    public int actionDelayTicks = 2;
    public boolean restoreSlot = true;

    public void sanitize() {
        actionDelayTicks = Math.clamp(actionDelayTicks, 1, 10);
    }
}
