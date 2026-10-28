package dev.amethyst.client.pvp;

public final class TotemWarningGate {
    private int ticksUntilRepeat;

    public boolean tick(boolean danger, int interval) {
        if (!danger) { clear(); return false; }
        if (ticksUntilRepeat > 0) ticksUntilRepeat--;
        if (ticksUntilRepeat > 0) return false;
        ticksUntilRepeat = Math.max(1, interval);
        return true;
    }

    public void clear() { ticksUntilRepeat = 0; }
}
