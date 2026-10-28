package dev.amethyst.client.pvp;

import dev.amethyst.client.config.AutoCrystalSettings;
import dev.amethyst.client.freecam.FreecamController;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.EntityHitResult;

/** Attacks existing loaded crystals only; never places blocks/items or changes the player's aim. */
public final class AutoCrystalController {
    private final CrystalHitCooldown cooldown = new CrystalHitCooldown();
    private ClientLevel world;
    private LocalPlayer body;

    public void tick(Minecraft client, AutoCrystalSettings settings) {
        if (client.level != world || client.player != body) {
            clear();
            world = client.level;
            body = client.player;
            cooldown.reset(settings.delayTicks);
        }
        if (!settings.enabled || !CombatActions.ready(client) || client.level == null || client.player == null || client.gameMode == null
                || client.screen != null || client.getOverlay() != null || client.isPaused()
                || !client.isWindowActive() || !body.isAlive() || body.isRemoved() || body.isSpectator()
                || client.gameMode.isSpectator() || FreecamController.isActive() || client.getCameraEntity() != body
                || body.isUsingItem() || (settings.requireAttackKey && !client.options.keyAttack.isDown())) {
            cooldown.reset(settings.delayTicks);
            return;
        }
        if (!cooldown.advance()) return;
        double reach = Math.min(settings.range, body.entityInteractionRange());
        if (!Double.isFinite(reach) || reach <= 0) return;
        var crystals = world.getEntitiesOfClass(EndCrystal.class, body.getBoundingBox().inflate(reach));
        var candidates = new ArrayList<CrystalTargeting.Candidate>();
        for (EndCrystal crystal : crystals) {
            boolean crosshair = client.hitResult instanceof EntityHitResult hit && hit.getEntity() == crystal;
            candidates.add(new CrystalTargeting.Candidate(crystal.getId(),
                    crystal.getBoundingBox().distanceToSqr(body.getEyePosition()),
                    crystal.isAlive() && !crystal.isRemoved(),
                    !settings.lineOfSight || body.hasLineOfSight(crystal), crosshair));
        }
        var selected = CrystalTargeting.select(candidates, settings.range, body.entityInteractionRange(),
                settings.crosshairOnly, settings.lineOfSight);
        if (selected.isEmpty()) return;
        for (EndCrystal crystal : crystals) {
            if (crystal.getId() != selected.getAsInt() || crystal.isRemoved() || !crystal.isAlive()) continue;
            if (CombatActions.attack(client, crystal, false)) cooldown.reset(settings.delayTicks);
            break;
        }
    }

    public void clear() {
        world = null;
        body = null;
        cooldown.reset(2);
    }
}
