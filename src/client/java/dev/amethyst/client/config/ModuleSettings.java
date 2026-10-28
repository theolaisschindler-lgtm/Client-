package dev.amethyst.client.config;

import java.util.EnumMap;
import java.util.EnumSet;

public class ModuleSettings {
    public boolean enabled = false;
    public boolean throughWalls = true;
    public boolean rainbow = false;
    public boolean pulse = false;
    public int range = 96;
    public float opacity = 0.9f;
    public float lineWidth = 2.0f;
    public EnumMap<TargetType, Integer> colors = new EnumMap<>(TargetType.class);
    public EnumSet<TargetType> targets = EnumSet.noneOf(TargetType.class);

    public ModuleSettings() {
        for (TargetType type : TargetType.values()) colors.put(type, type.defaultColor());
    }

    public static ModuleSettings storageDefaults() {
        ModuleSettings settings = new ModuleSettings();
        settings.targets = EnumSet.of(TargetType.CHEST, TargetType.HOPPER, TargetType.SHULKER);
        return settings;
    }

    public static ModuleSettings spawnerDefaults() {
        ModuleSettings settings = new ModuleSettings();
        settings.targets = EnumSet.of(TargetType.SPAWNER);
        settings.range = 128;
        return settings;
    }

    public void sanitize(boolean spawnerModule) {
        range = Math.clamp(range, 16, 256);
        opacity = Float.isFinite(opacity) ? Math.clamp(opacity, 0.05f, 1.0f) : 0.9f;
        lineWidth = Float.isFinite(lineWidth) ? Math.clamp(lineWidth, 1.0f, 5.0f) : 2.0f;
        if (colors == null) colors = new EnumMap<>(TargetType.class);
        for (TargetType type : TargetType.values()) {
            Integer value = colors.get(type);
            colors.put(type, value == null ? type.defaultColor() : value & 0xFFFFFF);
        }
        if (targets == null) targets = spawnerModule ? spawnerDefaults().targets : storageDefaults().targets;
        if (spawnerModule) targets.retainAll(EnumSet.of(TargetType.SPAWNER));
        else targets.retainAll(EnumSet.of(TargetType.CHEST, TargetType.HOPPER, TargetType.SHULKER, TargetType.BARREL));
    }
}
