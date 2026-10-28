package dev.amethyst.client.scan;

import dev.amethyst.client.config.ChunkFinderSettings;
import dev.amethyst.client.config.TargetType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;

/** Incremental loaded-chunk survey. Scan work is capped at 8192 blocks and roughly 2ms per tick. */
public final class ChunkScanner {
    private static final int BLOCK_BUDGET = 8192;
    private static final long TIME_BUDGET_NS = 2_000_000L;
    private final Map<ChunkPos, Survey> surveys = new HashMap<>();
    private final ArrayDeque<ChunkPos> queue = new ArrayDeque<>();
    private final Set<ChunkPos> queued = new HashSet<>();
    private ClientLevel world;
    private ChunkPos center;
    private int radius;
    private int refreshTicks;
    private Survey work;
    private ChunkPos workPos;
    private int cursor;
    private List<Highlight> highlights = List.of();

    private static final class Survey {
        final LevelChunk chunk;
        int amethyst, calcite, torches, crafting, beds, redstone;
        BlockPos geode, activity;

        Survey(LevelChunk chunk) { this.chunk = chunk; }
    }

    public List<Highlight> highlights() { return highlights; }
    public String status() { return surveys.size() + " chunks surveyed / " + highlights.size() + " signals"; }

    public void tick(Minecraft client, ChunkFinderSettings settings) {
        if (client.level == null || client.player == null || !settings.enabled) { clear(); return; }
        ChunkPos current = client.player.chunkPosition();
        if (world != client.level || !current.equals(center) || radius != settings.simulationDistance) {
            boolean changedWorld = world != client.level;
            queue.clear(); queued.clear(); work = null;
            if (changedWorld) surveys.clear();
            world = client.level; center = current; radius = settings.simulationDistance;
            refreshTicks = 0;
        }
        // A reload at the same coordinates is a different snapshot: do not retain its old evidence.
        surveys.entrySet().removeIf(entry -> !inside(entry.getKey())
                || loaded(entry.getKey()) != entry.getValue().chunk);
        if (refreshTicks-- <= 0) {
            refreshQueue();
            refreshTicks = 19;
        }

        long deadline = System.nanoTime() + TIME_BUDGET_NS;
        int remaining = BLOCK_BUDGET;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        while (remaining > 0 && System.nanoTime() < deadline) {
            if (work == null) {
                if (queue.isEmpty()) break;
                workPos = queue.removeFirst(); queued.remove(workPos);
                LevelChunk chunk = loaded(workPos);
                if (chunk == null || !inside(workPos)) { surveys.remove(workPos); continue; }
                work = new Survey(chunk); cursor = 0;
            }
            LevelChunk chunk = loaded(workPos);
            if (chunk != work.chunk) { surveys.remove(workPos); work = null; refreshTicks = 0; continue; }
            int minY = world.getMinY();
            int total = world.getHeight() * 256;
            // Keep a chunk reference only for this survey, and verify its identity again next tick.
            while (remaining > 0 && cursor < total && System.nanoTime() < deadline) {
                int sectionIndex = cursor >>> 12;
                // Palette checks skip ordinary terrain in constant-sized sections, rather than
                // reading all 98,304 positions in every full-height overworld chunk.
                if ((cursor & 4095) == 0 && !chunk.getSections()[sectionIndex]
                        .maybeHas(state -> relevantBlock(state.getBlock()))) {
                    cursor = (sectionIndex + 1) << 12;
                    remaining--; // Empty/irrelevant sections also consume finite work budget.
                    continue;
                }
                remaining--;
                int index = cursor++;
                pos.set(workPos.getMinBlockX() + (index & 15), minY + (index >>> 8),
                        workPos.getMinBlockZ() + ((index >>> 4) & 15));
                Block block = chunk.getBlockState(pos).getBlock();
                if (isAmethyst(block)) {
                    work.amethyst++; if (work.geode == null) work.geode = pos.immutable();
                } else if (block == Blocks.CALCITE) work.calcite++;
                else if (isTorch(block)) {
                    work.torches++; if (work.activity == null) work.activity = pos.immutable();
                } else if (block == Blocks.CRAFTING_TABLE) {
                    work.crafting++; work.activity = pos.immutable();
                } else if (block instanceof BedBlock) {
                    work.beds++; if (work.activity == null) work.activity = pos.immutable();
                } else if (isRedstone(block)) {
                    work.redstone++; if (work.activity == null) work.activity = pos.immutable();
                }
            }
            if (cursor >= total) { surveys.put(workPos, work); work = null; }
        }
        List<Highlight> result = new ArrayList<>();
        for (Map.Entry<ChunkPos, Survey> entry : surveys.entrySet()) {
            Survey survey = entry.getValue();
            // Removed evidence markers disappear immediately, without waiting for an area sweep.
            // This is deliberately conservative: remaining evidence reappears after its next survey.
            if (settings.targets.contains(TargetType.GEODE) && survey.geode != null
                    && isAmethyst(survey.chunk.getBlockState(survey.geode).getBlock())
                    && survey.calcite >= 2 && survey.amethyst >= settings.geodeThreshold())
                result.add(sectionHighlight(entry.getKey(), survey.geode, TargetType.GEODE));
            if (settings.targets.contains(TargetType.ACTIVITY) && survey.activity != null
                    && isActivity(survey.chunk.getBlockState(survey.activity).getBlock())
                    && ChunkFinderSettings.activityScore(survey.torches, survey.crafting, survey.beds, survey.redstone)
                    >= settings.activityThreshold())
                result.add(sectionHighlight(entry.getKey(), survey.activity, TargetType.ACTIVITY));
        }
        highlights = List.copyOf(result);
    }

