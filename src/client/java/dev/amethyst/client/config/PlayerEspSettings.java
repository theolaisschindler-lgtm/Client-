package dev.amethyst.client.config;

import java.util.EnumSet;

public final class PlayerEspSettings extends ModuleSettings {
    public boolean showInvisible = true;
    public boolean showSpectators = false;
    public boolean teamColors = false;

    public PlayerEspSettings() {
        range = 128;
        targets = EnumSet.of(TargetType.PLAYER);
    }

    public void sanitize() {
        EnumSet<TargetType> selected = targets == null ? EnumSet.of(TargetType.PLAYER) : targets.clone();
        super.sanitize(false);
        selected.retainAll(EnumSet.of(TargetType.PLAYER));
        targets = selected;
    }

    public boolean canHighlight(boolean self, boolean alive, boolean spectator, boolean invisible,
                                double distanceSquared) {
        return enabled && targets.contains(TargetType.PLAYER) && !self && alive
                && (showSpectators || !spectator) && (showInvisible || !invisible)
                && distanceSquared <= (double) range * range;
    }
}
