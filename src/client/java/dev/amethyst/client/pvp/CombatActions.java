package dev.amethyst.client.pvp;

import dev.amethyst.client.freecam.FreecamController;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Normal vanilla actions with common guards and a shared tick budget. */
public final class CombatActions {
    private static final CombatActionBudget BUDGET = new CombatActionBudget();

    private CombatActions() { }

    public static void beginTick() { BUDGET.reset(); }
    public static boolean isAvailable() { return BUDGET.available(); }
    public static boolean claim() { return BUDGET.claim(); }
    public static void markAction() { BUDGET.mark(); }

    public static boolean ready(Minecraft client) {
        var player = client.player;
        return client.level != null && player != null && client.gameMode != null
                && player.level() == client.level && player.isAlive() && !player.isRemoved()
                && !player.isSpectator() && !client.gameMode.isSpectator()
                && client.screen == null && client.getOverlay() == null && !client.isPaused()
                && client.isWindowActive() && !FreecamController.isActive()
                && client.getCameraEntity() == player && !player.isUsingItem()
                && player.containerMenu == player.inventoryMenu && player.inventoryMenu.getCarried().isEmpty();
    }

    public static boolean attack(Minecraft client, Entity target, boolean requireCooldown) {
        if (!ready(client) || !isAvailable() || target == null || target == client.player
                || !target.isAlive() || target.isRemoved() || target.level() != client.level
                || client.level.getEntity(target.getId()) != target) return false;
        double reach = client.player.entityInteractionRange();
        if (!Double.isFinite(reach) || reach <= 0
                || target.getBoundingBox().distanceToSqr(client.player.getEyePosition()) > reach * reach
                || (requireCooldown && client.player.getAttackStrengthScale(0.5f) < 0.9f)
                || !claim()) return false;
        client.gameMode.attack(client.player, target);
        client.player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    public static InteractionResult useBlock(Minecraft client, BlockHitResult hit) {
        if (!ready(client) || !isAvailable() || hit == null || hit.getType() != HitResult.Type.BLOCK
                || !client.level.hasChunkAt(hit.getBlockPos())
                || !client.level.getWorldBorder().isWithinBounds(hit.getBlockPos())) return InteractionResult.PASS;
        double reach = client.player.blockInteractionRange();
        if (!Double.isFinite(reach) || reach <= 0
                || client.player.getEyePosition().distanceToSqr(hit.getLocation()) > reach * reach
                || !claim()) return InteractionResult.PASS;
        var result = client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
        if (result.consumesAction()) client.player.swing(InteractionHand.MAIN_HAND);
        return result;
    }
}
