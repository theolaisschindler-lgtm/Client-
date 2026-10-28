package dev.amethyst.client.pvp;

import java.util.List;
import java.util.OptionalLong;

/** Pure selection and pacing used by crystal placement. */
public final class CrystalAuraPlacement {
    public record Candidate(long position, double distanceSquared, boolean eligible, boolean visible) { }

    private int waitTicks;

    public static double reach(double configured, double vanilla) {
        return Double.isFinite(configured) && Double.isFinite(vanilla)
                ? Math.max(0.0, Math.min(Math.min(configured, vanilla), 6.0)) : 0.0;
    }

    public static OptionalLong select(List<Candidate> candidates, double configured, double vanilla) {
        double reach = reach(configured, vanilla);
        if (reach <= 0) return OptionalLong.empty();
        Candidate best = null;
        for (Candidate candidate : candidates) {
            if (!candidate.eligible || !candidate.visible || !Double.isFinite(candidate.distanceSquared)
                    || candidate.distanceSquared < 0 || candidate.distanceSquared > reach * reach) continue;
            if (best == null || candidate.distanceSquared < best.distanceSquared
                    || (candidate.distanceSquared == best.distanceSquared && candidate.position < best.position)) {
                best = candidate;
            }
        }
        return best == null ? OptionalLong.empty() : OptionalLong.of(best.position);
    }

    public boolean advance() {
        if (waitTicks > 0) waitTicks--;
        return waitTicks == 0;
    }

    public void reset(int delay) { waitTicks = Math.clamp(delay, 2, 20); }
}
