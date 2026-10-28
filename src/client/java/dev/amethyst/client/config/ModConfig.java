package dev.amethyst.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("Amethyst/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public ModuleSettings storage = ModuleSettings.storageDefaults();
    public ModuleSettings spawners = ModuleSettings.spawnerDefaults();
    public ChunkFinderSettings chunks = new ChunkFinderSettings();
    public int accent = 0x9B7CFF;
    private transient Path path;

    public static ModConfig load(Path path) {
        path = path.toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) return defaultsAt(path);
        try (var reader = Files.newBufferedReader(path)) {
            ModConfig config = GSON.fromJson(reader, ModConfig.class);
            if (config == null) throw new IOException("Empty configuration");
            config.sanitize();
            config.path = path;
            return config;
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not read {}. Using defaults; original file left in place.", path, exception);
            return defaultsAt(path);
        }
    }

    public void sanitize() {
        if (storage == null) storage = ModuleSettings.storageDefaults();
        if (spawners == null) spawners = ModuleSettings.spawnerDefaults();
        storage.sanitize(false);
        spawners.sanitize(true);
        if (chunks == null) chunks = new ChunkFinderSettings();
        chunks.sanitize();
        accent &= 0xFFFFFF;
    }

    public ModuleSettings module(TargetType type) {
        return switch (type) {
            case GEODE, ACTIVITY -> chunks;
            case SPAWNER -> spawners;
            default -> storage;
        };
    }

    public int color(TargetType type) {
        ModuleSettings module = module(type);
        if (module.rainbow) {
            float hue = (System.currentTimeMillis() % 8000L) / 8000f + type.ordinal() * 0.1f;
            return Color.HSBtoRGB(hue % 1f, 0.65f, 1f) & 0xFFFFFF;
        }
        return module.colors.getOrDefault(type, type.defaultColor());
    }

    public void save() {
        sanitize();
        if (path == null) {
            LOGGER.warn("Configuration has no save location; load it from a path before saving.");
            return;
        }
        Path temporary = null;
        try {
            Files.createDirectories(path.getParent());
            temporary = Files.createTempFile(path.getParent(), "amethyst-", ".tmp");
            Files.writeString(temporary, GSON.toJson(this) + System.lineSeparator());
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.error("Could not save Amethyst configuration", exception);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private static ModConfig defaultsAt(Path path) {
        ModConfig config = new ModConfig();
        config.path = path.toAbsolutePath();
        return config;
    }
}
