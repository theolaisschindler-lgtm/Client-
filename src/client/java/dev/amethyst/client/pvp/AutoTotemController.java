package dev.amethyst.client.pvp;

import dev.amethyst.client.config.AutoTotemSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

/** Uses a normal offhand SWAP transaction; never drops, copies, or manually edits stacks. */
public final class AutoTotemController {
    private final AutoTotemTiming timing = new AutoTotemTiming();
    private ClientLevel world;
    private LocalPlayer body;
    private String status = "Auto Totem off";

    public void tick(Minecraft client, AutoTotemSettings settings) {
        if (client.level != world || client.player != body) {
            clear();
            world = client.level;
            body = client.player;
        }
        if (!settings.enabled || world == null || body == null || client.gameMode == null
                || client.screen != null || client.getOverlay() != null || client.isPaused()
                || !body.isAlive() || body.isRemoved() || body.isSpectator() || client.gameMode.isSpectator()
                || body.containerMenu != body.inventoryMenu || !body.inventoryMenu.getCarried().isEmpty()) {
            timing.clear();
            status = settings.enabled ? "Waiting for an available inventory" : "Auto Totem off";
            return;
        }

        boolean offhandTotem = body.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
        boolean needed = AutoTotemSelection.needsTotem(offhandTotem, settings.healthOnly,
                body.getHealth() + body.getAbsorptionAmount(), settings.healthThreshold);
        int sourceSlot = AutoTotemSelection.findSource(slot -> body.inventoryMenu.getSlot(slot)
                .getItem().is(Items.TOTEM_OF_UNDYING));
        status = offhandTotem ? "Offhand totem equipped" : !needed ? "Waiting for health threshold"
                : sourceSlot < 0 ? "No totem in your inventory" : "Preparing offhand totem";
        if (!timing.shouldSwap(needed, sourceSlot >= 0, settings.delayTicks)) return;

        // ClickType.SWAP with inventory slot40 is vanilla's offhand-swap action.
        // The previous offhand stack moves back to the selected source slot.
        client.gameMode.handleInventoryMouseClick(body.inventoryMenu.containerId, sourceSlot,
                Inventory.SLOT_OFFHAND, ClickType.SWAP, body);
        CombatActions.markAction();
        status = "Offhand swap requested";
    }

    public String status() { return status; }

    public void clear() {
        timing.clear();
        world = null;
        body = null;
        status = "Auto Totem off";
    }
}
