package dev.amethyst.client.config;

import com.google.gson.Gson;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChunkFinderSettingsTest {
    @Test void oldConfigurationsReceiveDisabledChunkFinder() {
        ModConfig config = new Gson().fromJson("{\"accent\":123}", ModConfig.class);
        config.sanitize();
        assertFalse(config.chunks.enabled);
        assertEquals(6, config.chunks.simulationDistance);
        assertSame(config.chunks, config.module(TargetType.GEODE));
        assertSame(config.chunks, config.module(TargetType.ACTIVITY));
    }
    @Test void settingsClampAndTargetsDoNotLeakIntoOtherModules() {
        ChunkFinderSettings settings = new ChunkFinderSettings();
        settings.sensitivity = 1000; settings.simulationDistance = -5;
        settings.targets = EnumSet.allOf(TargetType.class);
        settings.sanitize();
        assertEquals(100, settings.sensitivity);
        assertEquals(1, settings.simulationDistance);
        assertEquals(EnumSet.of(TargetType.GEODE, TargetType.ACTIVITY), settings.targets);
        settings.targets.clear(); settings.sanitize(); assertTrue(settings.targets.isEmpty());
    }
    @Test void higherSensitivityMonotonicallyAdmitsWeakerSignals() {
        ChunkFinderSettings settings = new ChunkFinderSettings();
        int previous = Integer.MAX_VALUE;
        for (int i = 1; i <= 100; i++) {
            settings.sensitivity = i;
            assertTrue(settings.activityThreshold() <= previous);
            previous = settings.activityThreshold();
        }
        assertEquals(0, ChunkFinderSettings.activityScore(100, 0, 0, 0));
        assertEquals(0, ChunkFinderSettings.activityScore(0, 0, 100, 0));
        assertTrue(ChunkFinderSettings.activityScore(4, 2, 1, 4) >= 14);
        int weakEvidence = ChunkFinderSettings.activityScore(1, 1, 0, 0);
        settings.sensitivity = 1;
        assertTrue(weakEvidence < settings.activityThreshold());
        assertEquals(8, settings.geodeThreshold());
        settings.sensitivity = 100;
        assertTrue(weakEvidence >= settings.activityThreshold());
        assertEquals(2, settings.geodeThreshold());
    }
    @Test void customizedChunkSettingsPersistInJson() {
        ModConfig config = new ModConfig();
        config.chunks.enabled = true; config.chunks.sensitivity = 78;
        config.chunks.simulationDistance = 12;
        config.chunks.colors.put(TargetType.GEODE, 0x123456);
        Gson gson = new Gson();
        ModConfig restored = gson.fromJson(gson.toJson(config), ModConfig.class);
        restored.sanitize();
        assertTrue(restored.chunks.enabled);
        assertEquals(78, restored.chunks.sensitivity);
        assertEquals(12, restored.chunks.simulationDistance);
        assertEquals(0x123456, restored.color(TargetType.GEODE));
        assertFalse(restored.storage.targets.contains(TargetType.GEODE));
    }
}
