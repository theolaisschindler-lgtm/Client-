package dev.amethyst.client.config;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class ModConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void defaultsKeepModulesOffAndSeparateTheirTargets() {
        ModConfig config = new ModConfig();

        assertFalse(config.storage.enabled);
        assertFalse(config.spawners.enabled);
        assertEquals(EnumSet.of(TargetType.CHEST, TargetType.HOPPER, TargetType.SHULKER), config.storage.targets);
        assertEquals(EnumSet.of(TargetType.SPAWNER), config.spawners.targets);
        assertSame(config.storage, config.module(TargetType.CHEST));
        assertSame(config.storage, config.module(TargetType.BARREL));
        assertSame(config.spawners, config.module(TargetType.SPAWNER));
    }

    @Test
    void invalidSettingsAreBoundedBeforeTheyReachTheRenderer() {
        ModConfig config = new ModConfig();
        config.storage.range = Integer.MIN_VALUE;
        config.storage.opacity = -10;
        config.storage.lineWidth = 20;
        config.spawners.range = Integer.MAX_VALUE;
        config.spawners.opacity = 8;
        config.spawners.lineWidth = -2;

        config.sanitize();

        assertAll(
            () -> assertEquals(16, config.storage.range),
            () -> assertEquals(0.05f, config.storage.opacity),
            () -> assertEquals(5f, config.storage.lineWidth),
            () -> assertEquals(256, config.spawners.range),
            () -> assertEquals(1f, config.spawners.opacity),
            () -> assertEquals(1f, config.spawners.lineWidth)
        );
    }

    @Test
    void nonFiniteRenderingValuesFallBackToUsableDefaults() {
        ModConfig config = new ModConfig();
        config.storage.opacity = Float.NaN;
        config.storage.lineWidth = Float.POSITIVE_INFINITY;
        config.spawners.opacity = Float.NEGATIVE_INFINITY;
        config.spawners.lineWidth = Float.NaN;

        config.sanitize();

        assertEquals(0.9f, config.storage.opacity);
        assertEquals(2f, config.storage.lineWidth);
        assertEquals(0.9f, config.spawners.opacity);
        assertEquals(2f, config.spawners.lineWidth);
    }

    @Test
    void missingModulesAndCollectionsRecoverWithoutLosingValidSettings() {
        ModConfig config = new ModConfig();
        config.storage = null;
        config.spawners.targets = null;
        config.spawners.colors = null;
        config.spawners.enabled = true;
        config.spawners.range = 200;

        config.sanitize();

        assertEquals(EnumSet.of(TargetType.CHEST, TargetType.HOPPER, TargetType.SHULKER), config.storage.targets);
        assertEquals(EnumSet.of(TargetType.SPAWNER), config.spawners.targets);
        assertTrue(config.spawners.enabled);
        assertEquals(200, config.spawners.range);
        assertEquals(TargetType.SPAWNER.defaultColor(), config.color(TargetType.SPAWNER));
    }

    @Test
    void sanitizingTargetsPreventsOneModuleFromEnablingTheOther() {
        ModConfig config = new ModConfig();
        config.storage.targets = EnumSet.allOf(TargetType.class);
        config.spawners.targets = EnumSet.allOf(TargetType.class);

        config.sanitize();

        assertFalse(config.storage.targets.contains(TargetType.SPAWNER));
        assertTrue(config.storage.targets.contains(TargetType.BARREL));
        assertEquals(EnumSet.of(TargetType.SPAWNER), config.spawners.targets);
    }

    @Test
    void intentionallyEmptyTargetSelectionStaysEmpty() {
        ModConfig config = new ModConfig();
        config.storage.targets.clear();
        config.spawners.targets.clear();

        config.sanitize();

        assertTrue(config.storage.targets.isEmpty());
        assertTrue(config.spawners.targets.isEmpty());
    }

    @Test
    void colorsStayIndependentAndAreRestrictedToRgb() {
        ModConfig config = new ModConfig();
        config.accent = 0xFF123456;
        config.storage.colors.put(TargetType.CHEST, 0xAA010203);
        config.storage.colors.put(TargetType.HOPPER, null);
        config.spawners.colors.put(TargetType.SPAWNER, 0xC04020);

        config.sanitize();

        assertEquals(0x123456, config.accent);
        assertEquals(0x010203, config.color(TargetType.CHEST));
        assertEquals(TargetType.HOPPER.defaultColor(), config.color(TargetType.HOPPER));
        assertEquals(0xC04020, config.color(TargetType.SPAWNER));
        assertEquals(TargetType.SHULKER.defaultColor(), config.color(TargetType.SHULKER));
    }

    @Test
    void jsonRoundTripPreservesCustomizedModules() {
        Gson gson = new Gson();
        ModConfig original = new ModConfig();
        original.storage.enabled = true;
        original.storage.targets.add(TargetType.BARREL);
        original.storage.colors.put(TargetType.SHULKER, 0x223344);
        original.storage.throughWalls = false;
        original.storage.opacity = 0.35f;
        original.storage.lineWidth = 4.5f;
        original.spawners.range = 240;
        original.spawners.pulse = true;
        original.spawners.rainbow = true;

        ModConfig restored = gson.fromJson(gson.toJson(original), ModConfig.class);
        restored.sanitize();

        assertAll(
            () -> assertTrue(restored.storage.enabled),
            () -> assertFalse(restored.spawners.enabled),
            () -> assertTrue(restored.storage.targets.contains(TargetType.BARREL)),
            () -> assertEquals(0x223344, restored.color(TargetType.SHULKER)),
            () -> assertFalse(restored.storage.throughWalls),
            () -> assertEquals(0.35f, restored.storage.opacity),
            () -> assertEquals(4.5f, restored.storage.lineWidth),
            () -> assertEquals(240, restored.spawners.range),
            () -> assertTrue(restored.spawners.pulse),
            () -> assertTrue(restored.spawners.rainbow)
        );
    }

    @Test
    void nullModuleJsonRecoversToDefaults() {
        ModConfig config = new Gson().fromJson("{\"storage\":null,\"spawners\":null}", ModConfig.class);

        config.sanitize();

        assertNotNull(config.storage);
        assertNotNull(config.spawners);
        assertEquals(96, config.storage.range);
        assertEquals(128, config.spawners.range);
    }

    @Test
    void firstSaveCreatesParentDirectoriesAndSettingsSurviveReload() throws IOException {
        Path path = temporaryDirectory.resolve("nested/config/amethyst.json");
        ModConfig config = ModConfig.load(path);
        assertFalse(Files.exists(path));
        config.storage.enabled = true;
        config.storage.colors.put(TargetType.CHEST, 0x102030);
        config.storage.targets.add(TargetType.BARREL);
        config.spawners.range = 192;

        config.save();
        ModConfig reloaded = ModConfig.load(path);

        assertTrue(Files.isRegularFile(path));
        assertTrue(reloaded.storage.enabled);
        assertEquals(0x102030, reloaded.color(TargetType.CHEST));
        assertTrue(reloaded.storage.targets.contains(TargetType.BARREL));
        assertEquals(192, reloaded.spawners.range);
        assertFalse(Files.readString(path).contains("\"path\""));
    }

    @Test
    void savingAgainReplacesExistingFileAndLeavesNoTemporaryFiles() throws IOException {
        Path path = temporaryDirectory.resolve("amethyst.json");
        ModConfig config = ModConfig.load(path);
        config.storage.enabled = true;
        config.save();
        config.storage.enabled = false;
        config.spawners.enabled = true;
        config.spawners.colors.put(TargetType.SPAWNER, 0x987654);

        config.save();
        ModConfig reloaded = ModConfig.load(path);

        assertFalse(reloaded.storage.enabled);
        assertTrue(reloaded.spawners.enabled);
        assertEquals(0x987654, reloaded.color(TargetType.SPAWNER));
        try (var entries = Files.list(temporaryDirectory)) {
            assertEquals(java.util.List.of(path), entries.toList());
        }
    }

    @Test
    void malformedJsonReturnsDefaultsAndPreservesOriginalUntilExplicitSave() throws IOException {
        Path path = temporaryDirectory.resolve("amethyst.json");
        String malformed = "{\"storage\": broken json";
        Files.writeString(path, malformed);

        ModConfig config = ModConfig.load(path);

        assertFalse(config.storage.enabled);
        assertEquals(96, config.storage.range);
        assertEquals(malformed, Files.readString(path));

        config.spawners.enabled = true;
        config.save();
        assertTrue(ModConfig.load(path).spawners.enabled);
    }

    @Test
    void loadingSavedSettingsSanitizesBeforeTheyAreUsed() throws IOException {
        Path path = temporaryDirectory.resolve("amethyst.json");
        Files.writeString(path, "{\"storage\":{\"range\":-10,\"opacity\":8,\"lineWidth\":99},\"spawners\":null}");

        ModConfig config = ModConfig.load(path);

        assertEquals(16, config.storage.range);
        assertEquals(1f, config.storage.opacity);
        assertEquals(5f, config.storage.lineWidth);
        assertEquals(128, config.spawners.range);
    }
}
