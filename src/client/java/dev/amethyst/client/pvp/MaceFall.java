package dev.amethyst.client.pvp;

/** Once per physical fall, even when the client receives another crosshair target. */
final class MaceFall {
    private boolean used;
    void landed() { used = false; }
    boolean eligible(double distance, double minimum, double velocityY, boolean onGround) {
        return !used && !onGround && Double.isFinite(distance) && Double.isFinite(minimum)
                && minimum >= 1.5 && distance >= minimum && velocityY < 0;
    }
    void used() { used = true; }
}
