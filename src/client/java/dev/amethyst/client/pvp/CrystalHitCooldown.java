package dev.amethyst.client.pvp;

/** Tick-based pacing, with no accumulated attacks to replay when a GUI or freecam closes. */
public final class CrystalHitCooldown {
    private int remaining;

    public void reset(int delayTicks) { remaining = Math.clamp(delayTicks, 2, 20); }

    public boolean advance() {
        if (remaining > 0) remaining--;
        return remaining == 0;
    }
}
