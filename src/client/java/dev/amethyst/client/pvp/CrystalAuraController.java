package dev.amethyst.client.pvp;

import dev.amethyst.client.config.AutoCrystalSettings;
import dev.amethyst.client.config.CrystalAuraSettings;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Places crystals with vanilla interactions; crystal breaking shares the global action budget. */
public final class CrystalAuraController {
    private final CrystalAuraPlacement placement = new CrystalAuraPlacement();
    private final AutoCrystalController breaker = new AutoCrystalController();
    private final AutoCrystalSettings breakSettings = new AutoCrystalSettings();
    private ClientLevel world;
    private LocalPlayer body;

    public void tick(Minecraft client, CrystalAuraSettings settings) {
        if (client.level != world || client.player != body) {
            clear();
            world = client.level;
            body = client.player;
            placement.reset(settings.placeDelayTicks);
        }
        if (!settings.enabled || !CombatActions.ready(client)
                || (settings.requireUseKey && !client.options.keyUse.isDown())) {
            placement.reset(settings.placeDelayTicks);
            breaker.clear();
            return;
        }
        if (settings.breakCrystals) {
            breakSettings.enabled = true;
            breakSettings.range = settings.range;
            breakSettings.delayTicks = settings.breakDelayTicks;
            breakSettings.crosshairOnly = false;
            breakSettings.requireAttackKey = false;
            breakSettings.lineOfSight = true;
            breaker.tick(client, breakSettings);
        } else {
            breaker.clear();
        }
        if (!placement.advance() || !CombatActions.isAvailable()) return;
        int crystalSlot = crystalSlot();
        if (crystalSlot < 0) return;
        BlockPos base = selectBase(client, settings);
        if (base == null || !CombatActions.isAvailable()) return;
        int previous = body.getInventory().getSelectedSlot();
        body.getInventory().setSelectedSlot(crystalSlot);
        try {
            // EndCrystalItem validates the support, air, occupancy and server permissions again.
            CombatActions.useBlock(client, topHit(base));
            placement.reset(settings.placeDelayTicks);
        } finally {
            if (settings.restoreSlot && body == client.player
                    && body.getInventory().getSelectedSlot() == crystalSlot) {
                body.getInventory().setSelectedSlot(previous);
            }
        }
    }

    private int crystalSlot() {
        int selected = body.getInventory().getSelectedSlot();
        if (body.getInventory().getItem(selected).is(Items.END_CRYSTAL)) return selected;
        for (int slot = 0; slot < 9; slot++) {
            if (body.getInventory().getItem(slot).is(Items.END_CRYSTAL)) return slot;
        }
        return -1;
    }

    private BlockPos selectBase(Minecraft client, CrystalAuraSettings settings) {
        double reach = CrystalAuraPlacement.reach(settings.range, body.blockInteractionRange());
        if (reach <= 0) return null;
        var candidates = new ArrayList<CrystalAuraPlacement.Candidate>();
        if (settings.crosshairOnly) {
            if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
            addCandidate(candidates, hit.getBlockPos());
        } else {
            BlockPos center = BlockPos.containing(body.getEyePosition());
            int radius = (int) Math.ceil(reach) + 1;
            // The radius is capped by reach() and only already loaded chunks are inspected.
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                    center.offset(radius, radius, radius))) addCandidate(candidates, pos);
        }
        var selected = CrystalAuraPlacement.select(candidates, settings.range, body.blockInteractionRange());
        return selected.isPresent() ? BlockPos.of(selected.getAsLong()) : null;
    }

    private void addCandidate(ArrayList<CrystalAuraPlacement.Candidate> candidates, BlockPos position) {
        if (!world.hasChunkAt(position) || !world.getWorldBorder().isWithinBounds(position)) return;
        var state = world.getBlockState(position);
        if ((!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK)) || !world.isEmptyBlock(position.above())) return;
        BlockPos above = position.above();
        AABB crystalSpace = new AABB(above.getX(), above.getY(), above.getZ(),
                above.getX() + 1.0, above.getY() + 2.0, above.getZ() + 1.0);
        if (!world.getEntities((Entity) null, crystalSpace).isEmpty()) return;
        Vec3 hitPoint = topHit(position).getLocation();
        BlockHitResult obstruction = world.clip(new ClipContext(body.getEyePosition(), hitPoint,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, body));
        boolean visible = obstruction.getType() == HitResult.Type.MISS || obstruction.getBlockPos().equals(position);
        candidates.add(new CrystalAuraPlacement.Candidate(position.asLong(),
                body.getEyePosition().distanceToSqr(hitPoint), true, visible));
    }

    private static BlockHitResult topHit(BlockPos base) {
        return new BlockHitResult(new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5),
                Direction.UP, base, false);
    }

    public void clear() {
        world = null;
        body = null;
        placement.reset(2);
        breaker.clear();
    }
}
