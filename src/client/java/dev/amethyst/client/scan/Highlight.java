package dev.amethyst.client.scan;

import dev.amethyst.client.config.TargetType;
import net.minecraft.core.BlockPos;

/** Immutable data passed from client ticking to render extraction. */
public record Highlight(BlockPos pos, TargetType type) { }
