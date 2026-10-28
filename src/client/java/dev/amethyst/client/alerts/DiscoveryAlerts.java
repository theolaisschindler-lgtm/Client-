package dev.amethyst.client.alerts;

import dev.amethyst.client.config.ChunkFinderSettings;
import dev.amethyst.client.config.TargetType;
import dev.amethyst.client.scan.Highlight;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** Local-only notification for newly discovered Chunk Finder signals. */
public final class DiscoveryAlerts {
    private final DiscoveryTracker<Signal> tracker = new DiscoveryTracker<>(8192, 60);
    private ClientLevel world;

    private record Signal(int chunkX, int chunkZ, TargetType type) { }

    public void tick(Minecraft client, ChunkFinderSettings settings, List<Highlight> highlights) {
        if (client.level == null || client.player == null || !settings.enabled) {
            clear();
            return;
        }
        if (world != client.level) {
            clear();
            world = client.level;
        }
        List<Signal> discoveries = new ArrayList<>();
        for (Highlight highlight : highlights) {
            TargetType type = highlight.type();
            if ((type == TargetType.GEODE || type == TargetType.ACTIVITY) && settings.targets.contains(type)) {
                discoveries.add(new Signal(highlight.pos().getX() >> 4, highlight.pos().getZ() >> 4, type));
            }
        }
        boolean audible = settings.alertSound && Double.isFinite(settings.alertVolume) && settings.alertVolume > 0;
        if (tracker.update(discoveries, audible)) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME,
                    1.0f, (float) Math.clamp(settings.alertVolume, 0.0, 1.0)));
        }
    }

    public void clear() {
        tracker.clear();
        world = null;
    }
}
