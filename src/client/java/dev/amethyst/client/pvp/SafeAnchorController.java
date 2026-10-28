package dev.amethyst.client.pvp;

import dev.amethyst.client.AmethystClient;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;

/** Prevents selected client right-clicks; never predicts blast damage or places/charges anchors. */
public final class SafeAnchorController {
    private SafeAnchorController() { }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            Minecraft client = Minecraft.getInstance();
            if (!world.isClientSide() || player != client.player || player.isSpectator()
                    || AmethystClient.config() == null) return InteractionResult.PASS;
            var settings = AmethystClient.config().safeAnchor;
            if (!settings.enabled) return InteractionResult.PASS;
            var state = world.getBlockState(hit.getBlockPos());
            if (!state.is(Blocks.RESPAWN_ANCHOR)) return InteractionResult.PASS;
            var main = player.getMainHandItem();
            var off = player.getOffhandItem();
            boolean bypassesBlock = player.isSecondaryUseActive() && (!main.isEmpty() || !off.isEmpty());
            // RESPAWN_ANCHOR_WORKS is server-side-only in vanilla's network codec.
            // Preserve standard Nether use. Honor an explicit rule if a client/mod supplies
            // one, and conservatively protect unknown custom dimensions otherwise.
            boolean anchorsWork = world.dimensionType().attributes().contains(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS)
                    ? world.environmentAttributes().getValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, hit.getBlockPos())
                    : world.dimension() == Level.NETHER;
            boolean blocked = SafeAnchorRules.shouldBlock(settings.enabled, settings.blockAllDetonation,
                    anchorsWork, state.getValue(RespawnAnchorBlock.CHARGE), hand == InteractionHand.MAIN_HAND,
                    main.is(Items.GLOWSTONE), off.is(Items.GLOWSTONE), bypassesBlock);
            return blocked ? InteractionResult.FAIL : InteractionResult.PASS;
        });
    }
}
