package dev.amethyst.client.config;

import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ExtendedPvpSettingsTest {
    @TempDir Path directory;

    @Test void oldFilesAcquireInactiveModulesAndKeepExistingPreferences() throws Exception {
        Path file = directory.resolve("amethyst.json");
        Files.writeString(file, """
                {"accent":1193046,"storage":{"enabled":true},
                 "autoCrystal":{"enabled":true,"range":4,"delayTicks":8},
                 "autoTotem":{"enabled":true},"safeAnchor":{"enabled":true}}
                """);
        ModConfig loaded = ModConfig.load(file);
        assertTrue(loaded.storage.enabled);
        assertTrue(loaded.autoCrystal.enabled);
        assertEquals(4, loaded.autoCrystal.range);
        assertEquals(8, loaded.autoCrystal.delayTicks);
        assertTrue(loaded.autoTotem.enabled);
        assertTrue(loaded.safeAnchor.enabled);
        assertEquals(0x123456, loaded.accent);
        assertInactive(loaded);
        assertTrue(loaded.crystalAura.crosshairOnly);
        assertTrue(loaded.aimAssist.requireAttackKey);
        assertTrue(loaded.autoMace.requireAttackKey);
    }

    @Test void explicitNullSectionsRecoverIndependently() {
        ModConfig loaded = new Gson().fromJson("""
                {"crystalAura":null,"anchorMacro":null,"triggerBot":null,"wTap":null,
                 "shieldBreak":null,"silentAim":null,"aimAssist":null,"autoMace":null,
                 "autoCrystal":{"enabled":true}}
                """, ModConfig.class);
        assertDoesNotThrow(loaded::sanitize);
        assertInactive(loaded);
        assertTrue(loaded.autoCrystal.enabled);
        assertEquals(3, loaded.crystalAura.range);
        assertEquals(45, loaded.silentAim.fov);
    }

    @Test void outOfRangeAndNonfiniteValuesBecomeUsableSettings() {
        ModConfig config = new ModConfig();
        config.crystalAura.range = Double.NaN;
        config.crystalAura.placeDelayTicks = Integer.MIN_VALUE;
        config.crystalAura.breakDelayTicks = Integer.MAX_VALUE;
        config.anchorMacro.actionDelayTicks = 0;
        config.triggerBot.cooldownThreshold = Double.NEGATIVE_INFINITY;
        config.triggerBot.delayTicks = 100;
        config.wTap.resetTicks = 100;
        config.shieldBreak.delayTicks = -1;
        config.silentAim.range = Double.POSITIVE_INFINITY;
        config.silentAim.fov = 720;
        config.aimAssist.range = -5;
        config.aimAssist.fov = Double.NaN;
        config.aimAssist.turnSpeed = Double.NaN;
        config.autoMace.minFallDistance = Double.POSITIVE_INFINITY;
        config.autoMace.range = 100;
        config.sanitize();
        assertEquals(3, config.crystalAura.range);
        assertEquals(2, config.crystalAura.placeDelayTicks);
        assertEquals(20, config.crystalAura.breakDelayTicks);
        assertEquals(1, config.anchorMacro.actionDelayTicks);
        assertEquals(.9, config.triggerBot.cooldownThreshold);
        assertEquals(20, config.triggerBot.delayTicks);
        assertEquals(3, config.wTap.resetTicks);
        assertEquals(1, config.shieldBreak.delayTicks);
        assertEquals(3, config.silentAim.range);
        assertEquals(120, config.silentAim.fov);
        assertEquals(1, config.aimAssist.range);
        assertEquals(70, config.aimAssist.fov);
        assertEquals(5, config.aimAssist.turnSpeed);
        assertEquals(2.5, config.autoMace.minFallDistance);
        assertEquals(6, config.autoMace.range);
        assertInactive(config);
    }

    @Test void settingsSurviveAtomicSaveAndReloadWithoutChangingOtherModules() {
        Path file = directory.resolve("nested/amethyst.json");
        ModConfig config = ModConfig.load(file);
        config.crystalAura.enabled = true;
        config.crystalAura.breakCrystals = false;
        config.crystalAura.requireUseKey = true;
        config.crystalAura.placeDelayTicks = 9;
        config.anchorMacro.enabled = true;
        config.anchorMacro.restoreSlot = false;
        config.triggerBot.enabled = true;
        config.triggerBot.playersOnly = false;
        config.triggerBot.cooldownThreshold = .75;
        config.wTap.enabled = true;
        config.wTap.resetTicks = 3;
        config.shieldBreak.enabled = true;
        config.shieldBreak.requireAttackKey = false;
        config.silentAim.enabled = true;
        config.silentAim.fov = 80;
        config.aimAssist.enabled = true;
        config.aimAssist.turnSpeed = 12;
        config.autoMace.enabled = true;
        config.autoMace.minFallDistance = 4;
        config.save();
        ModConfig restored = ModConfig.load(file);
        assertTrue(restored.crystalAura.enabled);
        assertFalse(restored.crystalAura.breakCrystals);
        assertTrue(restored.crystalAura.requireUseKey);
        assertEquals(9, restored.crystalAura.placeDelayTicks);
        assertTrue(restored.anchorMacro.enabled);
        assertFalse(restored.anchorMacro.restoreSlot);
        assertTrue(restored.triggerBot.enabled);
        assertFalse(restored.triggerBot.playersOnly);
        assertEquals(.75, restored.triggerBot.cooldownThreshold);
        assertTrue(restored.wTap.enabled);
        assertEquals(3, restored.wTap.resetTicks);
        assertTrue(restored.shieldBreak.enabled);
        assertFalse(restored.shieldBreak.requireAttackKey);
        assertTrue(restored.silentAim.enabled);
        assertEquals(80, restored.silentAim.fov);
        assertTrue(restored.aimAssist.enabled);
        assertEquals(12, restored.aimAssist.turnSpeed);
        assertTrue(restored.autoMace.enabled);
        assertEquals(4, restored.autoMace.minFallDistance);
        assertFalse(restored.autoCrystal.enabled);
        assertFalse(restored.freecam.enabled);
        assertFalse(restored.safeAnchor.enabled);
        assertFalse(restored.players.enabled);
    }

    private static void assertInactive(ModConfig config) {
        assertFalse(config.crystalAura.enabled);
        assertFalse(config.anchorMacro.enabled);
        assertFalse(config.triggerBot.enabled);
        assertFalse(config.wTap.enabled);
        assertFalse(config.shieldBreak.enabled);
        assertFalse(config.silentAim.enabled);
        assertFalse(config.aimAssist.enabled);
        assertFalse(config.autoMace.enabled);
    }
}
