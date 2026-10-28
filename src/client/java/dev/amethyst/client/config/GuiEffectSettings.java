package dev.amethyst.client.config;

public final class GuiEffectSettings {
    public boolean blur = true;
    public boolean glow = true;
    public boolean animations = true;
    public double glowIntensity = 0.6;

    public void sanitize() {
        glowIntensity = Double.isFinite(glowIntensity) ? Math.clamp(glowIntensity, 0.0, 1.0) : 0.6;
    }
}
