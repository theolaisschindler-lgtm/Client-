package dev.amethyst.client.pvp;

import dev.amethyst.client.config.AutoMaceSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.Items;

/** A falling crosshair attack with a hotbar mace, once before landing. */
public final class AutoMaceController {
    private final WeaponSlot slot = new WeaponSlot();
    private final MaceFall fall = new MaceFall();
    private LocalPlayer player;
    private ClientLevel world;

    public void tick(Minecraft client, AutoMaceSettings settings) {
        if (client.player != player || client.level != world) {
            clear();
            player = client.player;
            world = client.level;
        }
        if (player == null) return;
        if (!settings.enabled || !CombatActions.ready(client)) {
            restore(settings.restoreSlot);
            // Disabling and re-enabling in midair must not create a second smash.
            if (player.onGround()) fall.landed();
            return;
        }
        if (player.onGround()) {
            restore(settings.restoreSlot);
            fall.landed();
            return;
        }
        if (slot.active() && !slot.owns(player.getInventory().getSelectedSlot())) slot.clear();
        if (!CombatActions.isAvailable() || player.isFallFlying() || player.isInWater() || player.onClimbable()
                || (settings.requireAttackKey && !client.options.keyAttack.isDown())
                || !fall.eligible(player.fallDistance, settings.minFallDistance, player.getDeltaMovement().y, false)) return;
        var target = WeaponTargeting.crosshair(client, true, settings.range);
        if (target == null) return;
        int mace = findMace(player);
        if (mace < 0) return;
        int previous = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(mace);
        if (CombatActions.attack(client, target, false)) {
            if (previous != mace) slot.acquire(previous, mace);
            fall.used();
        } else {
            player.getInventory().setSelectedSlot(previous);
        }
    }

    private static int findMace(LocalPlayer player) {
        int selected = player.getInventory().getSelectedSlot();
        if (player.getInventory().getItem(selected).is(Items.MACE)) return selected;
        for (int index = 0; index < 9; index++) if (player.getInventory().getItem(index).is(Items.MACE)) return index;
        return -1;
    }

    private void restore(boolean enabled) {
        if (player != null) slot.release(player.getInventory().getSelectedSlot(), enabled)
                .ifPresent(player.getInventory()::setSelectedSlot);
        else slot.clear();
    }

    public boolean isBusy() { return slot.active(); }

    public void clear() {
        Minecraft client = Minecraft.getInstance();
        if (player != null && client.player == player && client.level == world) restore(true);
        player = null;
        world = null;
        slot.clear();
        fall.landed();
    }
}
