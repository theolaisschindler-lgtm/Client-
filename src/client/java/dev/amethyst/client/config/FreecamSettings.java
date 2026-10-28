package dev.amethyst.client.config;

/** Preferences persist, but a detached camera never resumes automatically after a restart. */
public final class FreecamSettings {
    public transient boolean enabled = false;
    public double speed = 0.5;
    public double boostMultiplier = 3.0;

    public void sanitize() {
        speed = Double.isFinite(speed) ? Math.clamp(speed, 0.1, 3.0) : 0.5;
        boostMultiplier = Double.isFinite(boostMultiplier) ? Math.clamp(boostMultiplier, 1.0, 5.0) : 3.0;
    }
}
