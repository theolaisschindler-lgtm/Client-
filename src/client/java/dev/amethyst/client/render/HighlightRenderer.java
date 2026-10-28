package dev.amethyst.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import dev.amethyst.client.AmethystClient;
import dev.amethyst.client.config.ModConfig;
import dev.amethyst.client.config.ChunkFinderSettings;
import dev.amethyst.client.config.ModuleSettings;
import dev.amethyst.client.config.TargetType;
import dev.amethyst.client.scan.Highlight;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

/** Uses 1.21.11 render pipelines rather than changing global OpenGL state. */
public final class HighlightRenderer implements AutoCloseable {
    private static final RenderPipeline VISIBLE = pipeline("outline_visible", DepthTestFunction.LEQUAL_DEPTH_TEST);
    private static final RenderPipeline THROUGH_WALLS = pipeline("outline_through_walls", DepthTestFunction.NO_DEPTH_TEST);
    private static final Vector4f WHITE = new Vector4f(1, 1, 1, 1);
    private static final Vector3f NO_OFFSET = new Vector3f();
    private static final Matrix4f NO_TEXTURE_TRANSFORM = new Matrix4f();

    private final ByteBufferBuilder allocator = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
    private volatile List<Outline> frame = List.of();
    private MappableRingBuffer vertexBuffer;

    private HighlightRenderer() { }

