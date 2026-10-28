package dev.amethyst.client.pvp;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrystalTargetingTest {
    private static CrystalTargeting.Candidate target(int id, double distance, boolean alive, boolean visible, boolean crosshair) {
        return new CrystalTargeting.Candidate(id, distance * distance, alive, visible, crosshair);
    }

    @Test void configuredRangeCannotExtendVanillaReach() {
        var candidates = List.of(target(1, 3.01, true, true, true), target(2, 3, true, true, true));
        assertEquals(2, CrystalTargeting.select(candidates, 6, 3, false, false).orElseThrow());
        assertTrue(CrystalTargeting.select(candidates, 2, 3, false, false).isEmpty());
    }

    @Test void crosshairAndVisibilityChoicesFilterBeforeNearestSelection() {
        var candidates = List.of(target(1, 1, true, false, true), target(2, 2, true, true, false),
                target(3, 2.5, true, true, true));
        assertEquals(3, CrystalTargeting.select(candidates, 3, 3, true, true).orElseThrow());
        assertEquals(2, CrystalTargeting.select(candidates, 3, 3, false, true).orElseThrow());
        assertEquals(1, CrystalTargeting.select(candidates, 3, 3, true, false).orElseThrow());
    }

    @Test void removedAndInvalidCandidatesNeverWinAndTiesAreStable() {
        var candidates = List.of(target(1, 0.1, false, true, true),
                new CrystalTargeting.Candidate(2, Double.NaN, true, true, true),
                new CrystalTargeting.Candidate(3, -1, true, true, true),
                target(6, 2, true, true, true), target(4, 2, true, true, true));
        assertEquals(4, CrystalTargeting.select(candidates, 3, 3, false, false).orElseThrow());
        assertTrue(CrystalTargeting.select(candidates, Double.NaN, 3, false, false).isEmpty());
    }

    @Test void pacingNeverCatchesUpMissedAttacksAfterBlocking() {
        CrystalHitCooldown cooldown = new CrystalHitCooldown();
        cooldown.reset(4);
        for (int i = 0; i < 3; i++) assertFalse(cooldown.advance());
        assertTrue(cooldown.advance());
        cooldown.reset(4);
        assertFalse(cooldown.advance());
        // A menu or freecam tick resets the wait instead of accumulating attacks.
        cooldown.reset(4);
        for (int i = 0; i < 3; i++) assertFalse(cooldown.advance());
        assertTrue(cooldown.advance());
    }
}
