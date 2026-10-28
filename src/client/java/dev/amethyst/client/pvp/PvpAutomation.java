package dev.amethyst.client.pvp;

import dev.amethyst.client.AmethystClient;
import net.minecraft.client.Minecraft;

/** Defines interaction priority and keeps staged hotbar operations exclusive. */
public final class PvpAutomation {
    private static final AutoTotemController TOTEM = new AutoTotemController();
    private static final TotemWarningController WARNING = new TotemWarningController();
    private static final AutoCrystalController HIT_CRYSTAL = new AutoCrystalController();
    private static final CrystalAuraController CRYSTAL = new CrystalAuraController();
    private static final AnchorMacroController ANCHOR = new AnchorMacroController();
    private static final TriggerBotController TRIGGER = new TriggerBotController();
    private static final ShieldBreakController SHIELD = new ShieldBreakController();
    private static final AutoMaceController MACE = new AutoMaceController();
    private static final AimAssistController AIM = new AimAssistController();
    private static final SilentAimController SILENT = new SilentAimController();

    private PvpAutomation() { }

    public static void tick(Minecraft client) {
        var config = AmethystClient.config();
        TOTEM.tick(client, config.autoTotem);
        WARNING.tick(client, config.totemWarning);
        WTapController.tick(client, config.wTap);
        ANCHOR.tick(client, config.anchorMacro);
        if (ANCHOR.isBusy()) return;
        // Finish an existing reservation before a higher-priority module can acquire one.
        boolean maceTicked = MACE.isBusy();
        boolean shieldTicked = SHIELD.isBusy();
        if (maceTicked) {
            MACE.tick(client, config.autoMace);
            if (MACE.isBusy()) return;
        }
        if (shieldTicked) {
            SHIELD.tick(client, config.shieldBreak);
            if (SHIELD.isBusy()) return;
        }
        AIM.tick(client, config.aimAssist);
        if (!maceTicked) MACE.tick(client, config.autoMace);
        if (MACE.isBusy()) return;
        if (!shieldTicked) SHIELD.tick(client, config.shieldBreak);
        if (SHIELD.isBusy()) return;
        CRYSTAL.tick(client, config.crystalAura);
        HIT_CRYSTAL.tick(client, config.autoCrystal);
        TRIGGER.tick(client, config.triggerBot);
    }

    public static boolean interceptAttack(Minecraft client) {
        if (ANCHOR.isBusy() || MACE.isBusy() || SHIELD.isBusy()) return true;
        return SILENT.interceptAttack(client, AmethystClient.config().silentAim);
    }

    public static boolean interceptUse(Minecraft client) {
        if (MACE.isBusy() || SHIELD.isBusy()) return false;
        return ANCHOR.interceptUse(client, AmethystClient.config().anchorMacro);
    }

    public static void clear() {
        TOTEM.clear();
        WARNING.clear();
        HIT_CRYSTAL.clear();
        CRYSTAL.clear();
        ANCHOR.clear();
        TRIGGER.clear();
        SHIELD.clear();
        MACE.clear();
        AIM.clear();
        SILENT.clear();
        WTapController.clear();
    }
}
