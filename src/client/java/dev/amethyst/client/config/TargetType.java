package dev.amethyst.client.config;

public enum TargetType {
    CHEST("Chests", 0xB393FF),
    HOPPER("Hoppers", 0x57D5E8),
    SHULKER("Shulkers", 0xF28BCB),
    BARREL("Barrels", 0xE6B86A),
    SPAWNER("Spawners", 0xFF647C),
    GEODE("Geodes", 0xBF8FFF),
    ACTIVITY("Activity clues", 0xFFCA70);

    private final String label;
    private final int defaultColor;

    TargetType(String label, int defaultColor) {
        this.label = label;
        this.defaultColor = defaultColor;
    }

    public String label() { return label; }
    public int defaultColor() { return defaultColor; }
}
