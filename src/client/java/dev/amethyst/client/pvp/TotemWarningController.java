package dev.amethyst.client.pvp;

import dev.amethyst.client.config.TotemWarningSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;

public final class TotemWarningController {
    private final TotemWarningGate gate = new TotemWarningGate();
    private ClientLevel world;
    private LocalPlayer player;

    public void tick(Minecraft client, TotemWarningSettings settings) {
        if (world != client.level || player != client.player) {
            gate.clear(); world = client.level; player = client.player;
        }
        if (!settings.enabled || player == null || world == null || !player.isAlive()
                || player.isSpectator() || client.isPaused()) { gate.clear(); return; }
        boolean hasTotem = player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
                || player.getMainHandItem().is(Items.TOTEM_OF_UNDYING);
        boolean danger = !hasTotem && player.getHealth() + player.getAbsorptionAmount() <= settings.healthThreshold;
        if (!gate.tick(danger, settings.cooldownTicks)) return;
        player.displayClientMessage(Component.literal("Amethyst: Low health — no held totem!"), true);
        if (settings.volume > 0) client.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 0.8f, (float) settings.volume));
    }

    public void clear() { gate.clear(); world = null; player = null; }
}
