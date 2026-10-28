package dev.amethyst.client.config;

import com.google.gson.Gson;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PvpSettingsTest {
    @Test void upgradesKeepAllNewCombatModulesDisabled() {
        ModConfig config = new Gson().fromJson("{\"storage\":{\"enabled\":true},\"autoCrystal\":null,\"autoTotem\":null,\"safeAnchor\":null,\"crystals\":null,\"totemWarning\":null}", ModConfig.class);
        config.sanitize();
        assertTrue(config.storage.enabled);
        assertFalse(config.autoCrystal.enabled);
        assertFalse(config.autoTotem.enabled);
        assertFalse(config.safeAnchor.enabled);
        assertFalse(config.crystals.enabled);
        assertFalse(config.totemWarning.enabled);
        assertTrue(config.autoCrystal.crosshairOnly);
        assertTrue(config.autoCrystal.lineOfSight);
    }

    @Test void interactionTimingAndConfiguredReachAreBounded() {
        AutoCrystalSettings crystal = new AutoCrystalSettings();
        crystal.range = 100;
        crystal.delayTicks = 0;
        crystal.sanitize();
        assertEquals(6, crystal.range);
        assertEquals(2, crystal.delayTicks);
        crystal.range = Double.NaN;
        crystal.delayTicks = 1000;
        crystal.sanitize();
        assertEquals(3, crystal.range);
        assertEquals(20, crystal.delayTicks);
        AutoTotemSettings totem = new AutoTotemSettings();
        totem.delayTicks = -1;
        totem.healthThreshold = Double.NaN;
        totem.sanitize();
        assertEquals(0, totem.delayTicks);
        assertEquals(12, totem.healthThreshold);
    }

    @Test void crystalOutlinesDoNotEnableOtherTargetCategories() {
        ModConfig config = new ModConfig();
        config.crystals.targets = EnumSet.allOf(TargetType.class);
        config.crystals.colors.put(TargetType.END_CRYSTAL, 0xAA010203);
        config.sanitize();
        assertEquals(EnumSet.of(TargetType.END_CRYSTAL), config.crystals.targets);
        assertSame(config.crystals, config.module(TargetType.END_CRYSTAL));
        assertEquals(0x010203, config.color(TargetType.END_CRYSTAL));
        config.crystals.targets.clear();
        config.sanitize();
        assertTrue(config.crystals.targets.isEmpty());
    }

    @Test void combatPreferencesPersistWithoutActivatingFreecam() {
        ModConfig config = new ModConfig();
        config.autoCrystal.enabled = true;
        config.autoCrystal.crosshairOnly = false;
        config.autoCrystal.requireAttackKey = true;
        config.autoTotem.enabled = true;
        config.autoTotem.healthOnly = true;
        config.autoTotem.healthThreshold = 8;
        config.safeAnchor.enabled = true;
        config.safeAnchor.blockAllDetonation = true;
        Gson gson = new Gson();
        ModConfig restored = gson.fromJson(gson.toJson(config), ModConfig.class);
        restored.sanitize();
        assertTrue(restored.autoCrystal.enabled);
        assertFalse(restored.autoCrystal.crosshairOnly);
        assertTrue(restored.autoCrystal.requireAttackKey);
        assertTrue(restored.autoTotem.enabled);
        assertTrue(restored.autoTotem.healthOnly);
        assertEquals(8, restored.autoTotem.healthThreshold);
        assertTrue(restored.safeAnchor.blockAllDetonation);
        assertFalse(restored.freecam.enabled);
    }

    @Test void warningThresholdAndVolumeCanBeConfiguredWithoutInvalidValues() {
        TotemWarningSettings settings = new TotemWarningSettings();
        settings.healthThreshold = 500;
        settings.volume = -10;
        settings.cooldownTicks = 0;
        settings.sanitize();
        assertEquals(20, settings.healthThreshold);
        assertEquals(0, settings.volume);
        assertEquals(40, settings.cooldownTicks);
    }
}
