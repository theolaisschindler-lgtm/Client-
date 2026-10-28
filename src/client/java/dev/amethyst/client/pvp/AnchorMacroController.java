package dev.amethyst.client.pvp;

import dev.amethyst.client.config.AnchorMacroSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** One right-click performs place, one charge and detonation using normal vanilla block use. */
public final class AnchorMacroController {
    private final AnchorMacroSequence sequence = new AnchorMacroSequence();
    private ClientLevel world;
    private LocalPlayer body;
    private BlockPos anchorPosition;
    private BlockHitResult placementHit;
    private boolean pressLocked;
    private boolean restoreSlot;
    private int originalSlot = -1;
    private int ownedSlot = -1;

    /** Called before vanilla startUseItem. Only accepted macro presses cancel vanilla handling. */
    public boolean interceptUse(Minecraft client, AnchorMacroSettings settings) {
        if (!settings.enabled || !CombatActions.ready(client)) return false;
        if (pressLocked) return true;
        if (sequence.isBusy()) return true;
        if (!CombatActions.isAvailable() || client.player.isSecondaryUseActive()
                || !(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !withinReach(client, hit) || !client.level.hasChunkAt(hit.getBlockPos())) return false;
        var state = client.level.getBlockState(hit.getBlockPos());
        boolean existing = state.is(Blocks.RESPAWN_ANCHOR);
        int charges = existing ? state.getValue(RespawnAnchorBlock.CHARGE) : 0;
        if (findSlot(client.player, null) < 0 || (charges == 0 && findSlot(client.player, Items.GLOWSTONE) < 0)
                || (charges < 4 && client.player.getOffhandItem().is(Items.GLOWSTONE))) return false;
        BlockPos position;
        if (existing) {
            position = hit.getBlockPos();
        } else {
            int slot = findSlot(client.player, Items.RESPAWN_ANCHOR);
            if (slot < 0) return false;
            var context = new BlockPlaceContext(client.player, InteractionHand.MAIN_HAND,
                    client.player.getInventory().getItem(slot), hit);
            if (!context.canPlace()) return false;
            position = context.getClickedPos();
        }
        if (!client.level.hasChunkAt(position) || !client.level.getWorldBorder().isWithinBounds(position)
                || anchorsWork(client.level, position)) return false;
        world = client.level;
        body = client.player;
        anchorPosition = position.immutable();
        placementHit = hit;
        originalSlot = body.getInventory().getSelectedSlot();
        ownedSlot = originalSlot;
        restoreSlot = settings.restoreSlot;
        pressLocked = true;
        sequence.begin(existing, charges, settings.actionDelayTicks);
        return true;
    }

    public void tick(Minecraft client, AnchorMacroSettings settings) {
        if (!client.options.keyUse.isDown()) pressLocked = false;
        if (!sequence.isBusy()) return;
        if (!settings.enabled || !CombatActions.ready(client) || client.level != world || client.player != body
                || body.getInventory().getSelectedSlot() != ownedSlot || body.isSecondaryUseActive()
                || !world.hasChunkAt(anchorPosition) || anchorsWork(world, anchorPosition)) {
            finish();
            return;
        }
        var state = world.getBlockState(anchorPosition);
        boolean anchorPresent = state.is(Blocks.RESPAWN_ANCHOR);
        int charges = anchorPresent ? state.getValue(RespawnAnchorBlock.CHARGE) : 0;
        sequence.tick(anchorPresent, charges);
        if (!sequence.isBusy()) { finish(); return; }
        AnchorMacroSequence.Step action = sequence.nextAction();
        if (action == AnchorMacroSequence.Step.IDLE || !CombatActions.isAvailable()) return;
        BlockHitResult hit = action == AnchorMacroSequence.Step.PLACE ? placementHit : anchorHit();
        if (hit == null || !withinReach(client, hit)) { finish(); return; }
        Item required = switch (action) {
            case PLACE -> Items.RESPAWN_ANCHOR;
            case CHARGE -> Items.GLOWSTONE;
            default -> null;
        };
        if (action == AnchorMacroSequence.Step.DETONATE && (charges <= 0
                || (charges < 4 && body.getOffhandItem().is(Items.GLOWSTONE)))) { finish(); return; }
        int slot = findSlot(body, required);
        if (slot < 0 || !CombatActions.isAvailable()) { finish(); return; }
        body.getInventory().setSelectedSlot(slot);
        ownedSlot = slot;
        // useBlock goes through Fabric UseBlockCallback. A Safe Anchor FAIL aborts the macro.
        var result = CombatActions.useBlock(client, hit);
        sequence.actionResult(result.consumesAction());
        if (!sequence.isBusy()) finish();
    }

    private BlockHitResult anchorHit() {
        BlockHitResult hit = world.clip(new ClipContext(body.getEyePosition(), anchorPosition.getCenter(),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, body));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(anchorPosition) ? hit : null;
    }

    private static boolean withinReach(Minecraft client, BlockHitResult hit) {
        double reach = client.player.blockInteractionRange();
        return Double.isFinite(reach) && reach > 0
                && client.player.getEyePosition().distanceToSqr(hit.getLocation()) <= reach * reach;
    }

    private static boolean anchorsWork(ClientLevel level, BlockPos position) {
        return level.dimension() == Level.NETHER
                || (level.dimensionType().attributes().contains(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS)
                && level.environmentAttributes().getValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, position));
    }

    /** null requests an empty/non-fuel slot for the deliberate detonation click. */
    private static int findSlot(LocalPlayer player, Item required) {
        int selected = player.getInventory().getSelectedSlot();
        if (matches(player, selected, required)) return selected;
        for (int slot = 0; slot < 9; slot++) if (matches(player, slot, required)) return slot;
        return -1;
    }

    private static boolean matches(LocalPlayer player, int slot, Item required) {
        var stack = player.getInventory().getItem(slot);
        return required != null ? stack.is(required) : !stack.is(Items.GLOWSTONE);
    }

    private void finish() {
        sequence.clear();
        if (restoreSlot && body != null && Minecraft.getInstance().player == body
                && body.getInventory().getSelectedSlot() == ownedSlot && originalSlot >= 0) {
            body.getInventory().setSelectedSlot(originalSlot);
        }
        world = null;
        body = null;
        anchorPosition = null;
        placementHit = null;
        originalSlot = -1;
        ownedSlot = -1;
    }

    public boolean isBusy() { return sequence.isBusy(); }

    public void clear() {
        finish();
        pressLocked = false;
    }
}
