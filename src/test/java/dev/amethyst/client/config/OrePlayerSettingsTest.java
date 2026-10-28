package dev.amethyst.client.config;

import com.google.gson.Gson;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrePlayerSettingsTest {
    @Test void upgradesPreserveExistingModulesAndKeepNewOnesOff() {
        ModConfig config = new Gson().fromJson("{\"storage\":{\"enabled\":true},\"ores\":null,\"players\":null}", ModConfig.class);
        config.sanitize();
        assertTrue(config.storage.enabled);
        assertFalse(config.ores.enabled);
        assertFalse(config.players.enabled);
        assertEquals(64, config.ores.range);
        assertEquals(128, config.players.range);
        assertEquals(EnumSet.of(TargetType.ORE_DIAMOND, TargetType.ORE_EMERALD,
                TargetType.ORE_GOLD, TargetType.ORE_ANCIENT_DEBRIS), config.ores.targets);
        assertEquals(EnumSet.of(TargetType.PLAYER), config.players.targets);
    }

    @Test void settingsKeepTargetCategoriesSeparateAndBoundScanCost() {
        ModConfig config = new ModConfig();
        config.ores.targets = EnumSet.allOf(TargetType.class);
        config.players.targets = EnumSet.allOf(TargetType.class);
        config.storage.targets = EnumSet.allOf(TargetType.class);
        config.ores.range = 999;
        config.ores.maxHighlights = Integer.MAX_VALUE;
        config.sanitize();
        assertEquals(OreEspSettings.oreTypes(), config.ores.targets);
        assertEquals(EnumSet.of(TargetType.PLAYER), config.players.targets);
        assertEquals(EnumSet.of(TargetType.CHEST, TargetType.HOPPER, TargetType.SHULKER, TargetType.BARREL), config.storage.targets);
        assertEquals(128, config.ores.range);
        assertEquals(4096, config.ores.maxHighlights);
        config.ores.range = -1;
        config.ores.maxHighlights = -1;
        config.ores.targets.clear();
        config.sanitize();
        assertEquals(16, config.ores.range);
        assertEquals(128, config.ores.maxHighlights);
        assertTrue(config.ores.targets.isEmpty());
    }

    @Test void playerFilteringHonorsSelfDeathRangeAndVisibilityChoices() {
        PlayerEspSettings settings = new PlayerEspSettings();
        settings.enabled = true;
        settings.range = 32;
        assertTrue(settings.canHighlight(false, true, false, false, 32 * 32));
        assertFalse(settings.canHighlight(false, true, false, false, 32 * 32 + 0.01));
        assertFalse(settings.canHighlight(true, true, false, false, 1));
        assertFalse(settings.canHighlight(false, false, false, false, 1));
        assertFalse(settings.canHighlight(false, true, true, false, 1));
        assertTrue(settings.canHighlight(false, true, false, true, 1));
        settings.showInvisible = false;
        assertFalse(settings.canHighlight(false, true, false, true, 1));
        settings.showSpectators = true;
        assertTrue(settings.canHighlight(false, true, true, false, 1));
        settings.targets.clear();
        assertFalse(settings.canHighlight(false, true, false, false, 1));
    }

    @Test void oreAndPlayerPreferencesAndColorsRoundTripIndependently() {
        ModConfig config = new ModConfig();
        config.ores.enabled = true;
        config.ores.colors.put(TargetType.ORE_DIAMOND, 0x123456);
        config.players.enabled = true;
        config.players.colors.put(TargetType.PLAYER, 0xABCDEF);
        config.players.showInvisible = false;
        config.players.showSpectators = true;
        config.players.teamColors = true;
        Gson gson = new Gson();
        ModConfig restored = gson.fromJson(gson.toJson(config), ModConfig.class);
        restored.sanitize();
        assertTrue(restored.ores.enabled);
        assertTrue(restored.players.enabled);
        assertSame(restored.ores, restored.module(TargetType.ORE_DIAMOND));
        assertSame(restored.players, restored.module(TargetType.PLAYER));
        assertEquals(0x123456, restored.color(TargetType.ORE_DIAMOND));
        assertEquals(0xABCDEF, restored.color(TargetType.PLAYER));
        assertFalse(restored.players.showInvisible);
        assertTrue(restored.players.showSpectators);
        assertTrue(restored.players.teamColors);
    }

    @Test void discoverySoundPreferencesSupportMuteAndPersist() {
        ModConfig config = new ModConfig();
        config.chunks.alertSound = false;
        config.chunks.alertVolume = 0;
        Gson gson = new Gson();
        ModConfig restored = gson.fromJson(gson.toJson(config), ModConfig.class);
        restored.sanitize();
        assertFalse(restored.chunks.alertSound);
        assertEquals(0, restored.chunks.alertVolume);
        restored.chunks.alertVolume = Double.NaN;
        restored.sanitize();
        assertEquals(0.6, restored.chunks.alertVolume);
    }
}
