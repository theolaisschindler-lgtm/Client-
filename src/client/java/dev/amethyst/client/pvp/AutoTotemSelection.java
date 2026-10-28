package dev.amethyst.client.pvp;

import java.util.function.IntPredicate;

/** Slot IDs refer to the vanilla player InventoryMenu, not underlying Inventory indices. */
public final class AutoTotemSelection {
    private AutoTotemSelection() { }

    public static int findSource(IntPredicate containsTotem) {
        // Main inventory first, preserving the current hotbar when another totem is available.
        for (int slot = 9; slot < 36; slot++) if (containsTotem.test(slot)) return slot;
        for (int slot = 36; slot < 45; slot++) if (containsTotem.test(slot)) return slot;
        return -1;
    }

    public static boolean needsTotem(boolean offhandHasTotem, boolean healthOnly,
                                     double effectiveHealth, double healthThreshold) {
        return !offhandHasTotem && (!healthOnly || (Double.isFinite(effectiveHealth)
                && Double.isFinite(healthThreshold) && effectiveHealth <= healthThreshold));
    }
}
