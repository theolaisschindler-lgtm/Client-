package dev.amethyst.client.scan;

import dev.amethyst.client.config.OreEspSettings;
import dev.amethyst.client.config.TargetType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;

/** Incremental read-only survey of already loaded chunks. Never loads terrain or sends packets. */
public final class OreScanner {
    private static final int BLOCK_BUDGET = 8192;
    private static final long TIME_BUDGET_NS = 2_000_000L;
    private static final int MAX_ORES_PER_CHUNK = 512;
    private final Map<ChunkPos, Survey> cache = new HashMap<>();
    private final ArrayDeque<ChunkPos> queue = new ArrayDeque<>();
    private final Set<ChunkPos> queued = new HashSet<>();
    private ClientLevel world;
    private ChunkPos center;
    private int centerSectionY;
    private int range;
    private int radius;
    private int refreshTicks;
    private int cachedOres;
    private Set<TargetType> targets = Set.of();
    private Survey work;
    private ChunkPos workPos;
    private int cursor;
    private int stop;
    private List<Highlight> highlights = List.of();

    private static final class Survey {
        final LevelChunk chunk;
        final List<Highlight> ores = new ArrayList<>();
        Survey(LevelChunk chunk) { this.chunk = chunk; }
    }

    public List<Highlight> highlights() { return highlights; }
    public String status() { return cache.size() + " chunks surveyed / " + highlights.size() + " ores shown"; }

    public void tick(Minecraft client, OreEspSettings settings) {
        if (client.level == null || client.player == null || !settings.enabled || settings.targets.isEmpty()) {
            clear();
            return;
        }
        ChunkPos current = client.player.chunkPosition();
        int sectionY = Math.floorDiv(client.player.blockPosition().getY(), 16);
        boolean changedWorld = world != client.level;
        boolean changedTargets = !targets.equals(settings.targets);
        if (changedWorld || changedTargets || !current.equals(center) || range != settings.range || centerSectionY != sectionY) {
            queue.clear(); queued.clear(); work = null;
            if (changedWorld || changedTargets) { cache.clear(); cachedOres = 0; }
            world = client.level; center = current; centerSectionY = sectionY;
            range = settings.range; radius = (range + 15) / 16;
            targets = Set.copyOf(settings.targets);
            refreshTicks = 0;
        }
        for (Iterator<Map.Entry<ChunkPos, Survey>> iterator = cache.entrySet().iterator(); iterator.hasNext();) {
            var entry = iterator.next();
            if (!inside(entry.getKey()) || loaded(entry.getKey()) != entry.getValue().chunk) {
                cachedOres -= entry.getValue().ores.size();
                iterator.remove();
            }
        }
        if (refreshTicks-- <= 0) { refreshQueue(); refreshTicks = 19; }

        int limit = Math.clamp(settings.maxHighlights, 128, 4096);
        int cacheLimit = limit * 2;
        long deadline = System.nanoTime() + TIME_BUDGET_NS;
        int remaining = BLOCK_BUDGET;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        while (remaining > 0 && System.nanoTime() < deadline) {
            if (work == null) {
                if (queue.isEmpty()) break;
                workPos = queue.removeFirst(); queued.remove(workPos);
                LevelChunk chunk = loaded(workPos);
                if (chunk == null || !inside(workPos)) continue;
                work = new Survey(chunk);
                int minY = world.getMinY();
                int startY = Math.max(minY, Math.floorDiv(client.player.blockPosition().getY() - range, 16) * 16);
                int endY = Math.min(minY + world.getHeight(), (Math.floorDiv(client.player.blockPosition().getY() + range, 16) + 1) * 16);
                cursor = (startY - minY) * 256;
                stop = (endY - minY) * 256;
            }
            LevelChunk chunk = loaded(workPos);
            if (chunk != work.chunk) { work = null; refreshTicks = 0; continue; }
            while (remaining > 0 && cursor < stop && System.nanoTime() < deadline) {
                int sectionIndex = cursor >>> 12;
                if ((cursor & 4095) == 0 && !chunk.getSections()[sectionIndex].maybeHas(state -> {
                    TargetType type = classify(state.getBlock());
                    return type != null && targets.contains(type);
                })) {
                    cursor = Math.min(stop, (sectionIndex + 1) << 12);
                    remaining--;
                    continue;
                }
                remaining--;
                int index = cursor++;
                pos.set(workPos.getMinBlockX() + (index & 15), world.getMinY() + (index >>> 8),
                        workPos.getMinBlockZ() + ((index >>> 4) & 15));
                if (!withinRange(pos, client.player.getX(), client.player.getY(), client.player.getZ(), range)) continue;
                TargetType type = classify(chunk.getBlockState(pos).getBlock());
                if (type != null && targets.contains(type) && work.ores.size() < Math.min(MAX_ORES_PER_CHUNK, limit))
                    work.ores.add(new Highlight(pos.immutable(), type));
            }
            if (cursor >= stop) {
                Survey previous = cache.put(workPos, work);
                if (previous != null) cachedOres -= previous.ores.size();
                cachedOres += work.ores.size();
                work = null;
                trimCache(cacheLimit, client.player.getX(), client.player.getZ());
            }
        }
        trimCache(cacheLimit, client.player.getX(), client.player.getZ());

        // Keep only the nearest visible results, independently of chunk iteration order.
        double px = client.player.getX(), py = client.player.getY(), pz = client.player.getZ();
        Comparator<Highlight> distance = Comparator.comparingDouble(h -> h.pos().distToCenterSqr(px, py, pz));
        PriorityQueue<Highlight> nearest = new PriorityQueue<>(limit, distance.reversed());
        for (Survey survey : cache.values()) {
            for (Iterator<Highlight> iterator = survey.ores.iterator(); iterator.hasNext();) {
                Highlight ore = iterator.next();
                if (classify(survey.chunk.getBlockState(ore.pos()).getBlock()) != ore.type()) {
                    iterator.remove(); cachedOres--; // Broken ores disappear on the next client tick.
                    continue;
                }
                if (!targets.contains(ore.type()) || !withinRange(ore.pos(), px, py, pz, range)) continue;
                if (nearest.size() < limit) nearest.add(ore);
                else if (distance.compare(ore, nearest.peek()) < 0) { nearest.remove(); nearest.add(ore); }
            }
        }
        List<Highlight> visible = new ArrayList<>(nearest);
        visible.sort(distance);
        highlights = List.copyOf(visible);
    }

