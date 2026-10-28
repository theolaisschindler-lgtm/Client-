package dev.amethyst.client.config;

import java.util.EnumSet;

public final class CrystalEspSettings extends ModuleSettings {
    public CrystalEspSettings() {
        range = 64;
        targets = EnumSet.of(TargetType.END_CRYSTAL);
    }

    public void sanitize() {
        EnumSet<TargetType> selected = targets == null ? EnumSet.of(TargetType.END_CRYSTAL) : targets.clone();
        super.sanitize(false);
        selected.retainAll(EnumSet.of(TargetType.END_CRYSTAL));
        targets = selected;
    }
}
