package dev.amethyst.client.pvp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SafeAnchorRulesTest {
    @Test void chargingCannotBeBlockedEvenInStrictMode() {
        for (int charges = 0; charges < 4; charges++) {
            assertFalse(SafeAnchorRules.shouldBlock(true, true, false, charges, true, true, false, false));
            assertFalse(SafeAnchorRules.shouldBlock(true, true, false, charges, true, false, true, false));
        }
    }

    @Test void fullAnchorsCannotAccidentallyExplodeWhileEitherHandHoldsFuel() {
        assertTrue(SafeAnchorRules.shouldBlock(true, false, false, 4, true, true, false, false));
        assertTrue(SafeAnchorRules.shouldBlock(true, false, false, 4, true, false, true, false));
        assertFalse(SafeAnchorRules.shouldBlock(true, false, false, 4, true, false, false, false));
    }

    @Test void strictModeAlsoBlocksDeliberateChargedAnchorUse() {
        for (int charges = 1; charges <= 4; charges++)
            assertTrue(SafeAnchorRules.shouldBlock(true, true, false, charges, true, false, false, false));
        assertFalse(SafeAnchorRules.shouldBlock(true, true, false, 0, true, false, false, false));
    }

    @Test void safeDimensionsAndDisabledProtectionKeepVanillaUse() {
        assertFalse(SafeAnchorRules.shouldBlock(true, true, true, 4, true, true, true, false));
        assertFalse(SafeAnchorRules.shouldBlock(false, true, false, 4, true, true, true, false));
    }

    @Test void offhandFallbackAndSneakPlacementAreNotDetonations() {
        assertFalse(SafeAnchorRules.shouldBlock(true, true, false, 4, false, true, true, false));
        assertFalse(SafeAnchorRules.shouldBlock(true, true, false, 4, true, true, true, true));
        assertTrue(SafeAnchorRules.shouldBlock(true, true, false, 1, true, false, false, false));
    }
}
