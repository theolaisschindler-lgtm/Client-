package dev.amethyst.client.config;

import java.util.EnumSet;

public final class OreEspSettings extends ModuleSettings {
    public int maxHighlights = 1024;

    public OreEspSettings() {
        range = 64;
        targets = defaultTargets();
    }

    public static EnumSet<TargetType> oreTypes() {
        return EnumSet.range(TargetType.ORE_COAL, TargetType.ORE_ANCIENT_DEBRIS);
    }

    private static EnumSet<TargetType> defaultTargets() {
        return EnumSet.of(TargetType.ORE_DIAMOND, TargetType.ORE_EMERALD,
                TargetType.ORE_GOLD, TargetType.ORE_ANCIENT_DEBRIS);
    }

    public void sanitize() {
        EnumSet<TargetType> selected = targets == null ? defaultTargets() : targets.clone();
        super.sanitize(false);
        selected.retainAll(oreTypes());
        targets = selected;
        range = Math.clamp(range, 16, 128);
        maxHighlights = Math.clamp(maxHighlights, 128, 4096);
    }
}
