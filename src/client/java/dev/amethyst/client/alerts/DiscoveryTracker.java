package dev.amethyst.client.alerts;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

/** Deduplicates discoveries independently of the audio device or world implementation. */
final class DiscoveryTracker<K> {
    private final int capacity;
    private final int cooldownTicks;
    private final LinkedHashMap<K, Boolean> seen = new LinkedHashMap<>(16, 0.75f, true);
    private final Set<K> pending = new HashSet<>();
    private long tick;
    private long nextSoundTick;

    DiscoveryTracker(int capacity, int cooldownTicks) {
        if (capacity < 1 || cooldownTicks < 0) throw new IllegalArgumentException("Invalid discovery limits");
        this.capacity = capacity;
        this.cooldownTicks = cooldownTicks;
    }

    boolean update(Collection<K> discoveries, boolean audible) {
        long now = tick++;
        Set<K> current = new HashSet<>(discoveries);
        for (K key : current) {
            if (seen.put(key, Boolean.TRUE) == null && audible) pending.add(key);
            if (seen.size() > capacity) {
                K oldest = seen.keySet().iterator().next();
                seen.remove(oldest);
                pending.remove(oldest);
            }
        }
        if (!audible) {
            pending.clear();
            return false;
        }
        // A pending unloaded/removed discovery must not produce a delayed phantom alert.
        pending.retainAll(current);
        if (pending.isEmpty() || now < nextSoundTick) return false;
        pending.clear();
        nextSoundTick = now + cooldownTicks;
        return true;
    }

    void clear() {
        seen.clear();
        pending.clear();
        tick = 0;
        nextSoundTick = 0;
    }

    int rememberedCount() { return seen.size(); }
}
