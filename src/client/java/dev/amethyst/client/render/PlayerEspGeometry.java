package dev.amethyst.client.render;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PlayerEspGeometry {
    private PlayerEspGeometry() { }

    /** Follow the same interpolated position as the rendered entity, preserving its pose dimensions. */
    public static AABB interpolatedBox(AABB currentBox, Vec3 currentPosition, Vec3 renderPosition) {
        return currentBox.move(renderPosition.subtract(currentPosition)).inflate(0.03);
    }
}