    private void refreshQueue() {
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            ChunkPos p = new ChunkPos(center.x + x, center.z + z);
            if (loaded(p) != null && !p.equals(workPosOfActiveSurvey()) && !queued.contains(p)) chunks.add(p);
        }
        chunks.sort(Comparator.comparingInt(p -> Math.abs(p.x - center.x) + Math.abs(p.z - center.z)));
        // New arrivals should not wait for the entire loaded-area rescan. Completed surveys go
        // to the back; queued older work keeps its place, so near chunks cannot starve far ones.
        for (ChunkPos p : chunks) {
            if (surveys.containsKey(p)) { queue.addLast(p); queued.add(p); }
        }
        for (int i = chunks.size() - 1; i >= 0; i--) {
            ChunkPos p = chunks.get(i);
            if (!surveys.containsKey(p)) { queue.addFirst(p); queued.add(p); }
        }
    }

    private ChunkPos workPosOfActiveSurvey() { return work == null ? null : workPos; }
    private LevelChunk loaded(ChunkPos p) { return world.getChunkSource().getChunkNow(p.x, p.z); }
    private boolean inside(ChunkPos p) { return Math.abs(p.x - center.x) <= radius && Math.abs(p.z - center.z) <= radius; }

    static Highlight sectionHighlight(ChunkPos chunk, BlockPos evidence, TargetType type) {
        return new Highlight(new BlockPos(chunk.getMinBlockX(), Math.floorDiv(evidence.getY(), 16) * 16,
                chunk.getMinBlockZ()), type, 16, 16, 16);
    }
    static boolean relevantBlock(Block block) { return isAmethyst(block) || block == Blocks.CALCITE || isActivity(block); }
    private static boolean isAmethyst(Block block) { return block == Blocks.AMETHYST_BLOCK || block == Blocks.BUDDING_AMETHYST; }
    private static boolean isTorch(Block block) {
        return block == Blocks.TORCH || block == Blocks.WALL_TORCH || block == Blocks.SOUL_TORCH || block == Blocks.SOUL_WALL_TORCH;
    }
    private static boolean isRedstone(Block block) { return block == Blocks.REDSTONE_WIRE || block == Blocks.REPEATER || block == Blocks.COMPARATOR; }
    private static boolean isActivity(Block block) { return isTorch(block) || block == Blocks.CRAFTING_TABLE || block instanceof BedBlock || isRedstone(block); }

    private void clear() {
        surveys.clear(); queue.clear(); queued.clear(); work = null; world = null; center = null; highlights = List.of();
    }
}
