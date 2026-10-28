package dev.amethyst.client.pvp;

import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AimGeometryTest {
    @Test void rotationsFollowMinecraftAxesAndShortestYawWrap() {
        assertEquals(0, AimGeometry.toward(Vec3.ZERO, new Vec3(0, 0, 2)).yaw(), 1e-5);
        assertEquals(-90, AimGeometry.toward(Vec3.ZERO, new Vec3(2, 0, 0)).yaw(), 1e-5);
        assertEquals(-90, AimGeometry.toward(Vec3.ZERO, new Vec3(0, 2, 0)).pitch(), 1e-5);
        var step = AimGeometry.step(179, 0, new AimGeometry.Angles(-179, 0), 1);
        assertEquals(180, step.yaw(), 1e-5);
        assertEquals(2, AimGeometry.angularDistance(179, 0, new AimGeometry.Angles(-179, 0)), 1e-5);
    }

    @Test void smoothingCapsCombinedMovementAndDoesNotOvershoot() {
        var step = AimGeometry.step(0, 0, new AimGeometry.Angles(30, 40), 5);
        assertEquals(3, step.yaw(), 1e-5);
        assertEquals(4, step.pitch(), 1e-5);
        assertEquals(new AimGeometry.Angles(2, -3), AimGeometry.step(0, 0, new AimGeometry.Angles(2, -3), 5));
        assertEquals(90, AimGeometry.step(0, 89, new AimGeometry.Angles(0, 120), 30).pitch(), 1e-5);
    }

    @Test void selectedAimPointIsInsideBoxAndActualReach() {
        Vec3 eye = new Vec3(0, 1.62, 0);
        AABB box = new AABB(2.8, 0, -0.3, 3.4, 1.8, 0.3);
        Vec3 point = AimGeometry.reachablePoint(eye, box, 3).orElseThrow();
        assertTrue(box.contains(point));
        assertTrue(eye.distanceToSqr(point) <= 9);
        assertTrue(AimGeometry.reachablePoint(eye, box, 2.7).isEmpty());
        assertTrue(AimGeometry.reachablePoint(eye, box, Double.NaN).isEmpty());
    }

    @Test void fullFovUsesHalfConeAndCannotExtendServerReach() {
        var candidates = List.of(candidate(1, 2, 21, true, true), candidate(2, 2.5, 19, true, true),
                candidate(3, 3.1, 1, true, true), candidate(4, 1, 180, true, true));
        assertEquals(2, AimTargetSelection.select(candidates, 6, 3, 40, true).orElseThrow());
        assertTrue(AimTargetSelection.select(candidates, 2, 3, 40, true).isEmpty());
    }

    @Test void targetSelectionFiltersVisibilityInvalidValuesAndUsesStableTieBreaks() {
        var candidates = List.of(candidate(1, 1, 0, false, true), candidate(2, 1, 0, true, false),
                candidate(3, 2, Double.NaN, true, true), candidate(6, 2, 5, true, true),
                candidate(5, 2, 5, true, true), candidate(7, 2.5, 5, true, true));
        assertEquals(5, AimTargetSelection.select(candidates, 3, 3, 60, true).orElseThrow());
        assertEquals(2, AimTargetSelection.select(candidates, 3, 3, 60, false).orElseThrow());
        assertTrue(AimTargetSelection.select(candidates, 3, 3, Double.NaN, true).isEmpty());
    }

    private static AimTargetSelection.Candidate candidate(int id, double distance, double angle,
                                                          boolean valid, boolean visible) {
        return new AimTargetSelection.Candidate(id, distance * distance, angle, valid, visible);
    }
}
