package dev.amethyst.client.pvp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.amethyst.client.pvp.AnchorMacroSequence.Step.*;

class AnchorMacroSequenceTest {
    @Test void fullSequenceWaitsForBlocksAndChargesOnlyOnce() {
        var sequence = new AnchorMacroSequence();
        sequence.begin(false, 0, 2);
        assertEquals(PLACE, sequence.nextAction());
        sequence.actionResult(true);
        for (int i = 0; i < 4; i++) sequence.tick(false, 0);
        assertEquals(IDLE, sequence.nextAction());
        sequence.tick(true, 0);
        assertEquals(CHARGE, sequence.nextAction());
        sequence.actionResult(true);
        for (int i = 0; i < 4; i++) sequence.tick(true, 0);
        assertEquals(IDLE, sequence.nextAction());
        sequence.tick(true, 1);
        assertEquals(DETONATE, sequence.nextAction());
        sequence.actionResult(true);
        assertFalse(sequence.isBusy());
    }

    @Test void alreadyChargedAnchorDetonatesAndEmptyAnchorStartsWithCharge() {
        var sequence = new AnchorMacroSequence();
        sequence.begin(true, 1, 2);
        assertEquals(DETONATE, sequence.nextAction());
        sequence.begin(true, 0, 2);
        assertEquals(CHARGE, sequence.nextAction());
        sequence.actionResult(true);
        sequence.tick(true, 1);
        assertEquals(IDLE, sequence.nextAction());
        sequence.tick(true, 1);
        assertEquals(DETONATE, sequence.nextAction());
    }

    @Test void rejectedActionRemovedAnchorAndServerTimeoutAbortWithoutRetry() {
        var sequence = new AnchorMacroSequence();
        sequence.begin(true, 0, 1);
        sequence.actionResult(false);
        assertFalse(sequence.isBusy());
        sequence.begin(true, 0, 1);
        sequence.actionResult(true);
        sequence.tick(false, 0);
        assertFalse(sequence.isBusy());
        sequence.begin(false, 0, 1);
        sequence.actionResult(true);
        for (int i = 0; i < 81; i++) sequence.tick(false, 0);
        assertFalse(sequence.isBusy());
    }
}
