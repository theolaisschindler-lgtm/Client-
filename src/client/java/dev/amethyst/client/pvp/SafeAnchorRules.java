package dev.amethyst.client.pvp;

/** Mirrors the block/hand decisions before an anchor would reach its empty-hand use. */
public final class SafeAnchorRules {
    private SafeAnchorRules() { }

    public static boolean shouldBlock(boolean enabled, boolean blockAllDetonation, boolean anchorsWork,
                                      int charges, boolean mainHand, boolean mainGlowstone,
                                      boolean offhandGlowstone, boolean secondaryUseBypassesBlock) {
        if (!enabled || anchorsWork || charges <= 0 || !mainHand || secondaryUseBypassesBlock) return false;
        // Glowstone charges an unfilled anchor. Main-hand use also defers to offhand fuel
        // while the anchor can still be charged. Neither path detonates the block.
        if (charges < 4 && (mainGlowstone || offhandGlowstone)) return false;
        return blockAllDetonation || charges >= 4 && (mainGlowstone || offhandGlowstone);
    }
}
