package dev.amethyst.client.freecam;

import org.junit.jupiter.api.Test;
import net.minecraft.world.phys.Vec3;
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

    @Test void forwardAndBackwardFollowMinecraftViewDirectionAtEveryHeading() {
        // Use Minecraft's own view-vector conversion so yaw sign errors are caught
        // against the engine convention, rather than another hand-written formula.
        // Vanilla uses a 65,536-sample float sine table, while flight uses double trig.
        double engineTolerance = 1.0E-4;
        for (float yaw : new float[] {-180, -135, -90, -45, 0, 45, 90, 135, 180, 359}) {
            for (float pitch : new float[] {-80, -30, 0, 30, 80}) {
                Vec3 look = Vec3.directionFromRotation(pitch, yaw);
                var forward = FreecamMovement.calculate(yaw, pitch, 1, 0, 0, 0.5);
                var backward = FreecamMovement.calculate(yaw, pitch, -1, 0, 0, 0.5);
                assertEquals(look.x * 0.5, forward.x(), engineTolerance);
                assertEquals(look.y * 0.5, forward.y(), engineTolerance);
                assertEquals(look.z * 0.5, forward.z(), engineTolerance);
                assertEquals(-forward.x(), backward.x(), EPSILON);
                assertEquals(-forward.y(), backward.y(), EPSILON);
                assertEquals(-forward.z(), backward.z(), EPSILON);
            }
        }
    }
}
