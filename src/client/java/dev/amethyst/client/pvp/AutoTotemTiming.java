package dev.amethyst.client.pvp;

/** Waits after need starts and spaces retries while a server confirmation is outstanding. */
public final class AutoTotemTiming {
    private long tick;
    private long neededSince = -1;
    private long nextRequestTick;

    public boolean shouldSwap(boolean needed, boolean sourceAvailable, int delayTicks) {
        tick++;
        if (!needed || !sourceAvailable) {
            neededSince = -1;
            nextRequestTick = 0;
            return false;
        }
        if (neededSince < 0) neededSince = tick;
        if (tick - neededSince < Math.clamp(delayTicks, 0, 20) || tick < nextRequestTick) return false;
        // A rejected or delayed response must not cause an inventory-click storm.
        nextRequestTick = tick + 10;
        return true;
    }

    public void clear() {
        tick = 0;
        neededSince = -1;
        nextRequestTick = 0;
    }
}
