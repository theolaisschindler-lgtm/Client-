package dev.amethyst.client.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FreecamSettingsTest {
    @Test void existingAndNullConfigurationsReceiveDisabledFreecam() {
        Gson gson = new Gson();
        for (String json : new String[] {"{}", "{\"freecam\":null}"}) {
            ModConfig config = gson.fromJson(json, ModConfig.class);
            config.sanitize();
            assertFalse(config.freecam.enabled);
            assertEquals(0.5, config.freecam.speed);
            assertEquals(3.0, config.freecam.boostMultiplier);
        }
    }

    @Test void preferencesPersistButEnabledStateDoesNot() {
        ModConfig config = new ModConfig();
        config.freecam.enabled = true;
        config.freecam.speed = 1.25;
        config.freecam.boostMultiplier = 4.5;
        Gson gson = new Gson();
        ModConfig restored = gson.fromJson(gson.toJson(config), ModConfig.class);
        restored.sanitize();
        assertFalse(restored.freecam.enabled);
        assertEquals(1.25, restored.freecam.speed);
        assertEquals(4.5, restored.freecam.boostMultiplier);
        // A handwritten enabled flag must also never auto-activate a camera on startup.
        restored = gson.fromJson("{\"freecam\":{\"enabled\":true}}", ModConfig.class);
        assertFalse(restored.freecam.enabled);
    }

    @Test void speedAndBoostAreBoundedAndFinite() {
        FreecamSettings settings = new FreecamSettings();
        settings.speed = -50;
        settings.boostMultiplier = 100;
        settings.sanitize();
        assertEquals(0.1, settings.speed);
        assertEquals(5.0, settings.boostMultiplier);
        settings.speed = 100;
        settings.boostMultiplier = -50;
        settings.sanitize();
        assertEquals(3.0, settings.speed);
        assertEquals(1.0, settings.boostMultiplier);
        settings.speed = Double.NaN;
        settings.boostMultiplier = Double.POSITIVE_INFINITY;
        settings.sanitize();
        assertEquals(0.5, settings.speed);
        assertEquals(3.0, settings.boostMultiplier);
    }
}
