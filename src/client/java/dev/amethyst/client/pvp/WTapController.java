package dev.amethyst.client.pvp;

import dev.amethyst.client.AmethystClient;
import dev.amethyst.client.config.WTapSettings;
import dev.amethyst.client.freecam.FreecamController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Releases forward input after the hit, preserving sprint knockback on that hit. */
public final class WTapController {
    private static final WTapTiming TIMING = new WTapTiming();
    private static LocalPlayer player;

    private WTapController() { }

    public static void onAttack(Player attacker, Entity target, boolean wasSprinting) {
        Minecraft client = Minecraft.getInstance();
        var config = AmethystClient.config();
        if (config == null || !config.wTap.enabled || attacker != client.player || !(target instanceof LivingEntity)
                || !wasSprinting || !CombatActions.ready(client) || !client.options.keyUp.isDown()) return;
        player = client.player;
        TIMING.arm(player.tickCount, config.wTap.resetTicks);
        player.setSprinting(false);
        player.connection.send(new ServerboundPlayerCommandPacket(player,
                ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
    }

    public static boolean releaseForward() {
        Minecraft client = Minecraft.getInstance();
        var config = AmethystClient.config();
        return config != null && config.wTap.enabled && player == client.player && TIMING.active()
                && !FreecamController.isActive() && client.screen == null && client.getCameraEntity() == player;
    }

    public static void tick(Minecraft client, WTapSettings settings) {
        if (player == null) return;
        if (!settings.enabled || client.player != player || !CombatActions.ready(client)) {
            clear();
            return;
        }
        if (!TIMING.advance(player.tickCount)) return;
        if (client.options.keyUp.isDown() && !client.options.keyShift.isDown() && !player.horizontalCollision
                && !player.isUsingItem() && (player.getFoodData().getFoodLevel() > 6 || player.getAbilities().mayfly)) {
            player.setSprinting(true);
            player.connection.send(new ServerboundPlayerCommandPacket(player,
                    ServerboundPlayerCommandPacket.Action.START_SPRINTING));
        }
        player = null;
    }

    public static void clear() { TIMING.clear(); player = null; }
}
