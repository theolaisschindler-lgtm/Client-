package dev.amethyst.client.pvp;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AutoTotemTest {
    @Test void sourceSelectionPrefersInventoryThenHotbarWithoutTouchingEquipmentOrOffhand() {
        Set<Integer> totems = new HashSet<>(Set.of(36, 35, 45, 5));
        assertEquals(35, AutoTotemSelection.findSource(totems::contains));
        totems.remove(35);
        assertEquals(36, AutoTotemSelection.findSource(totems::contains));
        totems.remove(36);
        assertEquals(-1, AutoTotemSelection.findSource(totems::contains));
        assertEquals(44, AutoTotemSelection.findSource(slot -> slot == 44));
    }

    @Test void anEquippedTotemAndHealthPreferencePreventUnnecessarySwaps() {
        assertFalse(AutoTotemSelection.needsTotem(true, false, 1, 12));
        assertTrue(AutoTotemSelection.needsTotem(false, false, 20, 12));
        assertFalse(AutoTotemSelection.needsTotem(false, true, 20, 12));
        assertTrue(AutoTotemSelection.needsTotem(false, true, 12, 12));
        assertFalse(AutoTotemSelection.needsTotem(false, true, Double.NaN, 12));
    }

    @Test void configuredDelayStartsWhenATotemIsNeededAndAvailable() {
        AutoTotemTiming timing = new AutoTotemTiming();
        assertFalse(timing.shouldSwap(true, false, 2));
        assertFalse(timing.shouldSwap(true, true, 2));
        assertFalse(timing.shouldSwap(true, true, 2));
        assertTrue(timing.shouldSwap(true, true, 2));
        timing.clear();
        assertTrue(timing.shouldSwap(true, true, 0));
    }

    @Test void delayedOrRejectedServerResponsesCannotSpamInventoryTransactions() {
        AutoTotemTiming timing = new AutoTotemTiming();
        assertTrue(timing.shouldSwap(true, true, 0));
        for (int i = 0; i < 9; i++) assertFalse(timing.shouldSwap(true, true, 0));
        assertTrue(timing.shouldSwap(true, true, 0));
    }

    @Test void confirmedEquipOrContextResetAllowsTheNextRefillToStartFresh() {
        AutoTotemTiming timing = new AutoTotemTiming();
        assertTrue(timing.shouldSwap(true, true, 0));
        assertFalse(timing.shouldSwap(false, true, 0));
        assertTrue(timing.shouldSwap(true, true, 0));
        timing.clear();
        assertFalse(timing.shouldSwap(true, true, 1));
        assertTrue(timing.shouldSwap(true, true, 1));
    }
}
