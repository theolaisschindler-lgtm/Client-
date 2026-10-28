package dev.amethyst.client.config;

import java.util.EnumSet;

public final class ChunkFinderSettings extends ModuleSettings {
    public int sensitivity = 50;
    public int simulationDistance = 6;

    public ChunkFinderSettings() {
        targets = EnumSet.of(TargetType.GEODE, TargetType.ACTIVITY);
        range = 256;
    }

    public void sanitize() {
        EnumSet<TargetType> selected = targets == null
                ? EnumSet.of(TargetType.GEODE, TargetType.ACTIVITY) : targets.clone();
        super.sanitize(false);
        selected.retainAll(EnumSet.of(TargetType.GEODE, TargetType.ACTIVITY));
        targets = selected;
        sensitivity = Math.clamp(sensitivity, 1, 100);
        simulationDistance = Math.clamp(simulationDistance, 1, 16);
        range = 256;
    }

    /** Larger sensitivity admits weaker combinations; natural geodes alone never count as activity. */
    public int activityThreshold() { return 14 - (sensitivity - 1) * 10 / 99; }
    public int geodeThreshold() { return 8 - (sensitivity - 1) * 6 / 99; }
    public static int activityScore(int torches, int crafting, int beds, int redstone) {
        int categories = (torches > 0 ? 1 : 0) + (crafting > 0 ? 1 : 0)
                + (beds > 0 ? 1 : 0) + (redstone > 0 ? 1 : 0);
        if (categories < 2) return 0;
        return Math.min(torches, 4) + Math.min(crafting, 3) * 3
                + Math.min(beds, 2) * 3 + Math.min(redstone, 4) * 2;
    }
}
