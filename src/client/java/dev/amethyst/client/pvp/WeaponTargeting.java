package dev.amethyst.client.pvp;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

final class WeaponTargeting {
    private WeaponTargeting() { }

    static LivingEntity crosshair(Minecraft client, boolean playersOnly, double configuredRange) {
        if (client.player == null || !(client.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity target) || target == client.player
                || !target.isAlive() || target.isRemoved() || !target.attackable()
                || (playersOnly && !(target instanceof Player))
                || (target instanceof Player player && player.isSpectator())
                || !client.player.hasLineOfSight(target)) return null;
        double reach = Math.min(configuredRange, client.player.entityInteractionRange());
        return inReach(target.getBoundingBox().distanceToSqr(client.player.getEyePosition()), reach) ? target : null;
    }

    static boolean inReach(double distanceSquared, double range) {
        return Double.isFinite(distanceSquared) && Double.isFinite(range)
                && distanceSquared >= 0 && range > 0 && distanceSquared <= range * range;
    }
}
