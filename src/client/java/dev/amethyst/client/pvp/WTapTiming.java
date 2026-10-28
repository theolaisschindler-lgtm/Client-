package dev.amethyst.client.pvp;

final class WTapTiming {
    private int remaining;
    private int armedTick;

    void arm(int tick, int ticks) {
        armedTick = tick;
        remaining = Math.clamp(ticks, 1, 3);
    }

    boolean active() { return remaining > 0; }

    boolean advance(int tick) {
        if (!active() || tick == armedTick) return false;
        armedTick = tick;
        return --remaining == 0;
    }

    void clear() { remaining = 0; }
}
