package dev.amethyst.client.render;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerEspGeometryTest {
    @Test void movingPlayersUseTheRenderedPositionWithoutLosingTheirSize() {
        AABB box = new AABB(9.7, 64, -5.3, 10.3, 65.8, -4.7);
        Vec3 current = new Vec3(10, 64, -5);
        AABB rendered = PlayerEspGeometry.interpolatedBox(box, current, new Vec3(9, 63.5, -6));
        assertEquals(9, rendered.getCenter().x, 1e-9);
        assertEquals(64.4, rendered.getCenter().y, 1e-9);
        assertEquals(-6, rendered.getCenter().z, 1e-9);
        assertEquals(0.66, rendered.getXsize(), 1e-9);
        assertEquals(1.86, rendered.getYsize(), 1e-9);
    }

    @Test void crouchedAndStandingBoxesPreserveActualPoseHeights() {
        for (double height : new double[] {0.6, 1.5, 1.8}) {
            AABB box = new AABB(-0.3, 10, -0.3, 0.3, 10 + height, 0.3);
            AABB rendered = PlayerEspGeometry.interpolatedBox(box, new Vec3(0, 10, 0), new Vec3(0, 10, 0));
            assertEquals(height + 0.06, rendered.getYsize(), 1e-9);
            assertEquals(9.97, rendered.minY, 1e-9);
        }
    }
}
