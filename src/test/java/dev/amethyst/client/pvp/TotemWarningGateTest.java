package dev.amethyst.client.pvp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TotemWarningGateTest {
    @Test void alertsImmediatelyThenWaitsTheFullInterval() {
        TotemWarningGate gate = new TotemWarningGate();
        assertTrue(gate.tick(true, 100));
        for (int i = 1; i < 100; i++) assertFalse(gate.tick(true, 100));
        assertTrue(gate.tick(true, 100));
    }

    @Test void recoveringOrResettingAllowsTheNextNewDangerToAlert() {
        TotemWarningGate gate = new TotemWarningGate();
        assertFalse(gate.tick(false, 100));
        assertTrue(gate.tick(true, 100));
        assertFalse(gate.tick(false, 100));
        assertTrue(gate.tick(true, 100));
        gate.clear();
        assertTrue(gate.tick(true, 100));
    }
}
