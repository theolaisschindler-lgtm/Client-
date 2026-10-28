package dev.amethyst.client.freecam;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FreecamMovementTest {
    private static final double EPSILON = 1.0E-8;

    @Test void movementFollowsYawAndPitch() {
        var south = FreecamMovement.calculate(0, 0, 1, 0, 0, 0.5);
        assertEquals(0, south.x(), EPSILON);
        assertEquals(0.5, south.z(), EPSILON);
        var west = FreecamMovement.calculate(90, 0, 1, 0, 0, 0.5);
        assertEquals(-0.5, west.x(), EPSILON);
        assertEquals(0, west.z(), EPSILON);
        var upward = FreecamMovement.calculate(0, -90, 1, 0, 0, 0.5);
        assertEquals(0.5, upward.y(), EPSILON);
    }

    @Test void strafingAndVerticalFlightRemainIndependentOfLookPitch() {
        var left = FreecamMovement.calculate(0, -60, 0, 1, 0, 0.5);
        assertEquals(0.5, left.x(), EPSILON);
        assertEquals(0, left.y(), EPSILON);
        var descend = FreecamMovement.calculate(47, 60, 0, 0, -1, 0.5);
        assertEquals(-0.5, descend.y(), EPSILON);
        assertEquals(0, descend.x(), EPSILON);
        assertEquals(0, descend.z(), EPSILON);
    }

    @Test void diagonalFlightDoesNotExceedConfiguredSpeedOrBoost() {
        for (double speed : new double[] {0.1, 0.5, 3, 15}) {
            var offset = FreecamMovement.calculate(37, -25, 1, 1, 1, speed);
            double length = Math.sqrt(offset.x() * offset.x() + offset.y() * offset.y() + offset.z() * offset.z());
            assertEquals(speed, length, EPSILON);
        }
    }

    @Test void idleAndInvalidInputsCannotMoveTheCamera() {
        assertEquals(new FreecamMovement.Offset(0, 0, 0), FreecamMovement.calculate(32, 15, 0, 0, 0, 0.5));
        assertEquals(new FreecamMovement.Offset(0, 0, 0), FreecamMovement.calculate(Float.NaN, 0, 1, 0, 0, 0.5));
        assertEquals(new FreecamMovement.Offset(0, 0, 0), FreecamMovement.calculate(0, 0, 1, 0, 0, Double.POSITIVE_INFINITY));
    }
}
