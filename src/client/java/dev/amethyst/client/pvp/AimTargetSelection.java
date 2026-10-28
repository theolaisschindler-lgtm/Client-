package dev.amethyst.client.pvp;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;

/** Selects by crosshair proximity, then distance and entity id, from actual world snapshots. */
public final class AimTargetSelection {
    private AimTargetSelection() { }

    public record Candidate(int id, double distanceSquared, double angleDegrees, boolean valid,
                            boolean visible) { }

    public static OptionalInt select(List<Candidate> candidates, double configuredRange, double vanillaReach,
                                     double fullFov, boolean requireLineOfSight) {
        double reach = Math.min(configuredRange, vanillaReach);
        if (!Double.isFinite(reach) || reach <= 0 || !Double.isFinite(fullFov) || fullFov <= 0) {
            return OptionalInt.empty();
        }
        return candidates.stream().filter(candidate -> candidate.valid()
                        && (!requireLineOfSight || candidate.visible())
                        && Double.isFinite(candidate.distanceSquared()) && candidate.distanceSquared() >= 0
                        && candidate.distanceSquared() <= reach * reach
                        && Double.isFinite(candidate.angleDegrees()) && candidate.angleDegrees() >= 0
                        && candidate.angleDegrees() <= Math.min(180, fullFov) / 2)
                .min(Comparator.comparingDouble(Candidate::angleDegrees)
                        .thenComparingDouble(Candidate::distanceSquared).thenComparingInt(Candidate::id))
                .map(candidate -> OptionalInt.of(candidate.id())).orElseGet(OptionalInt::empty);
    }
}
