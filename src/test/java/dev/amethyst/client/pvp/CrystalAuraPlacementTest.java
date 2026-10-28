package dev.amethyst.client.pvp;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrystalAuraPlacementTest {
    @Test void onlyVisibleEligibleSupportsWithinBothRangesAreSelected() {
        var candidates = List.of(new CrystalAuraPlacement.Candidate(1, 1, false, true),
                new CrystalAuraPlacement.Candidate(2, 2, true, false),
                new CrystalAuraPlacement.Candidate(3, 10, true, true),
                new CrystalAuraPlacement.Candidate(4, 9, true, true));
        assertEquals(4, CrystalAuraPlacement.select(candidates, 6, 3).orElseThrow());
        assertTrue(CrystalAuraPlacement.select(candidates, 2, 6).isEmpty());
    }

    @Test void invalidDistancesAndInvalidReachNeverSelect() {
        var candidates = List.of(new CrystalAuraPlacement.Candidate(1, Double.NaN, true, true),
                new CrystalAuraPlacement.Candidate(2, -1, true, true));
        assertTrue(CrystalAuraPlacement.select(candidates, 6, 6).isEmpty());
        assertEquals(0, CrystalAuraPlacement.reach(Double.NaN, 6));
        assertEquals(0, CrystalAuraPlacement.reach(4, -1));
        assertEquals(6, CrystalAuraPlacement.reach(50, 50));
    }

    @Test void deterministicNearestSelectionAndPacing() {
        var candidates = List.of(new CrystalAuraPlacement.Candidate(8, 4, true, true),
                new CrystalAuraPlacement.Candidate(3, 4, true, true));
        assertEquals(3, CrystalAuraPlacement.select(candidates, 3, 3).orElseThrow());
        var pace = new CrystalAuraPlacement();
        pace.reset(4);
        assertFalse(pace.advance());
        assertFalse(pace.advance());
        assertFalse(pace.advance());
        assertTrue(pace.advance());
        pace.reset(4);
        assertFalse(pace.advance());
    }
}
