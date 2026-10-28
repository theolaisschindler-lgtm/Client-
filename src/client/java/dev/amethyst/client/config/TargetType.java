package dev.amethyst.client.config;

public enum TargetType {
    CHEST("Chests", 0xB393FF),
    HOPPER("Hoppers", 0x57D5E8),
    SHULKER("Shulkers", 0xF28BCB),
    BARREL("Barrels", 0xE6B86A),
    SPAWNER("Spawners", 0xFF647C),
    GEODE("Geodes", 0xBF8FFF),
    ACTIVITY("Activity clues", 0xFFCA70),
    ORE_COAL("Coal", 0xA8A8B8),
    ORE_COPPER("Copper", 0xE49C76),
    ORE_IRON("Iron", 0xE9D1B7),
    ORE_GOLD("Gold", 0xFFD35A),
    ORE_REDSTONE("Redstone", 0xFF596E),
    ORE_LAPIS("Lapis", 0x658EFF),
    ORE_DIAMOND("Diamond", 0x5FE8EF),
    ORE_EMERALD("Emerald", 0x61E6A2),
    ORE_QUARTZ("Quartz", 0xF1E8E5),
    ORE_ANCIENT_DEBRIS("Ancient debris", 0xD798A6),
    PLAYER("Players", 0xAA8CFF),
    END_CRYSTAL("End crystals", 0xFF84CF);

    private final String label;
    private final int defaultColor;

    TargetType(String label, int defaultColor) {
        this.label = label;
        this.defaultColor = defaultColor;
    }

    public String label() { return label; }
    public int defaultColor() { return defaultColor; }
}
