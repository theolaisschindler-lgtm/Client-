package dev.amethyst.client.pvp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeaponAutomationTest {
    @Test void slotRestorationNeverOverridesManualSelection() {
        WeaponSlot slot = new WeaponSlot();
        slot.acquire(2, 7);
        assertTrue(slot.owns(7));
        assertEquals(2, slot.release(7, true).orElseThrow());
        assertFalse(slot.active());
        slot.acquire(2, 7);
        assertTrue(slot.release(3, true).isEmpty());
        slot.acquire(2, 7);
        assertTrue(slot.release(7, false).isEmpty());
    }

    @Test void maceOnlySmashesOncePerDescendingFall() {
        MaceFall fall = new MaceFall();
        assertFalse(fall.eligible(3, 2.5, .1, false));
        assertFalse(fall.eligible(3, 2.5, -.1, true));
        assertFalse(fall.eligible(2, 2.5, -.1, false));
        assertTrue(fall.eligible(2.5, 2.5, -.1, false));
        fall.used();
        assertFalse(fall.eligible(8, 2.5, -.1, false));
        fall.landed();
        assertTrue(fall.eligible(3, 2.5, -.1, false));
        assertFalse(fall.eligible(Double.NaN, 2.5, -.1, false));
    }

    @Test void wTapKeepsAFullInputTickAfterManualOrAutomaticHit() {
        WTapTiming timing = new WTapTiming();
        timing.arm(10, 2);
        assertFalse(timing.advance(10));
        assertTrue(timing.active());
        assertFalse(timing.advance(11));
        assertFalse(timing.advance(11));
        assertTrue(timing.advance(12));
        assertFalse(timing.active());
        timing.arm(20, 1);
        assertFalse(timing.advance(20));
        assertTrue(timing.advance(21));
        timing.arm(30, 3);
        timing.clear();
        assertFalse(timing.active());
    }

    @Test void reachUsesEyeToBoxDistanceAndRejectsInvalidValues() {
        assertTrue(WeaponTargeting.inReach(9, 3));
        assertFalse(WeaponTargeting.inReach(9.00001, 3));
        assertFalse(WeaponTargeting.inReach(-1, 3));
        assertFalse(WeaponTargeting.inReach(Double.NaN, 3));
        assertFalse(WeaponTargeting.inReach(1, Double.POSITIVE_INFINITY));
    }
}