    public static void register() {
        HighlightRenderer renderer = new HighlightRenderer();
        WorldRenderEvents.END_EXTRACTION.register(renderer::extract);
        WorldRenderEvents.END_MAIN.register(renderer::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> renderer.frame = List.of());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderer.close());
    }

    private static RenderPipeline pipeline(String name, DepthTestFunction depthTest) {
        return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath("amethyst", "pipeline/" + name))
                .withDepthTestFunction(depthTest)
                .withDepthWrite(false)
                .withCull(false)
                .build());
    }

    private void extract(WorldExtractionContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || client.options.hideGui) {
            frame = List.of();
            return;
        }
        ModConfig config = AmethystClient.config();
        Vec3 camera = context.worldState().cameraRenderState.pos;
        double pixelScale = Math.tan(Math.toRadians(client.options.fov().get() * 0.5))
                / Math.max(1, client.getWindow().getHeight());
        float pulse = (float) (0.72 + 0.28 * Math.sin(System.nanoTime() / 450_000_000.0));
        List<Outline> outlines = new ArrayList<>();
        for (Highlight highlight : AmethystClient.highlights()) {
            ModuleSettings module = config.module(highlight.type());
            if (!module.enabled || !module.targets.contains(highlight.type())) continue;
            double x = highlight.pos().getX() - camera.x;
            double y = highlight.pos().getY() - camera.y;
            double z = highlight.pos().getZ() - camera.z;
            double cx = x + highlight.width() * 0.5;
            double cy = y + highlight.height() * 0.5;
            double cz = z + highlight.depth() * 0.5;
            double distance = Math.sqrt(cx * cx + cy * cy + cz * cz);
            // Chunk Finder already bounds its loaded-chunk square by simulationDistance.
            if (!(module instanceof ChunkFinderSettings) && distance > module.range) continue;
            // Small quad prisms keep thickness adjustable without depending on driver line-width limits.
            float thickness = (float) Math.clamp(distance * pixelScale * module.lineWidth, 0.002, 0.05);
            float alpha = module.opacity * (module.pulse ? pulse : 1.0f);
            int color = (Math.clamp(Math.round(alpha * 255), 0, 255) << 24) | config.color(highlight.type());
            outlines.add(new Outline((float) x, (float) y, (float) z, highlight.width(), highlight.height(),
                    highlight.depth(), thickness, color, module.throughWalls));
        }
        extractPlayers(context, client, config, camera, pixelScale, pulse, outlines);
        extractCrystals(context, client, config, camera, pixelScale, pulse, outlines);
        // Drawing consumes only immutable frame data, with no retained world or block-entity references.
        frame = List.copyOf(outlines);
    }

    private void extractPlayers(WorldExtractionContext context, Minecraft client, ModConfig config,
                                Vec3 camera, double pixelScale, float pulse, List<Outline> outlines) {
        var settings = config.players;
        if (!settings.enabled) return;
        float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(false);
        for (var player : client.level.players()) {
            if (player.isRemoved()) continue;
            AABB box = PlayerEspGeometry.interpolatedBox(player.getBoundingBox(), player.position(),
                    player.getPosition(partialTick));
            double distanceSquared = box.getCenter().distanceToSqr(camera);
            if (!settings.canHighlight(player == client.player, player.isAlive(), player.isSpectator(),
                    player.isInvisibleTo(client.player), distanceSquared)) continue;
            int rgb = config.color(TargetType.PLAYER);
            if (settings.teamColors && !settings.rainbow && player.getTeam() != null
                    && player.getTeam().getColor().getColor() != null) rgb = player.getTeamColor() & 0xFFFFFF;
            float alpha = settings.opacity * (settings.pulse ? pulse : 1.0f);
            int color = (Math.clamp(Math.round(alpha * 255), 0, 255) << 24) | rgb;
            float thickness = (float) Math.clamp(Math.sqrt(distanceSquared) * pixelScale * settings.lineWidth, 0.002, 0.05);
            outlines.add(new Outline((float) (box.minX - camera.x), (float) (box.minY - camera.y),
                    (float) (box.minZ - camera.z), (float) box.getXsize(), (float) box.getYsize(),
                    (float) box.getZsize(), thickness, color, settings.throughWalls));
        }
    }

    private void extractCrystals(WorldExtractionContext context, Minecraft client, ModConfig config,
                                 Vec3 camera, double pixelScale, float pulse, List<Outline> outlines) {
        var settings = config.crystals;
        if (!settings.enabled || !settings.targets.contains(TargetType.END_CRYSTAL)) return;
        float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(false);
        for (var entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal crystal) || crystal.isRemoved() || !crystal.isAlive()) continue;
            AABB box = PlayerEspGeometry.interpolatedBox(crystal.getBoundingBox(), crystal.position(),
                    crystal.getPosition(partialTick));
            double distanceSquared = box.getCenter().distanceToSqr(camera);
            if (distanceSquared > (double) settings.range * settings.range) continue;
            float alpha = settings.opacity * (settings.pulse ? pulse : 1.0f);
            int color = (Math.clamp(Math.round(alpha * 255), 0, 255) << 24) | config.color(TargetType.END_CRYSTAL);
            float thickness = (float) Math.clamp(Math.sqrt(distanceSquared) * pixelScale * settings.lineWidth, 0.002, 0.05);
            outlines.add(new Outline((float) (box.minX - camera.x), (float) (box.minY - camera.y),
                    (float) (box.minZ - camera.z), (float) box.getXsize(), (float) box.getYsize(),
                    (float) box.getZsize(), thickness, color, settings.throughWalls));
        }
    }

    private void render(WorldRenderContext context) {
        List<Outline> outlines = frame;
        if (outlines.isEmpty()) return;
        drawBatch(context, outlines, false);
        drawBatch(context, outlines, true);
    }

    private void drawBatch(WorldRenderContext context, List<Outline> outlines, boolean throughWalls) {
        RenderPipeline pipeline = throughWalls ? THROUGH_WALLS : VISIBLE;
        BufferBuilder buffer = null;
        Matrix4fc pose = context.matrices().last().pose();
        for (Outline outline : outlines) {
            if (outline.throughWalls != throughWalls) continue;
            if (buffer == null) buffer = new BufferBuilder(allocator, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
            addOutline(buffer, pose, outline);
        }
        if (buffer == null) return;
        try (MeshData mesh = buffer.buildOrThrow()) {
            MeshData.DrawState state = mesh.drawState();
            int byteSize = state.vertexCount() * state.format().getVertexSize();
            if (vertexBuffer == null || vertexBuffer.size() < byteSize) {
                if (vertexBuffer != null) vertexBuffer.close();
                vertexBuffer = new MappableRingBuffer(() -> "Amethyst outlines", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, byteSize);
            }
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            try (GpuBuffer.MappedView mapped = encoder.mapBuffer(vertexBuffer.currentBuffer().slice(0, mesh.vertexBuffer().remaining()), false, true)) {
                MemoryUtil.memCopy(mesh.vertexBuffer(), mapped.data());
            }
            // Sequential quad indices avoid sorting hundreds of tiny edge faces every frame.
            RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
            GpuBuffer indexBuffer = indices.getBuffer(state.indexCount());
            GpuBufferSlice transform = RenderSystem.getDynamicUniforms()
                    .writeTransform(RenderSystem.getModelViewMatrix(), WHITE, NO_OFFSET, NO_TEXTURE_TRANSFORM);
            var target = Minecraft.getInstance().getMainRenderTarget();
            try (RenderPass pass = encoder.createRenderPass(() -> "Amethyst outlines", target.getColorTextureView(), OptionalInt.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
                pass.setPipeline(pipeline);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", transform);
                pass.setVertexBuffer(0, vertexBuffer.currentBuffer());
                pass.setIndexBuffer(indexBuffer, indices.type());
                pass.drawIndexed(0, 0, state.indexCount(), 1);
            }
            vertexBuffer.rotate();
        }
    }

    private static void addOutline(BufferBuilder buffer, Matrix4fc pose, Outline o) {
        float a = -0.003f, bx = o.width + 0.003f, by = o.height + 0.003f,
                bz = o.depth + 0.003f, t = o.thickness;
        for (int yi = 0; yi < 2; yi++) {
            float y = yi == 0 ? a : by;
            for (int zi = 0; zi < 2; zi++) {
                float z = zi == 0 ? a : bz;
                box(buffer, pose, o, a - t, y - t, z - t, bx + t, y + t, z + t);
            }
        }
        for (int xi = 0; xi < 2; xi++) {
            float x = xi == 0 ? a : bx;
            for (int zi = 0; zi < 2; zi++) {
                float z = zi == 0 ? a : bz;
                box(buffer, pose, o, x - t, a - t, z - t, x + t, by + t, z + t);
            }
            for (int yi = 0; yi < 2; yi++) {
                float y = yi == 0 ? a : by;
                box(buffer, pose, o, x - t, y - t, a - t, x + t, y + t, bz + t);
            }
        }
    }

    private static void box(BufferBuilder b, Matrix4fc m, Outline o, float x0, float y0, float z0, float x1, float y1, float z1) {
        x0 += o.x; x1 += o.x; y0 += o.y; y1 += o.y; z0 += o.z; z1 += o.z;
        vertex(b, m, x0, y0, z1, o.color); vertex(b, m, x1, y0, z1, o.color); vertex(b, m, x1, y1, z1, o.color); vertex(b, m, x0, y1, z1, o.color);
        vertex(b, m, x1, y0, z0, o.color); vertex(b, m, x0, y0, z0, o.color); vertex(b, m, x0, y1, z0, o.color); vertex(b, m, x1, y1, z0, o.color);
        vertex(b, m, x0, y0, z0, o.color); vertex(b, m, x0, y0, z1, o.color); vertex(b, m, x0, y1, z1, o.color); vertex(b, m, x0, y1, z0, o.color);
        vertex(b, m, x1, y0, z1, o.color); vertex(b, m, x1, y0, z0, o.color); vertex(b, m, x1, y1, z0, o.color); vertex(b, m, x1, y1, z1, o.color);
        vertex(b, m, x0, y1, z1, o.color); vertex(b, m, x1, y1, z1, o.color); vertex(b, m, x1, y1, z0, o.color); vertex(b, m, x0, y1, z0, o.color);
        vertex(b, m, x0, y0, z0, o.color); vertex(b, m, x1, y0, z0, o.color); vertex(b, m, x1, y0, z1, o.color); vertex(b, m, x0, y0, z1, o.color);
    }

    private static void vertex(BufferBuilder buffer, Matrix4fc pose, float x, float y, float z, int color) {
        buffer.addVertex(pose, x, y, z).setColor(color);
    }

    @Override
    public void close() {
        frame = List.of();
        allocator.close();
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }

    private record Outline(float x, float y, float z, float width, float height, float depth,
                           float thickness, int color, boolean throughWalls) { }
}
