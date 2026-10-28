package dev.amethyst.client.config;

public final class WTapSettings {
    public boolean enabled = false;
    public int resetTicks = 1;

    public void sanitize() {
        resetTicks = Math.clamp(resetTicks, 1, 3);
    }
}
