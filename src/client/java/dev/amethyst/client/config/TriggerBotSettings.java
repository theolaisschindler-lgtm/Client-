package dev.amethyst.client.config;

public final class TriggerBotSettings {
    public boolean enabled = false;
    public boolean playersOnly = true;
    public boolean requireAttackKey = false;
    public int delayTicks = 2;
    public double cooldownThreshold = 0.9;

    public void sanitize() {
        delayTicks = Math.clamp(delayTicks, 1, 20);
        cooldownThreshold = Double.isFinite(cooldownThreshold) ? Math.clamp(cooldownThreshold, 0.1, 1.0) : 0.9;
    }
}
