package dev.amethyst.client.pvp;

import dev.amethyst.client.config.ShieldBreakSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Items;

/** Equips a hotbar axe against an actively blocking player. */
public final class ShieldBreakController {
    private final WeaponSlot slot = new WeaponSlot();
    private LocalPlayer player;
    private ClientLevel world;
    private int nextAttack;
    private boolean attacked;

    public void tick(Minecraft client, ShieldBreakSettings settings) {
        if (client.player != player || client.level != world) {
            clear();
            player = client.player;
            world = client.level;
        }
        if (player == null) return;
        if (!settings.enabled || !CombatActions.ready(client)) {
            restore(settings.restoreSlot);
            nextAttack = player.tickCount + settings.delayTicks;
            return;
        }
        if (slot.active() && !slot.owns(player.getInventory().getSelectedSlot())) {
            slot.clear();
            attacked = false;
            nextAttack = player.tickCount + settings.delayTicks;
            return;
        }
        if (attacked) {
            if (player.tickCount >= nextAttack) restore(settings.restoreSlot);
            return;
        }
        if (!CombatActions.isAvailable() || player.tickCount < nextAttack) return;
        var target = WeaponTargeting.crosshair(client, true, player.entityInteractionRange());
        if (!(target instanceof Player enemy) || !enemy.isBlocking() || !enemy.getUseItem().is(Items.SHIELD)
                || (settings.requireAttackKey && !client.options.keyAttack.isDown())) {
            restore(settings.restoreSlot);
            return;
        }
        if (!slot.active()) {
            int axe = findAxe(player);
            if (axe < 0) return;
            int previous = player.getInventory().getSelectedSlot();
            if (previous != axe) {
                if (!CombatActions.claim()) return;
                slot.acquire(previous, axe);
                player.getInventory().setSelectedSlot(axe);
                nextAttack = player.tickCount + 1;
                return;
            }
        }
        if (player.getAttackStrengthScale(0) < .9f) return;
        if (CombatActions.attack(client, enemy, false)) {
            attacked = true;
            nextAttack = player.tickCount + settings.delayTicks;
        }
    }

    private static int findAxe(LocalPlayer player) {
        int current = player.getInventory().getSelectedSlot();
        if (player.getInventory().getItem(current).getItem() instanceof AxeItem) return current;
        for (int index = 0; index < 9; index++) {
            if (player.getInventory().getItem(index).getItem() instanceof AxeItem) return index;
        }
        return -1;
    }

    private void restore(boolean enabled) {
        if (player != null) slot.release(player.getInventory().getSelectedSlot(), enabled)
                .ifPresent(player.getInventory()::setSelectedSlot);
        else slot.clear();
        attacked = false;
    }

    public boolean isBusy() { return slot.active(); }

    public void clear() {
        Minecraft client = Minecraft.getInstance();
        if (player != null && client.player == player && client.level == world) restore(true);
        player = null;
        world = null;
        slot.clear();
        nextAttack = 0;
        attacked = false;
    }
}
