package dev.amethyst.client.pvp;

import java.util.Collection;
import java.util.Comparator;
import java.util.OptionalInt;

/** Target eligibility works on snapshots gathered exclusively from loaded EndCrystal entities. */
public final class CrystalTargeting {
    private CrystalTargeting() { }

    public record Candidate(int id, double distanceSquared, boolean alive, boolean visible, boolean crosshair) { }

    public static OptionalInt select(Collection<Candidate> candidates, double configuredRange, double vanillaReach,
                                     boolean crosshairOnly, boolean lineOfSight) {
        double reach = Math.min(configuredRange, vanillaReach);
        if (!Double.isFinite(reach) || reach <= 0) return OptionalInt.empty();
        double maximumDistanceSquared = reach * reach;
        return candidates.stream()
                .filter(candidate -> candidate.alive && Double.isFinite(candidate.distanceSquared)
                        && candidate.distanceSquared >= 0 && candidate.distanceSquared <= maximumDistanceSquared)
                .filter(candidate -> !crosshairOnly || candidate.crosshair)
                .filter(candidate -> !lineOfSight || candidate.visible)
                .min(Comparator.comparingDouble(Candidate::distanceSquared).thenComparingInt(Candidate::id))
                .map(candidate -> OptionalInt.of(candidate.id)).orElseGet(OptionalInt::empty);
    }
}
