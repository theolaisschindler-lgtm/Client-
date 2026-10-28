package dev.amethyst.client.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiEffectSettingsTest {
    @Test void oldAndNullConfigurationsKeepAllExistingModules() {
        Gson gson = new Gson();
        for (String json : new String[] {"{\"storage\":{\"enabled\":true}}",
                "{\"storage\":{\"enabled\":true},\"guiEffects\":null}"}) {
            ModConfig config = gson.fromJson(json, ModConfig.class);
            config.sanitize();
            assertTrue(config.storage.enabled);
            assertTrue(config.guiEffects.blur);
            assertTrue(config.guiEffects.glow);
            assertTrue(config.guiEffects.animations);
            assertEquals(0.6, config.guiEffects.glowIntensity);
        }
    }

    @Test void effectsCanBeDisabledAndPreferencesSurviveReload() {
        Gson gson = new Gson();
        ModConfig config = new ModConfig();
        config.guiEffects.blur = false;
        config.guiEffects.glow = false;
        config.guiEffects.animations = false;
        config.guiEffects.glowIntensity = 0.25;
        ModConfig restored = gson.fromJson(gson.toJson(config), ModConfig.class);
        restored.sanitize();
        assertFalse(restored.guiEffects.blur);
        assertFalse(restored.guiEffects.glow);
        assertFalse(restored.guiEffects.animations);
        assertEquals(0.25, restored.guiEffects.glowIntensity);
    }

    @Test void intensitySupportsZeroAndRejectsUnboundedValues() {
        GuiEffectSettings settings = new GuiEffectSettings();
        settings.glowIntensity = -1;
        settings.sanitize();
        assertEquals(0, settings.glowIntensity);
        settings.glowIntensity = 10;
        settings.sanitize();
        assertEquals(1, settings.glowIntensity);
        settings.glowIntensity = Double.NaN;
        settings.sanitize();
        assertEquals(0.6, settings.glowIntensity);
    }
}
