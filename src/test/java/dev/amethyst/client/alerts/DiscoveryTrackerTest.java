package dev.amethyst.client.alerts;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DiscoveryTrackerTest {
    @Test void persistentAndRevisitedSignalsDoNotRepeat() {
        var tracker = new DiscoveryTracker<String>(10, 0);
        assertTrue(tracker.update(List.of("chunk-a/geode", "chunk-a/geode"), true));
        assertFalse(tracker.update(List.of("chunk-a/geode"), true));
        assertFalse(tracker.update(List.of(), true));
        assertFalse(tracker.update(List.of("chunk-a/geode"), true));
        assertTrue(tracker.update(List.of("chunk-a/geode", "chunk-a/activity"), true));
    }

    @Test void cooldownAggregatesNewFindingsIntoOneLaterSound() {
        var tracker = new DiscoveryTracker<String>(10, 3);
        assertTrue(tracker.update(List.of("a"), true));
        assertFalse(tracker.update(List.of("a", "b"), true));
        assertFalse(tracker.update(List.of("a", "b", "c"), true));
        assertTrue(tracker.update(List.of("a", "b", "c"), true));
        for (int i = 0; i < 10; i++) assertFalse(tracker.update(List.of("a", "b", "c"), true));
    }

    @Test void mutedFindingsAndPendingAlertsStaySilentWhenUnmuted() {
        var tracker = new DiscoveryTracker<String>(10, 2);
        assertFalse(tracker.update(List.of("a"), false));
        assertFalse(tracker.update(List.of("a"), true));
        assertTrue(tracker.update(List.of("a", "b"), true));
        assertFalse(tracker.update(List.of("a", "b", "c"), true));
        assertFalse(tracker.update(List.of("a", "b", "c"), false));
        assertFalse(tracker.update(List.of("a", "b", "c"), true));
        assertTrue(tracker.update(List.of("a", "b", "c", "d"), true));
    }

    @Test void removedPendingCandidatesDoNotPlayLater() {
        var tracker = new DiscoveryTracker<String>(10, 3);
        assertTrue(tracker.update(List.of("a"), true));
        assertFalse(tracker.update(List.of("a", "b"), true));
        assertFalse(tracker.update(List.of("a"), true));
        assertFalse(tracker.update(List.of("a"), true));
    }

    @Test void newSessionClearsDeduplicationAndCooldown() {
        var tracker = new DiscoveryTracker<String>(10, 60);
        assertTrue(tracker.update(List.of("a"), true));
        tracker.clear();
        assertTrue(tracker.update(List.of("a"), true));
    }

    @Test void explorationMemoryIsBoundedAndEvictedOldSignalsCanBeDiscoveredAgain() {
        var tracker = new DiscoveryTracker<String>(3, 0);
        assertTrue(tracker.update(List.of("a"), true));
        assertTrue(tracker.update(List.of("b"), true));
        assertTrue(tracker.update(List.of("c"), true));
        assertFalse(tracker.update(List.of("a"), true)); // Refresh a; b becomes oldest.
        assertTrue(tracker.update(List.of("d"), true));
        assertEquals(3, tracker.rememberedCount());
        assertFalse(tracker.update(List.of("a"), true));
        assertTrue(tracker.update(List.of("b"), true));
        assertEquals(3, tracker.rememberedCount());
    }
}