    private void trimCache(int limit, double x, double z) {
        while (cachedOres > limit) {
            Survey farthest = null;
            double farthestDistance = -1;
            for (var entry : cache.entrySet()) {
                if (entry.getValue().ores.isEmpty()) continue;
                double dx = entry.getKey().getMinBlockX() + 8 - x;
                double dz = entry.getKey().getMinBlockZ() + 8 - z;
                double distance = dx * dx + dz * dz;
                if (distance > farthestDistance) { farthestDistance = distance; farthest = entry.getValue(); }
            }
            if (farthest == null) break;
            int remove = Math.min(cachedOres - limit, farthest.ores.size());
            farthest.ores.subList(farthest.ores.size() - remove, farthest.ores.size()).clear();
            cachedOres -= remove;
        }
    }

    private void refreshQueue() {
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            ChunkPos pos = new ChunkPos(center.x + x, center.z + z);
            if (loaded(pos) != null && !(work != null && pos.equals(workPos)) && !queued.contains(pos)) chunks.add(pos);
        }
        chunks.sort(Comparator.comparingInt(p -> Math.abs(p.x - center.x) + Math.abs(p.z - center.z)));
        for (ChunkPos pos : chunks) if (cache.containsKey(pos)) { queue.addLast(pos); queued.add(pos); }
        for (int i = chunks.size() - 1; i >= 0; i--) {
            ChunkPos pos = chunks.get(i);
            if (!cache.containsKey(pos)) { queue.addFirst(pos); queued.add(pos); }
        }
    }

    private LevelChunk loaded(ChunkPos pos) { return world.getChunkSource().getChunkNow(pos.x, pos.z); }
    private boolean inside(ChunkPos pos) { return Math.abs(pos.x - center.x) <= radius && Math.abs(pos.z - center.z) <= radius; }
    static boolean withinRange(BlockPos pos, double x, double y, double z, int range) {
        return pos.distToCenterSqr(x, y, z) <= (double) range * range;
    }

    static TargetType classify(Block block) {
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) return TargetType.ORE_COAL;
        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) return TargetType.ORE_COPPER;
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) return TargetType.ORE_IRON;
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE) return TargetType.ORE_GOLD;
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) return TargetType.ORE_REDSTONE;
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) return TargetType.ORE_LAPIS;
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) return TargetType.ORE_DIAMOND;
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) return TargetType.ORE_EMERALD;
        if (block == Blocks.NETHER_QUARTZ_ORE) return TargetType.ORE_QUARTZ;
        if (block == Blocks.ANCIENT_DEBRIS) return TargetType.ORE_ANCIENT_DEBRIS;
        return null;
    }

    public void clear() {
        cache.clear(); queue.clear(); queued.clear();
        world = null; center = null; work = null; workPos = null;
        cachedOres = 0; refreshTicks = 0; targets = Set.of(); highlights = List.of();
    }
}
