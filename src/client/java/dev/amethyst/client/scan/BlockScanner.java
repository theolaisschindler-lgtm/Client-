package dev.amethyst.client.scan;

import dev.amethyst.client.config.ModConfig;
import dev.amethyst.client.config.ModuleSettings;
import dev.amethyst.client.config.TargetType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/** Visits existing chunks only; never asks the world to load or generate chunks. */
public final class BlockScanner {
    private static final int CHUNKS_PER_TICK = 12;
    private final Map<ChunkPos, List<Highlight>> cache = new HashMap<>();
    private final ArrayDeque<ChunkPos> pending = new ArrayDeque<>();
    private ClientLevel lastLevel;
    private ChunkPos lastCenter;
    private int lastRadius;
    private List<Highlight> highlights = List.of();

    public List<Highlight> highlights() { return highlights; }

    public void tick(Minecraft client, ModConfig config) {
        if (client.level == null || client.player == null || (!config.storage.enabled && !config.spawners.enabled)) {
            clear();
            return;
        }
        if (lastLevel != client.level) {
            clear();
            lastLevel = client.level;
        }
        int range = Math.max(config.storage.enabled ? config.storage.range : 0,
                config.spawners.enabled ? config.spawners.range : 0);
        int radius = (range + 15) / 16;
        ChunkPos center = client.player.chunkPosition();
        if (!center.equals(lastCenter) || radius != lastRadius) {
            pending.clear();
            lastCenter = center;
            lastRadius = radius;
        }
        cache.keySet().removeIf(pos -> Math.abs(pos.x - center.x) > radius || Math.abs(pos.z - center.z) > radius
                || client.level.getChunkSource().getChunkNow(pos.x, pos.z) == null);

        if (pending.isEmpty()) {
            List<ChunkPos> positions = new ArrayList<>();
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) positions.add(new ChunkPos(center.x + x, center.z + z));
            }
            positions.sort(Comparator.comparingInt(pos -> Math.abs(pos.x - center.x) + Math.abs(pos.z - center.z)));
            pending.addAll(positions);
        }
        for (int i = 0; i < CHUNKS_PER_TICK && !pending.isEmpty(); i++) {
            ChunkPos pos = pending.removeFirst();
            LevelChunk chunk = client.level.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk == null) {
                cache.remove(pos);
                continue;
            }
            List<Highlight> entries = new ArrayList<>();
            for (BlockEntity entity : chunk.getBlockEntities().values()) {
                TargetType type = classify(entity);
                if (type != null && !entity.isRemoved()) entries.add(new Highlight(entity.getBlockPos().immutable(), type));
            }
            cache.put(pos, List.copyOf(entries));
        }

        List<Highlight> visible = new ArrayList<>();
        for (List<Highlight> entries : cache.values()) {
            for (Highlight highlight : entries) {
                ModuleSettings settings = config.module(highlight.type());
                if (!settings.enabled || !settings.targets.contains(highlight.type())) continue;
                if (highlight.pos().distToCenterSqr(client.player.getX(), client.player.getY(), client.player.getZ())
                        > (double) settings.range * settings.range) continue;
                // A removed container must disappear immediately, even before the next scan of its chunk.
                if (classify(client.level.getBlockEntity(highlight.pos())) != highlight.type()) continue;
                visible.add(highlight);
            }
        }
        highlights = List.copyOf(visible);
    }

    private static TargetType classify(BlockEntity entity) {
        if (entity instanceof ChestBlockEntity || entity instanceof EnderChestBlockEntity) return TargetType.CHEST;
        if (entity instanceof HopperBlockEntity) return TargetType.HOPPER;
        if (entity instanceof ShulkerBoxBlockEntity) return TargetType.SHULKER;
        if (entity instanceof BarrelBlockEntity) return TargetType.BARREL;
        if (entity instanceof SpawnerBlockEntity) return TargetType.SPAWNER;
        return null;
    }

    public void clear() {
        cache.clear();
        pending.clear();
        highlights = List.of();
        lastLevel = null;
        lastCenter = null;
        lastRadius = 0;
    }
}
